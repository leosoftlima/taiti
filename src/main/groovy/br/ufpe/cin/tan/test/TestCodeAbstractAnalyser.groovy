package br.ufpe.cin.tan.test

import br.ufpe.cin.tan.analysis.taskInterface.TestI
import br.ufpe.cin.tan.commit.change.gherkin.ChangedGherkinFile
import br.ufpe.cin.tan.commit.change.gherkin.GherkinManager
import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.commit.change.stepdef.ChangedStepdefFile
import br.ufpe.cin.tan.commit.change.unit.ChangedUnitTestFile
import br.ufpe.cin.tan.test.error.ParseErrorList
import br.ufpe.cin.tan.test.error.StepError
import br.ufpe.cin.tan.test.error.StepErrorList
import br.ufpe.cin.tan.test.ruby.routes.RoutesManager
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util
import gherkin.ast.Background
import gherkin.ast.Scenario
import gherkin.ast.ScenarioOutline
import gherkin.ast.Step
import groovy.util.logging.Slf4j

/***
 * Provê a lógica base para utilização de parsers para código de teste a fim de ser possível computar TestI.
 * Utiliza o padrão de projeto Template Method.
 */
@Slf4j
abstract class TestCodeAbstractAnalyser {

    RoutesManager routesManager
    String repositoryPath
    String stepsFilePath

    List<StepRegex> regexList
    Set methods //keys: name, args, optionalArgs, path
    static Set apiMethods //keys: name, args, optionalArgs, path
    List<String> projectFiles
    List<String> viewFiles

    AnalysisData analysisData
    protected Set notFoundViews
    GherkinManager gherkinManager
    Set codeFromViewAnalysis

    /***
     * Inicializa os campos usados para fazer a ligação entre a declaração de um step em Gherkin e o código que o
     * automatiza, os step definitions.
     *
     * @param repositoryPath O caminho de acesso ao repositório do projeto, que pode ser uma URL ou caminho local.
     * @param gherkinManager Parser de arquivos Gherkin, que contém declarações de step.
     */
    TestCodeAbstractAnalyser(String repositoryPath, GherkinManager gherkinManager) {
        this.repositoryPath = repositoryPath
        this.gherkinManager = gherkinManager
        configureStepsFilePath()
        regexList = []
        methods = [] as Set
        projectFiles = []
        viewFiles = []
        notFoundViews = [] as Set
        codeFromViewAnalysis = [] as Set
        analysisData = new AnalysisData()
        configureApiMethodsList()
    }

    /***
     * O método deve encontrar todas as páginas web acessadas via testes.
     * O resultado deve ficar armazenado no próprio visitor passado como parâmetro.
     *
     * @param visitor Visitor usado para analisar o código, específico de LP
     */
    abstract void findAllPages(TestCodeVisitorInterface visitor)

    /***
     * O método deve encontrar todas as expressões regulares encontradas no arquivo de código-fonte
     * passado como parâmetro. Supostamente, tais expressões servem para identificar step definitions.
     *
     * @param path nome completo do arquivo a ser analisado
     * @return lista expressões regulares que foram encontradas. Elas são armazenadas em objetos StepRegex.
     * */
    abstract List<StepRegex> doExtractStepsRegex(String path)

    /***
     * O método deve encontrar todos os step definitions em um arquivo de código-fonte passado como parâmetro.
     * Um step definition possui uma expressão regular em sua declaração.
     *
     * @param path nome completo do arquivo a ser analisado
     * @param content conteúdo do arquivo
     * @return list de step definitions encontrados. Eles são armazenados em objetos StepDefinition.
     */
    abstract List<StepDefinition> doExtractStepDefinitions(String path, String content)

    /***
     * O método deve encontrar todas as declarações de método em um arquivo de código-fonte.
     *
     * @param path nome completo do arquivo a ser analisado.
     * @return conjunto de declarações de métodos encontrado. Ele consiste em um Set cujos elementos possuem as seguintes
     * chaves: name (nome do método), args (quantidade total de argumentos; se a linguagem define argumentos opcionais,
     * então essa é a quantidade máxima de argumentos aceita pelo método), optionalArgs (quantidade de argumentos opcionais),
     * path (arquivo em que ele foi declarado).
     */
    abstract Set doExtractMethodDefinitions(String path)

    /***
     * O método deve visitar o corpo de todos os step definitions e as chamadas de método internas à eles.
     * O resultado é armazenado no visitor usado, que é retornado pelo método.
     *
     * @param file Arquivo a ser analisado e seus respectivos métodos a serem analisados, encapsulados em um objeto
     * do tipo FileToAnalyse.
     * @return visitor Visitor usado para analisar o código, específico de LP
     */
    abstract TestCodeVisitorInterface parseStepBody(FileToAnalyse file)

    /***
     *
     * O método deve visitar o corpo de métodos selecionados de um arquivo de código-fonte procurando por outras chamadas
     * de método. O resultado é armazenado com um campo do visitor de entrada.
     *
     * @param file um map que identifica o arquivo e seus respectivos métodos a serem analisados. As palavras-chave são
     * 'path' para identificar o arquivo e 'methods' para os métodos que, por usa vez, é descrito pelas chave 'name' e
     * 'step'.
     * @param visitor Visitor usado para analisar o código, específico de LP
     */
    abstract visitFile(file, TestCodeVisitorInterface visitor)

    /***
     * O método deve visitar o corpo de um teste unitário procurando por outras chamadas de método.
     * O resultado é armazenado no visitor usado.
     * Na versão atual da ferramenta, testes unitários não são usados para calcular TestI.
     * 
     * @param file o arquivo de teste unitário a ser analisado
     * @return Visitor usado para analisar o código, específico de LP
     */
    abstract TestCodeVisitorInterface parseUnitBody(ChangedUnitTestFile file)

    /***
     * O método deve visitar métodos de interesse de um arquivo de testes unitários procurando por outras chamadas de método.
     * O resultado é armazenado no visitor usado.
     * Na versão atual da ferramenta, testes unitários não são usados para calcular TestI.
     *
     * @param path o arquivo de teste unitário a ser analisado
     * @param content o conteúdo do arquivo a ser visitado
     * @param changedLines a linha em que os métodos de interesse estão declarados
     * @return
     */
    abstract ChangedUnitTestFile doExtractUnitTest(String path, String content, List<Integer> changedLines)

    /***
     * O método deve devolver o nome da classe declarada no arquivo de código-fonte passada como parâmetro.
     * Por exemplo, em um projeto Rails, dado o arquivo contact.rb, o método devolve o nome Contact
     *
     * @param path nome do arquivo de código-fonte
     * @return nome da classe
     */
    abstract String getClassForFile(String path)

    /***
     * O método deve informar se um arquivo possui erro de compilação/parse. Quando há esse tipo de erro, não é possível
     * gerar a AST para o arquivo.
     *
     * @param path arquivo a verificar se possui erro de compilação/parse
     * @return flag sinalizando se há erro (true) ou não (false)
     */
    abstract boolean hasCompilationError(String path)

    /***
     * O método inicializa as informações elementares sobre o projeto para ser possível calcular TestI. São elas:
     * o rastro da análise (trace), os arquivos de código-fonte do projeto em sua versão mais atual (na perspectiva da tarefa),
     * a lista de expressões regulares usadas para fazer o casamento entre steps e step definitions, a lista de métodos
     * declarados no projeto e a lista de views existentes no projeto.
     *
     * Para esse método funcionar corretamente é preciso que as propriedades referentes à estrutura de diretórios do
     * projeto estejam devidamente configuradas.
     */
    def configureProperties() {
        analysisData.trace = []
        projectFiles = Util.findFilesFromDirectoryByLanguage(repositoryPath)
        configureRegexList()
        configureMethodsList()
        def views = Util.findFilesFromDirectory(repositoryPath + File.separator + Util.VIEWS_FILES_RELATIVE_PATH)
        viewFiles = views.findAll { Util.isViewFile(it) }
    }

    /***
     * Método template para calcular TestI para tarefas concluídas (estudo de avaliação).
     *
     * @param gherkinFiles lista de arquivos Gherkin associados à tarefa, que são os que foram alterados por seus commits
     * @param stepFiles lista de arquivos de step definitions associados à tarefa, que são os que foram alterados por
     * seus commits, independente das mudanças feitas em arquivos Gherkin
     * @param removedSteps lista de steps em Gherkin que foram removidos pela tarefa, identificados por um set cujos
     * elementos possuem as chaves 'path' (identifica o arquivo Gherkin que continha o step) e 'text' (o conteúdo do step).
     * @return TestI da tarefa
     */
    TestI computeInterfaceForDoneTask(List<ChangedGherkinFile> gherkinFiles, List<ChangedStepdefFile> stepFiles,
                                      Set removedSteps) {
        configureProperties()
        List<AcceptanceTest> acceptanceTests = extractAcceptanceTest(gherkinFiles)
        List<StepCode> stepCodes1 = acceptanceTests*.stepCodes?.flatten()?.unique()
        List<FileToAnalyse> files1 = identifyMethodsPerFileToVisit(stepCodes1)
        List<FileToAnalyse> files2 = findCodeForStepsIndependentFromAcceptanceTest(stepFiles, acceptanceTests)
        List<FileToAnalyse> filesToAnalyse = collapseFilesToVisit(files1, files2)
        computeInterface(filesToAnalyse, removedSteps)
    }

    /***
     * Método template para calcular TestI para tarefas planejadas, não iniciadas.
     *
     * @param gherkinFiles lista de arquivos Gherkin associados à tarefa
     * @return TestI da tarefa
     */
    TestI computeInterfaceForTodoTask(List<ChangedGherkinFile> gherkinFiles) {
        List<AcceptanceTest> acceptanceTests = extractAcceptanceTest(gherkinFiles)
        List<StepCode> stepCodes = acceptanceTests*.stepCodes?.flatten()?.unique()
        List<FileToAnalyse> filesToAnalyse = identifyMethodsPerFileToVisit(stepCodes)
        computeInterface(filesToAnalyse, null)
    }

    /***
     * O método busca a implementação de um step Gherkin invocado por um step definition.
     *
     * @param call Objeto que representa a chamada para um step Gherkin.
     * @param extractArgs flag que sinaliza se devem ser considerados os valores usados como argumento na invocação do
     * step
     * @return lista de arquivos a serem analisados por conter o código de implementação do step de interesse.
     * Em tese, cada step possui uma implementação única, ou seja, casa com um único step definition. Porém, por
     * limitações no processo de extração de expressões regulares identificadoras de step definitions (caso particular
     * delas usarem variáveis, cujo valor é definido em tempo de execução), é possível haver múltiplos casamentos entre
     * step e step definitions.
     */
    List<FileToAnalyse> findCodeForStepOrganizedByFile(StepCall call, boolean extractArgs) {
        def calledSteps = []  //path, line, args, parentType
        List<FileToAnalyse> result = []

        /* find step declaration */
        def stepCodeMatch = regexList?.findAll { call.text ==~ it.value }
        def match = null
        if (!stepCodeMatch.empty) match = stepCodeMatch.first() //we consider only the first match
        if (stepCodeMatch.size() > 1) {
            log.warn "There are many implementations for step code: ${call.text}; ${call.path} (${call.line})"
            stepCodeMatch.each { log.info it.toString() }
            analysisData.multipleStepMatches += new StepError(path: call.path, text: call.text)
        }
        if (match) { //step code was found
            def args = []
            if (extractArgs) args = extractArgsFromStepText(call.text, match.value)
            calledSteps += [path: match.path, line: match.line, args: args, parentType: call.parentType]
        } else {
            log.warn "Step code was not found: ${call.text}; ${call.path} (${call.line})"
            def text = call.text.replaceAll(/".+"/,"\"\"").replaceAll(/'.+'/,"\'\'")
            analysisData.matchStepErrors += new StepError(path: call.path, text: text)
        }

        /* organizes step declarations in files */
        def files = (calledSteps*.path)?.flatten()?.unique()
        files?.each { file ->
            def codes = calledSteps.findAll { it.path == file }
            if (codes) {
                def methodsToAnalyse = []
                codes.each { code ->
                    methodsToAnalyse += new MethodToAnalyse(line: code.line, args: code.args, type: code.parentType)
                }
                result += new FileToAnalyse(path: file, methods: methodsToAnalyse.unique())
            }
        }
        result
    }

    /***
     * O método busca a implementação de um step Gherkin.
     *
     * Se forem encontradas múltiplas implementações para o step, apenas a primeira encontrada é considerada.
     * Se não for encontrada implementação para o step, é registrado a ocorrência de erro do tipo matchStepError.
     * No resultado de TestI, são desprezadas tarefas cujos testes possuem erro desse tipo, pois significa que os testes
     * não foram implementados.
     *
     * @param step Step Gherkin de interesse
     * @param path Arquivo em que o step Gherkin de interesse aparece.
     * @param extractArgs Flag que sinaliza se os argumentos usados no step Gherkin devem ser memorizados
     * @param last referência para implementação do último step analisado, cuja ocorrência antecede o step atual.
     * Isso é necessário para fazer o devido registro do tipo do step (Given, When, Then), visto que em Gherkin existem
     * outras palavras-chave cujo significado depende do contexto em que se encontram, como And e But.
     * @return código de implementação do step, representado por um conjunto de objetos
     */
    List<StepCode> findCodeForStep(step, String path, boolean extractArgs, StepCode last) {
        List<StepCode> code = []
        def stepCodeMatch = regexList?.findAll { step.text ==~ it.value }
        def match = null
        if (!stepCodeMatch.empty) match = stepCodeMatch.first() //we consider only the first match
        if (stepCodeMatch.size() > 1) {
            log.warn "There are many implementations for step code: ${step.text}; $path (${step.location.line})"
            stepCodeMatch.each { log.info it.toString() }
            analysisData.multipleStepMatches += new StepError(path: path, text: step.text)
        }
        if (match) { //step code was found
            def args = []
            if (extractArgs) args = extractArgsFromStepText(step.text, match.value)
            def keyword = step.keyword
            if (keyword == ConstantData.GENERIC_STEP) {
                keyword = match.keyword
                analysisData.genericStepKeyword += [path: path, text: step.text]
            }
            if (last && (keyword in [ConstantData.AND_STEP_EN, ConstantData.BUT_STEP_EN])) keyword = last.type
            code += new StepCode(step: step, codePath: match.path, line: match.line, args: args, type: keyword)
        } else {
            log.warn "Step code was not found: ${step.text}; $path (${step.location.line})"
            def text = step.text.replaceAll(/".+"/,"\"\"").replaceAll(/'.+'/,"\'\'")
            analysisData.matchStepErrors += new StepError(path: path, text: text)
        }
        code
    }

    /***
     * O método busca a implementação de um conjunto de step Gherkin usados em um mesmo arquivo, por estarem associados
     * ao mesmo scenario, scenario outline ou background Gherkin.
     *
     * @param steps O conjunto de steps de interesse.
     * @param path O arquivo em que tais steps são usados (ou seja, que contém a declaração do scenario, scenario outline
     * ou background) que usa os steps.
     * @return código de implementação dos steps, representado por um conjunto de objetos
     */
    List<StepCode> findCodeForSteps(List steps, String path) {
        List<StepCode> codes = []
        for (int i = 0; i < steps.size(); i++) {
            def previousStep = null
            if (i > 0) previousStep = codes.last()
            def code = findCodeForStep(steps.get(i), path, true, previousStep)
            if (code && !code.empty) codes += code
            else {
                codes = []
                break
            }
        }
        codes
    }

    /***
     * Esse método busca pelo código de implementação de um step definition que foi alterado diretamente.
     *
     * Para entender o contexto de uso desse método, ver o método findCodeForStepsIndependentFromAcceptanceTest.
     *
     * @param step step definition de interesse
     * @param acceptanceTests conjunto de testes de aceitação completos associados à uma tarefa. Esse método precisa dessa
     * informação porque ele tenta descobrir o tipo do step definition (given, when, then). Embora esse tipo apareça
     * no step definition, seu valor não tem significado para o cucumber. O significado real é definido pelo step Gherkin
     * que é automatizado pelo step definition em questão. Então tenta-se verificar se o referido step definition é usado
     * por algum teste de aceitação da tarefa, para inferir seu tipo.
     * @return lista de objetos map com chaves de path e text informando o arquivo em que a implementação do step
     * se encontra e seu conteúdo.
     */
    List findCodeForStepIndependentFromAcceptanceTest(StepDefinition step, List<AcceptanceTest> acceptanceTests) {
        def result = []
        def stepCodeMatch = regexList?.findAll { step.regex == it.value }
        def match = null
        if (!stepCodeMatch.empty) match = stepCodeMatch.first() //we consider only the first match
        if (stepCodeMatch.size() > 1) {
            log.warn "There are many implementations for step code: ${step.value}; ${step.path} (${step.line})"
            analysisData.multipleStepMatches += new StepError(path: step.path, text: step.value)
        }
        if (match) { //step code was found
            def type = findStepType(step, acceptanceTests)
            result += [path: match.path, line: match.line, keyword: type, text: match.value]
        } else {
            log.warn "Step code was not found: ${step.value}; ${step.path} (${step.line})"
            def text = step.value.replaceAll(/".+"/,"\"\"").replaceAll(/'.+'/,"\'\'")
            analysisData.matchStepErrors += new StepError(path: step.path, text: text)
        }
        result
    }

    /***
     * Esse método busca pelo código de implementação de step definitions que foram alterados diretamente, sem mudanças
     * sobre scenarios Gherkin.
     *
     * Em um estudo retroativo, consideramos tarefas já concluídas, associadas à um conjunto de commits. Os testes que
     * supostamente validam o comportamento esperado da tarefa são identificados com base nas mudanças realizadas por
     * seus commits. Os commits podem alterar scenarios Gherkin (e suas variantes, como scenario outline e background),
     * e em regra geral chamamos tal cenários de testes de aceitação. Porém, há commits que alteram steps
     * definitions diretamente. Um step definition automatiza a execução de um step de um scenario Gherkin e pode ser
     * reusado por diferentes scenarios.
     *
     * Esse método lida justamente com essa situação de achar o código de implementação mais atual de um determinado
     * step definition que foi alterado de forma independente dos arquivos Gherkin que o utilizam.
     *
     * @param stepFiles conjunto de arquivos que contém step definitions que foram alterados de forma independente dos
     * scenarios Gherkin
     * @param acceptanceTests conjunto de testes de aceitação completos associados à uma tarefa. Esse método precisa dessa
     * informação porque, no fim de tudo, é preciso organizar o código de teste completo associado à uma tarefa, considerando
     * o nome do arquivo e as linhas de interesse.
     * @return lista de objetos FileToAnalyse, que informa o arquivo a ser visitado e as linhas de interesse
     */
    List findCodeForStepsIndependentFromAcceptanceTest(List<ChangedStepdefFile> stepFiles, List<AcceptanceTest> acceptanceTests) {
        def values = []
        List<FileToAnalyse> result = []
        stepFiles?.each { file ->
            def partialResult = []
            file.changedStepDefinitions?.each { step ->
                def code = findCodeForStepIndependentFromAcceptanceTest(step, acceptanceTests) //path, line, keyword, text
                if (code && !code.empty) partialResult += code
            }
            if (!partialResult.empty) {
                def methodsToAnalyse = []
                if (Util.WHEN_FILTER) partialResult = partialResult?.findAll { it.keyword != ConstantData.THEN_STEP_EN }
                partialResult?.each {
                    methodsToAnalyse += new MethodToAnalyse(line: it.line, args: [], type: it.keyword)
                }
                if (!partialResult.empty) {
                    result += new FileToAnalyse(path: partialResult?.first()?.path, methods: methodsToAnalyse)
                }
            }
            values += partialResult
        }

        analysisData.foundStepDefs = values
        log.info "Number of implemented step definitions: ${values.size()}"
        def info = ""
        values?.each { info += "${it.text} (${it.path}: ${it.line})\n" }
        log.info info.toString()

        result
    }

    /***
     * O método procura pelo código que automatiza a execução de um scenario Gherkin.
     *
     * Lembrete: Esse método não pode ser private, pois nesse caso é lançada a exceção groovy.lang.MissingMethodException.
     *
     * @param scenarioDefinition Scenario Gherkin de interesse
     * @param scenarioDefinitionPath nome completo do arquivo Gherkin que contém a declaração do scenario de interesse
     * @return AcceptanceTest objeto que representa o casamento entre uma definição de scenario e sua implementação
     */
    AcceptanceTest configureAcceptanceTest(Scenario scenarioDefinition, String scenarioDefinitionPath) {
        AcceptanceTest test = null
        List<StepCode> codes = findCodeForSteps(scenarioDefinition.steps, scenarioDefinitionPath)
        if (!codes?.empty) test = new AcceptanceTest(gherkinFilePath: scenarioDefinitionPath, scenarioDefinition: scenarioDefinition, stepCodes: codes)
        test
    }

    /***
     * Esse método foi pensado para lidar com scenarios de background, de forma análoga ao que é feito com scenarios
     * convencionais e scenarios outline. Um scenario de background consiste em um conjunto de steps que é executado
     * antes da execução de cada scenario Gherkin, promovendo reuso de código no projeto de testes.
     * A implementação dele é morta porque definiu-se uma forma específica para lidar com Background Gherkin. Apesar
     * disso, ela se faz necessária, porque pela API do Cucumber, Background é subclasse de ScenarioDefinition, que
     * também é superclasse de Scenario e ScenarioOutline. Então como a versão do método configureAcceptanceTest a ser
     * executada é definida dinâmicamente, havia problemas. Essa implementação morta garante que caso o método
     * execute com um objeto Background, nenhuma inconsistência seja gerada.
     *
     * @param background Background Gherkin de interesse
     * @param scenarioDefinitionPath nome completo do arquivo Gherkin que contém a declaração do background de interesse
     * @return
     */
    AcceptanceTest configureAcceptanceTest(Background background, String scenarioDefinitionPath) {
        null
    }

    /***
     * O método procura pelo código que automatiza a execução de um scenario Gherkin que usa a sintaxe de um scenario outline
     * (onde é possível definir um conjunto de valores para serem usados como parâmetro para o scenario, de forma concisa).
     * Exemplo em https://cucumber.io/docs/gherkin/reference/#scenario-outline
     *
     * Lembrete: Esse método não pode ser private, pois nesse caso é lançada a exceção groovy.lang.MissingMethodException.
     *
     * @param scenarioDefinition Scenario outline Gherkin de interesse
     * @param scenarioDefinitionPath nome completo do arquivo Gherkin que contém a declaração do scenario outline de interesse
     * @return AcceptanceTest objeto que representa o casamento entre uma definição de scenario e sua implementação
     */
    AcceptanceTest configureAcceptanceTest(ScenarioOutline scenarioDefinition, String scenarioDefinitionPath) {
        AcceptanceTest test = null
        List<Step> stepsToAnalyse = []
        List<String> argName = scenarioDefinition.examples.tableHeader*.cells.flatten()*.value
        scenarioDefinition.steps.each { step ->
            if (!(step.text.contains("<") && step.text.contains(">"))) {
                stepsToAnalyse += step
            } else {
                def argsInLine = scenarioDefinition.examples.tableBody.get(0).cells.value
                argsInLine.each { argsLine ->
                    def transformedText = new String(step.text)
                    for (int i = 0; i < argName.size(); i++) {
                        def searchExpression = "<${argName.get(i)}>"
                        if (transformedText.contains(searchExpression)) {
                            transformedText = transformedText.replaceFirst(searchExpression, argsLine.get(i))
                        }
                    }
                    stepsToAnalyse += new Step(step.location, step.keyword, transformedText, step.argument)
                }
            }
        }

        List<StepCode> codes = findCodeForSteps(stepsToAnalyse, scenarioDefinitionPath)
        if (!codes?.empty) test = new AcceptanceTest(gherkinFilePath: scenarioDefinitionPath, scenarioDefinition: scenarioDefinition, stepCodes: codes)
        test
    }

    /***
     * Identifica os métodos para visitar dentre uma lista de chamadas de método, considerando o histórico de visitas.
     * Os métodos de interesse são definidos pelo código de teste.
     *
     * @param lastCalledMethods lista de objetos map que identificam os métodos chamados usando as chaves 'name', 'type',
     * 'file' and 'step'.
     * @param allVisitedFiles Uma coleção de todos os arquivos visitados identificados e seus métodos.
     * Arquivos são identificados pela chave 'path' e os métodos, pela chave 'methods'.
     * @return lista de métodos agrupados por arquivo
     */
    def listFilesToParse(lastCalledMethods, allVisitedFiles) {
        def validCalledMethods = lastCalledMethods.findAll { it.file != null && it.type != "StepCall" }
        def methods = groupMethodsToVisitByFile(validCalledMethods)
        def filesToVisit = []
        methods.each { file ->
            def match = allVisitedFiles?.find { it.path == file.path }
            if (match != null) {
                filesToVisit += [path: file.path, methods: file.methods - match.methods]
            } else {
                filesToVisit += [path: file.path, methods: file.methods]
            }
        }
        updateTraceMethod(filesToVisit)
        return filesToVisit
    }

    /***
     * Identifica os métodos para visitar dentre uma lista de chamadas de método.
     * Os métodos de interesse são definidos pelo código de teste.
     *
     * @param methodsList list of map objects identifying called methods by 'name', 'type' and 'file'.
     * @return lista de métodos agrupados por arquivo
     */
    static groupMethodsToVisitByFile(methodsList) {
        def testFiles = []
        def calledTestMethods = methodsList?.findAll { it.file != null && Util.isTestFile(it.file) }?.unique()
        calledTestMethods*.file.unique().each { path ->
            def methodsInPath = calledTestMethods.findAll { it.file == path }
            def methods = methodsInPath.collect { [name: it.name, step: it.step] }
            testFiles += [path: path, methods: methods]
        }
        return testFiles
    }

    static updateVisitedFiles(List allVisitedFiles, List filesToVisit) {
        def allFiles = allVisitedFiles + filesToVisit
        def paths = (allFiles*.path)?.unique()
        def result = []
        paths?.each { path ->
            def methods = (allFiles?.findAll { it.path == path }*.methods)?.flatten()?.unique()
            if (methods != null && !methods.isEmpty()) result += [path: path, methods: methods]
        }
        return result
    }

    /***
     * Método que atualiza o registro do rastro da análise de código de teste associado à uma tarefa.
     * @param file novo arquivos analisado, a ser incluído no registro de rastro
     */
    def updateTrace(FileToAnalyse file){
        analysisData.trace += [path: file.path, methods: file.methods*.line]
    }

    /***
     * Método que atualiza o registro do rastro da análise de código de teste associado à uma tarefa.
     * @param files novos arquivos analisados, a serem incluídos no registro de rastro
     */
    def updateTrace(List<FileToAnalyse> files){
        files.each{ updateTrace(it) }
    }

    /***
     * O método tenta identificar o tipo de um step definition (given, when, then) em caso dele ter sido modificado
     * diretamente pela tarefa.
     * Se o step definition é usado por algum teste de aceitação da tarefa, o tipo é definido por ele. Caso contrário,
     * se considera o tipo que aparece no step definition.
     *
     * @param stepDefinition step definition de interesse
     * @param acceptanceTests conjunto de testes de aceitação da tarefa
     * @return tipo do step definition de interesse (given, when, then)
     */
    private static findStepType(StepDefinition stepDefinition, List<AcceptanceTest> acceptanceTests) {
        def result = stepDefinition.keyword
        def stepCodeList = (acceptanceTests*.stepCodes.flatten())
        def match = stepCodeList.find { it.step.text ==~ stepDefinition.regex }
        if (match) { //Step (gherkin) and step definition (ruby) were both changed
            result = match.type
        }
        result
    }

    /***
     * Método que atualiza o registro do rastro da análise de código de teste associado à uma tarefa.
     * @param files novos arquivos analisados, a serem incluídos no registro de rastro
     */
    private updateTraceMethod(List files){
        files.each { file -> analysisData.trace += [path: file.path, methods: file.methods] }
    }

    /***
     * Método que consolida o registro do rastro da análise de código de teste associado à uma tarefa.
     */
    private consolidateTrace(){
        def aux = []
        def files = analysisData.trace*.path
        def groupsByFile = analysisData.trace.groupBy({ t -> t.path })
        files.each{ file ->
            def methods = groupsByFile[file].collect{ it.methods }.flatten().unique()
            def path = file-repositoryPath
            def index = path.indexOf(File.separator+File.separator)+1
            aux += [path: path.substring(index), methods:methods]
        }
        analysisData.trace = aux.sort { it.path }
    }

    /***
     * Método que efetivamente calcula TestI com base nos testes associados à tarefa, seja esta uma tarefa a fazer ou
     * uma tarefa já concluída (perspectiva de estudo retroativo).
     * @param filesToAnalyse Conjunto de arquivos a serem visitados e os métodos de interesse neles.
     * @param removedSteps Conjunto de steps Gherkin removidos pela tarefa, que não podem ser considerados no cálculo de
     * TestI. Só faz sentido no caso de tarefas já concluídas.
     * @return TestI da tarefa
     */
    private TestI computeInterface(List<FileToAnalyse> filesToAnalyse, Set removedSteps) {
        def interfaces = []
        List<StepCall> calledSteps = []

        updateTrace(filesToAnalyse)
        filesToAnalyse?.eachWithIndex { stepDefFile, index ->
            log.info stepDefFile.toString()

            /* Primeio nível: Identificar chamadas de método a partir do corpo de step definitions. */
            TestCodeVisitorInterface testCodeVisitor = parseStepBody(stepDefFile)

            /* Segundo nível: Visitar métodos até não haver mais chamadas de métodos de teste. */
            def visitedFiles = []
            def filesToParse = listFilesToParse(testCodeVisitor.taskInterface.methods, visitedFiles) //[path: file.path, methods: [name:, step:]]
            log.info "Files to parse: ${filesToParse.size()}"
            filesToParse.each {
                log.info "path: ${it?.path}"
                log.info "methods: ${it?.methods?.size()}"
                it.methods.each { m ->
                    log.info m.toString()
                }
            }

            while (!filesToParse.empty) {
                /* Copia métodos de TestI */
                def backupCalledMethods = testCodeVisitor.taskInterface.methods

                /* Visita cada arquivo */
                filesToParse.each { f ->
                    visitFile(f, testCodeVisitor)
                }

                /* Define os métodos para visitar com base no histórico de visitas */
                visitedFiles = updateVisitedFiles(visitedFiles, filesToParse)
                def lastCalledMethods = testCodeVisitor.taskInterface.methods - backupCalledMethods
                filesToParse = listFilesToParse(lastCalledMethods, visitedFiles)
            }

            /* Atualiza os steps chamados */
            calledSteps += testCodeVisitor.calledSteps

            /* Procura por views */
            if(!testCodeVisitor?.taskInterface?.calledPageMethods?.empty) {
                log.info "calledPageMethods:"
                testCodeVisitor?.taskInterface?.calledPageMethods?.each { log.info it.toString() }
                findAllPages(testCodeVisitor)
            }

            /* Atualiza TestI */
            interfaces += testCodeVisitor.taskInterface

            analysisData.visitCallCounter += testCodeVisitor.visitCallCounter
            def canonicalPath = Util.getRepositoriesCanonicalPath()
            analysisData.lostVisitCall += testCodeVisitor.lostVisitCall.collect {
                [path: it.path - canonicalPath, line: it.line]
            }
            analysisData.testCode += testCodeVisitor.methodBodies
        }

        /* Identifica mais step definitions para analisar, dado que testes podem invocar step definitions diretamente */
        log.info "calledSteps: ${calledSteps.size()}"
        calledSteps.each{ log.info it.toString() }

        List<FileToAnalyse> newStepsToAnalyse = identifyMethodsPerFileToVisitByStepCalls(calledSteps)
        newStepsToAnalyse = updateStepFiles(filesToAnalyse, newStepsToAnalyse)
        if (!newStepsToAnalyse.empty) interfaces += computeInterface(newStepsToAnalyse, removedSteps)

        /* Junta resultados parciais a fim de delimitar TestI único para a tarefa */
        fillTestI(interfaces, removedSteps)
    }

    /***
     * O método consolida o resultado de TestI para uma tarefa, considerando que podem ser gerados resultados parciais.
     *
     * @param interfaces resultados parciais de TestI
     * @param removedSteps steps que a tarefa removeu. Erros de análise associados a tais steps devem ser desprezados,
     * visto que eles não devem fazer parte de TestI. TestI não lida com remoção de código.
     * @return resultado de TestI consolidado
     */
    private fillTestI(List<TestI> interfaces, Set removedSteps) {
        def testi = TestI.collapseInterfaces(interfaces)
        testi.matchStepErrorsPerFile = organizeMatchStepErrors(removedSteps)
        testi.multipleStepMatches = organizeMultipleStepMatches(removedSteps)
        testi.genericStepKeyword = analysisData.genericStepKeyword
        testi.parseErrorsPerFile = organizeParseErrors()
        testi.codeFromViewAnalysis = this.getCodeFromViewAnalysis() //Esse método deve ser sobrescrito pela subclasse
        testi.notFoundViews = notFoundViews.sort()
        testi.foundAcceptanceTests = analysisData.foundAcceptanceTests
        testi.foundStepDefs = analysisData.foundStepDefs
        testi.visitCallCounter = analysisData.visitCallCounter
        testi.lostVisitCall = analysisData.lostVisitCall
        testi.code += analysisData.testCode
        testi.trace = consolidateTrace()
        testi
    }

    /***
     * Organiza a ocorrência de erros de casamento entre step Gherkin e step definitions.
     * Esse tipo de erro ocorre quando não se consegue localizar implementação para um dado step Gherkin.
     * @param removedSteps steps que a tarefa removeu. Erros de análise associados a tais steps devem ser desprezados,
     * visto que eles não devem fazer parte de TestI. TestI não lida com remoção de código.
     * @return lista de erros organizados por arquivo (para cada arquivo com erro, sabe-se o step sem implementação e a
     * quantidade de steps sem implementação).
     */
    private organizeMatchStepErrors(Set removedSteps) {
        def result = [] as Set
        def errors = analysisData.matchStepErrors - removedSteps
        def files = errors*.path.unique()
        files.each { file ->
            def index = file.indexOf(repositoryPath)
            def name = index >= 0 ? file.substring(index) - (repositoryPath + File.separator) : file
            def texts = (analysisData.matchStepErrors.findAll { it.path == file }*.text)?.unique()?.sort()
            result += new StepErrorList(path:name, text:texts, size:texts.size())
        }
        result
    }

    /***
     * Organiza a ocorrência de múltiplos casamentos entre um step Gherkin e step definitions.
     * Esse evento ocorre quando falhamos aos extrair as regex que identificam step definitions, visto que para o
     * Cucumber deve existir uma implementação única.
     * @param removedSteps steps que a tarefa removeu. Erros de análise associados a tais steps devem ser desprezados,
     * visto que eles não devem fazer parte de TestI. TestI não lida com remoção de código.
     */
    private organizeMultipleStepMatches(Set removedSteps) {
        def intersection = analysisData.multipleStepMatches.findAll { it in removedSteps }
        analysisData.multipleStepMatches - intersection
    }

    /***
     * Organiza a ocorrência de erros de parse identificados durante o cálculo de TestI de uma tarefa.
     * Esse tipo de erro ocorre quando não se consegue gerar a AST para os arquivos de teste.
     * @return lista de erros de parse organizados por arquivo (para cada arquivo com erro, sabe-se a
     * mensagem do erro).
     */
    private organizeParseErrors() {
        analysisData.parseErrors += gherkinManager.parseErrors
        def result = [] as Set
        def files = analysisData.parseErrors*.path.unique()
        files.each { file ->
            def msgs = analysisData.parseErrors.findAll { it.path == file }*.msg
            result += new ParseErrorList(path: file, msgs: msgs.unique())
        }
        result
    }

    /***
     * O metodo obtém o conjunto de testes de aceitação completos (scenarios Gherkin e código que o automatiza) para um
     * conjunto de arquivos Gherkin alterados. Tais alterações consistem no conjunto de testes associados à tarefa, num
     * contexto de estudo retroativo, em que as tarefas já estão concluídas.
     *
     * @param gherkinFiles conjunto de arquivos Gherkin alterados por uma tarefa
     * @return conjunto de testes de aceitção da tarefa, a partir dos quais se calcula TestI
     */
    private List<AcceptanceTest> extractAcceptanceTest(List<ChangedGherkinFile> gherkinFiles) {
        List<AcceptanceTest> acceptanceTests = []
        gherkinFiles?.each { gherkinFile ->
            /* finds step code of background from a Gherkin file */
            List<StepCode> backgroundCode = []
            Background background = (Background) gherkinFile.feature.children.find{ it instanceof Background }
            if (background) {
                backgroundCode = findCodeForSteps(background.steps, gherkinFile.path)
            }

            /* finds step code of changed scenario definitions from a Gherkin file */
            gherkinFile?.changedScenarioDefinitions?.each { definition ->
                def test = configureAcceptanceTest(definition, gherkinFile.path) //groovy dynamic method dispatch
                if (test) {
                    if (!backgroundCode.empty) {
                        test.stepCodes = (test.stepCodes + backgroundCode).unique()
                    }
                    acceptanceTests += test
                }
            }
        }

        analysisData.foundAcceptanceTests = acceptanceTests
        log.info "Number of implemented acceptance tests: ${acceptanceTests.size()}"
        acceptanceTests?.each { log.info it.toString() }
        acceptanceTests
    }

    /***
     * O método lista todas as expressões regulares usadas como identificadores de step definitions no projeto.
     * @return a lista de expressões regulares
     */
    private configureRegexList() {
        regexList = []
        def files = Util.findFilesFromDirectoryByLanguage(stepsFilePath)
        files.each { regexList += doExtractStepsRegex(it) }
    }

    /***
     * O método lista todas as declarações de métodos no projeto considerando o conjunto de pastas consideradas válidas
     * (que seriam aquelas que possuem métodos de aplicação e métodos de testes).
     *
     * @return a lista de métodos
     */
    private configureMethodsList() {
        methods = [] as Set
        def filesForSearchMethods = []
        Util.VALID_FOLDERS.each { folder ->
            filesForSearchMethods += Util.findFilesFromDirectoryByLanguage(repositoryPath + File.separator + folder)
        }
        filesForSearchMethods.each { methods += doExtractMethodDefinitions(it) }
    }

    /***
     * Método que lista métodos de API, a fim de possibilitar a diferenciação destes dos métodos declarados no projeto.
     * TestI só considera métodos declarados no projeto.
     */
    private configureApiMethodsList() {
        if (apiMethods != null) return
        apiMethods = [] as Set
        def filesForSearchMethods = Util.findFrameworkClassFiles()
        filesForSearchMethods.each { apiMethods += doExtractMethodDefinitions(it) }
    }

    /***
     * O método organiza a lista de métodos a serem analisados para se calcular TestI, organizados por arquivo.
     *
     * @param stepCalls conjunto de chamadas de step Gherkin realizadas a partir de step definitions
     * @return lista de métodos a serem analisados, organizados por arquivo. Eles são representados por uma lista de objetos
     * FileToAnalyse, que especifica o arquivo e a linha inicial da implementação de cada step.
     */
    private List<FileToAnalyse> identifyMethodsPerFileToVisitByStepCalls(List<StepCall> stepCalls) {
        List<FileToAnalyse> files = []
        stepCalls?.unique { it.text }?.each { stepCall ->
            List<FileToAnalyse> stepCode = findCodeForStepOrganizedByFile(stepCall, true)
            if (stepCode && !stepCode.empty) files += stepCode
        }
        identifyMethodsPerFile(files)
    }

    /***
     * O método identifica os métodos a serem analisados para calcular TestI, organizados por arquivo.
     * Esse método só é usado no caso específico de se lidar com chamada de step Gherkin feita internamente
     * à implementação de um dado step definition.
     * Para entender seu contexto de uso, ver o método identifyMethodsPerFileToVisitByStepCalls.
     *
     * @param filesToAnalyse conjunto de arquivos a serem analisados.
     * @return conjunto de métodos a serem analisados, organizados por arquivo
     */
    private static identifyMethodsPerFile(List<FileToAnalyse> filesToAnalyse) {
        List<FileToAnalyse> result = []
        def files = filesToAnalyse*.path?.unique()
        files?.each { file ->
            def entries = filesToAnalyse?.findAll { it.path == file }
            if (entries) {
                def methodsToAnalyse = entries*.methods.flatten()
                result += new FileToAnalyse(path: file, methods: methodsToAnalyse)
            }
        }
        result
    }

    /***
     * O método atualiza a lista de arquivos a serem analisados para calcular TestI, considerando que listas parciais
     * podem ser geradas. Em regra geral, os arquivos a serem analisados são identificados a partir dos scenarios Gherkin
     * associados à tarefa. Porém, dado que um step definition pode invocar um step Gherkin, então uma nova lista de
     * arquivos a serem analisados pode ser organizada.
     *
     * @param oldFiles conjunto de arquivos a serem analisados identificados a partir da implementação convencional de
     * scenarios
     * @param newFiles conjunto de arquivos a serem analisados identificados a partir de chamadas à steps Gherkin feitas
     * por step definitions (vinculadas à tarefa)
     * @return
     */
    private static updateStepFiles(List<FileToAnalyse> oldFiles, List<FileToAnalyse> newFiles) {
        List<FileToAnalyse> result = []
        newFiles?.each { newFile ->
            def visited = oldFiles?.findAll { it.path == newFile.path }
            if (!visited) result += newFile
            else {
                def visitedMethods = visited*.methods.flatten()
                def newMethods = newFile.methods - visitedMethods
                if (!newMethods.empty) result += new FileToAnalyse(path: newFile.path, methods: newMethods)
            }
        }
        result
    }

    /***
     * O método organiza a lista de métodos a serem analisados para se calcular TestI, organizados por arquivo.
     *
     * @param stepCodes lista de objetos StepCode, que engloba o step Gherkin e seu código de implementação. Tais objetos
     * representam o código de teste associado à tarefa.
     * @return lista de métodos a serem analisados, organizados por arquivo. Eles são representados por uma lista de objetos
     * FileToAnalyse, que especifica o arquivo e a linha inicial da implementação de cada step.
     */
    private static List<FileToAnalyse> identifyMethodsPerFileToVisit(List<StepCode> stepCodes) {
        List<FileToAnalyse> result = []
        def files = (stepCodes*.codePath)?.flatten()?.unique()
        files?.each { file ->
            def codes = []
            if (Util.WHEN_FILTER) codes = stepCodes.findAll {
                it.codePath == file && it.type != ConstantData.THEN_STEP_EN
            }
            else codes = stepCodes.findAll { it.codePath == file }
            List<MethodToAnalyse> methods = []
            codes?.each {
                methods += new MethodToAnalyse(line: it.line, args: it.args, type: it.type)
            }
            if (!methods.empty) result += new FileToAnalyse(path: file, methods: methods.unique())

        }
        result
    }

    /***
     * O método organiza uma lista única de arquivos a serem analisados a fim de se calcular TestI.
     *
     * @param files1 primeira lista identificada
     * @param files2 segunda lista identificada
     * @return lista consolidada, eliminando duplicadas entre as listas passadas como argumento
     */
    private static List<FileToAnalyse> collapseFilesToVisit(List<FileToAnalyse> files1, List<FileToAnalyse> files2) {
        List<FileToAnalyse> result = []
        def sum = files1 + files2
        def files = sum*.path.unique()
        files?.each { file ->
            def entries = sum.findAll { it.path == file }
            def methodsToAnalyse = entries*.methods.flatten().unique()
            result += new FileToAnalyse(path: file, methods: methodsToAnalyse)
        }
        result
    }

    /***
     * O método extrai os argumentos usados em um step Gherkin.
     *
     * @param text O texto do step Gherkin
     * @param regex A expressão regular que automatiza a execução do step Gherkin
     * @return
     */
    private static List<String> extractArgsFromStepText(String text, String regex) {
        List<String> args = []
        def matcher = (text =~ /${regex}/)
        if (matcher) {
            def counter = matcher.groupCount()
            for (int i = 1; i <= counter; i++) {
                def arg = matcher[0][i]
                if (arg) args += arg
            }
        }
        args
    }

    /***
     * Método que configura o caminho da pasta onde se localizam os arquivos de step definitions do projeto.
     * O Cucumber define um caminho default, mas os projetos podem configurá-lo também.
     */
    private configureStepsFilePath() {
        stepsFilePath = repositoryPath + File.separator + Util.STEPS_FILES_RELATIVE_PATH
        def directory = new File(stepsFilePath)
        def files = []
        if (directory.exists()) {
            files = Util.findFilesFromDirectoryByLanguage(stepsFilePath)
        }
        if (files.empty) {
            log.warn "Default folder of step definitions does not exists or it is empty: '${stepsFilePath}'"
            def subfolders = Util.findFoldersFromDirectory(repositoryPath + File.separator + ConstantData.DEFAULT_GHERKIN_FOLDER)
            def stepDefsFolder = subfolders.find { it.endsWith("step_definitions") }
            if (stepDefsFolder && !stepDefsFolder.equals(stepsFilePath)) {
                log.warn "Default folder of step definitions is empty."
                def stepDefFiles = Util.findFilesFromDirectoryByLanguage(stepDefsFolder)
                if (!stepDefFiles.empty) {
                    def index = stepDefsFolder.indexOf(repositoryPath)
                    stepsFilePath = stepDefsFolder.substring(index)
                    log.warn "We fix the folder of step definitions. The right one is: '${stepsFilePath}'"
                }
            }

        }
    }

}
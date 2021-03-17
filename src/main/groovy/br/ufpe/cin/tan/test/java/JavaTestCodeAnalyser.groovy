package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.commit.change.gherkin.GherkinManager
import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.commit.change.unit.ChangedUnitTestFile
import br.ufpe.cin.tan.test.FileToAnalyse
import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.test.TestCodeAbstractAnalyser
import br.ufpe.cin.tan.test.TestCodeVisitorInterface
import br.ufpe.cin.tan.test.error.ParseError
import br.ufpe.cin.tan.test.MethodBody
import br.ufpe.cin.tan.util.java.JavaUtil
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import groovy.util.logging.Slf4j

@Slf4j
class JavaTestCodeAnalyser extends TestCodeAbstractAnalyser {

    JavaTestCodeAnalyser(String repositoryPath, GherkinManager gherkinManager) {
        super(repositoryPath, gherkinManager)
    }

    /***
     * Só faz sentido em projeto web. Nesse primeiro momento, pode ficar de fora.
     */

    @Override
    void findAllPages(TestCodeVisitorInterface visitor) {

    }

    @Override
    List<StepRegex> doExtractStepsRegex(String path) {
        def node = this.generateAst(path)
        def visitor = new JavaStepRegexVisitor(path)
        node?.accept(visitor, null)
        visitor.regexs
    }

    @Override
    List<StepDefinition> doExtractStepDefinitions(String path, String content) {
        JavaStepDefinitionVisitor javaStepDefinitionVisitor = new JavaStepDefinitionVisitor(path, content)
        def compilationUnit = this.generateAst(path)
        compilationUnit?.accept(javaStepDefinitionVisitor, null)
        javaStepDefinitionVisitor.stepDefinitions
    }

    @Override
    Set doExtractMethodDefinitions(String path) {
        JavaMethodDefinitionVisitor javaMethodDefinitionVisitor = new JavaMethodDefinitionVisitor(path)
        def compilationUnit = this.generateAst(path)
        compilationUnit?.accept(javaMethodDefinitionVisitor, null)
        javaMethodDefinitionVisitor.methods
    }

    @Override
    TestCodeVisitorInterface parseStepBody(FileToAnalyse file) {
        def node = this.generateAst(file.path)
        def visitor = new JavaTestCodeVisitor(projectFiles, file.path, methods)
        def fileContent = recoverFileContent(file.path)
        def testCodeVisitor = new JavaStepsFileVisitor(file.methods, visitor, fileContent)
        node?.accept(testCodeVisitor, null)
        visitor.methodBodies.add(new MethodBody(testCodeVisitor.body))
        visitor
    }

    /***
     * O método deve visitar o corpo de métodos selecionados de um arquivo de código-fonte procurando por outras chamadas
     * de método. O resultado é armazenado com um campo do visitor de entrada.
     *
     * @param file um map que identifica o arquivo e seus respectivos métodos a serem analisados. As palavras-chave são
     * 'path' para identificar o arquivo e 'methods' para os métodos que, por usa vez, é descrito pelas chave 'name' e
     * 'step'.
     * @param visitor Visitor usado para analisar o código, específico de LP
     */
    @Override
    visitFile(file, TestCodeVisitorInterface visitor) {
        def node = this.generateAst(file.path)
        visitor.lastVisitedFile = file.path
        def fileContent = recoverFileContent(file.path)
        def auxVisitor = new JavaMethodVisitor(file.methods, (JavaTestCodeVisitor) visitor, fileContent)
        node?.accept(auxVisitor, null)
        visitor.methodBodies.add(new MethodBody(auxVisitor.body))
    }

    /***
     * Não precisa implementar porque não estamos usando testes unitários para calcular TestI.
     */
    @Override
    TestCodeVisitorInterface parseUnitBody(ChangedUnitTestFile file) {
        return null
    }

    /***
     * Não precisa implementar porque não estamos usando testes unitários para calcular TestI.
     */
    @Override
    ChangedUnitTestFile doExtractUnitTest(String path, String content, List<Integer> changedLines) {
        return null
    }

    @Override
    String getClassForFile(String path) {
        JavaUtil.getClassName(path)
    }

    @Override
    boolean hasCompilationError(String path) {
        def node = this.generateAst(path)
        if (!node) true else false
    }

    static List<String> recoverFileContent(String path) {
        FileReader reader = new FileReader(path)
        reader?.readLines()
    }

    /***
     * Gera a AST para um arquivo Java
     * @param path caminho do arquivo de interesse
     * @return objeto que representa a AST
     */
    def generateAst(String path) {
        def fixedPath = path
        if(!path.contains(repositoryPath)) fixedPath = repositoryPath + File.separator + path
        def errors = []
        CompilationUnit compilationUnit = null
        try{
            compilationUnit = StaticJavaParser.parse(new File(fixedPath))
        } catch(Exception ex){
            def msg = ""
            if (ex.message && !ex.message.empty) {
                def index = ex.message.indexOf(",")
                msg = index >= 0 ? ex.message.substring(index + 1).trim() : ex.message.trim()
            }
            errors += new ParseError(path: fixedPath, msg: msg)
        }
        def finalErrors = errors.findAll { it.path.contains("${File.separator}src${File.separator}") }
        if(!finalErrors.empty){
            analysisData.parseErrors += finalErrors
        }
        compilationUnit
    }

}

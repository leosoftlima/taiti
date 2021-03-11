package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.commit.change.gherkin.GherkinManager
import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.commit.change.unit.ChangedUnitTestFile
import br.ufpe.cin.tan.test.FileToAnalyse
import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.test.TestCodeAbstractAnalyser
import br.ufpe.cin.tan.test.TestCodeVisitorInterface
import br.ufpe.cin.tan.test.ruby.MethodBody
import groovy.util.logging.Slf4j
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.CompilationUnit;

@Slf4j
class JavaTestCodeAnalyser extends TestCodeAbstractAnalyser {
      
    CompilationUnit generateAst(String path) { 
      CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
      return compilationUnit;
    }    

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
        node?.accept(visitor)
        visitor.regexs
    }

    @Override
    List<StepDefinition> doExtractStepDefinitions(String path, String content) {
        JavaStepDefinitionVisitor javaStepDefinitionVisitor = new JavaStepDefinitionVisitor()
        def compilationUnit = this.generateAst(path);
        javaStepDefinitionVisitor?.visit(compilationUnit, null);
        return javaStepDefinitionVisitor.stepDefinitions
    }

    @Override
    Set doExtractMethodDefinitions(String path) {
        JavaMethodDefinitionVisitor javaMethodDefinitionVisitor = new JavaMethodDefinitionVisitor()
        def compilationUnit = this.generateAst(path);
        javaMethodDefinitionVisitor?.visit(compilationUnit, null);
        javaMethodDefinitionVisitor.signatures
    }

    @Override
    TestCodeVisitorInterface parseStepBody(FileToAnalyse file) {
        def node = this.generateAst(file.path)
        def visitor = new JavaTestCodeVisitor(projectFiles, file.path, methods)
        def fileContent = recoverFileContent(file.path)
        def testCodeVisitor = new JavaStepsFileVisitor(file.methods, visitor, fileContent)
        node?.accept(testCodeVisitor)
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
        node?.accept(auxVisitor)
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
        def node = generateAst(path)
        if (!node) true else false
    }

    static List<String> recoverFileContent(String path) {
        FileReader reader = new FileReader(path)
        reader?.readLines()
    }
}

package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.commit.change.gherkin.GherkinManager
import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.commit.change.unit.ChangedUnitTestFile
import br.ufpe.cin.tan.test.FileToAnalyse
import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.test.TestCodeAbstractAnalyser
import br.ufpe.cin.tan.test.TestCodeVisitorInterface
import groovy.util.logging.Slf4j
import com.github.javaparser.ast.Node

@Slf4j
class JavaTestCodeAnalyser extends TestCodeAbstractAnalyser {
      String routesFile

    CompilationUnit generateAst(String path) { 
      CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
      return compilationUnit;
    }    

    JavaTestCodeAnalyser(String repositoryPath, GherkinManager gherkinManager) {
        super(repositoryPath, gherkinManager)
         this.routesFile = repositoryPath + JavaConstantData.ROUTES_FILE
    }

    /***
     * Só faz sentido em projeto web. Nesse primeiro momento, pode ficar de fora.
     */

    @Override
    void findAllPages(TestCodeVisitorInterface visitor) {

    }

    @Override
    List<StepRegex> doExtractStepsRegex(String path) {
         return null
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
        return null
    }

    @Override
    def visitFile(Object file, TestCodeVisitorInterface visitor) {
        return null
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
        return null
    }

    @Override
    boolean hasCompilationError(String path) {
        return false
    }
}

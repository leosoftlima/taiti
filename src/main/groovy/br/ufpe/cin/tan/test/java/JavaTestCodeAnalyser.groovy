package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.commit.change.gherkin.GherkinManager
import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.commit.change.unit.ChangedUnitTestFile
import br.ufpe.cin.tan.test.FileToAnalyse
import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.test.TestCodeAbstractAnalyser
import br.ufpe.cin.tan.test.TestCodeVisitorInterface

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
        return null
    }

    @Override
    List<StepDefinition> doExtractStepDefinitions(String path, String content) {
        return null
    }

    @Override
    Set doExtractMethodDefinitions(String path) {
        return null
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

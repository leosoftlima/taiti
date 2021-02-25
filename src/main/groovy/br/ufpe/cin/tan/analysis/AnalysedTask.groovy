package br.ufpe.cin.tan.analysis

import br.ufpe.cin.tan.analysis.taskInterface.TaskI
import br.ufpe.cin.tan.analysis.taskInterface.TestI
import br.ufpe.cin.tan.analysis.task.DoneTask
import br.ufpe.cin.tan.evaluation.TaskInterfaceEvaluator
import br.ufpe.cin.tan.test.AcceptanceTest
import br.ufpe.cin.tan.test.TestCodeAbstractAnalyser
import br.ufpe.cin.tan.util.Util
import groovy.util.logging.Slf4j

@Slf4j
class AnalysedTask {

    DoneTask doneTask
    TestI testi
    TaskI taski
    List<String> methods
    int stepCalls
    String texti
    String stepMatchErrorsText
    int stepMatchErrors
    int multipleStepMatchesCounter
    String multipleStepMatchesText
    int genericStepKeywordCounter
    String genericStepKeywordText
    String compilationErrorsText
    int compilationErrors
    String gherkinCompilationErrorsText
    int gherkinCompilationErrors
    String stepDefCompilationErrorsText
    int stepDefCompilationErrors
    String unitCompilationErrorsText
    int unitCompilationErrors
    String rails
    String ruby

    AnalysedTask(DoneTask doneTask) {
        this.doneTask = doneTask
        this.testi = new TestI()
        this.taski = new TaskI()
        this.texti = ""
        this.rails = ""
        this.ruby = ""
    }

    void setTesti(TestI testi) {
        this.testi = testi
        this.stepCalls = testi?.methods?.findAll { it.type == "StepCall" }?.unique()?.size()
        this.methods = testi?.methods?.findAll { it.type == "Object" }?.unique()*.name
        this.extractStepMatchErrorText()
        this.extractCompilationErrorText()
        this.extractMultipleStepMatches()
        this.extractGenericStepKeyword()
    }

    Set getTrace() {
        testi.trace
    }

    boolean isRelevant() {
        if (testiIsEmpty()) false
        else true
    }

    int getDevelopers() {
        doneTask?.developers
    }

    def getRenamedFiles() {
        doneTask.renamedFiles
    }

    def hasChangedStepDefs() {
        !doneTask.changedStepDefinitions.empty
    }

    def hasStepMatchError() {
        if (stepMatchErrors > 0) true
        else false
    }

    def hasCompilationError() {
        if (compilationErrors > 0) true
        else false
    }

    def hasGherkinCompilationError() {
        if (gherkinCompilationErrors > 0) true
        else false
    }

    def hasStepDefCompilationError() {
        if (stepDefCompilationErrors > 0) true
        else false
    }

    def hasUnitCompilationError() {
        if (unitCompilationErrors > 0) true
        else false
    }

    def hasChangedGherkinDefs() {
        !doneTask.changedGherkinFiles.empty
    }

    def hasMergeCommit() {
        doneTask.hasMergeCommit()
    }

    def taskiFiles() {
        taski.findFilteredFiles()
    }

    def taskiHasControllers() {
        def controllers = taskiFiles().findAll { Util.isControllerFile(it) }
        !controllers.empty
    }

    def taskiIsEmpty() {
        taski.findFilteredFiles().empty
    }

    def testiFiles() {
        testi.findFilteredFiles()
    }

    def testiHasControllers() {
        def controllers = testiFiles().findAll { Util.isControllerFile(it) }
        !controllers.empty
    }

    def testiIsEmpty() {
        testiFiles().empty
    }

    def testiViewFiles() {
        testiFiles().findAll { Util.isViewFile(it) }
    }

    def filesFromViewAnalysis() {
        testi.codeFromViewAnalysis
    }

    double precision() {
        TaskInterfaceEvaluator.calculateFilesPrecision(testi, taski)
    }

    double recall() {
        TaskInterfaceEvaluator.calculateFilesRecall(testi, taski)
    }

    double f2Measure() {
        def p = precision()
        def r = recall()
        def denominator = 4 * p + r
        if (denominator == 0) 0
        else 5 * ((p * r) / denominator)
    }

    def getDates() {
        doneTask.dates
    }

    def getCommitMsg() {
        doneTask.commitMessage
    }

    def getRemovedFiles() {
        doneTask.removedFiles
    }

    def notFoundViews() {
        testi.notFoundViews
    }

    Set<AcceptanceTest> getAcceptanceTests() {
        testi.foundAcceptanceTests
    }

    def hasImplementedAcceptanceTests() {
        if (testi.foundAcceptanceTests.size() > 0) true
        else false
    }

    def isValid() {
        int zero = 0
        compilationErrors == zero && stepMatchErrors == zero &&
                hasImplementedAcceptanceTests() && !taskiFiles().empty
    }

    /**
     * Represents an analysed task as an array in order to export content to CSV files.
     * Task information is organized as follows: id, dates, #days, #commits, commit message, #developers,
     * #(gherkin tests), #(implemented gherkin tests), #(stepdefs), #(implemented stepdefs), unknown methods,
     * #(step calls), step match errors, #(step match errors), AST errors, #(AST errors), gherkin AST errors,
     * #(gherkin AST errors), step AST errors, #(step AST errors), renamed files, deleted files, not found views,
     * #views, #TestI, #TaskI, TestI, TaskI, precision, recall, hashes, timestamp, rails version,
     * #(calls to visit), #(views in TestI), #(files accessed by view analysis), files accessed by view analysis.
     * Complete version with 38 fields.
     * */
    def parseAllToArray() {
        def testIFiles = this.testiFiles()
        def testISize = testIFiles.size()
        def taskIFiles = this.taskiFiles()
        def taskISize = taskIFiles.size()
        def renames = renamedFiles
        if (renames.empty) renames = ""
        def views = notFoundViews()
        if (views.empty) views = ""
        def filesFromViewAnalysis = filesFromViewAnalysis()
        def viewFileFromTestI = testiViewFiles().size()
        def project = formatProjectName(doneTask.gitRepository.name)
        String[] array = [project, doneTask.id, dates, doneTask.days, doneTask.commitsQuantity, commitMsg, developers,
                          doneTask.gherkinTestQuantity, testi.foundAcceptanceTests.size(), doneTask.stepDefQuantity,
                          testi.foundStepDefs.size(), configureUnknownMethods(), stepCalls, stepMatchErrorsText, stepMatchErrors,
                          compilationErrorsText, compilationErrors, gherkinCompilationErrorsText, gherkinCompilationErrors,
                          stepDefCompilationErrorsText, stepDefCompilationErrors, renames, removedFiles, views,
                          views.size(), testISize, taskISize, testIFiles, taskIFiles, precision(), recall(),
                          doneTask.hashes, testi.timestamp, rails, testi.visitCallCounter, testi.lostVisitCall,
                          viewFileFromTestI, filesFromViewAnalysis.size(), filesFromViewAnalysis, hasMergeCommit(),
                          f2Measure(), multipleStepMatchesCounter, multipleStepMatchesText,
                          genericStepKeywordCounter, genericStepKeywordText]
        array
    }

    private static formatProjectName(String project) {
        def name = project.toLowerCase()
        if (name ==~ /.+_\d+/) {
            def index = name.lastIndexOf("_")
            name = name.substring(0, index)
        }
        if (name.contains("_")) {
            def index = name.indexOf("_")
            name = name.substring(index + 1)
        }
        name
    }

    /**
     * Represents an analysed task as an array in order to export content to CSV files.
     * Task information is organized as follows: id, dates, dates, #developers, #commits, hashes,
     * #(implemented gherkin tests), #(implemented stepdefs), #TestI, #TaskI, TestI, TaskI, precision, recall,
     * rails version, #(calls to visit), #(views in TestI), #(files accessed by view analysis), files accessed by view
     * analysis, unknown methods, renamed files, deleted files, views, #views, timestamp.
     * Partial version with 24 fields.
     * */
    def parseToArray() {
        def testIFiles = this.testiFiles()
        def testISize = testIFiles.size()
        def taskIFiles = this.taskiFiles()
        def taskISize = taskIFiles.size()
        def renames = renamedFiles
        if (renames.empty) renames = ""
        def views = notFoundViews()
        if (views.empty) views = ""
        def filesFromViewAnalysis = filesFromViewAnalysis()
        def viewFileFromTestI = testiViewFiles().size()
        def falsePositives = testIFiles - taskIFiles
        def falseNegatives = taskIFiles - testIFiles
        def hits = testIFiles.intersect(taskIFiles)
        String[] line = [doneTask.id, dates, developers, doneTask.commitsQuantity, doneTask.hashes,
                         testi.foundAcceptanceTests.size(), testi.foundStepDefs.size(), testISize, taskISize, testIFiles,
                         taskIFiles, precision(), recall(), rails, testi.visitCallCounter, testi.lostVisitCall,
                         viewFileFromTestI, filesFromViewAnalysis.size(), filesFromViewAnalysis, configureUnknownMethods(), renames,
                         removedFiles, views, views.size(), testi.timestamp, hasMergeCommit(), falsePositives.size(),
                         falseNegatives.size(), falsePositives, falseNegatives, hits.size(), hits, f2Measure()]
        line
    }

    private void extractStepMatchErrorText() {
        def stepErrors = testi.matchStepErrors
        def stepErrorsQuantity = 0
        def text = ""
        if (stepErrors.empty) text = ""
        else {
            stepErrorsQuantity = stepErrors*.size.flatten().sum()
            stepErrors.each { error ->
                text += "[path:${error.path}, size:${error.size}], "
            }
            text = text.substring(0, text.size() - 2)
        }
        this.stepMatchErrorsText = text
        this.stepMatchErrors = stepErrorsQuantity
    }

    private void extractMultipleStepMatches() {
        this.multipleStepMatchesCounter = testi.multipleStepMatches.size()
        this.multipleStepMatchesText = ""
        testi.multipleStepMatches?.each { msm ->
            this.multipleStepMatchesText += "[path:${msm.path}, text:${msm.text}], "
        }
        if (!this.multipleStepMatchesText.empty) {
            this.multipleStepMatchesText = this.multipleStepMatchesText.substring(0, this.multipleStepMatchesText.size() - 2)
        }
    }

    private void extractGenericStepKeyword() {
        this.genericStepKeywordCounter = testi.genericStepKeyword.size()
        this.genericStepKeywordText = ""
        testi.genericStepKeyword?.each { gsk ->
            this.genericStepKeywordText += "[path:${gsk.path}, text:${gsk.text}], "
        }
        if (!this.genericStepKeywordText.empty) {
            this.genericStepKeywordText = this.genericStepKeywordText.substring(0, this.genericStepKeywordText.size() - 2)
        }
    }

    private void extractCompilationErrorText() {
        def compilationErrors = testi.compilationErrors
        def compErrorsQuantity = 0
        def gherkinQuantity = 0
        def stepsQuantity = 0
        def unitQuantity = 0
        def gherkin = ""
        def steps = ""
        def unit = ""
        if (compilationErrors.empty) compilationErrors = ""
        else {
            gherkin = compilationErrors.findAll { Util.isGherkinFile(it.path) }
            gherkinQuantity = gherkin.size()
            if (gherkin.empty) gherkin = ""
            steps = compilationErrors.findAll { Util.isStepDefinitionFile(it.path) }
            stepsQuantity = steps.size()
            if (steps.empty) steps = ""
            unit = compilationErrors.findAll { Util.isUnitTestFile(it.path) }
            unitQuantity = unit.size()
            if (unit.empty) unit = ""
            compilationErrors -= unit
            compErrorsQuantity = compilationErrors*.msgs.flatten().size()
            compilationErrors = compilationErrors.toString()
        }

        this.compilationErrorsText = compilationErrors
        this.compilationErrors = compErrorsQuantity
        this.gherkinCompilationErrorsText = gherkin
        this.gherkinCompilationErrors = gherkinQuantity
        this.stepDefCompilationErrorsText = steps
        this.stepDefCompilationErrors = stepsQuantity
        this.unitCompilationErrorsText = unit
        this.unitCompilationErrors = unitQuantity
    }

    private configureUnknownMethods() {
        if (TestCodeAbstractAnalyser.apiMethods == null || TestCodeAbstractAnalyser.apiMethods.empty) return methods
        def unknownMethods = []
        methods.each { m ->
            def obj = TestCodeAbstractAnalyser.apiMethods.find { it.name == m }
            if (!obj) unknownMethods += m
        }
        return unknownMethods
    }

}

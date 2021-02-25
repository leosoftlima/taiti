package br.ufpe.cin.tan.analysis.data.csvExporter

import br.ufpe.cin.tan.analysis.AnalysedTask
import br.ufpe.cin.tan.util.CsvUtil
import br.ufpe.cin.tan.util.Util
import groovy.util.logging.Slf4j

@Slf4j
class EvaluationExporter {

    File file
    String url

    List<AnalysedTask> tasks
    List<AnalysedTask> emptyTaskI
    List<AnalysedTask> hasGherkinTest
    List<AnalysedTask> stepMatchError
    List<AnalysedTask> gherkinCompilationErrors
    List<AnalysedTask> compilationErrors
    List<AnalysedTask> stepDefCompilationErrors
    List<AnalysedTask> unitCompilationErrors
    List<AnalysedTask> invalidTasks
    List<AnalysedTask> noRequiredGems
    List<AnalysedTask> validTasks
    List<AnalysedTask> emptyTestI
    List<AnalysedTask> zeroPrecisionAndRecall
    List<AnalysedTask> others
    List<String[]> initialData

    boolean filterEmptyTaskI

    int stepCounter
    int gherkinCounter

    String[] MAIN_HEADER

    EvaluationExporter(String evaluationFile, List<AnalysedTask> tasks) {
        this(evaluationFile, tasks, true)
    }

    EvaluationExporter(String evaluationFile, List<AnalysedTask> tasks, boolean toFilter) {
        def measure1, measure2
        if (Util.SIMILARITY_ANALYSIS) {
            measure1 = "Jaccard"
            measure2 = "Cosine"
        } else {
            measure1 = "Precision"
            measure2 = "Recall"
        }
        MAIN_HEADER = ["Project", "Task", "Date", "#Days", "#Commits", "Commit_Message", "#Devs", "#Gherkin_Tests",
                       "#Impl_Gherkin_Tests", "#StepDef", "#Impl_StepDef", "Methods_Unknown_Type", "#Step_Call",
                       "Step_Match_Errors", "#Step_Match_Error", "AST_Errors", "#AST_Errors", "Gherkin_AST_Errors",
                       "#Gherkin_AST_Errors", "Steps_AST_Errors", "#Steps_AST_Errors", "Renamed_Files",
                       "Deleted_Files", "NotFound_Views", "#Views", "#TestI", "#TaskI", "TestI", "TaskI",
                       measure1, measure2, "Hashes", "Timestamp", "Rails", "Gems", "#Visit_Call",
                       "Lost_visit_call", "#Views_TestI", "#Code_View_Analysis", "Code_View_Analysis", "Has_Merge", "F2",
                       "#Multiple_Step_Matches", "Multiple_Step_Matches", "#Generic_Step_Keyword", "Generic_Step_Keyword"]
        file = new File(evaluationFile)
        this.tasks = tasks
        filterEmptyTaskI = toFilter
        if (tasks && !tasks.empty) url = tasks.first().doneTask.gitRepository.url
        else url = ""
        initializeValues()
        generateSummaryData()
    }

    def save() {
        if (!tasks || tasks.empty) return
        List<String[]> content = initialData
        tasks?.each { content += it.parseAllToArray() }
        CsvUtil.write(file.path, content)
    }

    private initializeValues() {
        validTasks = tasks.findAll { it.isValid() }

        def invalid = tasks - validTasks
        emptyTaskI = invalid.findAll { it.taskiIsEmpty() }
        if (filterEmptyTaskI) tasks -= emptyTaskI

        stepCounter = tasks.findAll { it.hasChangedStepDefs() }.size()
        gherkinCounter = tasks.findAll { it.hasChangedGherkinDefs() }.size()
        hasGherkinTest = tasks.findAll { it.hasImplementedAcceptanceTests() }
        stepMatchError = tasks.findAll { it.hasStepMatchError() }
        compilationErrors = tasks.findAll { it.hasCompilationError() }
        gherkinCompilationErrors = tasks.findAll { it.hasGherkinCompilationError() }
        stepDefCompilationErrors = tasks.findAll { it.hasStepDefCompilationError() }
        unitCompilationErrors = tasks.findAll { it.hasUnitCompilationError() }

        if (filterEmptyTaskI) invalidTasks = invalid
        else invalidTasks = (invalid + emptyTaskI).unique()
        noRequiredGems = invalid.findAll { !it.satisfiesGemsFilter() }
        emptyTestI = validTasks.findAll { it.testiIsEmpty() }
        def noEmptyTestI = validTasks - emptyTestI
        int zero = 0
        zeroPrecisionAndRecall = noEmptyTestI.findAll { it.precision() == zero && it.recall() == zero }
        others = noEmptyTestI - zeroPrecisionAndRecall
    }

    private generateSummaryData() {
        initialData = []
        initialData += ["Repository", url] as String[]
        initialData += ["Tasks with empty TaskI", emptyTaskI.size()] as String[]
        if (filterEmptyTaskI) initialData += ["Tasks with no-empty TaskI", tasks.size()] as String[]
        else initialData += ["Tasks with no-empty TaskI", (tasks - emptyTaskI).size()] as String[]
        initialData += ["Tasks with relevant AST error", compilationErrors.size()] as String[]
        initialData += ["Tasks with AST error of Gherkin files", gherkinCompilationErrors.size()] as String[]
        initialData += ["Tasks with AST error of StepDef files", stepDefCompilationErrors.size()] as String[]
        def productionErrors = compilationErrors.size() - (gherkinCompilationErrors.size() + stepDefCompilationErrors.size())
        initialData += ["Tasks with AST error of production files", productionErrors] as String[]
        initialData += ["Tasks with AST error of unit test files", unitCompilationErrors.size()] as String[]
        initialData += ["Tasks with step match error", stepMatchError.size()] as String[]
        initialData += ["Tasks without required gems", noRequiredGems.size()] as String[]
        initialData += ["Tasks with changed stepdef", stepCounter] as String[]
        initialData += ["Tasks with changed Gherkin", gherkinCounter] as String[]
        initialData += ["Tasks with implemented Gherkin scenarios", hasGherkinTest.size()] as String[]
        initialData += ["All invalid tasks", invalidTasks.size()] as String[]
        initialData += ["All valid tasks (Gherkin scenario, no empty TaskI, no error)", validTasks.size()] as String[]
        initialData += ["Valid tasks, but empty TestI", emptyTestI.size()] as String[]

        def measure
        if (Util.SIMILARITY_ANALYSIS) {
            measure = "similarity"
        } else {
            measure = "precision-recall"
        }

        initialData += ["Valid tasks, no empty TestI, but zero $measure", zeroPrecisionAndRecall.size()] as String[]
        initialData += ["Valid tasks, no empty TestI, no zero $measure", others.size()] as String[]
        initialData += MAIN_HEADER
    }

}

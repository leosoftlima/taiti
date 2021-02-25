package br.ufpe.cin.tan.analysis.data.csvExporter

import br.ufpe.cin.tan.analysis.data.ExporterUtil
import br.ufpe.cin.tan.evaluation.TaskInterfaceEvaluator
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.CsvUtil
import br.ufpe.cin.tan.util.Util

class AggregatedStatisticsExporter {

    String aggregatedStatisticsFile
    def relevantTasksFiles
    def relevantTasksControllerFiles
    def validTasksFiles
    def validTasksControllerFiles
    def correlationTestsPrecision
    def correlationTestsRecall
    def correlationTestIPrecision
    def correlationTestIRecall
    def correlationTestsF2
    def correlationTestIF2
    def correlationTestsFP
    def correlationTestsFN
    def correlationTestIFP
    def correlationTestIFN

    AggregatedStatisticsExporter(String folder) {
        aggregatedStatisticsFile = "${folder}${File.separator}aggregated.csv"
        def output = Util.findFilesFromDirectory(folder)
        if(Util.RUNNING_ALL_CONFIGURATIONS){
            output = output.findAll {
                it.contains("${File.separator}${ConstantData.SELECTED_TASKS_BY_CONFIGS_FOLDER}${File.separator}")
            }
        }
        relevantTasksFiles = output.findAll { it.endsWith(ConstantData.RELEVANT_TASKS_FILE_SUFIX) }
        relevantTasksControllerFiles = output.findAll { it.endsWith("-relevant" + ConstantData.CONTROLLER_FILE_SUFIX) }
        validTasksFiles = output.findAll { it.endsWith(ConstantData.VALID_TASKS_FILE_SUFIX) }
        validTasksControllerFiles = output.findAll { it.endsWith("-valid" + ConstantData.CONTROLLER_FILE_SUFIX) }
    }

    def generateAggregatedStatistics() {
        List<String[]> content = []
        content += ["Relevant tasks files"] as String[]
        content += aggregatedCorrelationTestsPrecisionRecall(relevantTasksFiles)
        content += ["Relevant controller tasks files"] as String[]
        content += aggregatedCorrelationTestsPrecisionRecall(relevantTasksControllerFiles)
        content += ["Valid tasks files"] as String[]
        content += aggregatedCorrelationTestsPrecisionRecall(validTasksFiles)
        content += ["Valid controller tasks files"] as String[]
        content += aggregatedCorrelationTestsPrecisionRecall(validTasksControllerFiles)
        CsvUtil.write(aggregatedStatisticsFile, content)
    }

    private aggregatedCorrelationTestsPrecisionRecall(List<String> files) {
        List<String[]> content = []
        def tests = []
        def precisionValues = []
        def recallValues = []
        def testISize = []
        def fpValues = []
        def fnValues = []
        def f2Values = []

        files?.each { file ->
            List<String[]> entries = CsvUtil.read(file)
            if (entries.size() > ExporterUtil.INITIAL_TEXT_SIZE_SHORT_HEADER) {
                def data = entries.subList(ExporterUtil.INITIAL_TEXT_SIZE_SHORT_HEADER, entries.size())
                tests += data.collect { it[ExporterUtil.IMPLEMENTED_GHERKIN_TESTS] as double }
                testISize += data.collect { it[ExporterUtil.TESTI_SIZE_INDEX_SHORT_HEADER] as double }
                precisionValues += data.collect { it[ExporterUtil.PRECISION_INDEX_SHORT_HEADER] as double }
                recallValues += data.collect { it[ExporterUtil.RECALL_INDEX_SHORT_HEADER] as double }
                fpValues += data.collect { it[ExporterUtil.FP_NUMBER_INDEX] as double }
                fnValues += data.collect { it[ExporterUtil.FN_NUMBER_INDEX] as double }
                f2Values += data.collect { it[ExporterUtil.F2_INDEX] as double }
            }
        }

        tests = tests.flatten()
        testISize = testISize.flatten()
        precisionValues = precisionValues.flatten()
        recallValues = recallValues.flatten()
        fpValues = fpValues.flatten()
        fnValues = fnValues.flatten()
        f2Values = f2Values.flatten()

        def measure1 = "Precision"
        def measure2 = "Recall"

        def text1 = "Correlation #Test-$measure1"
        def text2 = "Correlation #Test-$measure2"
        def text3 = "Correlation #TestI-$measure1"
        def text4 = "Correlation #TestI-$measure2"
        def text5 = "Correlation #Test-F2"
        def text6 = "Correlation #TestI-F2"
        def text7 = "Correlation #Test-FP"
        def text8 = "Correlation #Test-FN"
        def text9 = "Correlation #TestI-FP"
        def text10 = "Correlation #TestI-FN"

        correlationTestsPrecision = TaskInterfaceEvaluator.calculateCorrelation(tests as double[], precisionValues as double[])
        correlationTestsRecall = TaskInterfaceEvaluator.calculateCorrelation(tests as double[], recallValues as double[])
        content += [text1, correlationTestsPrecision.toString()] as String[]
        content += [text2, correlationTestsRecall.toString()] as String[]
        correlationTestIPrecision = TaskInterfaceEvaluator.calculateCorrelation(testISize as double[], precisionValues as double[])
        correlationTestIRecall = TaskInterfaceEvaluator.calculateCorrelation(testISize as double[], recallValues as double[])
        content += [text3, correlationTestIPrecision.toString()] as String[]
        content += [text4, correlationTestIRecall.toString()] as String[]
        correlationTestsF2 = TaskInterfaceEvaluator.calculateCorrelation(tests as double[], f2Values as double[])
        correlationTestIF2 = TaskInterfaceEvaluator.calculateCorrelation(testISize as double[], f2Values as double[])
        content += [text5, correlationTestsF2.toString()] as String[]
        content += [text6, correlationTestIF2.toString()] as String[]
        correlationTestsFP = TaskInterfaceEvaluator.calculateCorrelation(tests as double[], fpValues as double[])
        correlationTestsFN = TaskInterfaceEvaluator.calculateCorrelation(tests as double[], fnValues as double[])
        content += [text7, correlationTestsFP.toString()] as String[]
        content += [text8, correlationTestsFN.toString()] as String[]
        correlationTestIFP = TaskInterfaceEvaluator.calculateCorrelation(testISize as double[], fpValues as double[])
        correlationTestIFN = TaskInterfaceEvaluator.calculateCorrelation(testISize as double[], fnValues as double[])
        content += [text9, correlationTestIFP.toString()] as String[]
        content += [text10, correlationTestIFN.toString()] as String[]
        content
    }

}

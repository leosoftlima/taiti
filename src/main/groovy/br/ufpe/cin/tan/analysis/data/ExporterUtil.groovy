package br.ufpe.cin.tan.analysis.data

import br.ufpe.cin.tan.evaluation.TaskInterfaceEvaluator
import br.ufpe.cin.tan.similarity.test.TestSimilarityAnalyser
import br.ufpe.cin.tan.util.RegexUtil
import br.ufpe.cin.tan.util.Util
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics


class ExporterUtil {

    public static String[] SHORT_HEADER
    public static String[] SHORT_HEADER_PLUS
    public static String[] PLUS_HEADER
    public static final int RECALL_INDEX_SHORT_HEADER
    public static final int PRECISION_INDEX_SHORT_HEADER
    public static final int TASKI_INDEX_SHORT_HEADER
    public static final int TESTI_INDEX_SHORT_HEADER
    public static final int TESTI_SIZE_INDEX_SHORT_HEADER
    public static final int TASKI_SIZE_INDEX_SHORT_HEADER
    public static final int IMPLEMENTED_GHERKIN_TESTS
    public static final int INITIAL_TEXT_SIZE_SHORT_HEADER
    public static final int INITIAL_TEXT_SIZE_NO_CORRELATION_SHORT_HEADER
    public static final int TESTI_VIEWS_SIZE_INDEX_SHORT_HEADER

    public static final int FP_NUMBER_INDEX
    public static final int FN_NUMBER_INDEX
    public static final int FP_INDEX
    public static final int FN_INDEX
    public static final int HITS_NUMBER_INDEX
    public static final int HITS_INDEX
    public static final int F2_INDEX

    static final String measure1, measure2

    static {
        if (Util.SIMILARITY_ANALYSIS) {
            measure1 = "Jaccard"
            measure2 = "Cosine"
        } else {
            measure1 = "Precision"
            measure2 = "Recall"
        }
        SHORT_HEADER = ["Task", "Dates", "#Devs", "#Commits", "Hashes", "#Gherkin_Tests", "#Sted_defs",
                        "#TestI", "#TaskI", "TestI", "TaskI", measure1, measure2, "Rails", "#visit_call",
                        "Lost_visit_call", "#Views_TestI", "#Code_View_Analysis", "Code_View_Analysis",
                        "Methods_Unknown_Type", "Renamed_Files", "Deleted_Files", "NotFound_Views",
                        "#NotFound_Views", "Timestamp", "Has_Merge"]
        PLUS_HEADER = ["#FP", "#FN", "FP", "FN", "#Hits", "Hits", "f2"]
        SHORT_HEADER_PLUS = SHORT_HEADER + PLUS_HEADER
        RECALL_INDEX_SHORT_HEADER = SHORT_HEADER_PLUS.size() - 21
        PRECISION_INDEX_SHORT_HEADER = RECALL_INDEX_SHORT_HEADER - 1
        TASKI_INDEX_SHORT_HEADER = PRECISION_INDEX_SHORT_HEADER - 1
        TESTI_INDEX_SHORT_HEADER = TASKI_INDEX_SHORT_HEADER - 1
        TESTI_SIZE_INDEX_SHORT_HEADER = TESTI_INDEX_SHORT_HEADER - 2
        TASKI_SIZE_INDEX_SHORT_HEADER = TASKI_INDEX_SHORT_HEADER - 2
        IMPLEMENTED_GHERKIN_TESTS = 5
        INITIAL_TEXT_SIZE_SHORT_HEADER = 13
        INITIAL_TEXT_SIZE_NO_CORRELATION_SHORT_HEADER = INITIAL_TEXT_SIZE_SHORT_HEADER - 2
        TESTI_VIEWS_SIZE_INDEX_SHORT_HEADER = 16
        FP_NUMBER_INDEX = SHORT_HEADER_PLUS.size() - PLUS_HEADER.size()
        FN_NUMBER_INDEX = FP_NUMBER_INDEX + 1
        FP_INDEX = FN_NUMBER_INDEX + 1
        FN_INDEX = FP_INDEX + 1
        HITS_NUMBER_INDEX = FN_INDEX + 1
        HITS_INDEX = HITS_NUMBER_INDEX + 1
        F2_INDEX = HITS_INDEX + 1
    }

    static generateStatistics(double[] precisionValues, double[] recallValues, double[] tests, double[] f2Values) {
        int zero = 0
        if (!precisionValues || precisionValues.size() == zero || !recallValues || recallValues.size() == zero) return []
        List<String[]> content = []
        def precisionStats = new DescriptiveStatistics(precisionValues)
        def recallStats = new DescriptiveStatistics(recallValues)
        content += ["$measure1 mean (RT)", precisionStats.mean] as String[]
        content += ["$measure1 median (RT)", precisionStats.getPercentile(50.0)] as String[]
        content += ["$measure1 standard deviation (RT)", precisionStats.standardDeviation] as String[]
        content += ["$measure2 mean (RT)", recallStats.mean] as String[]
        content += ["$measure2 median (RT)", recallStats.getPercentile(50.0)] as String[]
        content += ["$measure2 standard deviation (RT)", recallStats.standardDeviation] as String[]
        def f2Stats = new DescriptiveStatistics(f2Values)
        content += ["F2 mean (RT)", f2Stats.mean] as String[]
        content += ["F2 median (RT)", f2Stats.getPercentile(50.0)] as String[]
        content += ["F2 standard deviation (RT)", f2Stats.standardDeviation] as String[]
        def correlationTestsPrecision = TaskInterfaceEvaluator.calculateCorrelation(tests, precisionValues)
        def correlationTestsRecall = TaskInterfaceEvaluator.calculateCorrelation(tests, recallValues)
        content += ["Correlation #Test-$measure1", correlationTestsPrecision.toString()] as String[]
        content += ["Correlation #Test-$measure2", correlationTestsRecall.toString()] as String[]
        content
    }

    static generateStatistics(double[] precisionValues, double[] recallValues, double[] f2Values) {
        int zero = 0
        if (!precisionValues || precisionValues.size() == zero || !recallValues || recallValues.size() == zero) return []
        List<String[]> content = []
        def precisionStats = new DescriptiveStatistics(precisionValues)
        def recallStats = new DescriptiveStatistics(recallValues)
        content += ["$measure1 mean (RT)", precisionStats.mean] as String[]
        content += ["$measure1 median (RT)", precisionStats.getPercentile(50.0)] as String[]
        content += ["$measure1 standard deviation (RT)", precisionStats.standardDeviation] as String[]
        content += ["$measure2 mean (RT)", recallStats.mean] as String[]
        content += ["$measure2 median (RT)", recallStats.getPercentile(50.0)] as String[]
        content += ["$measure2 standard deviation (RT)", recallStats.standardDeviation] as String[]
        def f2Stats = new DescriptiveStatistics(f2Values)
        content += ["F2 mean (RT)", f2Stats.mean] as String[]
        content += ["F2 median (RT)", f2Stats.getPercentile(50.0)] as String[]
        content += ["F2 standard deviation (RT)", f2Stats.standardDeviation] as String[]
        content
    }

    static computeTaskPairs(set) {
        def result = [] as Set
        if (!set || set.empty || set.size() == 1) return result
        set.eachWithIndex { v, k ->
            def next = set.drop(k + 1)
            result.add([task: v, pairs: next])
        }
        result
    }

    static String[] configureLine(String[] value) {
        def originalTestI = configureITask(value, TESTI_INDEX_SHORT_HEADER)
        def testI = findControllers(originalTestI)
        def originalTaskI = configureITask(value, TASKI_INDEX_SHORT_HEADER)
        def taski = findControllers(originalTaskI)
        def precision, recall
        if (Util.SIMILARITY_ANALYSIS) {
            def similarityAnalyser = new TestSimilarityAnalyser(testI, taski)
            precision = similarityAnalyser.calculateSimilarityByJaccard()
            recall = similarityAnalyser.calculateSimilarityByCosine()
        } else {
            precision = TaskInterfaceEvaluator.calculateFilesPrecision(testI, taski)
            recall = TaskInterfaceEvaluator.calculateFilesRecall(testI, taski)
        }

        def denominator = 4 * precision + recall
        def f2 = 0
        if (denominator != 0) f2 = 5 * ((precision * recall) / denominator)

        def falsePositives = testI - taski
        def falseNegatives = taski - testI
        def hits = testI.intersect(taski)

        String[] line = value
        line[TESTI_SIZE_INDEX_SHORT_HEADER] = testI.size()
        line[TASKI_SIZE_INDEX_SHORT_HEADER] = taski.size()
        line[TESTI_INDEX_SHORT_HEADER] = testI
        line[TASKI_INDEX_SHORT_HEADER] = taski
        line[PRECISION_INDEX_SHORT_HEADER] = precision
        line[RECALL_INDEX_SHORT_HEADER] = recall
        line[TESTI_VIEWS_SIZE_INDEX_SHORT_HEADER] = 0
        line[FP_NUMBER_INDEX] = falsePositives.size()
        line[FN_NUMBER_INDEX] = falseNegatives.size()
        line[FP_INDEX] = falsePositives
        line[FN_INDEX] = falseNegatives
        line[HITS_NUMBER_INDEX] = hits.size()
        line[HITS_INDEX] = hits
        line[F2_INDEX] = f2
        line
    }

    static Set<String> findControllers(Set<String> set) {
        set.findAll { Util.isControllerFile(it) }
    }

    static Set configureITask(String[] value, int index) {
        def originalTestI = value[index].replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, "/")
                .substring(1, value[index].size() - 1)
                .split(",")
                .flatten()
                .collect { it.trim() } as Set
        originalTestI
    }
}

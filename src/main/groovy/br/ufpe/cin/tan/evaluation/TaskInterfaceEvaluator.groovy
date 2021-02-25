package br.ufpe.cin.tan.evaluation

import br.ufpe.cin.tan.analysis.taskInterface.TaskInterface
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation

class TaskInterfaceEvaluator {

    /***
     * Calculates precision of test based task interface considering files only.
     *
     * @param testI task interface based in test code
     * @param taskI task interface computed after task is done
     * @return value between 0 and 1
     */
    static double calculateFilesPrecision(TaskInterface testI, TaskInterface taskI) {
        double result = 0
        if (invalidInput(testI, taskI)) return result
        def testFiles = testI.findFilteredFiles()
        def truePositives = calculateTruePositives(testFiles, taskI.findFilteredFiles())
        if (truePositives > 0) result = (double) truePositives / testFiles.size()
        result
    }

    /***
     * Calculates recall of test based task interface considering files only.
     *
     * @param testI task interface based in test code
     * @param taskI task interface computed after task is done
     * @return value between 0 and 1
     */
    static double calculateFilesRecall(TaskInterface testI, TaskInterface taskI) {
        double result = 0
        if (invalidInput(testI, taskI)) return result
        def realFiles = taskI.findFilteredFiles()
        def truePositives = calculateTruePositives(testI.findFilteredFiles(), realFiles)
        if (truePositives > 0) result = (double) truePositives / realFiles.size()
        result
    }

    static double calculateFilesPrecision(Set testI, Set taskI) {
        double result = 0
        if (invalidInput(testI, taskI)) return result
        def truePositives = calculateTruePositives(testI, taskI)
        if (truePositives > 0) result = (double) truePositives / testI.size()
        result
    }

    static double calculateFilesRecall(Set testI, Set taskI) {
        double result = 0
        if (invalidInput(testI, taskI)) return result
        def truePositives = calculateTruePositives(testI, taskI)
        if (truePositives > 0) result = (double) truePositives / taskI.size()
        result
    }

    static Double calculateCorrelation(double[] independent, double[] dependent) {
        if (!independent || !dependent || independent.length < 2 || dependent.length < 2) return null

        SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation()
        Double value = spearmansCorrelation.correlation(independent, dependent)
        if (Double.isNaN(value)) null
        else Math.round(value * 100) / 100
    }

    private static calculateTruePositives(Set set1, Set set2) {
        (set1.intersect(set2)).size()
    }

    private static invalidInput(testI, taskI) {
        if (!testI || testI.empty || !taskI || taskI.empty) true
        else false
    }

}

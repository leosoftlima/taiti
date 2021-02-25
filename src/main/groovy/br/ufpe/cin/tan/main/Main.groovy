package br.ufpe.cin.tan.main

import br.ufpe.cin.tan.analysis.TaskAnalyser
import br.ufpe.cin.tan.analysis.data.csvExporter.AggregatedStatisticsExporter
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util
import groovy.util.logging.Slf4j

@Slf4j
class Main {

    int limit

    Main(taskLimit) {
        this.limit = taskLimit
    }

    static void main(String[] args) {
        int limit = -1
        if (args) limit = Integer.parseInt(args[0])
        Main mainObj = new Main(limit)

        if (Util.MULTIPLE_TASK_FILES) mainObj.runAnalysisForMultipleFiles()
        else mainObj.runAnalysis(Util.TASKS_FILE)
    }

    private TaskAnalyser runAnalysis(String inputTasksFile) {
        def taskAnalyser = new TaskAnalyser(inputTasksFile, limit)
        taskAnalyser.analyseAll()
        taskAnalyser
    }

    private runAnalysisForMultipleFiles() {
        def cvsFiles = Util.findTaskFiles()
        cvsFiles?.each {
            runAnalysis(it)
        }
        AggregatedStatisticsExporter statisticsExporter = new AggregatedStatisticsExporter(ConstantData.DEFAULT_EVALUATION_FOLDER)
        statisticsExporter.generateAggregatedStatistics()
    }

}

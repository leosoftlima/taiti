package br.ufpe.cin.tan.test

import br.ufpe.cin.tan.analysis.taskInterface.TestI


trait TestCodeVisitorInterface {

    abstract TestI getTaskInterface()

    abstract void setLastVisitedFile(String path)

    abstract List<StepCall> getCalledSteps()

    MethodToAnalyse getStepDefinitionMethod() {
        null
    }

    int getVisitCallCounter(){
        0
    }

    Set getLostVisitCall(){
        [] as Set
    }

    Set<MethodBody> getMethodBodies() {
        []
    }

}
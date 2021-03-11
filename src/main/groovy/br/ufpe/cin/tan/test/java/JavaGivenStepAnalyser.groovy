package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.analysis.taskInterface.TestI
import br.ufpe.cin.tan.test.MethodToAnalyse
import br.ufpe.cin.tan.util.Util
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.CompilationUnit;

/***
 * Visits step definitions referenced by given steps.
 * It is needed when the "when filter" is enabled.
 * It is responsible for identifying visit calls. It ignores any other method call.
 */
class JavaGivenStepAnalyser {

    JavaTestCodeVisitor filteredVisitor
    JavaTestCodeVisitor auxVisitor
    JavaTestCodeVisitor methodCallVisitor

    JavaGivenStepAnalyser(JavaTestCodeVisitor methodCallVisitor) {
        this.methodCallVisitor = methodCallVisitor
    }
   
     def analyse(Node node, MethodToAnalyse method) {
        reset()

        filteredVisitor.stepDefinitionMethod = method
        node.accept(filteredVisitor)

        auxVisitor.stepDefinitionMethod = method
        node.accept(auxVisitor)

        organizeAnalysisResult(filteredVisitor, auxVisitor)
    }

    def analyse(MethodDeclaration methodDeclaration, step) {
        reset()
        filteredVisitor.step = step
        methodDeclaration.accept(filteredVisitor)
        filteredVisitor.step = null
        methodDeclaration.accept(auxVisitor)
        organizeAnalysisResult(filteredVisitor, auxVisitor)
    }

     private reset() {
        filteredVisitor = new JavaTestCodeVisitor(methodCallVisitor.projectFiles, methodCallVisitor.lastVisitedFile,
                methodCallVisitor.projectMethods)
        filteredVisitor.filteredAnalysis = true
        auxVisitor = new JavaTestCodeVisitor(methodCallVisitor.projectFiles, methodCallVisitor.lastVisitedFile,
                methodCallVisitor.projectMethods)
    }

    private organizeAnalysisResult(JavaTestCodeVisitor filteredVisitor, JavaTestCodeVisitor auxVisitor) {
        TestI diff = auxVisitor.taskInterface.minus(filteredVisitor.taskInterface)
        def calledSteps = (filteredVisitor.calledSteps + auxVisitor.calledSteps).unique()
        def testMethods = diff.methods.findAll { (it.file != null && Util.isTestFile(it.file)) }
        methodCallVisitor.calledSteps += calledSteps
        methodCallVisitor.stepCallCounter = methodCallVisitor.calledSteps.size()
        methodCallVisitor.taskInterface.methods += testMethods
        methodCallVisitor.taskInterface.collapseInterfaces(filteredVisitor.taskInterface)
    }

}
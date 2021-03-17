package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.test.MethodToAnalyse
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.CompilationUnit;
import br.ufpe.cin.tan.util.java.JavaUtil

/***
 * Visita step definitions de interesse buscando chamadas à métodos de aplicação.
 */
class JavaStepsFileVisitor extends VoidVisitorAdapter<Void>{

    List<MethodToAnalyse> methods
    List lines
    JavaTestCodeVisitor methodCallVisitor
    List<String> fileContent
    List<String> body
    List analysedLines

    JavaStepsFileVisitor(List<MethodToAnalyse> methodsToAnalyse, JavaTestCodeVisitor methodCallVisitor, List<String> fileContent) {
        this.lines = methodsToAnalyse*.line
        this.methods = methodsToAnalyse
        this.methodCallVisitor = methodCallVisitor
        this.fileContent = fileContent
        this.body = []
        this.analysedLines = []
    }

    /**
     * NodeList represents a method call with self as an implicit receiver. Step code are identified here.
     */
    @Override
    void visit(MethodDeclaration methodDeclaration, Void args) {
        super.visit(methodDeclaration, args)
        if (methodDeclaration.getRange().get().begin.line in lines) {
            def matches = methods.findAll { it.line == methodDeclaration.getRange().get().begin.line }
            matches?.each { method ->
                extractMethodBody(methodDeclaration)
                if (Util.WHEN_FILTER) filteredAnalysis(methodDeclaration, method)
                else noFilteredAnalysis(methodDeclaration, method)
            }
        }
    }

    private filteredAnalysis(MethodDeclaration node, MethodToAnalyse method) {
        switch (method.type) {
            case ConstantData.GIVEN_STEP_EN:
                JavaGivenStepAnalyser givenStepAnalyser = new JavaGivenStepAnalyser(methodCallVisitor)
                givenStepAnalyser.analyse(node, method)
                break
            case ConstantData.WHEN_STEP_EN: //common analysis
                noFilteredAnalysis(node, method)
            //we do not analyse "then" step
        }
    }

    private noFilteredAnalysis(MethodDeclaration node, MethodToAnalyse method) {
        methodCallVisitor.stepDefinitionMethod = method
        node?.accept(methodCallVisitor, null)
        methodCallVisitor.stepDefinitionMethod = null
    }

    private extractMethodBody(MethodDeclaration node) {
        if (!(node.getRange().get().begin in analysedLines)) {
            def methodBody = fileContent.getAt([node.getRange().get().begin.line..node.getRange().get().end.line])
            body += methodBody
            analysedLines += ((MethodDeclaration) node).getRange().get().begin.line
        }
    }

}

package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util

import com.github.javaparser.ast.body.CallableDeclaration.Signature;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.CompilationUnit;
import br.ufpe.cin.tan.util.java.JavaUtil

/***
 * Visits methods body searching for other method calls. It should be used associated to JavaTestCodeVisitor.
 */
class JavaMethodVisitor extends VoidVisitorAdapter<Void>{


    List<String> fileContent
    List<String> body
    def methods
    JavaTestCodeVisitor methodBodyVisitor

    JavaMethodVisitor(List methods, JavaTestCodeVisitor methodBodyVisitor, List<String> fileContent) {
        this.methods = methods
        this.methodBodyVisitor = methodBodyVisitor
        this.fileContent = fileContent
        this.body = []
    }
  @Override
    void visit(MethodDeclaration methodDeclaration, Void args) {
        super.visit(methodDeclaration, args)
        def foundMethod = methods.find { it.name == methodDeclaration.getSignature() }
        if (foundMethod) {
            extractMethodBody(methodDeclaration)
            if (Util.WHEN_FILTER && foundMethod.step == ConstantData.GIVEN_STEP_EN) {
                JavaGivenStepAnalyser givenStepAnalyser = new JavaGivenStepAnalyser(methodBodyVisitor)
                givenStepAnalyser.analyse(node, foundMethod.step)
            } else if (Util.WHEN_FILTER && foundMethod.step == ConstantData.THEN_STEP_EN) return
            else {
                methodBodyVisitor.step = foundMethod.step
                methodDeclaration?.accept(methodBodyVisitor, null)
            }
        }
    }

    private extractMethodBody(MethodDeclaration methodDeclaration) {
        def range = [methodDeclaration.getRange().get().begin.line..methodDeclaration.getRange().get().end.line]
        def methodBody = fileContent.getAt(range)
        body += methodBody
    }

}

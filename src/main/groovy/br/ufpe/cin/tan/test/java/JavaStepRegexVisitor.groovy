package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.util.ConstantData
import com.github.javaparser.ast.Node;
import java.nio.charset.StandardCharsets

/***
 * Visits step definition files looking for regex expressions. The regex is used to match steps in Gherkin files and
 * step definitions.
 */
class JavaStepRegexVisitor extends extends VoidVisitorAdapter<Void>{

    List<StepRegex> regexs
    String path

    JavaStepRegexVisitor(String path) {
        this.path = path
        regexs = []
    }

    
    private static boolean isStepDefinitionNode(RegexpNode node) {
        if (node instanceof MethodDeclaration && node.grand((MethodDeclaration) node).getAnnotation(0).getNameAsString() in
         ConstantData.ALL_STEP_KEYWORDS) true
        else false
    }

    @Override
    Object visitRegexpNode(Node node) {
        super.visit(node)
        if (isStepDefinitionNode(node)) {
            def stepdefType = ((MethodDeclaration) node).getAnnotation(0).getNameAsString();
            regexs += new StepRegex(path: path, value: new String(((MethodDeclaration) node).getAnnotation(0), StandardCharsets.UTF_8),
                    line: 0, keyword: stepdefType)
        }
        return iVisited
    }

}

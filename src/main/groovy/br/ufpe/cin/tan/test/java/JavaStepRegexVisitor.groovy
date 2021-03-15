package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.java.JavaUtil
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.ast.Node
import java.nio.charset.StandardCharsets
import com.github.javaparser.ast.CompilationUnit;

/***
 * Visits step definition files looking for regex expressions. The regex is used to match steps in Gherkin files and
 * step definitions.
 */
class JavaStepRegexVisitor extends VoidVisitorAdapter<Void>{

    List<StepRegex> regexs
    String path

    JavaStepRegexVisitor(String path) {
        this.path = path
        regexs = []
    }

    
    private static boolean isStepDefinitionNode(Node node) {
        if (node instanceof MethodDeclaration && ((MethodDeclaration) node).getAnnotation(0).getNameAsString() in
         ConstantData.ALL_STEP_KEYWORDS) true
        else false
    }

  @Override
    public void visit(CompilationUnit compilationUnit, Void args) {
        super.visit(compilationUnit, args);
      JavaUtil.getAllNodes(compilationUnit).each { node ->
           if(node instanceof MethodDeclaration){  
             if (isStepDefinitionNode(node)) {
              def stepdefType = ((MethodDeclaration) node).getAnnotation(0).getNameAsString();
              regexs += new StepRegex(path: path, value: new String(((MethodDeclaration) node).getAnnotation(0), StandardCharsets.UTF_8),
                    line: 0, keyword: stepdefType)
             }
           }
        }
      }  

    Object visitRegexpNode(Node node) {
        super.visit(node)
        if (isStepDefinitionNode(node)) {
            def stepdefType = ((MethodDeclaration) node).getAnnotation(0).getNameAsString();
            regexs += new StepRegex(path: path, value: new String(((MethodDeclaration) node).getAnnotation(0), StandardCharsets.UTF_8),
                    line: 0, keyword: stepdefType)
             }
    }        
}

package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.java.JavaUtil
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

import java.nio.charset.StandardCharsets

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

    
    private static boolean isStepDefinitionNode(MethodDeclaration node) {
        if (node instanceof MethodDeclaration && ((MethodDeclaration) node).getAnnotation(0).getNameAsString() in
         ConstantData.ALL_STEP_KEYWORDS) true
        else false
    }

  @Override
  void visit(MethodDeclaration methodDeclaration, Void args) {
      super.visit(methodDeclaration, args)
      if (isStepDefinitionNode(methodDeclaration)) {
          def annotation = methodDeclaration?.getAnnotation(0)
          def stepdefType = annotation?.getNameAsString()
          def value = annotation?.childNodes?.get(1) as String
          def index1 = value.indexOf('"')
          def index2 = value.lastIndexOf('"')
          if(index1>-1 && index2>-1) value = value.substring(index1+1, index2)
          else value = ""
          def position = 0
          if (annotation.begin.isPresent()) position = annotation.begin.get().line
          regexs += new StepRegex(path: path, value: value, line: position, keyword: stepdefType)
      }
  }

}

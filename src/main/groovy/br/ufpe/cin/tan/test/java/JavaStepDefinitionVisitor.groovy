package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.java.JavaUtil
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import groovy.util.logging.Slf4j

/***
 * Visits source code files looking for step definitions. It is used when a commit changed a step definition without
 * changed any Gherkin file.
 */
@Slf4j
class JavaStepDefinitionVisitor extends VoidVisitorAdapter<Void>{
    String path
    List<String> content
    List<StepDefinition> stepDefinitions

    JavaStepDefinitionVisitor(String path, String content) {
        this.path = path
        this.content = content.readLines()
        stepDefinitions = []
    }

    @Override
    void visit(MethodDeclaration methodDeclaration, Void args) {
        super.visit(methodDeclaration, args)

        String keyword = methodDeclaration?.getAnnotation(0)?.nameAsString //get Name annotation node with keyword
        if (keyword && !keyword.empty &&keyword in ConstantData.ALL_STEP_KEYWORDS) {
            JavaStepRegexVisitor regexVisitor = new JavaStepRegexVisitor(path)
            methodDeclaration?.accept(regexVisitor, null)
            if (!regexVisitor.regexs.empty) {
                def regex = regexVisitor.regexs.first()
                def value = regex.value   // return "ex.: @Then (value = "The order {long} is fully matched.")
                def startLine = methodDeclaration?.getRange()?.get()?.begin
                def endLine = methodDeclaration?.getRange()?.get()?.end
                def bodyStatements = methodDeclaration?.getBody()?.get()?.asBlockStmt()?.statements
                def body = bodyStatements.collect{ it.properties.get("expression") as String } as List<String>
                if (!body.empty) {
                    stepDefinitions += new StepDefinition(path: path, value: value, regex: regex.value, line: startLine.line,
                            end: endLine.line, body: body, keyword: keyword)
                }
            }
        }
    }

}
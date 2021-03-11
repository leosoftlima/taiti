package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.util.ConstantData
import groovy.util.logging.Slf4j

import java.util.LinkedList;
import java.util.List;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;


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

 private static ignoreArgs(String value) {
        if (!value && value.empty) return ""
        def result = ""
        def pipeIndex1 = value.indexOf("|")
        if (pipeIndex1 < 0) result = value
        else {
            def aux = value.substring(pipeIndex1 + 1)
            def pipeIndex2 = aux.indexOf("|")
            if (pipeIndex2 < 0) result = value
            else result = aux.substring(pipeIndex2 + 1)
        }
        result.trim()
    }

    private static extractBodyStyle1(String text) {
        def init = " do"
        def end = "end"
        def index1 = text.indexOf(init)
        def index2 = text.lastIndexOf(end)
        if (index1 < 0 || index2 < 0) return null
        def i = index1 + (init.size() - 1)
        def value = text.substring(i, index2)
        ignoreArgs(value)
    }

    private static extractBodyStyle2(String text) {
        def index1 = text.indexOf("{")
        def index2 = text.lastIndexOf("}")
        if (index1 > 0 && index2 > 0) {
            def i = index1 + 1
            def value = text.substring(i, index2)
            ignoreArgs(value)
        } else null
    }

    private extractNoLineBody(int startLine) {
        def body = []
        def text = content.get(startLine)
        def aux = extractBodyStyle1(text)
        if (aux == null) aux = extractBodyStyle2(text)
        if (aux != null) body += aux
        else {
            log.error "Error to extract body from step (File: $path, start line:${startLine + 1})"
        }
        body
    }
  
    @Override
    public void visit(CompilationUnit compilationUnit, Void args) {
        	super.visit(compilationUnit, args);
           // find all nodes tree the instance Method and gets referentes keywords Gherkins(Ex.: given, when, then, and.)
             JavaUtil.getAllNodes(compilationUnit).each {node -> 
	        	if(node instanceof MethodDeclaration) {	
                   String keyword = ((MethodDeclaration) node).getAnnotation(0).getNameAsString() //get Name annotation node with keywork

                   if (keyword in ConstantData.ALL_STEP_KEYWORDS) {  
                      JavaStepRegexVisitor regexVisitor = new JavaStepRegexVisitor(path);
                      compilationUnit.accept(regexVisitor);
                        if (!regexVisitor.regexs.empty) {
                           def regex = regexVisitor.regexs.first()  //get first elment array stepRegex.
                           def value =  regex.value   // return "ex.: @Then (value = "The order {long} is fully matched.")
                           def startLine = ((MethodDeclaration) node).getRange().get().begin //get position begin line code
                           def endLine = ((MethodDeclaration) node).getRange().get().end // get position end line code
                           def body = ((MethodDeclaration) n).getBody().get().asBlockStmt().toString() // get content block code for keyword reference

                            if (!body.empty) {
                                //Set information StepDefinition with context cenario referents keywords
                               stepDefinitions += new StepDefinition(path: path, value: value, regex: regex.value,
                               line: startLine, end: endLine, body: body, keyword: keyword)
                            }
                      }
                   }         
                }
             }
    }

}
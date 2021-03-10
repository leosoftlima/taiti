package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.analysis.taskInterface.CalledMethod
import br.ufpe.cin.tan.analysis.taskInterface.TestI
import br.ufpe.cin.tan.test.MethodToAnalyse
import br.ufpe.cin.tan.test.StepCall
import br.ufpe.cin.tan.test.TestCodeVisitorInterface
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util
import br.ufpe.cin.tan.util.java.JavaConstantData
import br.ufpe.cin.tan.util.java.JavaUtil
import groovy.util.logging.Slf4j
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.CompilationUnit;

@Slf4j
class JavaTestCodeVisitor extends VoidVisitorAdapter<Void> implements TestCodeVisitorInterface {

    TestI taskInterface
    List<String> projectFiles
    List<String> viewFiles
    String lastVisitedFile
    Set projectMethods //keys: name, args, path; all methods from project
    def applicationClass //keys: name, path; used when visiting RSpec files; try a better way to represent it!

    List<StepCall> calledSteps
    static int stepCallCounter

    MethodToAnalyse stepDefinitionMethod
    String step
    boolean filteredAnalysis

    Set<MethodBody> methodBodies

    int visitCallCounter
    Set lostVisitCall //keys: path, line

    JavaTestCodeVisitor(String currentFile) { //test purpose only
        taskInterface = new TestI()
        lastVisitedFile = currentFile
        calledSteps = []
        lostVisitCall = [] as Set
        methodBodies = [] as Set
        projectFiles = []
        viewFiles = []
        projectMethods = [] as Set
    }

    JavaTestCodeVisitor(List<String> projectFiles, String currentFile, Set methods) {
        this(currentFile)
        this.projectFiles = projectFiles
        viewFiles = projectFiles.findAll { it.contains(Util.VIEWS_FILES_RELATIVE_PATH + File.separator) }
        projectMethods = methods
    }

    private static int countArgsMethodCall(Node node) {
        def counter = 0
        if(node instanceof MethodDeclaration) {
            counter=  ((MethodDeclaration) node).getParameters().size();
        }
        counter
    }

    def searchForMethodMatch(Node node) {
        def matches = []
        def argsCounter = countArgsMethodCall(node)
        if(node instanceof MethodDeclaration) {
          matches = projectMethods.findAll {
            it.name == ((MethodDeclaration) node).getSignature() && argsCounter <= it.args && argsCounter >= it.args - it.optionalArgs
          }
        }
        matches
    }

    def searchForMethodMatch(String method, int argsCounter) {
        def matches = []
        matches = projectMethods.findAll {
            it.name == method && argsCounter <= it.args && argsCounter >= it.args - it.optionalArgs
        }
        matches
    }

 
    private configureStep() {
        if (stepDefinitionMethod) stepDefinitionMethod.type
        else step
    }
    def registryClassUsageUsingFilename(List<String> paths) {
        paths.each { path ->
            if (path?.contains(Util.VIEWS_FILES_RELATIVE_PATH)) {
                def index = path?.lastIndexOf(File.separator)
                taskInterface.classes += [name: path?.substring(index + 1), file: path, step: configureStep()]
            } else {
                taskInterface.classes += [name: JavaUtil.getClassName(path), file: path, step: configureStep()]
            }
        }
    }

    private registryStepCall(Node node) {
        taskInterface.methods += new CalledMethod(name: ((MethodDeclaration) node).getSignature(), type: "StepCall", file: "${++stepCallCounter}",
                step: configureStep())

        def lines = methodDeclaration.getParameters().toString();
       
        if (lines.empty) return
        registryCall(lines, node)       
    }

    private registryCall(lines, Node node) {
        lines.each { step ->
            if (!step.empty) {
                def keyword = ConstantData.STEP_KEYWORDS.find { step.startsWith(it) }
                if (keyword) step = step.replaceFirst(keyword, "").trim()
                calledSteps += new StepCall(text: step, path: lastVisitedFile, line: ((MethodDeclaration) node).getRange().get().begin, parentType: configureStep())
            }
        }
    }
    
     @Override
    public void visit(CompilationUnit compilationUnit, Void args) {
      	super.visit(compilationUnit, args);
        JavaUtil.getAllNodes(compilationUnit).forEach(node -> {
         if(node instanceof MethodDeclaration){
             registryStepCall(node)
         }
      }
    }

}

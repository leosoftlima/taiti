package br.ufpe.cin.tan.test.java
​
import br.ufpe.cin.tan.analysis.taskInterface.CalledMethod
import br.ufpe.cin.tan.analysis.taskInterface.TestI
import br.ufpe.cin.tan.test.MethodToAnalyse
import br.ufpe.cin.tan.test.StepCall
import br.ufpe.cin.tan.test.TestCodeVisitorInterface
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util
import br.ufpe.cin.tan.test.ruby.MethodBody
import br.ufpe.cin.tan.util.java.JavaConstantData
import br.ufpe.cin.tan.util.java.JavaUtil
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.MethodReferenceExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.modules.ModuleOpensDirective
import com.github.javaparser.ast.modules.ModuleUsesDirective
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt
import com.github.javaparser.ast.type.VarType
import groovy.util.logging.Slf4j
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.CompilationUnit;
​
@Slf4j
class JavaTestCodeVisitor extends VoidVisitorAdapter<Void> implements TestCodeVisitorInterface {
​
    TestI taskInterface
    List<String> projectFiles //todos os arquivos do projeto (em sua versão masi atual, na perspectiva da tarefa)
    List<String> viewFiles //todos as views existentes no projeto (em sua versão mais atual na perspectiva da tarefa)
    String lastVisitedFile //o arquivo em análise no momento
    Set projectMethods //keys: name, args, path; todos os métodos do projeto
​
    List<StepCall> calledSteps //chamadas à steps Gherkin no corpo de um step definition
    static int stepCallCounter //contador de step calls
​
    MethodToAnalyse stepDefinitionMethod
    String step
    boolean filteredAnalysis
​
    Set<MethodBody> methodBodies
​
    JavaTestCodeVisitor(String currentFile) { //test purpose only
        taskInterface = new TestI()
        lastVisitedFile = currentFile
        calledSteps = []
        methodBodies = [] as Set
        projectFiles = []
        viewFiles = []
        projectMethods = [] as Set
    }
​
    JavaTestCodeVisitor(List<String> projectFiles, String currentFile, Set methods) {
        this(currentFile)
        this.projectFiles = projectFiles
        viewFiles = projectFiles.findAll { it.contains(Util.VIEWS_FILES_RELATIVE_PATH + File.separator) }
        projectMethods = methods
    }
​
    private static int countArgsMethodCall(Node node) {
        def counter = 0
        if(node instanceof MethodDeclaration) {
            counter=  ((MethodDeclaration) node).getParameters().size();
        }
        counter
    }
​
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
​
    def searchForMethodMatch(String method, int argsCounter) {
        def matches = []
        matches = projectMethods.findAll {
            it.name == method && argsCounter <= it.args && argsCounter >= it.args - it.optionalArgs
        }
        matches
    }
​
    private configureStep() {
        if (stepDefinitionMethod) stepDefinitionMethod.type
        else step
    }
​
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
​
    /*
     Representa uma chamada de método sobre um objeto ou classe.
    * */
    @Override
    void visit(MethodCallExpr n, Void args){
        super.visit(n, args)
​
    }
​
    /*
     Representa uma referência de método, algo que pode ser usado em expressões lambda.
     Exemplo: String s = "Hello, Instance Method!";
              int t = tamanho(s::length);
              Aqui é chamado o método tamanho passando como parâmetro o método length do objeto String s
              a referência de método seria s::length
    * */
    void visit(MethodReferenceExpr n, Void args){
        super.visit(n, args)
        Expression scope = n.getScope();
	    String identifier = n.getIdentifier();
	    if (scope != null) {
	        n.getScope().accept(this, arg);
	    }

	    if (identifier != null) {
	     //  System.out.println(identifier);
	    }
    }
​
    /*
    Representa uma chamada à um construtor.
    Talvez seja dispensável, pois se o objeto é criado, em algum momento será feito uso dele e isso será capturado.
    * */
    void visit(ObjectCreationExpr n, Void args){
        super.visit(n, args)
    }
​
    /* Acesso à classe de um tipo. Exemplo: Object.class */
    void visit(ClassExpr n, Void args){
        super.visit(n, args)
    }
​
    /* Acesso à um campo de um objeto ou classe. Exemplo: pessoa.nome.
    * No caso, o interesse estaria em saber que o objeto pessoa foi manipulado, de forma análoga à chamada de método*/
    void visit(FieldAccessExpr n, Void args){
        super.visit(n, args)
    }
​
}
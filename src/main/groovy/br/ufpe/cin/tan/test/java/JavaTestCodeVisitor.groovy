package br.ufpe.cin.tan.test.java

import br.ufpe.cin.tan.analysis.taskInterface.CalledMethod
import br.ufpe.cin.tan.analysis.taskInterface.TestI
import br.ufpe.cin.tan.test.MethodBody
import br.ufpe.cin.tan.test.MethodToAnalyse
import br.ufpe.cin.tan.test.StepCall
import br.ufpe.cin.tan.test.TestCodeVisitorInterface
import br.ufpe.cin.tan.util.Util
import br.ufpe.cin.tan.util.java.JavaUtil
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import groovy.util.logging.Slf4j

@Slf4j
class JavaTestCodeVisitor extends VoidVisitorAdapter<Void> implements TestCodeVisitorInterface {

    TestI taskInterface
    List<String> projectFiles //todos os arquivos do projeto (em sua versão masi atual, na perspectiva da tarefa)
    List<String> viewFiles //todos as views existentes no projeto (em sua versão mais atual na perspectiva da tarefa)
    String lastVisitedFile //o arquivo em análise no momento
    Set projectMethods //keys: name, args, path; todos os métodos do projeto

    List<StepCall> calledSteps //chamadas à steps Gherkin no corpo de um step definition
    static int stepCallCounter //contador de step calls

    MethodToAnalyse stepDefinitionMethod
    String step
    boolean filteredAnalysis

    Set<MethodBody> methodBodies

    JavaTestCodeVisitor(String currentFile) { //test purpose only
        taskInterface = new TestI()
        lastVisitedFile = currentFile
        calledSteps = []
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

    private configureStep() {
        if (stepDefinitionMethod) stepDefinitionMethod.type
        else step
    }

    /*
     Representa uma chamada de método sobre um objeto ou classe.
    * */
    @Override
    void visit(MethodCallExpr n, Void args){
        super.visit(n, args)
        def name = n.name
        def receiverIsPresent = n.scope.present
        def receiver
        def paths = []

        if(receiverIsPresent){
            try{
                ResolvedType resolvedType = JavaParserFacade.get(
                        new JavaParserTypeSolver(new File("src"))).getType(n.scope.get())
                receiver = resolvedType.describe()
                paths = JavaUtil.getClassPathForJavaClass(receiver, projectFiles)
            } catch (Exception ignored){ //o receptor da chamada não existe no projeto
                return //o método chamado não é de interesse, então a execução encerra
            }
        } else { //receiver is this
            paths += lastVisitedFile
            receiver = JavaUtil.getClassName(lastVisitedFile)
        }

        paths.each{
            taskInterface.methods += new CalledMethod(name: name, type: receiver, file: it, step: configureStep())
        }

    }

    /*
     Representa uma referência de método, algo que pode ser usado em expressões lambda.
     Exemplo: String s = "Hello, Instance Method!";
              int t = tamanho(s::length);
              Aqui é chamado o método tamanho passando como parâmetro o método length do objeto String s
              a referência de método seria s::length
    * */
    void visit(MethodReferenceExpr n, Void args){
        super.visit(n, args)
    }

    /*
    Representa uma chamada à um construtor.
    Talvez seja dispensável, pois se o objeto é criado, em algum momento será feito uso dele e isso será capturado.
    * */
    void visit(ObjectCreationExpr n, Void args){
        super.visit(n, args)
        def paths = JavaUtil.getClassPathForJavaClass(n.typeAsString, projectFiles)
        paths.each{path ->
            taskInterface.classes += [name: n.typeAsString, file: path, step: configureStep()]
        }
    }

    /* Acesso à classe de um tipo. Exemplo: Object.class */
    void visit(ClassExpr n, Void args){
        super.visit(n, args)
        def paths = JavaUtil.getClassPathForJavaClass(n.typeAsString, projectFiles)
        paths.each{path ->
            taskInterface.classes += [name: n.typeAsString, file: path, step: configureStep()]
        }
    }

    /* Acesso à um campo de um objeto ou classe. Exemplo: pessoa.nome.
    * No caso, o interesse estaria em saber que o objeto pessoa foi manipulado, de forma análoga à chamada de método*/
    void visit(FieldAccessExpr n, Void args){
        super.visit(n, args)

        def paths = JavaUtil.getClassPathForJavaClass(n.scope.toString(), projectFiles)
        paths.each{path ->
            taskInterface.classes += [name: n.scope.toString(), file: path, step: configureStep()]
        }
    }

}
package br.ufpe.cin.tan.test.java


import com.github.javaparser.ast.body.CallableDeclaration.Signature
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.util.HashSet;
import java.util.Set;
import com.github.javaparser.ast.CompilationUnit;

/***
 * Finds all method definition in a file.
 */
class JavaMethodDefinitionVisitor extends VoidVisitorAdapter<Void>{

	Set methods //keys: name, args, optionalArgs, path
	String path

	JavaMethodDefinitionVisitor(String path){
		this.path = path
		methods = []
	}

	@Override
	void visit(MethodDeclaration methodDeclaration, Void args) {
		super.visit(methodDeclaration, args)
		def name = methodDeclaration.nameAsString
		def arguments = methodDeclaration?.parameters?.toList()
		def hasVarArg = arguments.findAll { it.isVarArgs() }?.size() > 0
		methods += [name:name, args:arguments?.size(), optionalArgs:0, path:path, hasVarArg:hasVarArg]
		/*Com varargs, a lista de parâmetros tem tamanho indefinido, não lidamos com isso. Do jeito que está aqui,
		* o vararg conta como sendo 1 parâmetro só e caso o método seja chamado com mais de um valor como argumento,
		* não haverá compatibilidade entre declaração e chamada.*/
    }

}
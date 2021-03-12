package br.ufpe.cin.tan.test.java

import com.github.javaparser.ast.body.CallableDeclaration.Signature
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

/***
 * Finds all method definition in a file.
 */
class JavaMethodDefinitionVisitor extends VoidVisitorAdapter<Void>{
   	Set<Signature> signatures = new HashSet<Signature>();
	
	@Override
	public void visit(MethodDeclaration methodDeclaration, Void args) {
		super.visit(methodDeclaration, args);
        this.signatures.add(methodDeclaration.getSignature());
    }

	public Object getMethodDefinitionAsString() {
		return this.signatures.toString();
	}

	public Set<Signature> getMethodsDefinitions(){
		return this.signatures;
	}

}
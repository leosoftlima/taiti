package br.ufpe.cin.tan.test.java


class TestingJavaVisitor {

    static void main(String[] args){
        def file = "CalculadoraBasica.java"
        JavaTestCodeAnalyser analyser = new JavaTestCodeAnalyser(null, null)
        def node = analyser.generateAst(file)
        JavaTestCodeVisitor visitor = new JavaTestCodeVisitor(file)
        node?.accept(visitor, null)
    }

}

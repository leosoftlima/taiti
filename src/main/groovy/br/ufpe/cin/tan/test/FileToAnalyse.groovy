package br.ufpe.cin.tan.test

/***
 * Representa um arquivo cujos métodos precisam ser visitados a fim de ser possível calcular TestI.
 */
class FileToAnalyse {

    /***
     * A localização do arquivo.
     */
    String path

    /***
     * Métodos a serem analisados, basicamente step definitions e métodos auxiliares.
     */
    List<MethodToAnalyse> methods

    @Override
    String toString() {
        def text = "File to analyse: $path\n"
        methods.sort { it.line }.each { text += it.toString() + "\n" }
        text
    }

}

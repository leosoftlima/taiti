package br.ufpe.cin.tan.test

/***
 * Representa uma chamada à um step Gherkin feita a partir de um step definition.
 */
class StepCall {

    /***
     * A expressão chamada. Ela necessária para ser possível identificar o step chamado.
     */
    String text

    /***
     * O arquivo que fez a chamada.
     */
    String path

    /***
     * A localização da chamada descrita pela linha no arquivo onde ela foi feita.
     * */
    int line

    String parentType

    @Override
    String toString() {
        "Step call in '${parentType}' step definition at $path ($line): $text"
    }

}

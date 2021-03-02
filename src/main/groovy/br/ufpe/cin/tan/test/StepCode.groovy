package br.ufpe.cin.tan.test

import br.ufpe.cin.tan.util.Util
import gherkin.ast.Step

/***
 * Representa o casamento entre um step Gherkin e seu código de implementação (step definition).
 * */
class StepCode {

    /***
     * Nó que representa um step em um arquivo Gherkin.
     */
    Step step

    /***
     * Localização da implementação do step.
     */
    String codePath

    /***
     * Linha inicial da implementação do step.
     */
    int line

    /**
     * O valor dos argumentos usados no step Gherkin.
     */
    List<String> args

    /**
     * Palavra-chave que identifica o tipo do step. Essa decisão é feita com base no step Gherkin e não no step definition.
     * */
    String type

    @Override
    String toString() {
        def location = codePath - Util.getRepositoriesCanonicalPath()
        "${type}: ${step.text}; location: $location ($line); args: $args"
    }

    void setType(String type) {
        if (type.endsWith(" ")) this.type = type
        else this.type = type + " "
    }

}

package br.ufpe.cin.tan.commit.change.gherkin

/***
 * Representa um step definition (método que automatiza a execução de um step em Gherkin).
 */
class StepDefinition {

    String keyword //keyword Gherkin associada ao step definition, mas que na prática não serve de identificador
    String path //arquivo em que o step defitinion foi declarado
    String value //o corpo do step definition
    String regex //a expressão regular usada para associar o step definition ao step Gherkin
    int line //linha inicial da declaração do step definition
    int end //linha final da declaração do step definition
    List<String> body

    @Override
    String toString() {
        def text = "Step ($line-$end): $value\n"
        body?.each { text += it + "\n" }
        return text
    }

    int size() {
        return end - line
    }

    void setKeyword(String keyword) {
        if (keyword.endsWith(" ")) this.keyword = keyword
        else this.keyword = keyword + " "
    }

}

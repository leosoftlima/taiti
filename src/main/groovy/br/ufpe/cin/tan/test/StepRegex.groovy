package br.ufpe.cin.tan.test

/***
 * Representa uma expressão regular identificada em um arquivo de código fonte.
 */
class StepRegex {

    String keyword //palavra-chave identificadora. Em Ruby, só é usado quando se trata de um step genérico (*)
    String path //arquivo em que a expressão foi declarada
    String value //a expressão em si
    int line //linha do arquivo em que a expressão se encontra

    @Override
    String toString() {
        "File $path ($line): $keyword - $value"
    }

    void setKeyword(String keyword) {
        if (keyword.endsWith(" ")) this.keyword = keyword
        else this.keyword = keyword + " "
    }

}

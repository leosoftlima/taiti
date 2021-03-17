package br.ufpe.cin.tan.test

import br.ufpe.cin.tan.test.error.ParseError
import br.ufpe.cin.tan.test.error.StepError

/***
 * Representa os dados referentes à análise do código de teste da tarefa, que são úteis para verificar ocorrência
 * de problemas durante o processo. São eles: ocorrência de erro no casamento entre steps e step definitions,
 * uso de keyword Gherkin para identificar um step genérico (ao invés de given, when, then, usa-se asterisco),
 * os testes de aceitação implementados, os step definitions encontrados, o código que foi alcançado através da análise
 * de arquivos d view, a contagem de chamadas para o método visit existentes (esse método só existe em projetos Rails e
 * serve para acessar uma view), a ocorrência de erros na análise de chamadas para o método visit, o rastro da análise
 * (lista de arquivos e suas respectivas linhas que foram analisadas) e o código de implementação dos testes, que
 * é usado para fins de conferência e também no cálculo da interface TextI.
 */
class AnalysisData {

    Set<StepError> matchStepErrors
    Set<StepError> multipleStepMatches
    Set genericStepKeyword
    Set<AcceptanceTest> foundAcceptanceTests
    Set foundStepDefs
    Set codeFromViewAnalysis
    int visitCallCounter
    Set lostVisitCall //keys: path, line
    Set trace //keys: path, lines
    Set<MethodBody> testCode
    Set<ParseError> parseErrors

    AnalysisData() {
        matchStepErrors = [] as Set
        multipleStepMatches = [] as Set
        genericStepKeyword = [] as Set
        foundAcceptanceTests = [] as Set
        foundStepDefs = [] as Set
        codeFromViewAnalysis = [] as Set
        lostVisitCall = [] as Set
        trace = [] as Set
        testCode = [] as Set
        parseErrors = [] as Set
    }

}

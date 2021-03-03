package br.ufpe.cin.tan.test

import gherkin.ast.ScenarioDefinition
import groovy.util.logging.Slf4j

/***
 * Representa um teste de aceitação completo, fazendo o casamento entre o scenario Gherkin e o código que automatiza
 * sua execução.
 */
@Slf4j
class AcceptanceTest {

    String gherkinFilePath
    ScenarioDefinition scenarioDefinition
    List<StepCode> stepCodes

    @Override
    String toString() {
        def text = "<Acceptance test>\n"
        text += "Location: $gherkinFilePath\n"
        text += "${scenarioDefinition.keyword}: (${scenarioDefinition.location.line}) ${scenarioDefinition.name}\n"
        text += "Steps:\n"
        scenarioDefinition.steps.each { step ->
            text += "(${step.location.line}) ${step.keyword} ${step.text}\n"
        }
        text += "Steps code:\n"
        stepCodes.each { step ->
            text += step.toString() + "\n"
        }
        return text
    }

}

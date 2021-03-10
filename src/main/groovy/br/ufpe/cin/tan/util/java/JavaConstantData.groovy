package br.ufpe.cin.tan.util.java

import br.ufpe.cin.tan.util.ConstantData

abstract class JavaConstantData {

    public static String ROUTES_FILE = File.separator + "config" + File.separator + "routes.story"
    public static String ROUTES_ID = "ROUTES" /*preciso ver com Thais isso aqui.....*/
    public static String ROUTE_PATH_SUFIX = "_path" /*preciso ver com Thais isso aqui.....*/
    public static String ROUTE_URL_SUFIX = "_url" /*preciso ver com Thais isso aqui.....*/

    public static List<String> EXCLUDED_PATH_METHODS = []

    public static String VIEW_ANALYSER_FILE = "view_analyser.story"
    public static String POM_FILE = "pom"
    public static List<String> POM_OF_INTEREST = ["jacoco", "cucumber", "simplecov"]

    public static OPERATORS = ["[]", "*", "/", "+", "-", "==", "!=", ">", "<", ">=", "<=", "<=>", "===", ".eql?",
                               "equal?", "defined?", "%", "<<", ">>", "=~", "&", "|", "^", "~", "!", "**"]
    public static IGNORED_METHODS = ["puts", "print", "assert", "should", "should_not"] + EXCLUDED_PATH_METHODS +
            ConstantData.ALL_STEP_KEYWORDS + OPERATORS
}
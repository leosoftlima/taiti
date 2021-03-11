package br.ufpe.cin.tan.util.java

import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util
import com.github.javaparser.ast.Node;

import java.util.regex.Matcher

class JavaUtil extends Util {

    static String getClassName(String path) {
        if (!path || path.empty || path.isAllWhitespace()) return ""
        def firstIndex = path.lastIndexOf(File.separator)
        def lastIndex = path.lastIndexOf(".")
        def underscore = ""
        if (firstIndex >= 0 && lastIndex >= 0 && firstIndex + 1 < lastIndex) underscore = path.substring(firstIndex + 1, lastIndex)
        underscoreToCamelCase(underscore)
    }

    static List<String> getClassPathForRubyClass(String original, Collection<String> projectFiles) {
        def underscore = camelCaseToUnderscore(original)
        getClassPathForVariable(underscore, projectFiles)
    }
    static List<String> getClassPathForVariable(String original, Collection<String> projectFiles) {
        if (original.empty || original.contains(" ")) return [] //invalid class reference
        def name = camelCaseToUnderscore(original) + ConstantData.JAVA_EXTENSION
        def exp = ".*$File.separator$name".replace(File.separator, Matcher.quoteReplacement(File.separator))
        projectFiles?.findAll { it ==~ /$exp/ }
    }

     static boolean isRouteMethod(String name) {
        if (!(name in JavaConstantData.EXCLUDED_PATH_METHODS) && name.endsWith(JavaConstantData.ROUTE_PATH_SUFIX)) true
        else false
    }

      private static  List<Node> getAllNodes(Node node) {
	   	List<Node> nodes = new LinkedList<>();
	    nodes.add(node);
	    node.getChildNodes().each {children -> 
	        nodes.addAll(getAllNodes(children));
	    };
	    return nodes;
	} 
}
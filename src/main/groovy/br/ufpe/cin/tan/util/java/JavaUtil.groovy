package br.ufpe.cin.tan.util.java

import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.Util
import com.github.javaparser.ast.Node

import java.util.regex.Matcher

class JavaUtil extends Util {

    static String getClassName(String path) {
        if (!path || path.empty || path.isAllWhitespace()) return ""
        def firstIndex = path.lastIndexOf(File.separator)
        def lastIndex = path.lastIndexOf(".")
        def name = ""
        if (firstIndex >= 0 && lastIndex >= 0 && firstIndex + 1 < lastIndex) name = path.substring(firstIndex + 1, lastIndex)
        name
    }

    static List<String> getClassPathForJavaClass(String original, Collection<String> projectFiles) {
        def name = original + ConstantData.JAVA_EXTENSION
        projectFiles?.findAll{ it.endsWith(name) }
    }

}
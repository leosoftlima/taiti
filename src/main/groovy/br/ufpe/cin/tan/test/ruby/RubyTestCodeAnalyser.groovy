package br.ufpe.cin.tan.test.ruby

import br.ufpe.cin.tan.analysis.taskInterface.CalledMethod
import br.ufpe.cin.tan.analysis.taskInterface.ReferencedPage
import br.ufpe.cin.tan.analysis.taskInterface.TestI
import br.ufpe.cin.tan.commit.change.gherkin.GherkinManager
import br.ufpe.cin.tan.commit.change.gherkin.StepDefinition
import br.ufpe.cin.tan.commit.change.unit.ChangedUnitTestFile
import br.ufpe.cin.tan.test.FileToAnalyse
import br.ufpe.cin.tan.test.error.ParseError
import br.ufpe.cin.tan.test.StepRegex
import br.ufpe.cin.tan.test.TestCodeAbstractAnalyser
import br.ufpe.cin.tan.test.TestCodeVisitorInterface
import br.ufpe.cin.tan.test.ruby.routes.Route
import br.ufpe.cin.tan.test.ruby.routes.RouteHelper
import br.ufpe.cin.tan.test.ruby.routes.RubyConfigRoutesVisitor
import br.ufpe.cin.tan.test.ruby.unitTest.RSpecFileVisitor
import br.ufpe.cin.tan.test.ruby.unitTest.RSpecTestDefinitionVisitor
import br.ufpe.cin.tan.test.ruby.views.ViewCodeExtractor
import br.ufpe.cin.tan.util.ConstantData
import br.ufpe.cin.tan.util.RegexUtil
import br.ufpe.cin.tan.util.Util
import br.ufpe.cin.tan.util.ruby.RubyConstantData
import br.ufpe.cin.tan.util.ruby.RubyUtil
import groovy.util.logging.Slf4j
import org.jrubyparser.CompatVersion
import org.jrubyparser.Parser
import org.jrubyparser.ast.Node
import org.jrubyparser.parser.ParserConfiguration

import java.util.regex.Matcher

/***
 * Visita código Ruby de forma geral.
 */
@Slf4j
class RubyTestCodeAnalyser extends TestCodeAbstractAnalyser {

    String routesFile
    Set<Route> routes
    Set<Route> problematicRoutes
    TestI interfaceFromViews
    ViewCodeExtractor viewCodeExtractor
    static counter = 1
    Set<MethodBody> methodBodies

    RubyTestCodeAnalyser(String repositoryPath, GherkinManager gherkinManager) {
        super(repositoryPath, gherkinManager)
        this.routesFile = repositoryPath + RubyConstantData.ROUTES_FILE
        this.routes = [] as Set
        this.problematicRoutes = [] as Set
        this.interfaceFromViews = new TestI()
        if (Util.VIEW_ANALYSIS) viewCodeExtractor = new ViewCodeExtractor()
        methodBodies = [] as Set
    }

    /***
     * Generates AST for Ruby file.
     * @param path path of interest file
     * @return the root node of the AST
     */
    Node generateAstForFile(String path) {
        def result1 = parseFile(new FileReader(path), path, CompatVersion.RUBY2_3)
        if (result1.errors.empty && result1.node) {
            return result1.node
        }

        def result2 = parseFile(new FileReader(path), path, CompatVersion.RUBY2_0)
        if (result2.errors.empty && result2.node) {
            return result2.node
        }

        def mininum = [result1, result2].find { it.errors.min() }
        if (mininum == null) return null
        analysisData.parseErrors += mininum.errors
        mininum.node
    }

    static List<String> recoverFileContent(String path) {
        FileReader reader = new FileReader(path)
        reader?.readLines()
    }

    Node generateAst(String path) {
        if (path.contains(Util.FRAMEWORK_PATH)) this.generateAstForFile(path)
        else {
            def index = path.indexOf(repositoryPath)
            def filename = index >= 0 ? path : repositoryPath + File.separator + path
            this.generateAstForFile(filename)
        }
    }

    Node generateAst(String path, String content) {
        def result1 = parseFile(new StringReader(content), path, CompatVersion.RUBY2_3)
        if (result1.errors.empty && result1.node) {
            return result1.node
        }

        def result2 = parseFile(new StringReader(content), path, CompatVersion.RUBY2_0)
        if (result2.errors.empty && result2.node) {
            return result2.node
        }

        def mininum = [result1, result2].find { it.errors.min() }
        analysisData.parseErrors += mininum.errors
        mininum.node
    }

    private static parseFile(reader, String path, CompatVersion version) {
        def errors = []
        Parser rubyParser = new Parser()
        ParserConfiguration config = new ParserConfiguration(0, version)
        Node result = null

        try {
            result = rubyParser.parse("<code>", reader, config)
        } catch (Exception ex) {
            //log.error "Problem to visit file $path (parser ${version.name()}): ${ex.message}"
            def msg = ""
            if (ex.message && !ex.message.empty) {
                def index = ex.message.indexOf(",")
                msg = index >= 0 ? ex.message.substring(index + 1).trim() : ex.message.trim()
            }
            errors += new ParseError(path: path, msg: msg)
        } finally {
            reader?.close()
        }
        def finalErrors = errors.findAll { !it.path.contains(Util.FRAMEWORK_PATH) }
        [node: result, errors: finalErrors]
    }

    private generateProjectRoutes() {
        if (routesManager && !routesManager.routes.empty) { //using Rails routes
            routes = routesManager.routes
            log.info "problematic routes generated by Rails:"
            routesManager.problematicRoutes.each {
                log.info it.toString()
            }
        } else { //computing routes
            def node = this.generateAst(routesFile)
            RubyConfigRoutesVisitor visitor = new RubyConfigRoutesVisitor(node)
            def allRoutes = visitor?.routingMethods
            problematicRoutes = allRoutes.findAll {
                !it.arg.contains("#") || (it.name != "root" && it.value ==~ /[\/\\(?\.*\\)?]+/ && it.value != "/")
            }
            routes = allRoutes - problematicRoutes
        }
        routes.collect {
            it.value = it.value.replaceAll("//", "/")
            it
        }
    }

    private extractMethodReturnUsingArgs(pageMethod) { //keywords: file, name, args
        def result = []
        def pageVisitor = new RubyConditionalVisitor(pageMethod.name, pageMethod.args, new FileReader(pageMethod.file)?.readLines())
        generateAst(pageMethod.file)?.accept(pageVisitor)
        methodBodies.add(new MethodBody(pageVisitor.body))
        result += pageVisitor.pages
        result += pageVisitor.auxiliaryMethods
        result
    }

    private extractAllPossibleReturnFromMethod(pageMethod) { //keywords: file, name, args
        def pageVisitor = new RubyMethodReturnVisitor(pageMethod.name, pageMethod.args, new FileReader(pageMethod.file)?.readLines())
        generateAst(pageMethod.file)?.accept(pageVisitor) //extracts path from method
        methodBodies.add(new MethodBody(pageVisitor.body))
        pageVisitor.values
    }

    private extractDataFromAuxiliaryMethod(pageMethod) { //keywords: file, name, args
        def result = [] as Set
        def specificFailed = false
        def extractSpecificReturn = !(pageMethod.args.empty)
        if (extractSpecificReturn) {
            log.info "extracting a specific return..."
            result += this.extractMethodReturnUsingArgs(pageMethod)
            if (result?.empty) specificFailed = true
        }
        if (!extractSpecificReturn || specificFailed) { //tries to extract all possible return values
            log.info "extracting all possible returns..."
            result += this.extractAllPossibleReturnFromMethod(pageMethod)
        }

        [result: result, specificReturn: extractSpecificReturn, specificFailed: specificFailed]
    }

    private registryPathData(RubyTestCodeVisitor visitor, String path) {
        def foundView = false
        def data = this.registryControllerAndActionUsage(visitor, path)
        def registryMethodCall = data.registry
        if (registryMethodCall) {
            def viewData = this.registryViewRelatedToAction(visitor, data.controller, data.action)
            foundView = viewData.found
            /*if(!foundView){
                viewData = this.registryViewRelatedToPath(visitor, path)
                foundView = viewData.found
            }*/
        }
        [registryMethodCall: registryMethodCall, foundView: foundView]
    }

    private extractActionFromView(String view) {
        def index1 = view.lastIndexOf("/")
        def index2 = view.indexOf(".")
        def action = view.substring(index1 + 1, index2)
        def controller = view.substring(0, index1) - this.repositoryPath
        if (action && controller) return "$controller#$action"
        else return null
    }

    private registryMethodCallAndViewAccessFromRoute(RubyTestCodeVisitor visitor, String path) {
        def registryMethodCall = false
        def foundView = false
        def valid = true

        if (RouteHelper.routeIsFile(path)) {
            log.info "ROUTE IS FILE: $path"
            if (RouteHelper.isViewFile(path)) {
                def views = viewFiles?.findAll { it.contains(path) }
                if (views && !views.empty) { //no case execute it yet
                    views.each { v ->
                        visitor?.taskInterface?.referencedPages += new ReferencedPage(file: v, step: visitor.step)
                        interfaceFromViews.referencedPages += new ReferencedPage(file: v, step: visitor.step)
                    }
                    foundView = true
                    //trying to find controller and action by view seems risky
                    /*def methodCall
                    views?.each{ view ->
                        def action = this.extractActionFromView(view)
                        log.info "ACTION EXTRACTED FROM VIEW '$view': $action"
                        if(action) {
                            def data = this.registryControllerAndActionUsage(visitor, path)
                            methodCall = data.registry
                            if(methodCall) registryMethodCall = methodCall
                        }
                    }*/
                }
            } else {
                log.info "ROUTE FILE IS INVALID: $path"
                valid = false
            }
        } else if (RouteHelper.routeIsAction(path)) {
            def r = this.registryPathData(visitor, path)
            registryMethodCall = r.registryMethodCall
            foundView = r.foundView
        } else {
            log.info "PATH: $path"
            def candidates = this.routes.findAll { path == it.value } //trying to find an equal path
            if (!candidates.empty) {
                def candidate = candidates.first()
                log.info "CANDIDATE: $candidate"
                if (candidate.arg && !candidate.arg.empty) {
                    def r = this.registryPathData(visitor, candidate.arg)
                    if (r.registryMethodCall) registryMethodCall = r.registryMethodCall
                    if (r.foundView) foundView = r.foundView
                }
            } else { //if it was not found, we trie to find a compatible one
                candidates = this.routes.findAll { path ==~ /${it.value}/ }
                if (candidates.empty) {
                    def viewData = this.registryViewRelatedToPath(visitor, path)
                    foundView = viewData.found
                } else {
                    def candidate = candidates.first()
                    log.info "CANDIDATE: $candidate"
                    if (candidate.arg && !candidate.arg.empty) {
                        def r = this.registryPathData(visitor, candidate.arg)
                        if (r.registryMethodCall) registryMethodCall = r.registryMethodCall
                        if (r.foundView) foundView = r.foundView
                    }
                }
            }
        }

        [path: path, call: registryMethodCall, view: foundView, valid: valid]
    }

    private static extractControllerAndActionFromPath(String route) {
        def result = null
        def name = route.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        if (name.contains("#")) {
            def index = name.indexOf("#")
            def controller = name.substring(0, index)
            def action = name.substring(index + 1, name.length())
            if (controller && action) result = [controller: controller, action: action]
        }
        result
    }

    private registryUsedPaths(RubyTestCodeVisitor visitor, Set<String> usedPaths) {
        log.info "All used paths (${usedPaths.size()}): $usedPaths"
        if (!usedPaths || usedPaths.empty) return
        def result = []
        usedPaths?.each { path ->
            result += this.registryMethodCallAndViewAccessFromRoute(visitor, path)
        }
        def methodCall = result.findAll { it.call }*.path
        def view = result.findAll { it.view }*.path
        def problematic = result.findAll { !it.call && !it.view && it.valid }*.path
        def invalid = result.findAll { !it.call && !it.view && !it.valid }*.path

        log.info "Used paths with method call (${methodCall.size()}): $methodCall"
        log.info "Used paths with view (${view.size()}): $view"
        log.info "All found views until the moment: ${visitor?.taskInterface?.referencedPages*.file.unique()}"
        log.info "Invalid used paths (${invalid.size()}): $invalid"
        log.info "Valid used paths with no data (${problematic.size()}): $problematic"
        this.notFoundViews += problematic
    }

    private registryControllerAndActionUsage(RubyTestCodeVisitor visitor, String value) {
        def controller = null
        def action = null
        def registryMethodCall = false
        def data = extractControllerAndActionFromPath(value)
        if (data) {
            registryMethodCall = true
            def className = RubyUtil.underscoreToCamelCase(data.controller + "_controller")
            def filePaths = RubyUtil.getClassPathForRubyClass(className, this.projectFiles)
            filePaths.each { filePath ->
                visitor?.taskInterface?.methods += new CalledMethod(name: data.action, type: className, file: filePath,
                        step: visitor.step)
                interfaceFromViews.methods += new CalledMethod(name: data.action, type: className, file: filePath,
                        step: visitor.step)
            }
            controller = data.controller
            action = data.action
        }
        [controller: controller, action: action, registry: registryMethodCall]
    }

    private registryView(RubyTestCodeVisitor visitor, List<String> views) {
        def foundView = false
        if (views && !views.empty) {
            views.each { v ->
                visitor?.taskInterface?.referencedPages += new ReferencedPage(file: v, step: visitor.step)
                interfaceFromViews.referencedPages += new ReferencedPage(file: v, step: visitor.step)
            }
            foundView = true
        }
        foundView
    }

    private registryViewRelatedToAction(RubyTestCodeVisitor visitor, String controller, String action) {
        def views = RubyUtil.searchViewFor(controller, action, this.viewFiles)
        def foundView = registryView(visitor, views)
        [views: views, found: foundView]
    }

    private registryDirectAccessedViews(RubyTestCodeVisitor visitor, List<String> files) {
        log.info "All direct accessed views: ${files.size()}"
        def views = []
        def found = []
        def problematic = []
        files.each { file ->
            def founds = this.viewFiles.findAll { it.endsWith(file) }
            founds.each {
                views += it
                def index2 = it.indexOf(this.repositoryPath)
                found += it.substring(index2 + this.repositoryPath.size() + 1)
            }
            if (founds.empty) problematic += file
        }

        def types = [ConstantData.ERB_EXTENSION, ConstantData.HTML_ERB_EXTENSION, ConstantData.HAML_EXTENSION,
                     ConstantData.HTML_HAML_EXTENSION]
        problematic.each { problem ->
            def extensionIndex = problem.indexOf(".")
            def extension = problem.substring(extensionIndex)
            def mainName = problem.substring(0, extensionIndex)
            def typesToSearch = types - [extension]
            for (int i = 0; i < typesToSearch.size(); i++) {
                def type = typesToSearch.get(i)
                def newName = mainName + type
                def founds = this.viewFiles.findAll { it.endsWith(newName) }
                founds.each {
                    views += it
                    def index2 = it.indexOf(this.repositoryPath)
                    found += it.substring(index2 + this.repositoryPath.size() + 1)
                }
                if (!founds.empty) {
                    problematic -= problem
                    break
                }
            }
        }

        registryView(visitor, views)
        log.info "Found direct accessed views: ${views.size()}"
        views.each { log.info it.toString() }

        log.info "Not found direct accessed views: ${problematic.size()}"
        problematic.each { log.info it.toString() }
        this.notFoundViews += problematic
    }

    private registryViewRelatedToPath(RubyTestCodeVisitor visitor, String path) {
        def views = []
        def index = path.lastIndexOf("/")
        def controller = path.substring(0, index + 1)
        def action = path.substring(index + 1)
        def regex = /.*${controller}_?$action.+/
        if (File.separator == "\\") regex = regex.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, "\\\\\\\\")
        def matches = viewFiles?.findAll { it ==~ regex }
        if (matches && matches.size() > 0) {
            if (matches.size() == (1 as int)) views = matches
            else {
                def match = matches.find { it.contains("index") }
                if (match) views += match
                else views = matches
            }
        }
        def foundView = registryView(visitor, views)
        [views: views, found: foundView]
    }

    private registryUsedRailsPaths(RubyTestCodeVisitor visitor, Set<String> railsPathMethods) {
        log.info "All used rails path methods (${railsPathMethods.size()}): $railsPathMethods"
        if (!railsPathMethods || railsPathMethods.empty) return
        def methodCall = []
        def view = []
        def foundViews = []
        def projectMethods = []

        railsPathMethods?.each { method -> //it was used some *_path method generated by Rails
            def registryMethodCall = false
            def foundView = false
            def views = []
            def route = this.routes?.find { it.name ==~ /${method}e?/ }
            if (route) { //it is really a route method
                if (route.arg && route.arg != "") { //there is controller#action related to path method
                    def data = this.registryControllerAndActionUsage(visitor, route.arg)
                    registryMethodCall = data.registry
                    if (registryMethodCall) {
                        methodCall += method
                        //tries to find view file
                        def viewData = registryViewRelatedToAction(visitor, data.controller, data.action)
                        foundView = viewData.found
                        if (foundView) {
                            view += method
                            views = viewData.views
                        }
                    }
                }
                /*if (route.value && route.value != "" && !foundView) { //tries to extract data from path
                    def viewData = this.registryViewRelatedToPath(visitor, route.value)
                    foundView = viewData.found
                    if(foundView){
                        view += method
                        views = viewData.views
                    }
                }*/
            } else { //maybe it is a method defined by the project
                def methodName1 = method + RubyConstantData.ROUTE_PATH_SUFIX
                def methodName2 = method + RubyConstantData.ROUTE_URL_SUFIX
                def matches = methods.findAll { it.name == methodName1 || it.name == methodName2 }
                matches?.each { m ->
                    def newMethod = new CalledMethod(name: m.name, type: RubyUtil.getClassName(m.path), file: m.path,
                            step: visitor.step)
                    visitor?.taskInterface?.methods += newMethod
                    interfaceFromViews.methods += newMethod
                    log.info "False rails path method: ${newMethod.name} (${newMethod.file})"
                }
                if (!matches.empty) {
                    projectMethods += method
                }
            }
            foundViews += views
            registryView(visitor, foundViews)
            log.info "Found route related to rails path method '${method}': ${registryMethodCall || foundView}"
        }

        def problematic = railsPathMethods - ((methodCall + view + projectMethods)?.unique())
        this.notFoundViews += problematic
        log.info "Rails path methods with method call (${methodCall.size()}): $methodCall"
        log.info "Rails path methods with view (${view.size()}): $view"
        log.info "All found views from rails path methods (${foundViews.size()}): ${foundViews}"
        log.info "Rails path methods that are actually project methods (${projectMethods.size()}): ${projectMethods}"
        log.info "Rails path methods with no data (${problematic.size()}): ${problematic}"
    }

    private extractCallsFromViewFiles(RubyTestCodeVisitor visitor, Set<String> analysedViewFiles) {
        def viewFiles = visitor.taskInterface.getViewFilesForFurtherAnalysis()
        if (analysedViewFiles && !analysedViewFiles.empty) viewFiles -= analysedViewFiles
        def calls = []
        viewFiles?.each { viewFile ->
            def path = viewFile.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement("/"))
            try {
                def r = []
                String code = viewCodeExtractor?.extractCode(path)
                code?.eachLine { line -> r += Eval.me(line) }
                log.info "Extracted code from view (${r.size()}): $path"
                r.each { log.info it.toString() }
                calls += r
            } catch (Exception ex) {
                def src = new File(path)
                if (src.exists()) {
                    def dst = new File(ConstantData.DEFAULT_VIEW_ANALYSIS_ERROR_FOLDER + File.separator + src.name + counter)
                    dst << src.text
                }
                log.error "Error to extract code from view file: $path (${ex.message})"
                counter++
            }
        }
        calls.unique()
    }

    private organizeRailsPathMethodCalls(calls, RubyTestCodeVisitor visitor) {
        def railsPathMethods = calls?.findAll {
            it.receiver.empty &&
                    (it.name.endsWith(RubyConstantData.ROUTE_PATH_SUFIX) || it.name.endsWith(RubyConstantData.ROUTE_URL_SUFIX))
        }
        def railsMethods = (railsPathMethods*.name).collect {
            it - RubyConstantData.ROUTE_PATH_SUFIX - RubyConstantData.ROUTE_URL_SUFIX
        }
        this.registryUsedRailsPaths(visitor, railsMethods as Set)
        railsPathMethods
    }

    private organizePathAccess(calls, RubyTestCodeVisitor visitor) {
        def usedPaths = calls?.findAll { it.receiver.empty && it.name.contains("/") }
        def paths = usedPaths*.name
        this.registryUsedPaths(visitor, paths as Set)
        usedPaths
    }

    private static organizeClassUsage(calls, RubyTestCodeVisitor visitor) {
        def classesOnly = calls?.findAll { it.name.empty && !it.receiver.empty }
        classesOnly?.each {
            String name = it.receiver
            if (name.startsWith("@")) name = name.substring(1)
            visitor.registryClassUsage(name)
        }
        log.info "Used classes (${classesOnly.size()}): ${classesOnly*.receiver}"
        classesOnly
    }

    private static organizeMethodCallsWithReceiver(calls, RubyTestCodeVisitor visitor) {
        def found = []
        def methodsWithReceiver = calls?.findAll { !it.receiver.empty && !it.name.empty }

        methodsWithReceiver?.each {
            String receiverName = it.receiver
            if (receiverName.startsWith("@")) receiverName = receiverName.substring(1)
            def result = visitor.registryCallFromInstanceVariable(it.name, 0, receiverName)
            if (result) found += it
        }

        log.info "Method calls in views with receiver: ${methodsWithReceiver.size()}"
        methodsWithReceiver.each { log.info it.toString() }
        log.info "Method calls in views with receiver correctly registered: ${found.size()}"
        found.each { log.info it.toString() }
        def notFound = methodsWithReceiver - found
        log.info "Method calls in views with receiver with no data: ${notFound.size()}"
        notFound.each { log.info it.toString() }

        methodsWithReceiver
    }

    private organizeMethodCallsNoReceiver(calls, RubyTestCodeVisitor visitor) {
        def methodsUnknownReceiver = calls?.findAll { it.receiver.empty && !it.name.empty }
        def notFoundMethods = []
        methodsUnknownReceiver.each { method ->
            log.info "method with unknown receiver: ${method}"
            def matches = methods.findAll {
                def counter = method.arguments as int
                it.name == method.name && counter <= it.args && counter >= it.args - it.optionalArgs
            }
            matches?.each { m ->
                def newMethod = new CalledMethod(name: m.name, type: RubyUtil.getClassName(m.path), file: m.path,
                        step: visitor.step)
                visitor?.taskInterface?.methods += newMethod
                interfaceFromViews.methods += newMethod
            }
            if (matches.empty) {
                log.info "method with unknown receiver: '${method}' has no match"
                def newMethod = new CalledMethod(name: method.name, type: "Object", file: null, step: visitor.step)
                visitor?.taskInterface?.methods += newMethod
                interfaceFromViews.methods += newMethod
                notFoundMethods += method
            }
        }
        log.info "Methods calls in views with unknown receiver: ${methodsUnknownReceiver.size()}"
        methodsUnknownReceiver.each { log.info it.toString() }
        log.info "Methods calls in views with unknown receiver with no data: ${notFoundMethods.size()}"
        notFoundMethods.each { log.info it.toString() }

        methodsUnknownReceiver
    }

    private organizeViewFileAccess(calls, RubyTestCodeVisitor visitor) {
        def files = calls?.findAll {
            it.receiver.empty &&
                    (it.name.endsWith(ConstantData.ERB_EXTENSION) || it.name.endsWith(ConstantData.HAML_EXTENSION))
        }
        def accessedViewFiles = files*.name?.collect {
            String n = it.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            if (!n.contains(Util.VIEWS_FILES_RELATIVE_PATH)) {
                def aux = Util.VIEWS_FILES_RELATIVE_PATH.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX,
                        Matcher.quoteReplacement(File.separator))
                if (!n.startsWith(File.separator)) n = File.separator + n
                n = aux + n
            }
            n
        }
        this.registryDirectAccessedViews(visitor, accessedViewFiles)
        files
    }

    private registryCallsIntoViewFiles(RubyTestCodeVisitor visitor, Set<String> analysedViewFiles) {
        if (!(visitor instanceof RubyTestCodeVisitor)) return

        def calls = extractCallsFromViewFiles(visitor, analysedViewFiles)
        if (calls.empty) return

        log.info "All calls from view file(s): ${calls.size()}"
        calls?.each { log.info it.toString() }

        analysedViewFiles += visitor.taskInterface.getViewFilesForFurtherAnalysis()
        def files = organizeViewFileAccess(calls, visitor)

        def noFiles = calls - files
        def usedPaths = organizePathAccess(noFiles, visitor)

        def railsPathMethods = organizeRailsPathMethodCalls(noFiles - usedPaths, visitor)
        def others = noFiles - (usedPaths + railsPathMethods)

        def classesOnly = organizeClassUsage(others, visitor)
        others -= classesOnly

        def methodsWithReceiver = organizeMethodCallsWithReceiver(others, visitor)
        others -= methodsWithReceiver
        def methodsUnknownReceiver = organizeMethodCallsNoReceiver(others, visitor)
        others -= methodsUnknownReceiver

        log.info "Calls from view file(s) that we can not deal with: ${others.size()}"
        others.each { log.info it.toString() }

        //check if there is new view files to analyse
        registryCallsIntoViewFiles(visitor, analysedViewFiles)
    }

    @Override
    void findAllPages(TestCodeVisitorInterface visitor) {
        /* generates all routes according to config/routes.rb file */
        if (this.routes.empty) {
            this.generateProjectRoutes()
            log.info "All project routes:"
            this.routes.each { log.info it.toString() }
            log.info "Problematic routes (${problematicRoutes.size()}):"
            this.problematicRoutes.each { log.info it.toString() }
        }

        /* identifies used routes */
        def calledPaths = visitor?.taskInterface?.calledPageMethods
        calledPaths.removeAll([null])  //it is null if test code references a class or file that does not exist
        def usedPaths = [] as Set

        def auxiliaryMethods = calledPaths.findAll { it.file != RubyConstantData.ROUTES_ID }
        def railsPathMethods = (calledPaths - auxiliaryMethods)?.findAll { !it.name.contains("/") }
        def explicityPaths = calledPaths - (auxiliaryMethods + railsPathMethods)
        usedPaths += explicityPaths*.name

        /* identifies used routes (by auxiliary methods) */
        def usedRoutesByAuxMethods = [] as Set
        auxiliaryMethods?.each { auxMethod -> //it was used an auxiliary method; the view path must be extracted
            log.info "FIND_ALL_PAGES; visiting file '${auxMethod.file}' and method '${auxMethod.name} (${auxMethod.args})'"
            def data = this.extractDataFromAuxiliaryMethod(auxMethod)
            usedRoutesByAuxMethods += data.result
            def allReturn = false
            if (!data.specificReturn || (data.specificReturn && data.specificFailed)) allReturn = true
            log.info "Found route related to auxiliary path method '${auxMethod.name}': ${!data.result.empty} (all return: $allReturn)"
            visitor.methodBodies += methodBodies
        }

        /* deals with rails *_path methods */
        def pathMethodsReturnedByAuxMethods = usedRoutesByAuxMethods.findAll {
            it.contains(RubyConstantData.ROUTE_PATH_SUFIX)
        }
        usedPaths += usedRoutesByAuxMethods - pathMethodsReturnedByAuxMethods
        railsPathMethods = railsPathMethods*.name
        railsPathMethods += pathMethodsReturnedByAuxMethods?.collect { it - RubyConstantData.ROUTE_PATH_SUFIX }

        /* extracts data from used routes */
        this.registryUsedPaths((RubyTestCodeVisitor) visitor, usedPaths)
        this.registryUsedRailsPaths((RubyTestCodeVisitor) visitor, railsPathMethods as Set)

        /* extracts data from view (ERB or HAML) files (this code must be moved in the future) */
        if (viewCodeExtractor) this.registryCallsIntoViewFiles((RubyTestCodeVisitor) visitor, [] as Set)
    }

    /***
     * Finds all regex expression in a source code file.
     *
     * @param path ruby file
     * @return map identifying the file and its regexs
     */
    @Override
    List<StepRegex> doExtractStepsRegex(String path) {
        def node = this.generateAst(path)
        def visitor = new RubyStepRegexVisitor(path)
        node?.accept(visitor)
        visitor.regexs
    }

    @Override
    List<StepDefinition> doExtractStepDefinitions(String path, String content) {
        def node = this.generateAst(path, content)
        def visitor = new RubyStepDefinitionVisitor(path, content)
        node?.accept(visitor)
        visitor.stepDefinitions
    }

    @Override
    Set doExtractMethodDefinitions(String file) {
        RubyMethodDefinitionVisitor visitor = new RubyMethodDefinitionVisitor()
        visitor.path = file
        def node = this.generateAst(file)
        node?.accept(visitor)
        visitor.methods
    }

    /***
     * Visits a step body and method calls inside it. The result is stored as a field of the returned visitor.
     *
     * @param file List of map objects that identifies files by 'path' and 'lines'.
     * @return visitor to visit method bodies
     */
    @Override
    TestCodeVisitorInterface parseStepBody(FileToAnalyse file) {
        def node = this.generateAst(file.path)
        def visitor = new RubyTestCodeVisitor(projectFiles, file.path, methods)
        def fileContent = recoverFileContent(file.path)
        def testCodeVisitor = new RubyStepsFileVisitor(file.methods, visitor, fileContent)
        node?.accept(testCodeVisitor)
        visitor.methodBodies.add(new MethodBody(testCodeVisitor.body))
        visitor
    }

    /***
     * O método deve visitar o corpo de métodos selecionados de um arquivo de código-fonte procurando por outras chamadas
     * de método. O resultado é armazenado com um campo do visitor de entrada.
     *
     * @param file um map que identifica o arquivo e seus respectivos métodos a serem analisados. As palavras-chave são
     * 'path' para identificar o arquivo e 'methods' para os métodos que, por usa vez, é descrito pelas chave 'name' e
     * 'step'.
     * @param visitor Visitor usado para analisar o código, específico de LP
     */
    @Override
    visitFile(file, TestCodeVisitorInterface visitor) {
        def node = this.generateAst(file.path)
        visitor.lastVisitedFile = file.path
        def fileContent = recoverFileContent(file.path)
        def auxVisitor = new RubyMethodVisitor(file.methods, (RubyTestCodeVisitor) visitor, fileContent)
        node?.accept(auxVisitor)
        visitor.methodBodies.add(new MethodBody(auxVisitor.body))
    }

    @Override
    TestCodeVisitorInterface parseUnitBody(ChangedUnitTestFile file) {
        def node = generateAst(file.path)
        def visitor = new RubyTestCodeVisitor(projectFiles, file.path, methods)
        visitor.lastVisitedFile = file.path
        visitor.applicationClass = file.applicationClass //keywords: name, path
        def testCodeVisitor = new RSpecFileVisitor(file.tests*.lines, visitor)
        node?.accept(testCodeVisitor)
        visitor
    }

    @Override
    ChangedUnitTestFile doExtractUnitTest(String path, String content, List<Integer> changedLines) {
        ChangedUnitTestFile unitFile = null
        try {
            def visitor = new RSpecTestDefinitionVisitor(path, content, repositoryPath)
            def node = generateAst(path, content)
            node?.accept(visitor)
            if (visitor.tests.empty) {
                log.info "The unit file does not contain any test definition!"
            } else {
                def changedTests = visitor.tests.findAll { it.lines.intersect(changedLines) }
                if (changedTests) {
                    unitFile = new ChangedUnitTestFile(path: path, tests: changedTests, applicationClass: visitor.applicationClass)
                } else {
                    log.info "No unit test was changed or the changed one was not found!"
                }
            }
        } catch (FileNotFoundException ex) {
            log.warn "Problem to parse unit test file: ${ex.message}. Reason: The commit deleted it."
        }
        unitFile
    }

    @Override
    String getClassForFile(String path) {
        RubyUtil.getClassName(path)
    }

    @Override
    Set<String> getCodeFromViewAnalysis() {
        interfaceFromViews.getFiles().sort()
    }

    @Override
    boolean hasCompilationError(String path) {
        def node = generateAst(path)
        if (!node) true else false
    }
}

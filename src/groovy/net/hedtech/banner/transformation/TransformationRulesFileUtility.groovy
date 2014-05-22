package net.hedtech.banner.transformation

import grails.converters.deep.JSON
import groovy.time.TimeCategory

class TransformationRulesFileUtility {

    private static final def envName = 'BANNER_TRANSFORMATION'
    private static Map transformationRules = null

    static boolean transformationRulesExist() {
        if (transformationRules == null) {
            loadRules()
        }
        return transformationRules.size()>0
    }

    static Map rulesForClass(String className) {
        if (transformationRules == null) {
            loadRules()
        }
        return transformationRules.get(className)
    }

    private static void loadRules(){
        def t0 = new Date()
        def xmlParser = new XmlParser()
        def loadCount = 0
        // first time set rules to a not null value, this way system only attempts to load once
        transformationRules = [:]
        //load up to 2 xml files, can be extended to any number without significant change
        for (int i=1; i<3; i++)  {
            def fileLocation = System.getenv("$envName$i")
            if (fileLocation){
                File xmlFile = new File(fileLocation)
                if (xmlFile.exists()){
                    def xmlRules = xmlParser.parse(xmlFile)
                    addRules(convertNode(xmlRules))
                    println "Loaded rules from $fileLocation"
                    loadCount++
                }
            }
        }
        def t1 = new Date()
        if (loadCount) {
            println "Loaded metadata in ${TimeCategory.minus(t1, t0)}"
        }
        //printMap()
    }

    private static void addRules(Map rules) {
        if (rules?.size()) {
            ArrayList rulesArray = []
            if (transformationRules) {
                rulesArray.add(transformationRules)
            }
            rulesArray.add(rules)
            transformationRules = mergeMaps(rulesArray)
        }
    }

    private static Map convertNode(Node xmlNode)  {
        Map  groovyRules =[:]
        if (xmlNode instanceof Node) {
            def shell = new GroovyShell()
            for (Node ruleNode : xmlNode.value()) {
                def domainName = ruleNode.get("@id")
                if (domainName) {
                    def text = ruleNode.value()[0]
                    def domainRules = shell.evaluate(text)
                    groovyRules << [(domainName): domainRules]
                }
            }
        }
        return groovyRules
    }

    static Map mergeMaps(ArrayList<Map> maps) {
        if (!maps?.size())
            throw new Exception("List of metadata must have at least one entry")
        // Use merge method in ConfigObject to merge the maps
        def result = new ConfigObject()
        result.putAll(maps[0])
        for (int i = 1; i < maps.size(); i++) {
            def added = new ConfigObject()
            added.putAll(maps[i])
            result.merge(added)
        }
        return result
    }

    static void printMap() {
        transformationRules.each {
            println "Domain: $it.key \n $it.value"
        }

    }
}

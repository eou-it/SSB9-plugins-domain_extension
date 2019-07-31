/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

package net.hedtech.banner.transformation


import groovy.time.TimeCategory

class TransformationRulesFileUtility {

    private static final String envName = 'BANNER_TRANSFORMATION'
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
        println("load rules is working ")
        Date t0 = new Date()
        XmlParser xmlParser = null
        Integer loadCount = 0
        // first time set rules to a not null value, this way system only attempts to load once
        transformationRules = [:]
        //load up to 2 xml files, can be extended to any number without significant change
        for (int i=1; i<3; i++)  {
            String fileLocation = System.getenv("$envName$i")
            if (fileLocation){
                File xmlFile = new File(fileLocation)
                if (xmlFile.exists()){
                    if (!xmlParser) {
                        xmlParser = new XmlParser()
                    }
                    addRules(convertNode(xmlParser.parse(xmlFile)))
                    println "Loaded rules from $fileLocation"
                    loadCount++
                } else {
                    throw new Exception("Error: file $fileLocation does not exist.")
                }
            }
        }
        if (loadCount) {
            println "Loaded metadata in ${TimeCategory.minus(new Date(), t0)}"
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
        Map  groovyRules = [:]
        if (xmlNode instanceof Node) {
            GroovyShell shell = new GroovyShell()
            for (Node ruleNode : xmlNode.value()) {
                String domainName = ruleNode.get("@id")
                if (domainName) {
                    String text = ruleNode.value()[0]
                    Map domainRules = shell.evaluate(text)
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
        ConfigObject result = new ConfigObject()
        result.putAll(maps[0])
        for (int i = 1; i < maps.size(); i++) {
            ConfigObject added = new ConfigObject()
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

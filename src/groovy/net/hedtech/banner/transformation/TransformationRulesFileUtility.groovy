package net.hedtech.banner.transformation

class TransformationRulesFileUtility {

    private static final String fileLocation = System.getenv('BANNER_TRANSFORMATION')

    private static Node transformationRules = null

    static boolean transformationRulesExist() {
        if (!fileLocation) {
            return false
        }

        File transformationRulesFile = new File(fileLocation)
        transformationRules = new XmlParser().parse(transformationRulesFile)

        return transformationRules ? transformationRules instanceof Node : false
    }


    static Map rulesForClass(String className) {
        if (transformationRules == null) {
            transformationRulesExist()
        }

        Map rules = [:]
        if (transformationRules instanceof Node) {
            for (Node ruleNode : transformationRules.value()) {
                if (ruleNode.get("@id") == className) {
                    def text = ruleNode.value()[0]
                    rules = new GroovyShell().evaluate(text)
                    break;
                }
            }
        }
        return rules
    }
}

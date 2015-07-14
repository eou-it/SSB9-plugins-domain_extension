/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

class DomainExtensionGrailsPlugin {
    // the plugin version
    def version = "1.1"
    // Next line is supposed to exclude the plugin from the war file
    // It seems to exclude plugins/domain-extension-1.0, but not other artifacts
    //def scopes = [excludes:['war','run']]
    def scopes = [includes:'test', excludes:'war' ]
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3.7 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Ellucian"
    def authorEmail = ""
    def title = "Domain Extension Tool"
    def description = '''\\
This tool uses Global AST Transformation to modify Grails Domain classes based on XML Metadata
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/domain-extension"

    def doWithWebDescriptor = { xml ->

    }

    def doWithSpring = {

    }

    def doWithDynamicMethods = { ctx ->

    }

    def doWithApplicationContext = { applicationContext ->

    }

    def onChange = { event ->

    }

    def onConfigChange = { event ->

    }
}

/*
 * Copyright 2014-2020 Ellucian Company L.P. and its affiliates.
 */
package domain.extension

import grails.plugins.*

class DomainExtensionGrailsPlugin extends Plugin {
	
	// the plugin version
    def version = "9.14"
	
	 // Next line is supposed to exclude the plugin from the war file
    // It seems to exclude plugins/domain-extension-1.0, but not other artifacts
    //def scopes = [excludes:['war','run']]
    def scopes = [includes:'test', excludes:'war' ]
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.11 > *"
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
    def profiles = ['web']

   
    Closure doWithSpring() { {->
          //no-op
        }
    }

    void doWithDynamicMethods() {
         //no-op
    }

    void doWithApplicationContext() {
         //no-op
    }

    void onChange(Map<String, Object> event) {
         //no-op
    }

    void onConfigChange(Map<String, Object> event) {
         //no-op
    }

    void onShutdown(Map<String, Object> event) {
         //no-op
    }
}

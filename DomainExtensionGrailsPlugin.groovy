/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

class DomainExtensionGrailsPlugin {
    // the plugin version
    def version = "1.0"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3.7 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]
	
	// Next line is supposed to exclude the plugin from the war file
    // It seems to exclude plugins/domain-extension-1.0, but not other artifacts
    def scopes = [excludes:'war']

    // TODO Fill in these fields
    def author = "Ellucian"
    def authorEmail = ""
    def title = "Domain Extension Tool"
    def description = '''\\
This tool uses Global AST Transformation to modify Grails Domain classes based on XML Metadata
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/domain-extension"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}

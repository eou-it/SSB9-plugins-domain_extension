/*
 * Copyright 2015 Ellucian Company L.P. and its affiliates.
 */
grails.project.class.dir="target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        if (System.properties['PROXY_SERVER_NAME']) {
            mavenRepo "${System.properties['PROXY_SERVER_NAME']}"
        }
        grailsCentral()
        mavenCentral()
        mavenRepo "https://code.lds.org/nexus/content/groups/main-repo"
        mavenRepo "http://repository.jboss.org/maven2/"
    }

    plugins {
        runtime  ":hibernate:3.6.10.19"
        test ':code-coverage:2.0.3-3'
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
    }
}

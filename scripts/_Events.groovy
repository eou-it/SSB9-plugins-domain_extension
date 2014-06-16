/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

eventCompileStart = {target ->
//eventCompileEnd = {target ->
//eventSetClassPath = { target ->
//eventPackagingEnd = { target ->
//eventCreatePluginArchiveStart = { stagingDir ->
    if (target.args=="ast" || new File("${basedir}/lib/bannerTransform.jar").exists()==false )
        compileAST()
}


def compileAST() {
    def pluginBasedir=domainExtensionPluginDir.toString().replace('\\','/')
    def destDir="$pluginBasedir/target/classes"
    println "AST compile -> $destDir ... "
    ant.delete(dir: destDir)
    ant.mkdir dir: destDir
    //Delete so compiler doesn't do this AST
    //ant.delete(file:"${pluginBasedir}/lib/bannerTransform.jar")
    ant.sequential {
        path id: "grails.compile.classpath", compileClasspath
        def classpathId = "grails.compile.classpath"
        ant.groovyc(destdir: destDir,
                srcDir: "$pluginBasedir/src/groovy/net/hedtech/banner/transformation",
                classpathref: classpathId,
                verbose: grailsSettings.verboseCompile,
                stacktrace: "yes",
                encoding: "UTF-8")
    }
    ant.copy(todir: "${destDir}/META-INF")  {
        fileset(dir:"${pluginBasedir}/templates/META-INF")
    }
    ant.jar ( destfile : "${basedir}/lib/bannerTransform.jar" , basedir : destDir)
    ant.delete(dir: destDir)

}
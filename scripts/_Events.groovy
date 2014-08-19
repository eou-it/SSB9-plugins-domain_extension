/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

// Create the bannerTransform.jar file
eventCompileStart = {target ->
    if (target.args=="ast" || new File("${basedir}/lib/bannerTransform.jar").exists()==false )
        compileAST()

}

// Clean up files from stagingDir - DET doesn't have to be in War
eventCreateWarStart = { warName, stagingDir ->
    println "Remove Domain Extension Tool artifacts from staging area"
    /* Tried to match on directory name without version like below:
    ant.delete(){
        ant.dirset( dir: "${stagingDir}/WEB-INF/plugins/", includes : "domain-extension-*")
    }
    Doesn't seem to work, use Groovy File methods instead
    */
    new File("${stagingDir}/WEB-INF/plugins/").eachDirMatch(~/domain-extension-.*/){
        it.deleteDir()
    }
    //When removing the next classes, the war cannot be deployed
    //There seems to be a bug in the war building in that it doesn't exclude the plugin init when war is not in scope
    //ant.delete(){fileset dir: "${stagingDir}/WEB-INF/classes/", includes:"**/DomainExtensionGrailsPlugin*.class" }
    ant.delete(includeEmptyDirs: true) { fileset dir: "${stagingDir}/WEB-INF/classes/net/hedtech/banner/transformation/" }
    ant.delete(file: "${stagingDir}/WEB-INF/lib/bannerTransform.jar")
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
}
//description "Generate AST JAR", "grails compile ast"
//
//println "AST JAR creation Script"
//
//try {
//    if (new File("build/classes/groovy/main").exists()) {
//        def basedir = System.getProperty('user.dir')
//        println("===In generating AST JAR Dir Path==")
//        def filePath = "lib/bannerTransform.jar"
//        def tmpDir = "build/temp"
//        def file = new File(filePath)
//        if (file.exists()) {
//            ant.delete(dir: file)
//        }
//        ant.copy(todir: tmpDir) {
//            fileset(dir: "build/classes/groovy/main")
//        }
//        ant.copy(todir: "${tmpDir}/META-INF/services") {
//            fileset(dir: "grails-app/conf/services")
//        }
//        ant.jar(destfile: "${filePath}", basedir: tmpDir)
//        ant.path(id: 'classpath', location: 'lib/javax.persistence.jar')
//        println "Global AST jar file: lib/bannerTransform.jar"
//    }
//} catch(Exception e) {
//    println "Unable to generate AST JAR"
//    e.stackTrace()
//}

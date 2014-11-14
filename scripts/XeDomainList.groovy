import org.apache.oro.io.GlobFilenameFilter

/*******************************************************************************
 Copyright 2014 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
target(main: "List XE Domains and Tables") {

    def ln = System.getProperty('line.separator')
    def output = new File("target/xe_domains.html")
    def newDate = new Date()
    def appName = grails.util.Metadata.current.'app.name'
    def grailsVersion = grails.util.Metadata.current.'app.grails.version'
    output.write "<!DOCTYPE html>${ln}"
    output.append "<html>${ln}"
    output.append "<body>${ln}"
    output.append""" <head>
                      <title>Domain Table Entity Report</title>
                      <style type="text/css">
                            .report {
                                border: solid 1px #DDEEEE;
                                border-collapse: collapse;
                                border-spacing: 0;
                                font: normal 13px Arial, sans-serif;
                                background-color:#F7F7F7;
                            }
                            .report thead th {
                                background-color: #DDEFEF;
                                border: solid 1px #DDEEEE;
                                color: #336B6B;
                                padding: 10px;
                                text-align: left;
                                text-shadow: 1px 1px 1px #fff;
                            }
                            .report tbody td {
                                border: solid 1px #DDEEEE;
                                color: #333;
                                padding: 10px;
                                text-shadow: 1px 1px 1px #fff;
                            }
                            .report-highlight tbody tr:hover {
                                background-color: #CCE7E7;
                            }
                            .report-horizontal tbody td {
                                border-left: none;
                                border-right: none;
                            }
                      </style>
              </head>${ln}"""
    output.append "<h1>${appName}  Grails Version ${grailsVersion}        ${newDate}<h1>${ln}"
    output.append "<h2>Base Directory ${System.properties['base.dir']}<h2>${ln}"
    List domains = []

    def plugins = new File(System.properties['base.dir'] + "/plugins")


    plugins.eachDir { plugin ->
        if ( plugin.isDirectory() ) {
            def pluginName   = plugin.toString().split("/")[-1]

            def srcfolder = plugin.toString() + '/src/groovy/net/hedtech/banner'
            def srcDir = new File(srcfolder)
            if (srcDir.isDirectory()) {
                domains = listDir(srcDir,   pluginName )
            }
            if ( domains.size()) {
                output.append "<h2>${ln}Plugin ${pluginName}<h2>${ln}"
                output.append """<table class="report report-horizontal report-highlight">${ln}"""
                output.append "<thead>${ln}"
                output.append "<tr>${ln}"
                output.append "   <th>Package</th>${ln}"
                output.append "   <th>Class Name</th>${ln}"
                output.append "   <th>Table or View</th>${ln}"
                output.append "</tr>${ln}"
                output.append "</thead>${ln}"
                domains.each {
                    output.append "<tbody>${ln}"
                    output.append "<tr>${ln}"
                    output.append "   <td>${it.package}</td>${ln}"
                    output.append "   <td>${it.className}</td>${ln}"
                    output.append "   <td>${it.tableName}</td>${ln}"
                    output.append "</tr>${ln}"
                    output.append "</tbody>${ln}"
                }
                output.append """</table>${ln}"""
            }
            domains = []
        }
    }
    output.append "</body>${ln}"
    println "XE Domain List Report ${output} created."
}

setDefaultTarget "main"


def listDir(def dir,   def pluginName) {
    List domains = []
    dir.eachFileRecurse { src ->
        if (src.isDirectory()) {
            groovylist = dir.listFiles(new GlobFilenameFilter('*.groovy') as FilenameFilter)
            if (groovylist.size() > 0) {
                groovylist.each {
                   def rec =  reportFile(it, pluginName)
                   if ( rec ) domains << rec
                }
            } else {
                List subDomains = []
                subDomains = listDir(src, pluginName)
                if ( subDomains.size() > 0 ){
                    domains.addAll(subDomains)
                }
            }
        } else {
            if (src.isFile()) {
                def ext = src.name.tokenize('.')[-1]
                if (ext == "groovy") {
                   def rec =  reportFile(src, pluginName)
                   if ( rec ) domains << rec
                }
            }
        }
    }
    return domains
}



def reportFile(fileName, pluginName) {
    def parentDir = fileName.parent
    def className  =  fileName.name.tokenize(".")[0]
    def ext = fileName.name.tokenize('.')[-1]
    def packageName
    def tableName
    fileName.eachLine { line ->
        if ( line =~ "package" && line =~ "net.hedtech.banner"){
            def stPack = line.indexOf("net.hedtech.banner")
            if ( stPack ) packageName = line.substring(stPack)
        }
        if ( line =~ "@Table"){
            def st = line.indexOf("\"")
            if ( st ) {
                def tab1 = line.substring(st + 1)
                tableName = tab1.replace("\"", "").replace(")", "")
            }
        }
    }
    if ( packageName && tableName) {
        return [pluginName: pluginName, package:  packageName , className:  className , tableName:  tableName ]
    }
    else return ""
}


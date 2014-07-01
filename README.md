<!--- 
Copyright 2014 Ellucian Company L.P. and its affiliates. 
-->
#Global AST Transformation - Banner Domain Extension Tool.

This is not a standard Banner project and some special steps need documentation.

* Compile to create lib/bannerTransform.jar
    1. Delete lib/bannerTransform.jar manually (the jar is in use when running ant in a script)
    2. grails compile ast

* Execute Unit test
    1. set environment variable  BANNER_TRANSFORMATION1=test/ast-definition/test-base.xml
    2. set environment variable  BANNER_TRANSFORMATION2=test/ast-definition/test-additional.xml
    3. grails clean
    4. grails compile all
    5. grails test-app unit: -echoOut
    
* Execute Integration test
    1. set environment variable  BANNER_TRANSFORMATION1=test/ast-definition/banner-transformation-rules1.xml
    2. grails clean
    3. grails compile all
    4. grails test-app integration: -echoOut

##Documentation
* Go to the Ellucian Client Support web site: http://www.ellucian.com/Solutions/Ellucian-Client-Support.

* Click the Ellucian Hub button to log in to the Ellucian Hub.

* Choose the Ellucian Support Center application.

* Documentation for the Domain Extension Tool is in the Banner Extensibility library

##Example XML files
Some example XML files with transformation rules used in testing are included in the domain_extension plugin source:

* test/ast-definition/banner-transformation-rules1.xml
* test/ast-definition/test-base.xml
* test/ast-definition/test-additional.xml

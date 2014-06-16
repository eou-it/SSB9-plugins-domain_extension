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

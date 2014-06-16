<!--- 
Copyright 2014 Ellucian Company L.P. and its affiliates. 
-->
#Global AST Transformation - Banner Domain Extension Tool.
Initial version created by Amrit for Grails 1.3.7

This is not a standard Banner project and some special steps need documentation.

* Compile to create lib/bannerTransform.jar
    1. Delete lib/bannerTransform.jar manually (the jar is in use when running ant in a script)
    2. grails compile ast

* Test with sample domains
    1. set environment variable  BANNER_TRANSFORMATION=test/ast-definition/test.xml
    2. grails clean
    3. grails compile all
    4. grails test-app integration: -echoOut

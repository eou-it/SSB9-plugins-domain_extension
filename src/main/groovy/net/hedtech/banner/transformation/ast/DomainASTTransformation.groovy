/*
 * Copyright 2014-2020 Ellucian Company L.P. and its affiliates.
 */

package net.hedtech.banner.transformation.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.CompilePhase
import org.hibernate.annotations.Type

import javax.persistence.Column
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Temporal
import javax.persistence.TemporalType
import javax.persistence.Transient

public class DomainASTTransformation {

    private static List fieldNameBlackList = ['id'] //, 'lastModified', 'lastModifiedBy', 'version', 'dataOrigin'


    static ClassNode applyTransformation(ClassNode classNode, Map rules) {
        if (classNode && rules) {
            applyTransformationForTableOrView(classNode, rules.tableOrView)
            applyTransformationForFields(classNode, rules.fields)
            applyTransformationForNamedQueries(classNode, rules.namedQueries)
            applyTransformationForMethods(classNode, rules.methods)
        }
    }


    private static void applyTransformationForTableOrView(ClassNode classNode, String tableOrViewName) {
        if (tableOrViewName) {
            AnnotationNode tableNode = BannerASTUtils.retrieveTable(classNode)
            println "Replace Table/View: ${ tableNode?.members?.name?.text } with: ${ tableOrViewName }"
            tableNode?.members?.clear()
            tableNode?.addMember('name', new ConstantExpression(tableOrViewName))
        }
    }


    private static void applyTransformationForFields(ClassNode classNode, Map fields) {
        if (fields) {
            fields.each {String fieldName, Map fieldMetaData ->
                if (!fieldNameBlackList.contains(fieldName)) {
                    addOrModifyProperty(classNode, fieldName, fieldMetaData)
                }
            }
        }
    }


    private static void applyTransformationForNamedQueries(ClassNode classNode, Map namedQueries) {
        if (namedQueries) {
            namedQueries.each {String namedQueryName, String namedQueryQuery ->
                addOrReplaceNamedQuery(classNode, namedQueryName, namedQueryQuery)
            }
        }
    }


    private static void applyTransformationForMethods(ClassNode classNode, List methods) {
        if (methods) {
            methods.each {String methodSource ->
                MethodNode methodNode = makeMethod(classNode, methodSource)
                MethodNode existingMethod = classNode.getMethod(methodNode.name, methodNode.parameters)
                if (existingMethod) {
                    //clone relevant parts
                    existingMethod.setCode(methodNode.code)
                    existingMethod.setModifiers(methodNode.modifiers)
                    existingMethod.setVariableScope(methodNode.variableScope)
                    println "Modify method:      $methodNode.name($methodNode.parameters.name)"
                } else {
                    classNode.addMethod(methodNode)
                    println "Add method:         $methodNode.name($methodNode.parameters.name)"
                }
            }
        }
    }


    private static PropertyNode addOrModifyProperty(ClassNode classNode, String propertyName, Map propertyMetaData) {
        boolean existingProperty = BannerASTUtils.isExistingProperty(classNode, propertyName)

        if (existingProperty) {
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Column.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Temporal.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Transient.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Type.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, ManyToOne.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, OneToOne.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, JoinColumns.name)

            BannerASTUtils.removeConstraintExpressionsForProperty(classNode, propertyName)
        }
        addProperty(classNode, propertyName, propertyMetaData, existingProperty)
    }


    private static PropertyNode addProperty(ClassNode classNode, String propertyName, Map propertyMetaData, boolean existingProperty = false) {
        PropertyNode propertyNode = existingProperty ? BannerASTUtils.retrieveProperty(classNode, propertyName) : BannerASTUtils.addProperty(classNode, propertyName, propertyMetaData)
        if (!propertyNode) {
            println "Property $propertyName not created"
            return null
        }

        FieldNode fieldNode = propertyNode.field

        //default value
        if (propertyMetaData.containsKey("defaultValue")) {
            fieldNode.setInitialValueExpression(new ConstantExpression(propertyMetaData.defaultValue))
        }

        if (propertyMetaData.containsKey("transient") && propertyMetaData.transient == true) {          //add annotation @Transient
            BannerASTUtils.addAnnotationToProperty(propertyNode, Transient, [:])
        } else if (propertyMetaData.containsKey("persistenceProperties")) {                             //add annotation @Column
            BannerASTUtils.addAnnotationToProperty(propertyNode, Column, propertyMetaData.persistenceProperties ?: [:])
        } else if (propertyMetaData.containsKey("manyToOneProperties")) {                               //add annotations @ManyToOne, @JoinColumns
            BannerASTUtils.addAnnotationToProperty(propertyNode, ManyToOne, [:])

            ListExpression listExpression = new ListExpression()
            //annotations @JoinColumn
            propertyMetaData.manyToOneProperties.each {joinColumnMetaData ->
                AnnotationNode joinColumn = new AnnotationNode(new ClassNode(JoinColumn))
                joinColumnMetaData.each {attribute, value ->
                    joinColumn.addMember(attribute, new ConstantExpression(value))
                }
                listExpression.addExpression(new AnnotationConstantExpression(joinColumn))
            }
            //add annotation @JoinColumns
            BannerASTUtils.addAnnotationToProperty(propertyNode, JoinColumns, [value: listExpression])
        } else if (propertyMetaData.containsKey("oneToOneProperties")) {                               //add annotations @OneToOne, @JoinColumns
            BannerASTUtils.addAnnotationToProperty(propertyNode, OneToOne, [:])

            ListExpression listExpression = new ListExpression()
            //annotations @JoinColumn
            propertyMetaData.oneToOneProperties.each {joinColumnMetaData ->
                AnnotationNode joinColumn = new AnnotationNode(new ClassNode(JoinColumn))
                joinColumnMetaData.each {attribute, value ->
                    joinColumn.addMember(attribute, new ConstantExpression(value))
                }
                listExpression.addExpression(new AnnotationConstantExpression(joinColumn))
            }
            //add annotation @JoinColumns
            BannerASTUtils.addAnnotationToProperty(propertyNode, JoinColumns, [value: listExpression])
        }

        if (fieldNode.getType().name == Date.name) {                    //handle @Temporal annotation for java.util.Date
            String temporalType = propertyMetaData.temporalType ?: "DATE"
            PropertyExpression expression = new AstBuilder().buildFromString("${ TemporalType.name }.${ temporalType }")?.get(0)?.getStatements()?.get(0)?.getExpression()
            BannerASTUtils.addAnnotationToProperty(propertyNode, Temporal, [value: expression])
        } else if (fieldNode.getType().name == Boolean.name) {          //handle @org.hibernate.annotations.Type annotation for java.lang.Boolean
            String booleanType = propertyMetaData.booleanType ?: "yes_no"
            BannerASTUtils.addAnnotationToProperty(propertyNode, Type, [type: booleanType])
        }

        BannerASTUtils.addConstraintsForProperty(classNode, propertyName, propertyMetaData.constraintExpression)
        String addOrReplacePropertyText = existingProperty ? "Replace property:   " : "Add property:       "
        println addOrReplacePropertyText + propertyName.padRight(25) + "-- Type: " + fieldNode.type
        return classNode.getProperty(propertyName)
    }


    private static AnnotationNode addOrReplaceNamedQuery(ClassNode classNode, String namedQueryName, String namedQueryQuery) {
        if (!namedQueryName.startsWith(classNode.getNameWithoutPackage())) {
            namedQueryName = classNode.getNameWithoutPackage() + "." + namedQueryName
        }
        // Check for a NamedQueries Annotation in the domain
        AnnotationNode existingNamedQueriesNode = BannerASTUtils.retrieveNamedQueries(classNode)
        if (!existingNamedQueriesNode) {
            // Check if there's a single NamedQuery Annotation (without NamedQueries) defined in the domain
            AnnotationNode singleNamedQueryNode = BannerASTUtils.retrieveSingleNamedQuery(classNode)
            if (singleNamedQueryNode) {                    // If found then remove the NamedQuery from the domain
                BannerASTUtils.removeNamedQueryFromClassNode(classNode, singleNamedQueryNode)
            }
            // Create a new NamedQueries annotation and add either the removed NamedQuery or a new NamedQuery
            AnnotationNode namedQueryNode = singleNamedQueryNode ?: BannerASTUtils.createNamedQuery(namedQueryName, namedQueryQuery)
            BannerASTUtils.createNamedQueriesAndAddNamedQuery(classNode, namedQueryNode)
        }
        AnnotationNode namedQueriesNode = existingNamedQueriesNode ?: BannerASTUtils.retrieveNamedQueries(classNode)
        // Check for existing NamedQuery
        AnnotationConstantExpression existingNamedQuery = BannerASTUtils.retrieveNamedQuery(classNode, namedQueryName)
        if (existingNamedQuery) {
            // remove existingNamedQuery from NamedQueries
            BannerASTUtils.removeNamedQueryFromNamedQueries(namedQueriesNode, existingNamedQuery)
        }
        if (existingNamedQuery && existingNamedQueriesNode) {
            println "Replace NamedQuery: " + namedQueryName
        } else {
            println "Add NamedQuery:     " + namedQueryName
        }
        // create new NamedQuery based on values from XML
        BannerASTUtils.addNamedQueryToNamedQueries(namedQueriesNode, namedQueryName, namedQueryQuery)
    }


    private static MethodNode makeMethod(ClassNode classNode, String methodSource) {
        String errorMessage = ""
        String packageText = (classNode?.packageName) ? "package $classNode.packageName" : ""

        String code = """
                        $packageText
                        class $classNode.nameWithoutPackage {
                            $methodSource
                        }
                   """
        MethodNode result
        try {
            //It doesn't seem possible to use other Domain classes in methodSource
            //Tried several CompilePhase's but when too early, no methods nodes are generated, when later,
            //an error occurs: Apparent variable 'Department' was found in a static scope but doesn't refer to a local variable.
            List<ASTNode> nodes = new AstBuilder().buildFromString(CompilePhase.CLASS_GENERATION, true, code)
            // get text from start of source code that should have the method name
            String methodSourceNameFragment = methodSource.substring(0, methodSource.indexOf("("))
            result = nodes[1]?.methods.find {method -> methodSourceNameFragment.contains(method.name)}

        } catch (e) {
            errorMessage = e.getMessage()
        }
        if (!result)
            println "Error making method:\n $errorMessage"
        return result
    }
}

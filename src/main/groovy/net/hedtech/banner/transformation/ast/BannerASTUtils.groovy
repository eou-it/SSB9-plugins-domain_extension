/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

package net.hedtech.banner.transformation.ast

import org.apache.commons.lang.ClassUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table

import static org.springframework.asm.Opcodes.ACC_PRIVATE
import static org.springframework.asm.Opcodes.ACC_PUBLIC

class BannerASTUtils extends org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils {

    static AnnotationNode retrieveTable(ClassNode classNode) {
        AnnotationNode tableNode = classNode.getAnnotations().find {
            it.classNode?.name?.equals(Table.name)
        }
        return tableNode
    }


    static AnnotationNode retrieveNamedQueries(ClassNode classNode) {
        AnnotationNode namedQueriesNode = classNode.getAnnotations().find {
            it.classNode?.name?.equals(NamedQueries.name)
        }
        return namedQueriesNode
    }


    static AnnotationConstantExpression retrieveNamedQuery(ClassNode classNode, String namedQueryName) {
        AnnotationConstantExpression annotationConstantExpression = retrieveNamedQueries(classNode)?.members?.value?.expressions?.find {
            it?.value?.members?.name?.text?.equals(namedQueryName)
        }
        return annotationConstantExpression
    }


    static AnnotationNode retrieveSingleNamedQuery(ClassNode classNode) {
        AnnotationNode namedQueryNode = classNode.getAnnotations().find {
            it.classNode?.name?.equals(NamedQuery.name)
        }
        return namedQueryNode
    }


    static AnnotationNode createNamedQuery(String namedQueryName, String namedQueryQuery) {
        AnnotationNode newNamedQuery = new AnnotationNode(new ClassNode(NamedQuery))
        newNamedQuery.addMember('name', new ConstantExpression(namedQueryName))
        newNamedQuery.addMember('query', new ConstantExpression(namedQueryQuery))

        return newNamedQuery
    }


    static ClassNode createNamedQueriesAndAddNamedQuery(ClassNode classNode, AnnotationNode namedQuery) {
        AnnotationNode namedQueries = new AnnotationNode(new ClassNode(NamedQueries))
        ListExpression listExpression = new ListExpression()
        listExpression.addExpression(new AnnotationConstantExpression(namedQuery))
        namedQueries.setMember('value', listExpression)
        classNode.addAnnotation(namedQueries)

        return classNode
    }


    static AnnotationNode addNamedQueryToNamedQueries(AnnotationNode namedQueriesNode, String namedQueryName, String namedQueryQuery) {
        AnnotationNode newNamedQuery = new AnnotationNode(new ClassNode(NamedQuery))
        newNamedQuery.addMember('name', new ConstantExpression(namedQueryName))
        newNamedQuery.addMember('query', new ConstantExpression(namedQueryQuery))
        namedQueriesNode?.members?.value?.addExpression(new AnnotationConstantExpression(newNamedQuery))

        return namedQueriesNode
    }


    static void removeNamedQueryFromClassNode(ClassNode classNode, AnnotationNode singleExistingNamedQuery) {
        classNode.annotations?.remove(singleExistingNamedQuery)
    }


    static void removeNamedQueryFromNamedQueries(AnnotationNode namedQueriesNode, AnnotationConstantExpression existingNamedQuery) {
        namedQueriesNode.members?.value?.expressions?.remove(existingNamedQuery)
    }


    static boolean isExistingProperty(ClassNode classNode, String propertyName) {
        return hasProperty(classNode, propertyName)
    }


    static PropertyNode retrieveProperty(ClassNode classNode, String propertyName) {
        if (isExistingProperty(classNode, propertyName)) {
            return classNode.getProperty(propertyName)
        }
    }


    static PropertyNode addProperty(ClassNode classNode, String propertyName, Map propertyMetaData) {
        ClassNode newClassNode
        try {
            newClassNode = new ClassNode(ClassUtils.getClass(this.classLoader, propertyMetaData.type))
        } catch (e) {
            newClassNode = ClassHelper.make(propertyMetaData.type)
        }

        if (newClassNode) {
            FieldNode field = new FieldNode(propertyName, ACC_PRIVATE, newClassNode, new ClassNode(classNode.getClass()), null)
            classNode.addProperty(new PropertyNode(field, ACC_PUBLIC, null, null))
            return classNode.getProperty(propertyName)
        } else {
            return null
        }
    }


    static AnnotationNode addAnnotationToProperty(PropertyNode propertyNode, String annotation, Map annotationAttributes) {
        FieldNode fieldNode = propertyNode.getField()
        AnnotationNode annotationNode = new AnnotationNode(new ClassNode(ClassUtils.getClass(annotation)))
        annotationAttributes?.each {attribute, value ->
            def expression = value
            if (value instanceof String) {
                expression = new ConstantExpression(value)
            }
            annotationNode.addMember(attribute, expression)
        }
        fieldNode.addAnnotation(annotationNode)

        return annotationNode
    }


    static FieldNode retrieveConstraintField(ClassNode classNode) {
        classNode?.getProperty(GrailsDomainClassProperty.CONSTRAINTS)?.getField()
    }


    static ArrayList retrieveConstraintExpressionsForProperty(ClassNode classNode, String propertyName) {
        ArrayList expressions = []
        FieldNode constraintsField = retrieveConstraintField(classNode)
        if (constraintsField) {
            expressions = constraintsField.initialValueExpression.getCode().statements.findAll {statement ->
                statement.expression.method.value == propertyName
            }
        }
        return expressions
    }


    static void removeConstraintExpressionsForProperty(ClassNode classNode, String propertyName) {
        ArrayList statements = retrieveConstraintExpressionsForProperty(classNode, propertyName)
        if (statements) {
            retrieveConstraintField(classNode).initialValueExpression.getCode().statements.removeAll(statements)
        }
    }


    static AnnotationNode retrieveAnnotationForProperty(ClassNode classNode, String propertyName, String annotation) {
        AnnotationNode annotationForProperty = retrieveProperty(classNode, propertyName)?.field?.getAnnotations()?.find {annotationNode ->
            annotationNode.classNode.name == annotation
        }
        return annotationForProperty
    }


    static void removeAnnotationForProperty(ClassNode classNode, String propertyName, String annotation) {
        retrieveProperty(classNode, propertyName)?.field?.getAnnotations()?.remove(retrieveAnnotationForProperty(classNode, propertyName, annotation))
    }


    static void addConstraintsForProperty(ClassNode classNode, String propertyName, constraintExpressionSource) {
        if (classNode && !StringUtils.isBlank(propertyName) && !StringUtils.isBlank(constraintExpressionSource)) {
            ArrayList statements = retrieveProperty(classNode, GrailsDomainClassProperty.CONSTRAINTS)?.field?.getInitialExpression()?.getCode()?.getStatements()
            if (statements) {
                MethodCallExpression expression = new AstBuilder().buildFromString(constraintExpressionSource)?.get(0)?.getStatements()?.get(0)?.getExpression()
                if (expression) {
                    statements.add(new ExpressionStatement(expression))
                }
            }
        }
    }
}

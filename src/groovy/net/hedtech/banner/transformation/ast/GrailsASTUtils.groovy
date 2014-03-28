package net.hedtech.banner.transformation.ast

import org.apache.commons.lang.ClassUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Transient

import static org.springframework.asm.Opcodes.ACC_PRIVATE
import static org.springframework.asm.Opcodes.ACC_PUBLIC

class GrailsASTUtils extends org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils {

    static boolean isJpaDomainClass(ClassNode classNode) {
        def entityAnnotation = classNode?.annotations?.find { annotation ->
            annotation.classNode.name == Entity.name
        }
        return entityAnnotation ? true : false
    }

    static boolean isExistingProperty(ClassNode classNode, String propertyName) {
        return hasProperty(classNode, propertyName)
    }


    static def retrieveProperty(ClassNode classNode, String propertyName) {
        if (!isExistingProperty(classNode, propertyName)) {
            return null
        }

        return classNode.getProperty(propertyName)
    }


    static def addProperty(ClassNode classNode, String propertyName, Map propertyMetaData) {
        ClassNode newClassNode
        try {
            newClassNode = new ClassNode(ClassUtils.getClass(this.classLoader, propertyMetaData.type))
        } catch (e)  {
            //org.codehaus.groovy.ast.ModuleNode module = classNode.getModule();
            //def unit=classNode.getCompileUnit()
            //newClassNode = unit.getClass(propertyMetaData.type)
            newClassNode = ClassHelper.make(propertyMetaData.type)
            println "new node: $newClassNode"
        }

        if (newClassNode) {
            FieldNode field = new FieldNode(propertyName, ACC_PRIVATE, newClassNode , new ClassNode(classNode.getClass()), null)
//        FieldNode field = new FieldNode(propertyName, ACC_PRIVATE, ClassHelper.make(propertyMetaData.type), new ClassNode(classNode.getClass()), null)
        classNode.addProperty(new PropertyNode(field, ACC_PUBLIC, null, null))
        return classNode.getProperty(propertyName)
        } else {
            return null
        }
    }


    static def addAnnotationToProperty(PropertyNode propertyNode, String annotation, Map annotationAttributes) {
        FieldNode fieldNode = propertyNode.getField()
        AnnotationNode annotationNode = new AnnotationNode(new ClassNode(ClassUtils.getClass(annotation)))
        annotationAttributes?.each { attribute, value ->
            def expression = value
            if (value instanceof String) {
                expression = new ConstantExpression(value)
            }
            annotationNode.addMember(attribute, expression)
        }

        fieldNode.addAnnotation(annotationNode)

        return annotationNode
    }


    static def retrieveConstraintField(ClassNode classNode) {
        classNode?.getProperty(GrailsDomainClassProperty.CONSTRAINTS)?.getField()
    }


    static def retrieveConstraintExpressionsForProperty(ClassNode classNode, String propertyName) {
        def expressions = []
        def constraintsField = retrieveConstraintField(classNode)
        if (constraintsField) {
            expressions = constraintsField.initialValueExpression.getCode().statements.findAll { statement ->
                statement.expression.method.value == propertyName
            }
        }
        return expressions
    }


    static void removeConstraintExpressionsForProperty(ClassNode classNode, String propertyName) {
        def statements = retrieveConstraintExpressionsForProperty(classNode, propertyName)
        if (statements) {
            retrieveConstraintField(classNode).initialValueExpression.getCode().statements.removeAll(statements)
        }
    }


    static def retrieveAnnotationForProperty(ClassNode classNode, String propertyName, String annotation) {
        def temp = retrieveProperty(classNode, propertyName)?.field?.getAnnotations()?.find { annotationNode ->
            annotationNode.classNode.name == annotation
        }

        return temp
    }


    static void removeAnnotationForProperty(ClassNode classNode, String propertyName, String annotation) {
        retrieveProperty(classNode, propertyName)?.field?.getAnnotations()?.remove(retrieveAnnotationForProperty(classNode, propertyName, annotation))
    }


    static void addConstraintsForProperty(ClassNode classNode, String propertyName, constraintExpressionSource) {
        if (!classNode || StringUtils.isBlank(propertyName)  || StringUtils.isBlank(constraintExpressionSource)) {
            return
        }

        def statements = retrieveProperty(classNode, GrailsDomainClassProperty.CONSTRAINTS)?.field?.getInitialExpression()?.getCode()?.getStatements()
        if (!statements) {
            return
        }

        def expression = new AstBuilder().buildFromString(constraintExpressionSource)?.get(0)?.getStatements()?.get(0)?.getExpression()
//        def expression = new AstBuilder().buildFromString(CompilePhase.CONVERSION, true, constraintExpressionSource)?.get(0)?.getStatements()?.get(0)?.getExpression()
        if (expression) {
            statements.add(new ExpressionStatement(expression))
        }
    }
}

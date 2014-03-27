package net.hedtech.banner.transformation.ast

import org.apache.commons.lang.ClassUtils
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.hibernate.annotations.Type

import javax.persistence.Column
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne
import javax.persistence.Temporal
import javax.persistence.TemporalType
import javax.persistence.Transient

import static org.springframework.asm.Opcodes.ACC_PRIVATE
import static org.springframework.asm.Opcodes.ACC_PUBLIC

public class DomainASTTransformation {

    private List fieldNameBlackList = ['id'] //, 'lastModified', 'lastModifiedBy', 'version', 'dataOrigin'


    def applyTransformation(ClassNode classNode, Map rules) {
        if (!classNode || !rules) {
            return
        }

        // fields
        applyTransformationForFields(classNode, rules.fields)

        //named queries
        //methods
    }


    private void applyTransformationForFields(ClassNode classNode, Map fields) {
        if (!fields) {
            return
        }

        fields.each { String fieldName, Map fieldMetaData ->
            if (!fieldNameBlackList.contains(fieldName)) {
                addOrModifyProperty(classNode, fieldName, fieldMetaData)
            }
        }
    }


    private def addOrModifyProperty(ClassNode classNode, String propertyName, Map propertyMetaData) {
        boolean existingProperty = GrailsASTUtils.isExistingProperty(classNode, propertyName)

        if (existingProperty) {
            GrailsASTUtils.removeAnnotationForProperty(classNode, propertyName, Column.name)
            GrailsASTUtils.removeAnnotationForProperty(classNode, propertyName, Temporal.name)
            GrailsASTUtils.removeAnnotationForProperty(classNode, propertyName, Transient.name)
            GrailsASTUtils.removeAnnotationForProperty(classNode, propertyName, Type.name)

            GrailsASTUtils.removeConstraintExpressionsForProperty(classNode, propertyName)
        }

        addProperty(classNode, propertyName, propertyMetaData, existingProperty)
    }


    private def addProperty(ClassNode classNode, String propertyName, Map propertyMetaData, boolean existingProperty = false) {
        PropertyNode propertyNode = existingProperty ? GrailsASTUtils.retrieveProperty(classNode, propertyName) : GrailsASTUtils.addProperty(classNode, propertyName, propertyMetaData)
        FieldNode fieldNode = propertyNode.field

        //default value
        if (propertyMetaData.containsKey("defaultValue")) {
            fieldNode.setInitialValueExpression(new ConstantExpression(propertyMetaData.defaultValue))
        }

        if (propertyMetaData.containsKey("transient") && propertyMetaData.transient == true) {          //add annotation @Transient
            GrailsASTUtils.addAnnotationToProperty(propertyNode, Transient.name, [:])
        } else if (propertyMetaData.containsKey("persistenceProperties")) {                             //add annotation @Column
            GrailsASTUtils.addAnnotationToProperty(propertyNode, Column.name, propertyMetaData.persistenceProperties ?: [:])
        } else if (propertyMetaData.containsKey("manyToOneProperties")) {                               //add annotations @ManyToOne, @JoinColumns
            GrailsASTUtils.addAnnotationToProperty(propertyNode, ManyToOne.name, [:])

            ListExpression listExpression = new ListExpression()
            //annotations @JoinColumn
            propertyMetaData.manyToOneProperties.each { joinColumnMetaData ->
                AnnotationNode joinColumn = new AnnotationNode(new ClassNode(JoinColumn))
                joinColumnMetaData.each { attribute, value ->
                    joinColumn.addMember(attribute, new ConstantExpression(value))
                }
                listExpression.addExpression(new AnnotationConstantExpression(joinColumn))
            }

            //add annotation @JoinColumns
            GrailsASTUtils.addAnnotationToProperty(propertyNode, JoinColumns.name, [value: listExpression])
        }

        if (fieldNode.getType().name == Date.name) {                    //handle @Temporal annotation for java.util.Date
            def temporalType = propertyMetaData.temporalType ?: "DATE"
            def expression = new AstBuilder().buildFromString("${TemporalType.name}.${temporalType}")?.get(0)?.getStatements()?.get(0)?.getExpression()
//            def expression = new AstBuilder().buildFromString(CompilePhase.CONVERSION, true, "${TemporalType.name}.${temporalType}")?.get(0)?.getStatements()?.get(0)?.getExpression()
            GrailsASTUtils.addAnnotationToProperty(propertyNode, Temporal.name, [value: expression])
        } else if (fieldNode.getType().name == Boolean.name) {          //handle @org.hibernate.annotations.Type annotation for java.lang.Boolean
            def booleanType = propertyMetaData.booleanType ?: "yes_no"
            GrailsASTUtils.addAnnotationToProperty(propertyNode, Type.name, [type: booleanType])
        }

        GrailsASTUtils.addConstraintsForProperty(classNode, propertyName, propertyMetaData.constraintExpression)

        return classNode.getProperty(propertyName)
    }
}
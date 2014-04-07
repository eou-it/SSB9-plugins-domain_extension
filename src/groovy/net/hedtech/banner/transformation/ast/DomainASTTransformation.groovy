package net.hedtech.banner.transformation.ast

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
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
        boolean existingProperty = BannerASTUtils.isExistingProperty(classNode, propertyName)

        if (existingProperty) {
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Column.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Temporal.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Transient.name)
            BannerASTUtils.removeAnnotationForProperty(classNode, propertyName, Type.name)

            BannerASTUtils.removeConstraintExpressionsForProperty(classNode, propertyName)
        }

        addProperty(classNode, propertyName, propertyMetaData, existingProperty)
    }


    private def addProperty(ClassNode classNode, String propertyName, Map propertyMetaData, boolean existingProperty = false) {
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
            BannerASTUtils.addAnnotationToProperty(propertyNode, Transient.name, [:])
        } else if (propertyMetaData.containsKey("persistenceProperties")) {                             //add annotation @Column
            BannerASTUtils.addAnnotationToProperty(propertyNode, Column.name, propertyMetaData.persistenceProperties ?: [:])
        } else if (propertyMetaData.containsKey("manyToOneProperties")) {                               //add annotations @ManyToOne, @JoinColumns
            BannerASTUtils.addAnnotationToProperty(propertyNode, ManyToOne.name, [:])

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
            BannerASTUtils.addAnnotationToProperty(propertyNode, JoinColumns.name, [value: listExpression])
        } else if (propertyMetaData.containsKey("oneToOneProperties")) {                               //add annotations @OneToOne, @JoinColumns
            BannerASTUtils.addAnnotationToProperty(propertyNode, OneToOne.name, [:])

            ListExpression listExpression = new ListExpression()
            //annotations @JoinColumn
            propertyMetaData.oneToOneProperties.each { joinColumnMetaData ->
                AnnotationNode joinColumn = new AnnotationNode(new ClassNode(JoinColumn))
                joinColumnMetaData.each { attribute, value ->
                    joinColumn.addMember(attribute, new ConstantExpression(value))
                }
                listExpression.addExpression(new AnnotationConstantExpression(joinColumn))
            }

            //add annotation @JoinColumns
            BannerASTUtils.addAnnotationToProperty(propertyNode, JoinColumns.name, [value: listExpression])
        }

        if (fieldNode.getType().name == Date.name) {                    //handle @Temporal annotation for java.util.Date
            def temporalType = propertyMetaData.temporalType ?: "DATE"
            def expression = new AstBuilder().buildFromString("${TemporalType.name}.${temporalType}")?.get(0)?.getStatements()?.get(0)?.getExpression()
//            def expression = new AstBuilder().buildFromString(CompilePhase.CONVERSION, true, "${TemporalType.name}.${temporalType}")?.get(0)?.getStatements()?.get(0)?.getExpression()
            BannerASTUtils.addAnnotationToProperty(propertyNode, Temporal.name, [value: expression])
        } else if (fieldNode.getType().name == Boolean.name) {          //handle @org.hibernate.annotations.Type annotation for java.lang.Boolean
            def booleanType = propertyMetaData.booleanType ?: "yes_no"
            BannerASTUtils.addAnnotationToProperty(propertyNode, Type.name, [type: booleanType])
        }

        BannerASTUtils.addConstraintsForProperty(classNode, propertyName, propertyMetaData.constraintExpression)

        return classNode.getProperty(propertyName)
    }
}
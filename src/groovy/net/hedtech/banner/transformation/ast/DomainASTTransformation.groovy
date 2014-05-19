package net.hedtech.banner.transformation.ast

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
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

    private List fieldNameBlackList = ['id'] //, 'lastModified', 'lastModifiedBy', 'version', 'dataOrigin'


    def applyTransformation(ClassNode classNode, Map rules) {
        if (!classNode || !rules ) {
            return
        }
        //table (or view) definition
        applyTransformationForTableOrView(classNode, rules.tableOrView)
        //named queries
        applyTransformationForNamedQueries(classNode, rules.namedQueries)
        // fields
        applyTransformationForFields(classNode, rules.fields)
        //methods
        applyTransformationForMethods(classNode, rules.methods)
    }

    private void applyTransformationForMethods(ClassNode classNode, methods) {
        if (!methods) {
            return
        }
        methods.each { method ->
            MethodNode m = makeMethod(classNode, method.name, method.source)
            def existingMethod = classNode.getMethod(method.name, m.parameters)
            if (existingMethod) {
                //clone relevant parts
                existingMethod.setCode( m.code )
                existingMethod.setModifiers(m.modifiers)
                existingMethod.setVariableScope(m.variableScope)
            } else {
                classNode.addMethod(m)
            }
            println "added/modified added method $method.name"
        }
    }

    private MethodNode makeMethod( ClassNode classNode, String methodName, String methodSource )  {
        def errorMessage=""
        def code = """
                        package $classNode.packageName
                        class $classNode.nameWithoutPackage {
                            $methodSource
                        }
                   """
        def result
        try {
            //It doesn't seem possible to use other Domain classes in methodSource
            //Tried several CompilePhase's but when too early, no methods nodes are generated, when later,
            //an error occurs: Apparent variable 'Department' was found in a static scope but doesn't refer to a local variable.
            def nodes = new AstBuilder().buildFromString(CompilePhase.CLASS_GENERATION, true, code)
            result = nodes[1].methods.find{it.name == methodName}
        }  catch (e) {
            errorMessage=e.getMessage()
        }
        if (!result)
            println "Error making $methodName:\n $errorMessage"
        return result
    }


    private void applyTransformationForTableOrView(ClassNode classNode, String tableOrViewName) {
        if (!tableOrViewName) {
            return
        }
        // Check for Table annotation on ClassNode (domain)
        AnnotationNode tableNode = BannerASTUtils.retrieveTable(classNode)
        // Remove the Table annotations' members
        tableNode?.members?.clear()
        // Add a new member based on Table or View name from XML file
        tableNode?.addMember('name', new ConstantExpression(tableOrViewName))
    }


    private void applyTransformationForNamedQueries(ClassNode classNode, Map namedQueries) {
        if (!namedQueries) {
            return
        }
        namedQueries.each {String namedQueryName, String namedQueryQuery ->
            addOrReplaceNamedQuery(classNode, namedQueryName, namedQueryQuery)
        }
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


    private def addOrReplaceNamedQuery(ClassNode classNode, String namedQueryName, String namedQueryQuery) {
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
        // create new NamedQuery based on values from XML
        BannerASTUtils.addNamedQueryToNamedQueries(namedQueriesNode, namedQueryName, namedQueryQuery)
    }


    private def addOrModifyProperty(ClassNode classNode, String propertyName, Map propertyMetaData) {
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
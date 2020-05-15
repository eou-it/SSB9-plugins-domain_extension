/*
 * Copyright 2014-2020 Ellucian Company L.P. and its affiliates.
 */

package net.hedtech.banner.transformation

import domain.extension.Application
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.util.Holders
import org.grails.core.io.support.GrailsFactoriesLoader
import org.grails.datastore.gorm.validation.constraints.ValidatorConstraint
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.validation.discovery.ConstrainedDiscovery
import org.junit.After
import org.junit.Before
import org.junit.Test

import javax.persistence.NamedQueries

import static org.junit.Assert.*

@Integration(applicationClass = Application.class)
@Rollback
class ASTIntegrationTests {

    def                      grailsApplication
    GroovyClassLoader        loader1
    GroovyClassLoader        loader2
    GroovyClassLoader        loader3
    String                   astTestClassDefinition
    String                   astTestLookupClassDefinition
    String                   astTestLookup2ClassDefinition
    GroovyCodeSource         astTestSource
    GroovyCodeSource         astTestLookupSource
    GroovyCodeSource         astTestLookup2Source
    Class                    astTestClass
    Class                    astTestLookupClass
    Class                    astTestLookup2Class
    PersistentEntity         astTestDomainClass
    PersistentEntity         astTestLookupDomainClass
    PersistentEntity         astTestLookup2DomainClass
    Map                      astTestConstrainedProperties
    Map                      astTestLookupConstrainedProperties
    Map                      astTestLookup2ConstrainedProperties

    @Before
    void setUp() {
        ProcessEnvironment.theCaseInsensitiveEnvironment.put("BANNER_TRANSFORMATION1","src/testData/ast-definition/test-base.xml")
        ProcessEnvironment.theCaseInsensitiveEnvironment.put("BANNER_TRANSFORMATION2","src/testData/ast-definition/test-additional.xml")

        ClassLoader parent = this.class.getClassLoader()
        loader1 = new GroovyClassLoader(parent)

        astTestClassDefinition =
                """
        package net.hedtech.banner.transformation
        import javax.persistence.*
        @Entity
        class AstTestClass {
            Long       id
            Long       version
            Long       dummy

            static constraints = {
                dummy(nullable: true)
            }
        }
        """

        astTestLookupClassDefinition =
                """
        package net.hedtech.banner.transformation
        import javax.persistence.*
        @Entity
        class AstTestLookupClass {
            Long       id
            Long       version
            Long       dummy

            static constraints = {
                dummy(nullable: true)
            }
        }
        """

        astTestLookup2ClassDefinition =
                """
        package net.hedtech.banner.transformation
        import javax.persistence.*
        @Entity
        class AstTestLookup2Class {
            Long       id
            Long       version
            Long       dummy

            static constraints = {
                dummy(nullable: true)
            }
        }
        """
        ConstrainedDiscovery constrainedDiscovery = GrailsFactoriesLoader.loadFactory(ConstrainedDiscovery.class)

        grailsApplication = Holders.grailsApplication

        astTestLookupSource = new GroovyCodeSource(astTestLookupClassDefinition, "testscript", "src/groovy/astTestLookupClass")
        astTestLookupClass = loader1.parseClass(astTestLookupSource)
        astTestLookupDomainClass = grailsApplication.mappingContext.addPersistentEntity(astTestLookupClass)
        astTestLookupConstrainedProperties = constrainedDiscovery.findConstrainedProperties(astTestLookupDomainClass)

        loader2 = new GroovyClassLoader(loader1)
        astTestLookup2Source = new GroovyCodeSource(astTestLookup2ClassDefinition, "testscript", "src/main/groovy/astTestLookup2Class")
        astTestLookup2Class = loader2.parseClass(astTestLookup2Source)
        astTestLookup2DomainClass = grailsApplication.mappingContext.addPersistentEntity(astTestLookup2Class)
        astTestLookup2ConstrainedProperties = constrainedDiscovery.findConstrainedProperties(astTestLookup2DomainClass)

        loader3 = new GroovyClassLoader(loader2)
        astTestSource = new GroovyCodeSource(astTestClassDefinition, "testscript", "src/main/groovy/testClass")
        astTestClass = loader3.parseClass(astTestSource)
        astTestDomainClass = grailsApplication.mappingContext.addPersistentEntity(astTestClass)
        astTestConstrainedProperties = constrainedDiscovery.findConstrainedProperties(astTestDomainClass)

    }


    @After
    void tearDown() {
        // Tear down logic here

    }


    // test that AST has added expected fields to domains
    @Test
    void testASTFields() {
        // fields
       // assertTrue astTestDomainClass.persistentProperties.name.contains("id")
        assertTrue astTestDomainClass.persistentProperties.name.contains("version")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstStringVarcharText")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstIntegerNumber")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstLongNumber")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstBigDecNumber")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstDate")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstBoolVarcharIndicator")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstLookupClass")
        assertTrue astTestDomainClass.persistentProperties.name.contains("tstLookup2Class")

       // assertTrue astTestLookupDomainClass.persistentProperties.name.contains("id")
        assertTrue astTestLookupDomainClass.persistentProperties.name.contains("version")
        assertTrue astTestLookupDomainClass.persistentProperties.name.contains("tstPrimaryKey")

        //assertTrue astTestLookup2DomainClass.persistentProperties.name.contains("id")
        assertTrue astTestLookup2DomainClass.persistentProperties.name.contains("version")
        assertTrue astTestLookup2DomainClass.persistentProperties.name.contains("tstPrimaryKey1")
        assertTrue astTestLookup2DomainClass.persistentProperties.name.contains("tstPrimaryKey2")

    }

    @Test
    void testASTLookup() {
        assertTrue astTestLookupConstrainedProperties.tstPrimaryKey.hasAppliedConstraint("nullable")
        assertFalse astTestLookupConstrainedProperties.tstPrimaryKey.getAppliedConstraint("nullable").nullable
        assertTrue astTestLookupConstrainedProperties.tstPrimaryKey.hasAppliedConstraint("maxSize")
        assertEquals 2, astTestLookupConstrainedProperties.tstPrimaryKey.getAppliedConstraint("maxSize").maxSize
    }


    @Test
    void testASTLookup2() {
        assertTrue astTestLookup2ConstrainedProperties.tstPrimaryKey1.hasAppliedConstraint("nullable")
        assertFalse astTestLookup2ConstrainedProperties.tstPrimaryKey1.getAppliedConstraint("nullable").nullable
        assertTrue astTestLookup2ConstrainedProperties.tstPrimaryKey1.hasAppliedConstraint("maxSize")
        assertEquals 2, astTestLookup2ConstrainedProperties.tstPrimaryKey1.getAppliedConstraint("maxSize").maxSize

        assertTrue astTestLookup2ConstrainedProperties.tstPrimaryKey2.hasAppliedConstraint("nullable")
        assertFalse astTestLookup2ConstrainedProperties.tstPrimaryKey2.getAppliedConstraint("nullable").nullable
        assertTrue astTestLookup2ConstrainedProperties.tstPrimaryKey2.hasAppliedConstraint("maxSize")
        assertEquals 2, astTestLookup2ConstrainedProperties.tstPrimaryKey2.getAppliedConstraint("maxSize").maxSize
    }


    @Test
    void testASTStringConstraints() {
        assertFalse astTestConstrainedProperties.tstStringVarcharText.nullable
        assertEquals 20, astTestConstrainedProperties.tstStringVarcharText.maxSize
        assertFalse astTestConstrainedProperties.tstStringVarcharText.blank
        assertEquals ( ['A', 'B', 'C'], astTestConstrainedProperties.tstStringVarcharText.inList )
        assertEquals ( '[D-E]', astTestConstrainedProperties.tstStringVarcharText.matches )
        assertTrue astTestConstrainedProperties.tstStringVarcharText.hasAppliedConstraint("validator")
        assertNotNull  astTestConstrainedProperties.tstStringVarcharText.getAppliedConstraint("validator")
        assertTrue astTestConstrainedProperties.tstStringVarcharText.getAppliedConstraint("validator") instanceof ValidatorConstraint
    }


    @Test
    void testASTIntegerConstraints() {
        assertTrue astTestConstrainedProperties.tstIntegerNumber.nullable
        assertEquals (-999, astTestConstrainedProperties.tstIntegerNumber.min)
        assertEquals 999, astTestConstrainedProperties.tstIntegerNumber.max
    }


    @Test
    void testASTLongConstraints() {
        assertTrue astTestConstrainedProperties.tstLongNumber.nullable
    }


    @Test
    void testASTBigDecConstraints() {
        assertTrue astTestConstrainedProperties.tstBigDecNumber.nullable
        assertEquals(-9999.999, astTestConstrainedProperties.tstBigDecNumber.min)
        assertEquals 9999.999, astTestConstrainedProperties.tstBigDecNumber.max
        assertEquals 3, astTestConstrainedProperties.tstBigDecNumber.scale
        assertTrue astTestConstrainedProperties.tstBigDecNumber.hasAppliedConstraint("validator")
        assertNotNull  astTestConstrainedProperties.tstBigDecNumber.getAppliedConstraint("validator")
        assertTrue astTestConstrainedProperties.tstBigDecNumber.getAppliedConstraint("validator") instanceof ValidatorConstraint
    }


    @Test
    void testASTDateConstraints() {
        assertTrue astTestConstrainedProperties.tstDate.nullable
        assertTrue astTestConstrainedProperties.tstDate.hasAppliedConstraint("validator")
        assertNotNull  astTestConstrainedProperties.tstDate.getAppliedConstraint("validator")
        assertTrue astTestConstrainedProperties.tstDate.getAppliedConstraint("validator") instanceof ValidatorConstraint
    }


    @Test
    void testASTBooleanConstraints() {
        assertTrue astTestConstrainedProperties.tstBoolVarcharIndicator.hasAppliedConstraint("nullable")
        assertTrue astTestConstrainedProperties.tstBoolVarcharIndicator.getAppliedConstraint("nullable").nullable
    }


    @Test
    void testASTOneColumnLookup() {

        assertTrue astTestConstrainedProperties.tstLookupClass.hasAppliedConstraint("nullable")
        assertFalse astTestConstrainedProperties.tstLookupClass.getAppliedConstraint("nullable").nullable

        // not yet figured out how to test for existence of manyToOneProperties: [[name:"a", referencedColumnName:"a"]]
    }


    @Test
    void testASTMultiColumnLookup() {

        assertTrue astTestConstrainedProperties.tstLookup2Class.hasAppliedConstraint("nullable")
        assertFalse astTestConstrainedProperties.tstLookup2Class.getAppliedConstraint("nullable").nullable

        // not yet figured out how to test for existence of manyToOneProperties: [[name:"a", referencedColumnName:"a"], [name:"b", referencedColumnName:"b"]]
        /*astTestDomainClass.properties[n].oneToOne is true
        oneToMany = false
        manyToMany = false
        manyToOne = false */
    }


    @Test
    void testNamedQuery() {

        List namedQueries = astTestClass.annotations.find { it instanceof NamedQueries }.h.memberValues.get("value")

        assertEquals 2, namedQueries.size()
        assertNotNull namedQueries.find { it.h.memberValues.get("name").equals("AstTestClass.namedQuery1") }
        assertNotNull namedQueries.find { it.h.memberValues.get("name").equals("AstTestClass.namedQuery2") }
    }


    @Test
    void testMethod() {

        assertNotNull astTestClass.declaredMethods.find { it.name.equals("fetchByCode") }
        assertNotNull astTestClass.declaredMethods.find { it.name.equals("fetchByAnotherCode") }

    }
}



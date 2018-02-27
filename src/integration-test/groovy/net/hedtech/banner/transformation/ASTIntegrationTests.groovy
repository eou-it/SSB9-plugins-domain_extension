/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

package net.hedtech.banner.transformation

import org.grails.core.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.validation.ValidatorConstraint
import javax.persistence.NamedQueries
import static org.junit.Assert.*
import org.junit.*

class ASTIntegrationTests {

    def                      grailsApplication
    def                      classLoader
    String                   astTestClassDefinition
    String                   astTestLookupClassDefinition
    String                   astTestLookup2ClassDefinition
    GroovyCodeSource         astTestSource
    GroovyCodeSource         astTestLookupSource
    GroovyCodeSource         astTestLookup2Source
    Class                    astTestClass
    Class                    astTestLookupClass
    Class                    astTestLookup2Class
    DefaultGrailsDomainClass astTestDomainClass
    DefaultGrailsDomainClass astTestLookupDomainClass
    DefaultGrailsDomainClass astTestLookup2DomainClass
    Map                      astTestConstrainedProperties
    Map                      astTestLookupConstrainedProperties
    Map                      astTestLookup2ConstrainedProperties

    @Before
    void setUp() {

        classLoader = grailsApplication.classLoader

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

        astTestLookupSource = new GroovyCodeSource(astTestLookupClassDefinition, "testscript", "src/groovy/astTestLookupClass")
        astTestLookupClass = classLoader.parseClass(astTestLookupSource, false)
        astTestLookupDomainClass = new DefaultGrailsDomainClass(astTestLookupClass)
        astTestLookupConstrainedProperties = astTestLookupDomainClass.constrainedProperties

        astTestLookup2Source = new GroovyCodeSource(astTestLookup2ClassDefinition, "testscript", "src/groovy/astTestLookup2Class")
        astTestLookup2Class = classLoader.parseClass(astTestLookup2Source, false)
        astTestLookup2DomainClass = new DefaultGrailsDomainClass(astTestLookup2Class)
        astTestLookup2ConstrainedProperties = astTestLookup2DomainClass.constrainedProperties

        astTestSource = new GroovyCodeSource(astTestClassDefinition, "testscript", "src/groovy/testClass")
        astTestClass = classLoader.parseClass(astTestSource, false)
        astTestDomainClass = new DefaultGrailsDomainClass(astTestClass)
        astTestConstrainedProperties = astTestDomainClass.constrainedProperties

    }


    @After
    void tearDown() {
        // Tear down logic here

    }


    // test that AST has added expected fields to domains
    @Test
    void testASTFields() {

        // fields
        assertTrue astTestDomainClass.hasProperty("id")
        assertTrue astTestDomainClass.hasProperty("version")
        assertTrue astTestDomainClass.hasProperty("tstStringVarcharText")
        assertTrue astTestDomainClass.hasProperty("tstIntegerNumber")
        assertTrue astTestDomainClass.hasProperty("tstLongNumber")
        assertTrue astTestDomainClass.hasProperty("tstBigDecNumber")
        assertTrue astTestDomainClass.hasProperty("tstDate")
        assertTrue astTestDomainClass.hasProperty("tstBoolVarcharIndicator")
        assertTrue astTestDomainClass.hasProperty("tstLookupClass")
        assertTrue astTestDomainClass.hasProperty("tstLookup2Class")

        assertTrue astTestLookupDomainClass.hasProperty("id")
        assertTrue astTestLookupDomainClass.hasProperty("version")
        assertTrue astTestLookupDomainClass.hasProperty("tstPrimaryKey")

        assertTrue astTestLookup2DomainClass.hasProperty("id")
        assertTrue astTestLookup2DomainClass.hasProperty("version")
        assertTrue astTestLookup2DomainClass.hasProperty("tstPrimaryKey1")
        assertTrue astTestLookup2DomainClass.hasProperty("tstPrimaryKey2")

    }

    @Test
    void testASTLookup() {

        assertFalse astTestLookupConstrainedProperties.tstPrimaryKey.nullable
        assertEquals 2, astTestLookupConstrainedProperties.tstPrimaryKey.maxSize
    }


    @Test
    void testASTLookup2() {

        assertFalse astTestLookup2ConstrainedProperties.tstPrimaryKey1.nullable
        assertEquals 2, astTestLookup2ConstrainedProperties.tstPrimaryKey1.maxSize
        assertFalse astTestLookup2ConstrainedProperties.tstPrimaryKey2.nullable
        assertEquals 2, astTestLookup2ConstrainedProperties.tstPrimaryKey2.maxSize
    }


    @Test
    void testASTStringConstraints() {

        assertFalse astTestConstrainedProperties.tstStringVarcharText.nullable
        assertEquals 20, astTestConstrainedProperties.tstStringVarcharText.maxSize
        assertFalse astTestConstrainedProperties.tstStringVarcharText.blank
        assertEquals ( ['A', 'B', 'C'], astTestConstrainedProperties.tstStringVarcharText.inList )
        assertEquals ( '[D-E]', astTestConstrainedProperties.tstStringVarcharText.matches )
        assertEquals 1, astTestConstrainedProperties.tstStringVarcharText.appliedConstraints.findAll { it instanceof ValidatorConstraint }.size()
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
        assertEquals 1, astTestConstrainedProperties.tstBigDecNumber.appliedConstraints.findAll { it instanceof ValidatorConstraint }.size()
    }


    @Test
    void testASTDateConstraints() {

        assertTrue astTestConstrainedProperties.tstDate.nullable
        assertEquals 1, astTestConstrainedProperties.tstDate.appliedConstraints.findAll { it instanceof ValidatorConstraint }.size()
    }


    @Test
    void testASTBooleanConstraints() {
        assertTrue astTestConstrainedProperties.tstBoolVarcharIndicator.nullable
    }


    @Test
    void testASTOneColumnLookup() {

        assertFalse astTestConstrainedProperties.tstLookupClass.nullable

        // not yet figured out how to test for existence of manyToOneProperties: [[name:"a", referencedColumnName:"a"]]
    }


    @Test
    void testASTMultiColumnLookup() {

        assertFalse astTestConstrainedProperties.tstLookup2Class.nullable

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



/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

package net.hedtech.banner.transformation

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/*
To run this unit test, 2 environment variables are required.
BANNER_TRANSFORMATION1=test/ast-definition/test-base.xml
BANNER_TRANSFORMATION2=test/ast-definition/test-additional.xml
Since it is impossible or very difficult to set environment variables within the test, they have to be set
in the IDE or environment.
 */
@TestMixin(GrailsUnitTestMixin)
class TransformationRulesFileUtilityTests {

    @Before
    public void setUp() {
        // Setup logic here
    }

    @After
    public void tearDown() {
        // Tear down logic here
    }

    @Test
    void testMergeMetaData() {
        def base = [
                fields: [
                        a: [
                                type                 : "java.lang.String",
                                persistenceProperties: [name: "db_a"]
                        ],
                        b: [
                                type                 : "java.lang.String",
                                persistenceProperties: [name: "db_b"]
                        ]
                ]
        ]
        def additional = [
                fields : [
                        b: [
                                type                 : "java.lang.String",
                                persistenceProperties: [name: "db_bprime"]  // can we change an attribute?
                        ],
                        //can we add a field?
                        c: [
                                type                 : "java.lang.String",
                                persistenceProperties: [name: "db_c"]
                        ]
                ],
                methods: [
                        [
                                name  : "methodA",
                                source: "methodASource"
                        ]

                ]
        ]

        def result = TransformationRulesFileUtility.mergeMaps( [base, additional] )
        assertNotNull(result.fields.a)
        assertEquals("Failure changing field b","db_bprime",result.fields.b.persistenceProperties.name)
        assertEquals("Failure adding field c","db_c",result.fields.c.persistenceProperties.name)
        assertNotNull("Failed to add methods", result.methods)

    }

    //This test loads 2 xml files and checks if merged correctly
    @Test
    void testRulesForClass() {
        def e1=TransformationRulesFileUtility.rulesForClass("ast.domain.ext.Employee")
        def e2=TransformationRulesFileUtility.rulesForClass("Emp")
        def a=TransformationRulesFileUtility.rulesForClass("AddedClass")
        def d=TransformationRulesFileUtility.rulesForClass("Dept")
        assertNotNull("Employee is not found",e1)
        assertNotNull("Emp is not found",e2)
        assertNotNull("AddedClass is not found",a)
        assertNotNull("Dept is not found",d)
        assertEquals("Unexpected persistenceProperties for surnamePrefix", "surname_prefix_new",e2.fields.surnamePrefix.persistenceProperties.name)
        assertNotNull("Employee misses surnamePrefix field",e1.fields.surnamePrefix)
    }
}

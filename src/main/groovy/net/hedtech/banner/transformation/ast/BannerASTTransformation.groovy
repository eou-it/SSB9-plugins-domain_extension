/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

package net.hedtech.banner.transformation.ast

import net.hedtech.banner.transformation.TransformationRulesFileUtility
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class BannerASTTransformation implements ASTTransformation {


    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (TransformationRulesFileUtility.transformationRulesExist() && astNodes && astNodes[0]) {
            if (astNodes.size() > 1) { //Local AST
                if (astNodes[0] instanceof AnnotationNode && astNodes[1] && astNodes[1] instanceof ClassNode) {
                    applyTransformationIfAny((ClassNode) astNodes[1], sourceUnit)
                }
            } else { //Global AST
                if (astNodes[0] instanceof ModuleNode) {
                    ModuleNode moduleNode = (ModuleNode) astNodes[0]
                    List<ClassNode> classes = moduleNode.getClasses()
                    if (!classes.isEmpty()) {
                        applyTransformationIfAny(classes.get(0), sourceUnit)
                    }
                }
            }
        }
    }


    private void applyTransformationIfAny(ClassNode classNode, SourceUnit sourceUnit) {
        Map rules = TransformationRulesFileUtility.rulesForClass(classNode.getName())
        if (rules && GrailsASTUtils.isDomainClass(classNode, sourceUnit)) {
            println "#################################################"
            println "# Domain Class: " + classNode.getNameWithoutPackage()
            println "#################################################"
            new DomainASTTransformation().applyTransformation(classNode, rules)
        }
    }
}

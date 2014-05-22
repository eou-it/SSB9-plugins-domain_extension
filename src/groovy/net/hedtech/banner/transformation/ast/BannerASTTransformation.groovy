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
//@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
class BannerASTTransformation implements ASTTransformation {


    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (!TransformationRulesFileUtility.transformationRulesExist()) {
            return
        }

        if(!astNodes) return
        if(!astNodes[0]) return

        if (astNodes.size() > 1) { //Local AST
            if(!(astNodes[0] instanceof AnnotationNode)) return;
            if(!astNodes[1]) return;
            if(!(astNodes[1] instanceof ClassNode)) return;

            applyTransformationIfAny((ClassNode)astNodes[1], sourceUnit)
        } else { //Global AST
            if (!(astNodes[0] instanceof ModuleNode)) return

            ModuleNode moduleNode = (ModuleNode) astNodes[0]

            List<ClassNode> classes = moduleNode.getClasses()
            if (!classes.isEmpty()) {
                applyTransformationIfAny(classes.get(0), sourceUnit)
            }
        }
    }


    private void applyTransformationIfAny(ClassNode classNode, SourceUnit sourceUnit) {
        String className = classNode.getName();

        def rules = TransformationRulesFileUtility.rulesForClass(className)
        if (!rules) {
            return
        }

        if (GrailsASTUtils.isDomainClass(classNode, sourceUnit)) {
            println "#################################################"
            println "Domain Class: " + classNode.getNameWithoutPackage()
            println "#################################################"
            new DomainASTTransformation().applyTransformation(classNode, rules)
        }
    }
}

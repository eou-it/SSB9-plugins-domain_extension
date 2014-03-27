package net.hedtech.banner.transformation.ast

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.CLASS)
@Target([ElementType.TYPE])
@GroovyASTTransformationClass("net.hedtech.banner.transformation.ast.BannerASTTransformation")
public @interface BannerTransformation {
}
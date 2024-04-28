/* Created on 2021-09-08 */
package org.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.work.Incremental

class CheckModuleNamingConvention extends DefaultTask {
	
	@Incremental
	@InputFiles
	def pathModuleInfo = project.layout.files('src/main/java/module-info.java')
	
	CheckModuleNamingConvention() {
		group project.check.group
		description 'Checks module naming conventions.'
	}
	
	private final def charset = 'utf-8'
	
	@TaskAction
	def checkModuleNamingConvention() {
		
		final def mvnGroupId = project.group
		final def mvnArtifactId = project.name
		final def mvnVersion = project.version
	
		logger.lifecycle "==> Checking module naming conventions"
		final def mavenCoordinates = "${mvnGroupId}:${mvnArtifactId}:${mvnVersion}"
		logger.lifecycle " -> Maven coordinates »$mavenCoordinates«"
		// simple check for module artifact naming conventions:
		final def m1 = mvnGroupId.replaceAll('-', '_')
		final def m2 = mvnArtifactId.replaceAll('\\W', '.')
		final def expectedModuleName = m1 + '.' + m2
		final def strModuleInfo = project.resources.text.fromFile(pathModuleInfo, charset).asString()
		final def matcher = strModuleInfo =~ /\smodule\s+(\w+(\.\w+)*)\s*\{/
		final def actualModuleName = matcher[0][1]
			
		if (actualModuleName != expectedModuleName) {
			logger.warn " -> JPMS module name  »$actualModuleName« does not match the expected name »$expectedModuleName«"
		} else {
			logger.lifecycle " -> JPMS module name  »$actualModuleName« OK"
		}
	}
}
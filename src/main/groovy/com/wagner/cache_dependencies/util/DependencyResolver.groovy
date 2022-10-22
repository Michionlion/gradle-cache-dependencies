package com.wagner.cache_dependencies.util

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ComponentArtifactsResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

class DependencyResolver {

    Project project

    ProgressLoggerWrapper logger

    static final def VALID_CONFIGURATION = { it.canBeResolved && it.name != "default" && it.name != "archives" }


    DependencyResolver(Project project, ProgressLoggerWrapper logger) {
        this.project = project
        this.logger = logger
    }

    List<ResolvedArtifact> getArtifacts() {
        List<ResolvedArtifact> artifacts = []
        def addAll = { it -> artifacts.addAll(it.resolvedConfiguration.resolvedArtifacts) }
        project.buildscript.configurations.findAll(VALID_CONFIGURATION).each(addAll)
        project.configurations.findAll(VALID_CONFIGURATION).each(addAll)
        return artifacts
    }

    List<ComponentArtifactsResult> getPOMs() {
        List<ComponentIdentifier> ids = []
        def addAll = { it -> ids.addAll(it.incoming.resolutionResult.allDependencies.collect { it.selected.id }) }
        project.buildscript.configurations.findAll(VALID_CONFIGURATION).each(addAll)
        project.configurations.findAll(VALID_CONFIGURATION).each(addAll)

        return project.dependencies.createArtifactResolutionQuery()
                .forComponents(ids)
                .withArtifacts(MavenModule, MavenPomArtifact)
                .execute().resolvedComponents.asList()
                .grep( { it.id instanceof ModuleComponentIdentifier })
    }

    protected String getPath(String group, String module, String version) {
        return "${group.replace('.', '/')}/${module}/${version}"
    }

    Map<File, String> resolve() {
        Map<File, String> copyList = [:]
        logger.progress('Generating artifact list')

        for (ResolvedArtifact artifact : getArtifacts()) {
            logger.progress("Generating artifact list > Working on ${artifact.name}")
            ModuleVersionIdentifier id = artifact.moduleVersion.id
            copyList.put(artifact.file, getPath(id.group, id.name, id.version))
        }

        logger.progress('Generating POM list')

        for(ComponentArtifactsResult poms : getPOMs()) {
            logger.progress("Generating POM list > Working on ${poms.id.displayName}")
            String dest = getPath(poms.id.group, poms.id.module, poms.id.version)
            poms.getArtifacts(MavenPomArtifact).each { artifact ->
                copyList.put(artifact.file, dest)
            }
        }

        return copyList
    }
}

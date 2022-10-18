package com.wagner.cache_dependencies

import javax.inject.Inject

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
* Plugin to download and store dependencies for offline use
**/
class CacheDependenciesPlugin implements Plugin<Project> {


    @Override
    void apply(Project project) {
        project.task('cacheToMaven', type: CacheToMavenDirectory) {
            group = 'Cache'
            description = 'Cache all dependencies to a maven repository'
        }
    }

}

abstract class CacheToMavenDirectory extends DefaultTask {

    @Input
    abstract DirectoryProperty getDestination()

    @Inject
    abstract FileSystemOperations getFileSystem()

    @Inject
    CacheToMavenDirectory(ProjectLayout layout, ProviderFactory providers) {
        destination.convention(layout.dir(providers.provider(() -> new File(super.project.repositories.mavenLocal().url))))
    }

    @TaskAction
    void run() {
        ProgressLoggerWrapper logger = new ProgressLoggerWrapper(super.project, 'List Dependencies')

        Map<File, File> copyList = [:]

        logger.started()
        logger.progress('Generating artifact list')

        // ignore deprecated configurations
        def validConfiguration = { it.canBeResolved && it.name != "default" && it.name != "archives" }

        List<ResolvedArtifact> artifacts = []
        def addAll = { it -> artifacts.addAll(it.resolvedConfiguration.resolvedArtifacts) }
        project.buildscript.configurations.findAll(validConfiguration).each(addAll)
        project.configurations.findAll(validConfiguration).each(addAll)

        for (ResolvedArtifact artifact : artifacts) {
            logger.progress("Generating artifact list > Working on ${artifact.name}")
            ModuleVersionIdentifier id = artifact.moduleVersion.id
            copyList.put(artifact.file, destination.dir(getPath(id.group, id.name, id.version)).get().getAsFile())
        }

        logger.progress('Generating POM list')

        List<ComponentIdentifier> ids = []
        addAll = { it -> ids.addAll(it.incoming.resolutionResult.allDependencies.collect { it.selected.id }) }
        project.buildscript.configurations.findAll(validConfiguration).each(addAll)
        project.configurations.findAll(validConfiguration).each(addAll)

        project.dependencies.createArtifactResolutionQuery()
                .forComponents(ids)
                .withArtifacts(MavenModule, MavenPomArtifact)
                .execute().resolvedComponents.each { component ->
                    ComponentIdentifier id = component.id
                    logger.progress("Generating POM list > Working on ${id.displayName}")
                    if (id instanceof ModuleComponentIdentifier) {
                        File moduleDir = destination.dir(getPath(id.group, id.module, id.version)).get().getAsFile()
                        component.getArtifacts(MavenPomArtifact).each { pom ->
                            copyList.put(pom.file, moduleDir)
                        }
                    }
                }

        logger.progress('Copying files')

        copyList.eachWithIndex { src, dest, index ->
            fileSystem.sync {
                from src
                into dest
            }
            def items = "${index+1}/${copyList.size()}"
            def percent = (int) Math.round((index + 1) / copyList.size() * 100)
            logger.progress("Copying files > Finshed ${items}: ${percent}% complete > Working on ${src.name}")
        }

        logger.completed()


    }

    protected String getPath(String group, String module, String version) {
        return "${group.replace('.', '/')}/${module}/${version}"
    }
}

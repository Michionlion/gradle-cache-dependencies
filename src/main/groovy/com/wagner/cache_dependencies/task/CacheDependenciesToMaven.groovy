package com.wagner.cache_dependencies.task

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

import com.wagner.cache_dependencies.util.DependencyResolver
import com.wagner.cache_dependencies.util.ProgressLoggerWrapper


abstract class CacheDependenciesToMaven extends DefaultTask {

    @Input
    abstract DirectoryProperty getDestination()

    @Inject
    abstract FileSystemOperations getFileSystem()

    @Inject
    CacheDependenciesToMaven(ProjectLayout layout, ProviderFactory providers) {
        destination.convention(layout.dir(providers.provider(() -> new File(super.project.repositories.mavenLocal().url))))
    }

    @TaskAction
    void run() {
        ProgressLoggerWrapper logger = new ProgressLoggerWrapper(super.project, 'Cache Dependencies to Maven')
        DependencyResolver resolver = new DependencyResolver(super.project, logger)


        logger.started()
        Map<File, String> copyList = resolver.resolve()

        copyList.each { file, dest ->
            println "${file}\n${dest}\n"
        }

        logger.progress('Copying files')

        copyList.eachWithIndex { src, dest, index ->
            fileSystem.sync {
                from src
                into destination.dir(dest)
            }
            def items = "${index+1}/${copyList.size()}"
            def percent = (int) Math.round((index + 1) / copyList.size() * 100)
            logger.progress("Copying files > Finshed ${items}: ${percent}% complete > Working on ${src.name}")
        }
        logger.completed()
    }
}

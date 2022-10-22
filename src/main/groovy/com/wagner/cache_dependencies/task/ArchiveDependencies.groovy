package com.wagner.cache_dependencies.task

import javax.inject.Inject

import java.nio.file.Paths

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.wagner.cache_dependencies.util.DependencyResolver
import com.wagner.cache_dependencies.util.ProgressLoggerWrapper
import com.wagner.cache_dependencies.util.Archiver


abstract class ArchiveDependencies extends DefaultTask {

    @Input
    abstract RegularFileProperty getArchiveFile()

    @Input
    abstract Property<Boolean> getFlatten()

    @Inject
    abstract ArchiveOperations getArchiveOperations()

    @Inject
    ArchiveDependencies(ProjectLayout layout, ProviderFactory providers) {
        archiveFile.convention(layout.file(providers.provider(() -> new File("dependencies.tgz"))))
        flatten.convention(true)
    }

    @TaskAction
    void run() {
        ProgressLoggerWrapper logger = new ProgressLoggerWrapper(super.project, 'Archive Dependencies to file')
        DependencyResolver resolver = new DependencyResolver(super.project, logger)

        logger.started()
        Map<File, String> copyList = resolver.resolve()

        logger.progress('Archiving files')

        try(Archiver archiver = new Archiver(archiveFile.get().getAsFile())) {
            copyList.eachWithIndex { src, dest, index ->
                archiver.write(src, getName(src, dest))
                def items = "${index+1}/${copyList.size()}"
                def percent = (int) Math.round((index + 1) / copyList.size() * 100)
                logger.progress("Copying files > Finshed ${items}: ${percent}% complete > Working on ${src.name}")
            }
        }

        logger.completed()
    }

    String getName(File file, String dest) {
        if(flatten) {
            if(file.name.endsWith("pom")) {
                return Paths.get("poms", file.name).toString()
            }
            return file.name
        } else {
            return Paths.get(dest, file.name).toString()
        }
    }
}

package com.wagner.cache_dependencies

import javax.inject.Inject

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.wagner.cache_dependencies.task.CacheDependenciesToMaven
import com.wagner.cache_dependencies.task.ArchiveDependencies

class CacheDependenciesPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.task('cacheDependenciesToMaven', type: CacheDependenciesToMaven) {
            group = 'Cache'
            description = 'Cache all dependencies to a maven repository'
        }
        project.task('archiveDependencies', type: ArchiveDependencies) {
            group = 'Cache'
            description = 'Archive all dependencies to a tar.gz file'
        }
    }
}

package com.wagner.cache_dependencies

import javax.inject.Inject

import org.gradle.api.Plugin
import org.gradle.api.Project

class CacheDependenciesPlugin implements Plugin<Project> {


    @Override
    void apply(Project project) {
        project.task('cacheToMaven', type: CacheToMavenDirectory) {
            group = 'Cache'
            description = 'Cache all dependencies to a maven repository'
        }
    }

}

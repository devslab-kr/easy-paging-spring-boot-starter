pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "easy-paging-spring-boot-starter"

// Subprojects. Each publishable artifact lives under its own subproject.
// `core` produces the `kr.devslab:easy-paging-spring-boot-starter` artifact
// (the published name is set explicitly in core/build.gradle.kts via
// `mavenPublishing.coordinates(...)` so the on-disk path can stay short
// while the Maven coordinates stay backward-compatible).
include("core")

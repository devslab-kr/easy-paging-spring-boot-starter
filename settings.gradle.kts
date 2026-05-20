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
// The on-disk path is short (`core`, `reactive`); the Maven artifact ID is
// pinned via `mavenPublishing.coordinates(...)` in each module's build file.
//
//   core      → kr.devslab:easy-paging-spring-boot-starter
//   reactive  → kr.devslab:easy-paging-spring-boot-starter-reactive
include("core")
include("reactive")

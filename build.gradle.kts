// Multi-module orchestration. The root project is not published — each
// publishable artifact lives under its own subproject (see settings.gradle.kts)
// and applies the publishing plugin itself.
//
// Plugin versions are declared here with `apply false` so subprojects can
// apply them without repeating version numbers, and so the version drift
// between modules stays at zero.

plugins {
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    // Pinned to 0.30.0: 0.34.0+ removed the `mavenPlainJavadocJar` task that
    // core/build.gradle.kts and reactive/build.gradle.kts reference to fix the
    // GitHub Release asset filename. The SB3 maintenance line keeps the same
    // pin as main for consistency until that override is rewritten against the
    // newer plugin API. (PR #56 bumped this to 0.36.0 and broke v3.0.0's
    // release workflow on 2026-05-23 — see the v3.0.0 retag thread.)
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
}

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION").get()
}

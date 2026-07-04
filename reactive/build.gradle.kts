// Reactive companion module: native R2DBC + WebFlux support.
//
// Published as `kr.devslab:easy-paging-spring-boot-starter-reactive`. Pulls
// in `:core` transitively so consumers add a single coordinate and get the
// full reactive paging stack (offset + keyset, MyBatis + R2DBC).

plugins {
    `java-library`
    jacoco
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("com.vanniktech.maven.publish")
}

base {
    archivesName.set("easy-paging-spring-boot-starter-reactive")
}

// See the matching override in core/build.gradle.kts for the full rationale.
afterEvaluate {
    tasks.named<AbstractArchiveTask>("mavenPlainJavadocJar").configure {
        archiveBaseName.set("easy-paging-spring-boot-starter-reactive")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "-Xlint:all,-classfile,-processing,-serial",
        "-Werror"
    ))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addBooleanOption("Xdoclint:none", true)
        addBooleanOption("html5", true)
        locale = "en_US"
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.15")
    }
}

dependencies {
    // The core module is the foundation — keyset cursor, PageResponse,
    // KeysetRequest, CursorCodec all live there. Consumers of this reactive
    // artifact get the core artifact transitively via `api(project(...))`.
    api(project(":core"))

    // Reactive stack. Everything `compileOnly` because the consumer's app
    // dictates whether it's a WebFlux app, an R2DBC app, both, or a mix.
    // The relevant auto-configurations gate themselves on `@ConditionalOnClass`
    // so dead code doesn't run when a piece is absent.
    compileOnly("io.projectreactor:reactor-core")
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-starter-data-r2dbc")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test — full reactive stack present so we can exercise both auto-configs.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.r2dbc:r2dbc-h2")
    testImplementation("com.h2database:h2")
    testImplementation("org.assertj:assertj-core")

    // Testcontainers — dialect-compat tests against real PostgreSQL R2DBC.
    // r2dbc-h2 in the fast tests catches API-shape regressions; PostgreSQL
    // catches driver-specific type-binding differences (TIMESTAMP WITH TIME
    // ZONE, UUID, etc.) that we won't see against H2.
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")
    testImplementation("org.postgresql:r2dbc-postgresql")
    // Used by Testcontainers' Postgres module to run init scripts via JDBC.
    testImplementation("org.postgresql:postgresql")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    // Fast feedback loop: H2-via-R2DBC only. Dialect-compat (PostgreSQL R2DBC
    // via Testcontainers) runs via the separate `testDialect` task — see
    // core/build.gradle.kts for the rationale on the split.
    useJUnitPlatform {
        excludeTags("dialect-compat")
    }
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
    systemProperty("file.encoding", "UTF-8")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("testDialect") {
    description = "Runs dialect-compat tests against real PostgreSQL R2DBC via Testcontainers. " +
            "Requires a working Docker daemon."
    group = "verification"
    useJUnitPlatform {
        includeTags("dialect-compat")
    }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter("test")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
    systemProperty("file.encoding", "UTF-8")
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

mavenPublishing {
    // Explicit coordinates so the published artifact ID stays
    // `easy-paging-spring-boot-starter-reactive` regardless of subproject
    // directory name (see settings.gradle.kts for the convention).
    coordinates(
        providers.gradleProperty("GROUP").get(),
        "easy-paging-spring-boot-starter-reactive",
        providers.gradleProperty("VERSION").get()
    )

    pom {
        // Per-module name/description override. The shared POM_* properties
        // (license, scm, inception year, etc.) still come from the root
        // gradle.properties via Vanniktech's defaults.
        name.set("Easy Paging Spring Boot Starter - Reactive (R2DBC + WebFlux)")
        description.set("Native R2DBC and WebFlux support for easy-paging. Companion artifact to easy-paging-spring-boot-starter.")

        developers {
            developer {
                id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
                name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
                email.set(providers.gradleProperty("POM_DEVELOPER_EMAIL"))
                organization.set(providers.gradleProperty("POM_ORGANIZATION_NAME"))
                organizationUrl.set(providers.gradleProperty("POM_ORGANIZATION_URL"))
            }
        }

        organization {
            name.set(providers.gradleProperty("POM_ORGANIZATION_NAME"))
            url.set(providers.gradleProperty("POM_ORGANIZATION_URL"))
        }

        issueManagement {
            system.set(providers.gradleProperty("POM_ISSUE_SYSTEM"))
            url.set(providers.gradleProperty("POM_ISSUE_URL"))
        }
    }
}

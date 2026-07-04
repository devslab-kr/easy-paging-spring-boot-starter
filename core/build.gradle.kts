plugins {
    `java-library`
    jacoco
    // Plugin versions are declared at the root with `apply false`; we just
    // apply them here. Keeps the two subprojects (core + future reactive)
    // in lockstep automatically.
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("com.vanniktech.maven.publish")
}

// `group` and `version` are set on all projects from the root build.gradle.kts.

base {
    // On-disk jar filename. Without this override Gradle defaults to the
    // subproject name (`core-X.Y.Z.jar`), which is misleading when downloaded
    // from a GitHub Release. The Maven Central artifact ID is enforced
    // separately via `mavenPublishing.coordinates(...)` below.
    archivesName.set("easy-paging-spring-boot-starter")
}

// Vanniktech maven-publish creates its own Jar task for the javadoc artifact
// (named `mavenPlainJavadocJar`) and hardcodes its archive base name to
// `<subproject>-maven-javadoc`, ignoring `base.archivesName`. Without this
// override the GitHub Release ends up with `core-maven-javadoc-X.Y.Z-javadoc.jar`
// next to a properly named `easy-paging-spring-boot-starter-X.Y.Z.jar`, which is
// confusing for anyone manually downloading from the release page. (The Maven
// Central upload itself is unaffected — Vanniktech renames artifacts by
// coordinates at publish time.)
//
// Vanniktech sets the archive base name on the task during its plugin's own
// `afterEvaluate` hook, so a plain `configureEach` (which runs at task
// realization, earlier) gets overwritten. Running our override inside an
// `afterEvaluate` block makes it the last writer.
//
// Note: Vanniktech's `JavadocJar` task type does *not* extend
// `org.gradle.api.tasks.bundling.Jar` — it extends `AbstractArchiveTask`
// directly. Targeting the common parent type avoids the "not a subclass"
// error from `tasks.named<Jar>(...)`.
afterEvaluate {
    tasks.named<AbstractArchiveTask>("mavenPlainJavadocJar").configure {
        archiveBaseName.set("easy-paging-spring-boot-starter")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -parameters: keep AOP-readable param names. -Xlint enabled but the noisy/cosmetic
    // categories (classfile/processing/serial) are excluded so -Werror stays usable
    // for genuine code issues without tripping on Spring's JSR-305 quirks or harmless
    // annotation-processor warnings.
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
    // Required at runtime — pulled in transitively for consumers.
    // The Spring Boot BOM (above) controls versions so they align with whatever
    // Boot version the consumer is on at resolution time.
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.data:spring-data-commons")
    api("com.github.pagehelper:pagehelper-spring-boot-starter:2.1.1")

    // MyBatis Spring Boot Starter is exposed as api because PageHelper requires
    // MyBatis at runtime, and PageHelper 2.1.x still ships its own transitive
    // mybatis-spring-boot-starter:2.3.2 (Spring Boot 2.7 line). Declaring 3.0.4
    // directly here forces Gradle's conflict resolution to pick the Boot-3-
    // compatible starter for every consumer — they no longer need to add it
    // themselves and the wrong-line transitive footprint stops mattering.
    // Override with `exclude(group = "org.mybatis.spring.boot")` + a direct
    // declaration if you need a different MyBatis line for some reason.
    api("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5")

    // Silences "cannot find javax.annotation.Nonnull" cosmetic warnings emitted when
    // resolving Spring's @Nullable. Not exposed to consumers (compileOnly).
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // Auto-configuration metadata processor — produces additional-spring-configuration-metadata.json
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Optional: only needed when the consumer is a Servlet/WebFlux app or uses Jackson.
    // Marked compileOnly so consumers without these features don't pay the cost.
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("io.projectreactor:reactor-core")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    // mybatis-spring-boot-starter is inherited via the main `api` declaration,
    // so tests automatically run against whatever consumers get.
    testImplementation("io.projectreactor:reactor-core")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.assertj:assertj-core")

    // Testcontainers — real-database compat layer. Tagged @Tag("dialect-compat")
    // and run by the `testDialect` task, separate from the fast H2 path. H2
    // catches most logic bugs in seconds; PostgreSQL + MySQL catch the
    // dialect-specific PageHelper rewriting paths the H2 dialect would miss.
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.postgresql:postgresql")
    testImplementation("com.mysql:mysql-connector-j")

    // Explicit launcher pin. JUnit Jupiter 5.11+ (shipped by Spring Boot 3.5)
    // requires junit-platform-launcher >= 1.11 for OutputDirectoryProvider,
    // but Gradle 8.10.x bundles 1.10.x by default — without this declaration
    // the BOM's 1.11.x doesn't make it onto the test runtime classpath and
    // discovery fails with "OutputDirectoryProvider not available".
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    // Fast feedback loop: H2 in-memory only. Tests tagged "dialect-compat"
    // (PostgreSQL / MySQL Testcontainers) live in the same source set but run
    // via the separate `testDialect` task below, so a CI matrix and local
    // `gradle test` runs stay fast (~25s) while we still get dialect coverage
    // before each release.
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
    description = "Runs the dialect-compat tests (PostgreSQL + MySQL via Testcontainers). " +
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
    // 0.8.13 supports Java 21+ bytecode (records, sealed, pattern matching).
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)    // consumed by Codecov in CI
        html.required.set(true)   // human-readable report at build/reports/jacoco/test/html
        csv.required.set(false)
    }
}

mavenPublishing {
    // Explicit coordinates so the published artifact ID stays
    // `easy-paging-spring-boot-starter` even though the subproject directory
    // is `core/`. Without this override Vanniktech would default the
    // artifact ID to the subproject name (`core`), breaking every existing
    // consumer's coordinate.
    coordinates(
        providers.gradleProperty("GROUP").get(),
        "easy-paging-spring-boot-starter",
        providers.gradleProperty("VERSION").get()
    )

    pom {
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
// NOTE: name/description/inceptionYear/url/license/scm are sourced from gradle.properties
//       (Vanniktech maven-publish reads POM_* automatically). Only fields the plugin does
//       not auto-read (developer, organization, issueManagement) are configured here.

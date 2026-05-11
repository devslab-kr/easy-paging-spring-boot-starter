plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION").get()

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
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5")
    }
}

dependencies {
    // Required at runtime — pulled in transitively for consumers.
    // The Spring Boot BOM (above) controls versions so they align with whatever
    // Boot version the consumer is on at resolution time.
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.data:spring-data-commons")
    api("com.github.pagehelper:pagehelper-spring-boot-starter:2.1.0")

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
    compileOnly("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.4")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.4")
    testImplementation("io.projectreactor:reactor-core")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.assertj:assertj-core")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
    systemProperty("file.encoding", "UTF-8")
}

mavenPublishing {
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

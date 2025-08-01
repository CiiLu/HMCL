plugins {
    id("checkstyle")
}

group = "org.jackhuang"
version = "3.0"

subprojects {
    apply {
        plugin("java")
        plugin("idea")
        plugin("maven-publish")
        plugin("checkstyle")
    }

    repositories {
        flatDir {
            name = "libs"
            dirs = setOf(rootProject.file("lib"))
        }
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://libraries.minecraft.net")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    configure<CheckstyleExtension> {
        sourceSets = setOf()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging.showStandardStreams = true
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
        repositories {
            mavenLocal()
        }
    }
}

tasks.register("checkTranslations") {
    doLast {
        val hmclLangDir = file("HMCL/src/main/resources/assets/lang")

        val en = java.util.Properties().apply {
            hmclLangDir.resolve("I18N.properties").bufferedReader().use { load(it) }
        }

        val zh = java.util.Properties().apply {
            hmclLangDir.resolve("I18N_zh.properties").bufferedReader().use { load(it) }
        }

        val zh_CN = java.util.Properties().apply {
            hmclLangDir.resolve("I18N_zh_CN.properties").bufferedReader().use { load(it) }
        }

        var success = true

        zh_CN.forEach {
            if (!en.containsKey(it.key)) {
                project.logger.warn("I18N.properties missing key '${it.key}'")
                success = false
            }
        }

        zh_CN.forEach {
            if (!zh.containsKey(it.key)) {
                project.logger.warn("I18N_zh.properties missing key '${it.key}'")
                success = false
            }
        }

        zh_CN.forEach {
            if (it.value.toString().contains("帐户")) {
                project.logger.warn("The misspelled '帐户' in '${it.key}' should be replaced by '账户'")
                success = false
            }
        }

        zh_CN.forEach {
            if (it.value.toString().contains("其它")) {
                project.logger.warn("The misspelled '其它' in '${it.key}' should be replaced by '其他'")
                success = false
            }
        }

        if (!success) {
            throw GradleException("Part of the translation is missing")
        }
    }
}

org.jackhuang.hmcl.gradle.javafx.JavaFXUtils.register(rootProject)

defaultTasks("clean", "build")

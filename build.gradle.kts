/*
 * Copyright 2024 Benjamin Martin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions // Import added
import org.gradle.language.jvm.tasks.ProcessResources // Import added

plugins {
    java
    id("com.gradleup.shadow") version "8.3.2" // Import shadow API.
    eclipse
}

group = "net.trueog.afkplusog"
version = "3.4.4-trueog"
val apiVersion = "1.19"
description = "AFK for professional servers"

base.archivesName.set("AFKPlus-OG")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }

    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven { url = uri("https://repo.purpurmc.org/snapshots") }
    maven {
        name = "lapismc-repo"
        url = uri("https://maven.lapismc.net/repository/maven/")
    }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.essentialsx.net/releases/") }
}

dependencies {
    implementation("net.lapismc:LapisCore:1.12.5")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.8.Final")

    compileOnly("net.essentialsx:EssentialsX:2.20.1")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3")
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
}

// Configure processResources outside the tasks block
tasks.named<ProcessResources>("processResources") {
    filesMatching("**/*") {
        expand(
            "version" to project.version,
            "apiVersion" to apiVersion
        )
    }
}

tasks {
    val defaultTaskNames = listOf("clean", "build", "shadowJar")
    register("default") {
        dependsOn(defaultTaskNames)
    }

    // Configure Javadoc task to suppress warnings
    named<Javadoc>("javadoc") {
        // Suppress warnings about missing comments
        isFailOnError = false
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")

        source = sourceSets["main"].allJava
        include("net/trueog/afkplusog/**")
    }

    // Configure ShadowJar
    named<ShadowJar>("shadowJar") {
        archiveFileName.set("${base.archivesName.get()}.jar")
        exclude("javax/**")
        relocate("org.ocpsoft.prettytime", "net.lapismc.afkplus.util.prettytime")
        relocate("net.lapismc.lapiscore", "net.lapismc.afkplus.util.core")
        from("LICENSE") {
            into("/")
        }
        exclude("io.github.miniplaceholders.*")
        minimize()
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    named("build") {
        dependsOn(named("shadowJar"))
    }

    named<Jar>("jar") {
        archiveClassifier.set("part")
    }

    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation"))
        options.encoding = "UTF-8"
        options.isFork = true
    }
}

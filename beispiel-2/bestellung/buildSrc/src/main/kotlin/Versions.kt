@file:Suppress("unused", "KDocMissingDocumentation")
/*
* Copyright (C) 2019 - present Juergen Zimmermann, Hochschule Karlsruhe
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

// https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources

@Suppress("MemberVisibilityCanBePrivate", "Reformat")
object Versions {
    const val javaMin = "14.0.2"

    // IntelliJ IDEA unterstuetzt noch nicht Java 16
    //const val javaMax = "16"
    const val javaMax = "15"

    const val kotlin = "1.4.0"

    // https://youtrack.jetbrains.com/issue/KT-29857
    //val kotlinJvmTarget = org.gradle.api.JavaVersion.VERSION_13.majorVersion
    val kotlinJvmTarget = org.gradle.api.JavaVersion.VERSION_11.majorVersion

    const val buildPackJava = "14.0.2"

    val jibJava = org.gradle.api.JavaVersion.VERSION_14.majorVersion
    //val jibJava = org.gradle.api.JavaVersion.VERSION_11.majorVersion

    const val graalVM = "20.1.0"
    val graalVMJava = org.gradle.api.JavaVersion.VERSION_11.majorVersion

    const val springBoot = "2.4.0-M2"

    object Plugins {
        const val kotlin = Versions.kotlin
        const val allOpen = Versions.kotlin
        const val noArg = Versions.kotlin
        const val kapt = Versions.kotlin

        const val springBoot = Versions.springBoot
        const val graal = "0.7.1-20-g113a84d"
        const val testLogger = "2.1.0"
        const val allure = "2.8.1"

        const val vplugin = "3.0.5"
        const val versions = "0.29.0"
        const val detekt = "1.12.0"
        const val dokka = "1.4.0-rc"
        const val jib = "2.5.0"
        const val sweeney = "4.2.0"
        const val owaspDependencyCheck = "5.3.2.1"
        const val asciidoctorConvert = "3.2.0"
        const val asciidoctorPdf = asciidoctorConvert
        const val asciidoctorDiagram = asciidoctorConvert
        const val jig = "2020.8.5"
        const val jk1DependencyLicenseReport = "1.14"
        //const val jaredsBurrowsLicense = "0.8.80"
        //const val hierynomusLicense = "0.15.0"
    }

    const val annotations = "20.0.0"
    const val paranamer = "2.8"
    //const val javaxEl = "3.0.1-b08"
    const val kotlinLogging = "1.8.3"
    const val springSecurityRsa = "1.0.9.RELEASE"

    // -------------------------------------------------------------------------------------------
    // Versionsnummern aus BOM-Dateien ueberschreiben
    // siehe org.springframework.boot:spring-boot-dependencies
    //    https://github.com/spring-projects/spring-boot/blob/master/spring-boot-dependencies/pom.xml
    // -------------------------------------------------------------------------------------------

    const val assertj = "3.17.1"
    const val hibernateValidator = "7.0.0.Alpha6"
    const val jackson = "2.11.2"
    const val jakartaEl = "4.0.0-RC2"
    //const val jakartaMail = "2.0.0-RC4"
    //const val jakartaMail = "1.6.5"
    //const val jakartaValidationApi = "3.0.0-M1"
    const val jakartaXmlBindApi = "3.0.0-RC3"
    const val junitJupiterBom = "5.7.0-RC1"
    const val kotlinCoroutines = "1.3.9"
    //const val mongodb = "4.1.0"
    //const val mongoDriverReactivestreams = mongodb
    //const val reactiveStreams = "1.0.3"
    //const val reactorBom = "2020.0.0-M2"
    // fuer kapt mit spring-context-indexer
    const val springBom = "5.3.0-M2"
    const val springDataBom = "2020.0.0-M2"
    //const val springDataMongoDB = "3.1.0-M2"
    const val springGraalVMNative = "0.8.0-RC1"
    //const val springHateoas = "1.2.0-M1"
    //const val springRetry = "1.2.5.RELEASE"
    //const val springSecurityBom = "5.4.0-RC1"
    //const val thymeleaf = "3.0.11.RELEASE"
    // org.apache.tomcat.embed:tomcat-embed-core   javax/servlet/Servlet -> jakarta/servlet/Servlet
    //const val tomcat = "10.0.0-M7"
    //const val tomcat = "9.0.37"

    const val mockk = "1.10.0"

    const val ktlint = "0.38.1"
    const val ktlintKotlin = kotlin
    //const val ktlintKotlin = "1.3.72"
    const val httpClientKtlint = "4.5.12"
    // https://www.eclemma.org/jacoco : 0.8.6-20200410.013514-49
	const val jacocoVersion = "0.8.6-SNAPSHOT"
    //const val jacocoVersion = "0.8.6"
    const val allure = "2.13.5"
    const val allureCommandline = allure
    const val allureJunit = allure
    const val asciidoctorj = "2.4.0"
    const val asciidoctorjDiagram = "2.0.2"
    const val asciidoctorjPdf = "1.5.3"
}

fun isGraalVM() = Runtime.version().toString().startsWith(Versions.graalVMJava)

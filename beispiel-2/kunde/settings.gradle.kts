pluginManagement {
    repositories {
        gradlePluginPortal()
        //maven("https://plugins.gradle.org/m2")
        mavenCentral()

        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        //maven("https://dl.bintray.com/kotlin/kotlin-dev") {
        //    mavenContent { snapshotsOnly() }
        //}
        // https://spring.io/blog/2020/10/29/notice-of-permissions-changes-to-repo-spring-io-fall-and-winter-2020#january-6-2021
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/plugins-release")

        //jcenter()

        // Snapshots von Spring Framework, Spring Data, Spring Security und Spring Cloud
        //maven("https://repo.spring.io/libs-snapshot")
    }
}

rootProject.name = "kunde"

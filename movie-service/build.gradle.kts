// movie-service: WAR для развёртывания на Payara (Payara Micro для локального запуска).
import java.net.URI
import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
    war
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Embedded Tomcat не попадает в WAR — сервис разворачивается на Payara.
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
}

// Версия Payara Micro, которой запускается собранный WAR локально.
val payaraMicroVersion = "6.2025.4"
val payaraMicroJar = layout.buildDirectory.file("libs/payara-micro.jar")

tasks.register("downloadPayaraMicro") {
    group = "application"
    description = "Скачивает Payara Micro для локального запуска WAR."
    outputs.file(payaraMicroJar)
    doLast {
        val target = payaraMicroJar.get().asFile
        if (!target.exists()) {
            target.parentFile.mkdirs()
            val url = URI(
                "https://repo1.maven.org/maven2/fish/payara/extras/payara-micro/" +
                    "$payaraMicroVersion/payara-micro-$payaraMicroVersion.jar"
            )
            url.toURL().openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

tasks.register("runPayara") {
    group = "application"
    description = "Запускает movie-service на embedded Payara Micro (java -jar payara-micro.jar --deploy movie.war)."
    dependsOn("bootWar", "downloadPayaraMicro")
    doLast {
        val war = tasks.named<BootWar>("bootWar").get().archiveFile.get().asFile
        val jar = payaraMicroJar.get().asFile
        providers.exec {
            commandLine("java", "-jar", jar.absolutePath, "--deploy", war.absolutePath)
        }.result.get()
    }
}
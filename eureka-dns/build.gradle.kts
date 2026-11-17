// eureka-dns: standalone DNS-прокси, публикующий Eureka-инстансы как SRV/A записи.
// Без Spring — чистый Java 11 (dnsjava) + gson для парсинга JSON.

dependencies {
    implementation("dnsjava:dnsjava:3.5.2")
    implementation("com.google.code.gson:gson")
    // dnsjava логирует через slf4j-api — NOP-провайдер убирает предупреждение "no providers".
    runtimeOnly("org.slf4j:slf4j-nop")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("eureka-dns.jar")
}
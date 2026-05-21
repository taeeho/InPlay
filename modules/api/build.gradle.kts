plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:collector"))
    implementation(project(":modules:ingest"))
    implementation(project(":modules:ml-inference"))
    implementation(project(":modules:decision"))
    implementation(project(":modules:notify"))
    implementation(project(":modules:journal"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testImplementation("org.testcontainers:mongodb:1.20.2")
}

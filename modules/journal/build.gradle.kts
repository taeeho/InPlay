dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:ingest"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

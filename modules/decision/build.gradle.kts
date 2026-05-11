dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:ml-inference"))
    implementation(project(":modules:ingest"))
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

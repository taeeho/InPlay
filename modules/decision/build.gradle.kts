dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:ml-inference"))
    implementation(project(":modules:ingest"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

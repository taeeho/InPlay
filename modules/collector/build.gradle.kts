dependencies {
    implementation(project(":modules:core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web") // RestClient
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

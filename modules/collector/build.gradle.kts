dependencies {
    implementation(project(":modules:core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web") // RestClient
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    // ADR-009 headless 회색지대 운영. 분리 source set 로 옮기는 것은 후속(컨테이너 size 최적화 시).
    implementation("com.microsoft.playwright:playwright:1.49.0")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

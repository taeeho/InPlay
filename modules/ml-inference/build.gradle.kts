dependencies {
    implementation(project(":modules:core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

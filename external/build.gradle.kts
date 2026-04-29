dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

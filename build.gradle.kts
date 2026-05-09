plugins {
    `java-library`
    `maven-publish`
}

group = "com.dupedb"
version = "1.0.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.code.gson:gson:2.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).apply {
        // Records auto-generate accessor methods; doclint flags missing @param for
        // every component. Suppress that one category, keep the rest active.
        addStringOption("Xdoclint:-missing", "-quiet")
        encoding = "UTF-8"
    }
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.dupedb"
            artifactId = "dupedb-api"
            from(components["java"])
        }
    }
}

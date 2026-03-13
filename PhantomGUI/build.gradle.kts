plugins {
    java
    application
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "vn.ntrung"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("vn.ntrung.phantomgui")
    mainClass.set("vn.ntrung.phantomgui.LauncherKt")
}
kotlin {
    jvmToolchain(17)
}

javafx {
    version = "17.0.14"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing", "javafx.media")
}

// Thêm JavaFX native libs cho tất cả platform vào shadowJar
val javafxVersion = "17.0.14"
val javafxModules = listOf("javafx-base", "javafx-controls", "javafx-fxml", "javafx-graphics", "javafx-media", "javafx-swing", "javafx-web")
val javafxPlatforms = listOf("win", "linux", "mac", "mac-aarch64")

configurations {
    create("javafxNatives")
}

dependencies {
    javafxPlatforms.forEach { platform ->
        javafxModules.forEach { module ->
            add("javafxNatives", "org.openjfx:$module:$javafxVersion:$platform")
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.8.0")
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
        exclude(group = "org.openjfx")
    }
    implementation("net.synedra:validatorfx:0.6.1") {
        exclude(group = "org.openjfx")
    }
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")
    implementation("com.github.almasb:fxgl:17.3") {
        exclude(group = "org.openjfx")
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Cấu hình Shadow JAR để tạo executable JAR với tất cả dependencies
tasks.shadowJar {
    archiveBaseName.set("PhantomGUI")
    archiveClassifier.set("all")
    archiveVersion.set("1.0-SNAPSHOT")

    manifest {
        attributes(
            "Main-Class" to "vn.ntrung.phantomgui.LauncherKt"
        )
    }

    // Bundle JavaFX native libraries cho tất cả platform
    from(project.configurations.getByName("javafxNatives").map { f -> if (f.isDirectory) f else zipTree(f) })

    // Loại bỏ module-info để tránh conflict với fat JAR
    exclude("module-info.class")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    mergeServiceFiles()
}

// Thêm shadowJar vào build task
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Task để chạy shadow JAR
tasks.register<JavaExec>("runJar") {
    group = "application"
    description = "Chạy ứng dụng từ shadow JAR"
    dependsOn(tasks.shadowJar)

    val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
    classpath = files(jarFile)
    mainClass.set("vn.ntrung.phantomgui.LauncherKt")
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}

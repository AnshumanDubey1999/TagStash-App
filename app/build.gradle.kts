plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.detekt)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(files("${rootProject.rootDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

android {
    namespace = "com.anshuman.tagstash"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anshuman.tagstash"
        minSdk = 33
        targetSdk = 36
        
        val appVersionCode = (project.findProperty("app.version.code") as? String)?.toInt() ?: 1
        val appVersionName = (project.findProperty("app.version.name") as? String) ?: "0.1.0"
        
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_FILE")
                ?: (project.findProperty("RELEASE_KEYSTORE_FILE") as? String)
            val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as? String)
            val keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
            val keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)

            if (keystorePath != null && keystorePassword != null && keyAlias != null) {
                val keystoreFile = if (File(keystorePath).isAbsolute) {
                    File(keystorePath)
                } else {
                    rootProject.file(keystorePath).canonicalFile
                }
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = keystorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword ?: keystorePassword
                } else {
                    throw org.gradle.api.GradleException("Release keystore file specified at '$keystorePath' (resolved to '${keystoreFile.absolutePath}') was not found.")
                }
            } else {
                val isReleaseBuildRequested = gradle.startParameter.taskNames.any { task ->
                    task.contains("Release", ignoreCase = true) &&
                    (task.contains("assemble", ignoreCase = true) || task.contains("bundle", ignoreCase = true) || task.contains("package", ignoreCase = true))
                }
                if (isReleaseBuildRequested) {
                    throw org.gradle.api.GradleException(
                        "Release signing credentials (RELEASE_KEYSTORE_FILE, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD) are missing. " +
                        "Please provide them via environment variables or gradle.properties."
                    )
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.exifinterface)
    implementation(libs.material)
    
    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation("io.github.awxkee:avif-coder:2.2.0")
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // UI & Screenshot Testing
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

tasks.withType<Test>().configureEach {
    if (name.contains("ReleaseUnitTest")) {
        isEnabled = false
    }
}

val checkFileLength by tasks.registering {
    group = "verification"
    description = "Checks that no production Kotlin file in app/src/main/java exceeds 500 lines of code."
    doLast {
        val maxLines = 500
        val srcDir = file("src/main/java")
        val violations = mutableListOf<Pair<File, Int>>()

        srcDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val lines = file.readLines().filter { it.trim().isNotEmpty() && !it.trim().startsWith("//") && !it.trim().startsWith("/*") && !it.trim().startsWith("*") }
            if (lines.size > maxLines) {
                violations.add(file to lines.size)
            }
        }

        if (violations.isNotEmpty()) {
            val report = violations.joinToString("\n") { (file, count) ->
                "  - ${file.relativeTo(projectDir)}: $count lines of code (max allowed: $maxLines)"
            }
            throw GradleException("File length limit exceeded! The following production files exceed $maxLines lines of code:\n$report")
        } else {
            println("✓ All production Kotlin files in app/src/main/java are under $maxLines lines of code.")
        }
    }
}

tasks.named("check") {
    dependsOn(checkFileLength)
}
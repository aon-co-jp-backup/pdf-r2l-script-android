plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tokyo.aon.pdfr2lscript"
    compileSdk = 35

    defaultConfig {
        applicationId = "tokyo.aon.pdfr2lscript"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    // 署名鍵は環境変数からのみ受け取る(keystoreも秘密もコミットしない)。揃っていなければ署名なしのreleaseになる。
    val ksFile = System.getenv("PDFR2L_KEYSTORE_FILE")
    val ksPass = System.getenv("PDFR2L_KEYSTORE_PASSWORD")
    val ksAlias = System.getenv("PDFR2L_KEY_ALIAS")
    val hasSigning = !ksFile.isNullOrBlank() && !ksPass.isNullOrBlank() && !ksAlias.isNullOrBlank()

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(ksFile!!)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Rust製コアを cargo-ndk で各ABIの.soにして src/main/jniLibs へ出力する。
// 要: Rust(+ `rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android`)、`cargo install cargo-ndk`、Android NDK。
val rustJniLibs = layout.projectDirectory.dir("src/main/jniLibs")
val buildRustJni = tasks.register<Exec>("buildRustJni") {
    group = "build"
    description = "Rust製コアを cargo-ndk でビルドして src/main/jniLibs へ出力する"
    workingDir = rootDir
    val home = File(System.getProperty("user.home"))
    val cargoBin = File(home, ".cargo/bin")
    val sdkDir = (File(rootDir, "local.properties").takeIf { it.exists() }?.readLines()
        ?.firstOrNull { it.startsWith("sdk.dir=") }?.substringAfter("=")?.replace("\\\\", "/")
        ?: System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT"))
    val ndk = System.getenv("ANDROID_NDK_HOME")
        ?: sdkDir?.let { File(it, "ndk").listFiles()?.filter { f -> f.isDirectory }?.maxByOrNull { f -> f.name }?.absolutePath }
    environment("PATH", cargoBin.absolutePath + File.pathSeparator + System.getenv("PATH"))
    if (ndk != null) environment("ANDROID_NDK_HOME", ndk)
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86_64", "-P", "26",
        "-o", rustJniLibs.asFile.absolutePath, "build", "--release",
    )
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(buildRustJni) }

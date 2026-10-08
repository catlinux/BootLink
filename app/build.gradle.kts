import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Plugin del compilador de Compose, obligatorio desde Kotlin 2.0.
    alias(libs.plugins.kotlin.compose)
    // Serialización a JSON de la lista de apps configuradas.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.catlinux.bootlink"
    compileSdk = 36
    // Google Play exige la API 36 para las apps nuevas desde el 31-08-2026.
    // El SDK de esta máquina trae las build-tools 36.0.0 (AGP usaría las 35.0.0 por defecto).
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.catlinux.bootlink"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.5.0"
    }

    buildTypes {
        release {
            // Todavía no hay nada que ofuscar: la app no usa reflexión ni serialización.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // Activa Jetpack Compose.
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // El BOM fija de una vez las versiones de todas las bibliotecas de Compose.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Navegación entre la lista de apps y el selector.
    implementation(libs.androidx.navigation.compose)

    // El ViewModel que guarda el estado de las pantallas y el estado ligado al ciclo de vida.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Capa de datos: la lista de apps y el modo de arranque viven en DataStore, en JSON.
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // Pruebas unitarias que corren en la máquina, sin emulador ni teléfono.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

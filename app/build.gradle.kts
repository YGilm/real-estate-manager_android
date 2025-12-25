plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")   // Compose-плагин (Kotlin 2.x)
    id("com.google.devtools.ksp")              // KSP для Room
    kotlin("kapt")                             // KAPT для Hilt
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.real_estate_manager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.real_estate_manager"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // Настройки KSP для Room (схемы, инкрементальная генерация, генерация Kotlin-стабов)
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Важно: чтобы java.time (LocalDate и т.п.) работали на API < 26
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    // Для Kotlin 2.x composeOptions не требуются — используем compose-плагин

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

/**
 * Важно для KAPT: терпимо обрабатывает отсутствующие/экспериментальные аннотации
 * (устраняет @error.NonExistentClass в Java-ставах и подобные штуки).
 */
kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Базовые AndroidX/Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    implementation("androidx.biometric:biometric:1.1.0")

    // Навигация
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Room (через KSP)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt (через KAPT — стабильная сборка)
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    // Для @HiltViewModel и интеграции с Compose
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore (сессия пользователя)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Загрузка изображений
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Desugaring JDK (java.time на API < 26)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Тесты
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

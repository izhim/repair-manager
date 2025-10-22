plugins {
    alias(libs.plugins.android.application)
    // TODO para utilizar Safe Args y pasar datos seguros entre fragments
    id("androidx.navigation.safeargs") version "2.8.8"
}

android {
    namespace = "com.example.gestoravisos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gestoravisos"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // TODO para habilitar Safe Args para Java
    viewBinding {
        enable = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.room.common)
    implementation(libs.room.runtime)
    implementation(libs.constraintlayout.compose.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // TODO Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.8.8")
    implementation("androidx.navigation:navigation-ui:2.8.8")

    // TODO Para soportar Navigation en actividades con ViewModels
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.8")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.8")

    // TODO Agregamos dependencias para el uso de Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // TODO dependencia de la biblioteca de seguridad para uso de EncryptedSharedPreferences
    implementation ("androidx.security:security-crypto:1.1.0-alpha06")

    implementation ("com.auth0:java-jwt:4.4.0")
    // TODO Usamos Gson para serializar y deserializar un Map a JSON
    implementation("com.google.code.gson:gson:2.8.8")
    annotationProcessor(libs.room.compiler)

    implementation ("androidx.preference:preference:1.2.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.github.prolificinteractive:material-calendarview:1.4.3")
    implementation ("com.jakewharton.threetenabp:threetenabp:1.4.0")

    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")

    implementation ("androidx.recyclerview:recyclerview:1.2.1")

    implementation ("androidx.work:work-runtime:2.8.1")
}
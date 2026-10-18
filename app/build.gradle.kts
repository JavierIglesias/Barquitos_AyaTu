import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ayatu.barquitos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ayatu.barquitos"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"
        resourceConfigurations += listOf("es", "en")
    }

    // Firma de release para Google Play. El almacén de claves y sus contraseñas nunca entran en
    // git: se leen de keystore.properties en la raíz del proyecto (ignorado por git), con
    //   storeFile=C:/ruta/al/almacen.jks
    //   storePassword=...
    //   keyAlias=...
    //   keyPassword=...
    // Sin ese fichero, la compilación de release se queda sin firmar (debug no se ve afectada).
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }
        ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps != null) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Los tres documentos vivos del proyecto (Docs/) se empaquetan como assets para que las pantallas
// «Novedades», «Manual» y «Registro» los lean dentro de la app sin tocar la red — mismo esquema
// que Rutas Ayatu y Agueda.
val syncDocs by tasks.registering(Copy::class) {
    from(rootProject.file("Docs/historial.md"))
    from(rootProject.file("Docs/novedades.md"))
    from(rootProject.file("Docs/manual_usuario.md"))
    into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn(syncDocs) }

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)

    testImplementation(libs.junit)
}

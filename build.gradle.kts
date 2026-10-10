// Fichero de nivel superior: las versiones de los plugins se declaran en el catálogo
// (gradle/libs.versions.toml) y se aplican con `apply false` en cada módulo que las use.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

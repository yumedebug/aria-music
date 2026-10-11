plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // :innertube が使う Kotlin JVM / serialization もバージョンを確定させるため
    // ルートで宣言しておく（未宣言だと subproject 側で
    // "plugin is already on the classpath with an unknown version" になる）。
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

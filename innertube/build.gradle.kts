@file:Suppress("UnstableApiUsage")

// YouTube Music バックエンド。OuterTune の手法（InnerTube API +
// NewPipeExtractor、GPL-3.0）を移植し、`innertube/src` に同梱実装。
//
// ツールチェーンはアプリ側に合わせて Kotlin 2.0.21 / JVM 17 に固定。
// 依存解決で Kotlin メタデータ非互換が出た場合は、ここを緩めるのではなく
// gradle/libs.versions.toml の `kotlin`（と `ktor`）を上げること。
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.encoding)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.brotli)
    implementation(libs.newpipe.extractor)

    testImplementation(libs.junit)
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NewPipeExtractor（ストリームURLの署名解除）は JitPack にしか公開されていない。
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AriaMusic"
include(":app")
// YouTube Music バックエンド（InnerTube API + NewPipeExtractor、GPL-3.0）。
// 実装は innertube/src に同梱。
include(":innertube")

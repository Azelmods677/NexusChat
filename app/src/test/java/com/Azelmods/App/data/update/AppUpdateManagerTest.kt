package com.Azelmods.App.data.update

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AppUpdateManagerTest : StringSpec({
    "una versión mayor es más nueva" {
        isNewerVersion("8.1.0", "8.0.0") shouldBe true
        isNewerVersion("v8.0.10", "8.0.9") shouldBe true
        isNewerVersion("10.0.0", "9.9.9") shouldBe true
    }

    "la misma versión o una anterior no avisan" {
        isNewerVersion("8.0.0", "8.0.0") shouldBe false
        isNewerVersion("8.0", "8.0.0") shouldBe false
        isNewerVersion("7.9.9", "8.0.0") shouldBe false
    }

    "cada APK se reconoce por su nombre" {
        abiOf("app-arm64-v8a-release.apk") shouldBe "arm64-v8a"
        abiOf("app-armeabi-v7a-release.apk") shouldBe "armeabi-v7a"
        abiOf("app-universal-release.apk") shouldBe "universal"
        abiOf("nexuschat.apk") shouldBe "universal"
    }

    "una version es obligatoria salvo que sus notas digan opcional" {
        isMandatoryRelease("Novedades de la 8.0") shouldBe true
        isMandatoryRelease("") shouldBe true
        isMandatoryRelease("Pequeños arreglos [opcional]") shouldBe false
    }
})

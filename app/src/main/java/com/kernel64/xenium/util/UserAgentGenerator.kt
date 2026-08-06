package com.kernel64.xenium.util

import kotlin.random.Random

object UserAgentGenerator {

    /**
     * Generates a random User Agent string for incognito tabs on Android.
     * Rules:
     * - OS: Android (14..17)
     * - Browsers: Chrome, Firefox, Opera
     * - Version range: 145..150
     */
    fun generateRandomIncognitoUserAgent(): String {
        val version = Random.nextInt(145, 151) // Range 145..150 inclusive
        val androidVer = Random.nextInt(14, 18) // 14, 15, 16, 17
        val browserType = listOf("chrome", "firefox", "opera").random()

        return when (browserType) {
            "chrome" -> "Mozilla/5.0 (Linux; Android $androidVer; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version.0.0.0 Mobile Safari/537.36"
            "firefox" -> "Mozilla/5.0 (Android $androidVer; Mobile; rv:$version.0) Gecko/$version.0 Firefox/$version.0"
            "opera" -> "Mozilla/5.0 (Linux; Android $androidVer; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version.0.0.0 Mobile Safari/537.36 OPR/$version.0.0"
            else -> "Mozilla/5.0 (Linux; Android $androidVer; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version.0.0.0 Mobile Safari/537.36"
        }
    }
}

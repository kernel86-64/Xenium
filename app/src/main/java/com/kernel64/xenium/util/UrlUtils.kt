package com.kernel64.xenium.util

import java.net.URLEncoder
import java.util.regex.Pattern

object UrlUtils {

    private val IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +
                "(:[0-9]{1,5})?(?:/.*)?$"
    )

    private val IPV6_PATTERN = Pattern.compile(
        "^(\\[[0-9a-fa-f:]+])|([0-9a-fa-f]{1,4}:[0-9a-fa-f:]+)(:[0-9]{1,5})?(?:/.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val DOMAIN_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:[0-9]{1,5})?(?:/.*)?$"
    )

    fun processInputToUrl(input: String, searchEngine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return searchEngine.homepageUrl

        // 1. Explicit protocol
        if (trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }

        // 2. IPv4
        if (IPV4_PATTERN.matcher(trimmed).matches()) {
            return "http://$trimmed"
        }

        // 3. IPv6
        if (IPV6_PATTERN.matcher(trimmed).matches()) {
            return if (trimmed.startsWith("[")) {
                "http://$trimmed"
            } else {
                // If contains port or path after colon, ensure bracket wrapper around IP
                val slashIndex = trimmed.indexOf('/')
                val portIndex = trimmed.indexOf("]:")
                if (trimmed.contains(':') && !trimmed.startsWith("[")) {
                    "http://[$trimmed]"
                } else {
                    "http://$trimmed"
                }
            }
        }

        // 4. Localhost check
        if (trimmed.equals("localhost", ignoreCase = true) || 
            trimmed.startsWith("localhost:", ignoreCase = true) ||
            trimmed.startsWith("localhost/", ignoreCase = true)) {
            return "http://$trimmed"
        }

        // 5. Standard domain name without spaces
        if (!trimmed.contains(" ") && DOMAIN_PATTERN.matcher(trimmed).matches()) {
            return "https://$trimmed"
        }

        // 6. Fallback: Search Query
        return try {
            String.format(searchEngine.urlTemplate, URLEncoder.encode(trimmed, "UTF-8"))
        } catch (e: Exception) {
            String.format(searchEngine.urlTemplate, trimmed)
        }
    }
}

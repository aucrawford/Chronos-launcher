package com.soc.launcher.util

object SpamDetector {
    fun isSpam(address: String, body: String): Boolean {
        // Initial spam check
        val hasLink = body.contains("http", ignoreCase = true) || 
                     body.contains("bit.ly", ignoreCase = true) || 
                     body.contains(".com/", ignoreCase = true) ||
                     body.contains(".net/", ignoreCase = true)
        
        val isAlphanumeric = address.any { it.isLetter() }
        val isShortCode = address.isNotBlank() && address.length <= 6 && address.all { it.isDigit() }
        
        // OTP Detection: Generally 4-8 digits, often accompanied by keywords
        val otpKeywords = listOf("OTP", "verification code", "code is", "security code", "login code", "your code", "confirm code", "verify your")
        val isOtp = otpKeywords.any { body.contains(it, ignoreCase = true) } ||
                    (body.any { it.isDigit() } && body.filter { it.isDigit() }.length in 4..8 && isShortCode)

        return (hasLink || isAlphanumeric || isShortCode) && !isOtp
    }
}

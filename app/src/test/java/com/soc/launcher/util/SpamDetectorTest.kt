package com.soc.launcher.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamDetectorTest {

    @Test
    fun testIsSpam() {
        // Legitimate personal message
        assertFalse(SpamDetector.isSpam("+1234567890", "Hey, how are you?"))
        
        // Potential Spam with link
        assertTrue(SpamDetector.isSpam("+1234567890", "Win a free prize at http://evil.com"))
        
        // Alphanumeric sender (often marketing)
        assertTrue(SpamDetector.isSpam("PROMO", "Get 50% off your next purchase!"))
        
        // Short code sender (often marketing or service)
        assertTrue(SpamDetector.isSpam("12345", "Your package is arriving today."))
    }

    @Test
    fun testOtpPreservation() {
        // OTP from alphanumeric sender
        assertFalse(SpamDetector.isSpam("GOOGLE", "Your Google verification code is 123456"))
        
        // OTP from short code
        assertFalse(SpamDetector.isSpam("56789", "Your OTP is 9876"))
        
        // OTP with link (sometimes banks do this)
        assertFalse(SpamDetector.isSpam("BANKOK", "Your code is 112233. Verify at bank.com/v"))
    }
}

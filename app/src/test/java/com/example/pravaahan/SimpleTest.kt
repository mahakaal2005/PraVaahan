package com.example.pravaahan

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Simple test to verify JUnit 5 setup is working
 */
class SimpleTest {

    @Test
    fun `simple test should pass`() {
        val result = 2 + 2
        assertEquals(4, result)
        assertTrue(result > 0)
    }

    @Test
    fun `string test should pass`() {
        val message = "Hello PraVaahan"
        assertNotNull(message)
        assertTrue(message.contains("PraVaahan"))
    }
}
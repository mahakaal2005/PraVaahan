package com.example.pravaahan.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 extension for setting up coroutine test dispatchers
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTestExtension(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}

/**
 * Test utilities for railway control system testing
 */
object TestUtils {
    
    /**
     * Creates a test dispatcher for coroutine testing
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun createTestDispatcher(): TestDispatcher = StandardTestDispatcher()
    
    /**
     * Runs a test with proper coroutine setup
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    inline fun runCoroutineTest(
        testDispatcher: TestDispatcher = StandardTestDispatcher(),
        crossinline testBody: suspend () -> Unit
    ) {
        kotlinx.coroutines.test.runTest(testDispatcher) {
            testBody()
        }
    }
}
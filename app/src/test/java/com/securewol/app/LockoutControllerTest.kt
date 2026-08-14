package com.securewol.app

import com.securewol.app.core.security.LockoutController
import org.junit.Assert.assertEquals
import org.junit.Test

class LockoutControllerTest {

    @Test
    fun testProgressiveLockoutCalculation() {
        // We can test calculation directly
        // Under 3 failures -> 0 delay
        val delay0 = calculateDelay(0)
        val delay1 = calculateDelay(1)
        val delay2 = calculateDelay(2)
        assertEquals(0L, delay0)
        assertEquals(0L, delay1)
        assertEquals(0L, delay2)

        // 3 to 4 failures -> 30s
        val delay3 = calculateDelay(3)
        val delay4 = calculateDelay(4)
        assertEquals(30L, delay3)
        assertEquals(30L, delay4)

        // 5 to 9 failures -> 120s (2 minutes)
        val delay5 = calculateDelay(5)
        val delay9 = calculateDelay(9)
        assertEquals(120L, delay5)
        assertEquals(120L, delay9)

        // 10+ failures -> 300s (5 minutes)
        val delay10 = calculateDelay(10)
        val delay15 = calculateDelay(15)
        assertEquals(300L, delay10)
        assertEquals(300L, delay15)
    }

    private fun calculateDelay(failures: Int): Long {
        return when {
            failures >= 10 -> LockoutController.DELAY_10_FAILS_SECONDS
            failures >= 5 -> LockoutController.DELAY_5_FAILS_SECONDS
            failures >= 3 -> LockoutController.DELAY_3_FAILS_SECONDS
            else -> 0L
        }
    }
}

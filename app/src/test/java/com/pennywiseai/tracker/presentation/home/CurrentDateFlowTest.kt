package com.pennywiseai.tracker.presentation.home

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentDateFlowTest {
    private val minute = 60_000L

    @Test
    fun `emits the next date after midnight so a trend window can roll forward`() = runTest {
        val origin = LocalDateTime.of(2026, 9, 1, 23, 58)
        val emitted = mutableListOf<LocalDate>()
        val job = launch {
            currentDateFlow { origin.plus(currentTime, ChronoUnit.MILLIS) }.toCollection(emitted)
        }

        runCurrent()
        assertEquals(listOf(LocalDate.of(2026, 9, 1)), emitted)

        advanceTimeBy(3 * minute)
        runCurrent()
        assertEquals(listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2)), emitted)

        job.cancel()
    }

    @Test
    fun `does not re-emit while the date is unchanged`() = runTest {
        val origin = LocalDateTime.of(2026, 9, 1, 10, 0)
        val emitted = mutableListOf<LocalDate>()
        val job = launch {
            currentDateFlow { origin.plus(currentTime, ChronoUnit.MILLIS) }.toCollection(emitted)
        }

        advanceTimeBy(30 * minute)
        runCurrent()

        assertEquals(listOf(LocalDate.of(2026, 9, 1)), emitted)
        job.cancel()
    }

    @Test
    fun `notices a date change even when the wall clock jumps past a long wait`() = runTest {
        // Simulates the clock moving on while the coroutine was parked (e.g. the
        // device slept overnight): the next short step must pick up the new day.
        var jumpMillis = 0L
        val origin = LocalDateTime.of(2026, 9, 1, 12, 0)
        val emitted = mutableListOf<LocalDate>()
        val job = launch {
            currentDateFlow { origin.plus(currentTime + jumpMillis, ChronoUnit.MILLIS) }.toCollection(emitted)
        }

        runCurrent()
        jumpMillis = 14 * 60 * minute // 02:00 the next day
        advanceTimeBy(2 * minute)
        runCurrent()

        assertEquals(listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2)), emitted)
        job.cancel()
    }
}

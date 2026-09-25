package com.lastwave.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepTimerTest {

    @Test
    fun testMusicPlayerStateSleepTimerDefault() {
        val state = MusicPlayerState()
        assertNull(state.sleepTimerRemainingMs)
        assertNull(state.sleepTimerRemainingTracks)
    }

    @Test
    fun testMusicPlayerStateSleepTimerTracks() {
        val state = MusicPlayerState(sleepTimerRemainingTracks = 3)
        assertNull(state.sleepTimerRemainingMs)
        assertEquals(3, state.sleepTimerRemainingTracks)
    }

    @Test
    fun testMusicPlayerStateSleepTimerMinutes() {
        val state = MusicPlayerState(sleepTimerRemainingMs = 900_000L)
        assertEquals(900_000L, state.sleepTimerRemainingMs)
        assertNull(state.sleepTimerRemainingTracks)
    }
}

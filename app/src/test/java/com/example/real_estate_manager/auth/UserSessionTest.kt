package com.example.real_estate_manager.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class UserSessionTest {

    private lateinit var session: UserSession
    private var now: Long = 0L

    private val ttlMs = 15 * 60_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        session = UserSession(context)
        session.nowProvider = { now }
        runBlocking { session.signOut() }
    }

    @Test
    fun signInRememberFalse_expiredForeground_clearsUser() {
        setNow(1_000_000L)

        runBlocking {
            session.signIn("u1", remember = false)
            session.onAppBackground()
        }

        val afterBg = runBlocking { session.stateFlow.first() }
        assertTrue(afterBg.expiresAtMillis > 0L)

        setNow(afterBg.expiresAtMillis + 1)
        runBlocking { session.onAppForeground() }

        val afterFg = runBlocking { session.stateFlow.first() }
        assertNull(afterFg.userId)
        assertFalse(afterFg.locked)
    }

    @Test
    fun signInRememberTrue_expiredForeground_locksUser() {
        setNow(2_000_000L)

        runBlocking {
            session.signIn("u1", remember = true)
            session.onAppBackground()
        }

        val afterBg = runBlocking { session.stateFlow.first() }
        setNow(afterBg.expiresAtMillis + 1)
        runBlocking { session.onAppForeground() }

        val afterFg = runBlocking { session.stateFlow.first() }
        assertEquals("u1", afterFg.userId)
        assertTrue(afterFg.locked)
    }

    @Test
    fun notExpiredForeground_unlocksAndExtendsExpiry() {
        setNow(3_000_000L)

        runBlocking {
            session.signIn("u1", remember = true)
            session.onAppBackground()
        }

        val afterBg = runBlocking { session.stateFlow.first() }
        setNow(afterBg.expiresAtMillis - 1)
        runBlocking { session.onAppForeground() }

        val afterFg = runBlocking { session.stateFlow.first() }
        assertEquals("u1", afterFg.userId)
        assertFalse(afterFg.locked)
        assertTrue(afterFg.expiresAtMillis > afterBg.expiresAtMillis)
    }

    @Test
    fun unlockAndExtend_clearsLock_andExtendsExpiry() {
        setNow(4_000_000L)

        runBlocking {
            session.signIn("u1", remember = true)
            session.onAppBackground()
        }

        val afterBg = runBlocking { session.stateFlow.first() }
        setNow(afterBg.expiresAtMillis + 1)
        runBlocking { session.onAppForeground() }

        setNow(5_000_000L)
        runBlocking { session.unlockAndExtend() }

        val afterUnlock = runBlocking { session.stateFlow.first() }
        assertEquals("u1", afterUnlock.userId)
        assertFalse(afterUnlock.locked)
        assertEquals(5_000_000L + ttlMs, afterUnlock.expiresAtMillis)
    }

    private fun setNow(millis: Long) {
        now = millis
    }
}

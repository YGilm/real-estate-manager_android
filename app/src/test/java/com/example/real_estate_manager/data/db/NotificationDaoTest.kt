package com.example.real_estate_manager.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.notificationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun unreadCountIncrementsAndMarkReadDecrements() = runBlocking {
        dao.insert(notification("n1"))
        dao.insert(notification("n2"))

        assertEquals(2, dao.unreadCount(USER_ID).first())

        dao.markRead(USER_ID, "n1", now = 3_000L)

        assertEquals(1, dao.unreadCount(USER_ID).first())
        val read = dao.getById(USER_ID, "n1")
        assertEquals(true, read?.isRead)
        assertEquals(3_000L, read?.readAt)
    }

    @Test
    fun deleteHidesNotificationButKeepsDatabaseRecord() = runBlocking {
        dao.insert(notification("n1"))

        dao.markDeleted(USER_ID, "n1", now = 4_000L)

        assertTrue(dao.observeAll(USER_ID).first().isEmpty())
        assertNotNull(dao.getById(USER_ID, "n1"))
        assertEquals("DELETED", dao.getById(USER_ID, "n1")?.syncStatus)
    }

    @Test
    fun notificationSurvivesNewObservationUntilExplicitDelete() = runBlocking {
        dao.insert(notification("n1"))

        assertEquals(listOf("n1"), dao.observeAll(USER_ID).first().map { it.id })
        assertEquals(listOf("n1"), dao.observeAll(USER_ID).first().map { it.id })
    }

    private fun notification(id: String): NotificationEntity =
        NotificationEntity(
            id = id,
            userId = USER_ID,
            title = "Reminder",
            message = "Persistent notification",
            type = NotificationType.REMINDER.name,
            relatedEntityId = "reminder-id",
            relatedEntityType = NotificationRelatedEntityType.REMINDER.name,
            isRead = false,
            createdAt = 1_000L,
            readAt = null,
            actionPayload = null,
            updatedAt = 1_000L
        )

    private companion object {
        const val USER_ID = "user-id"
    }
}

package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SessionRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database by lazy {
        CheckPilotDatabase.build(
            context = ApplicationProvider.getApplicationContext(),
            name = folder.root.resolve(CheckPilotDatabase.FILE_NAME).absolutePath,
            openHelperFactory = null
        )
    }
    private val repository by lazy { SessionRepositoryImpl(dao = database.sessionDao()) }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun anInsertedSessionIsObservedAsItWasSaved() =
        runTest {
            // Given
            val session =
                Session(
                    id = "session-1",
                    createdAt = Instant.parse("2026-11-22T09:30:00Z"),
                    date = LocalDate.of(2026, 11, 1),
                    cm1StaffNumber = null,
                    cm2StaffNumber = checkNotNull(StaffNumber.parse("04512")),
                    departure = checkNotNull(AirportCode.parse("DOH")),
                    destination = checkNotNull(AirportCode.parse("SLL"))
                )

            // When
            repository.insert(session)

            // Then
            assertEquals(session, repository.observe("session-1").first())
        }

    @Test
    fun anUnknownSessionIsObservedAsNull() =
        runTest {
            // Given: no Session saved

            // When
            val session = repository.observe("missing").first()

            // Then
            assertNull(session)
        }
}

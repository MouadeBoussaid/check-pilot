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
    fun aChangedDateIsObservedAndTheRestOfTheSessionIsKept() =
        runTest {
            // Given
            val session = session(id = "session-1", createdAt = Instant.parse("2026-11-22T09:30:00Z"))
            repository.insert(session)

            // When
            repository.updateDate(id = "session-1", date = LocalDate.of(2026, 11, 5))

            // Then
            assertEquals(session.copy(date = LocalDate.of(2026, 11, 5)), repository.observe("session-1").first())
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

    @Test
    fun theLastSessionIsTheLatestCreatedWithTiesGoingToTheLaterInsert() =
        runTest {
            // Given
            val tieTime = Instant.parse("2026-11-22T10:00:00Z")
            repository.insert(session(id = "b-first-at-tie", createdAt = tieTime))
            repository.insert(session(id = "c-older", createdAt = Instant.parse("2026-11-22T08:00:00Z")))
            repository.insert(session(id = "a-second-at-tie", createdAt = tieTime))

            // When
            val last = repository.observeLast().first()

            // Then
            assertEquals("a-second-at-tie", last?.id)
        }

    @Test
    fun withoutSessionsThereIsNoLastSession() =
        runTest {
            // Given: no Session saved

            // When
            val last = repository.observeLast().first()

            // Then
            assertNull(last)
        }

    private fun session(
        id: String,
        createdAt: Instant,
    ): Session =
        Session(
            id = id,
            createdAt = createdAt,
            date = LocalDate.of(2026, 11, 22),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = null,
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )
}

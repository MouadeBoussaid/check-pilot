package com.mebiad.checkpilot.data.local.db

import com.mebiad.checkpilot.data.local.db.dao.SessionDao
import com.mebiad.checkpilot.data.local.db.entity.SessionEntity
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.SessionFilter
import com.mebiad.checkpilot.domain.session.SessionRepository
import com.mebiad.checkpilot.domain.session.StaffNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class SessionRepositoryImpl(
    private val dao: SessionDao,
) : SessionRepository {
    override suspend fun insert(session: Session) = dao.insert(session.toEntity())

    override suspend fun updateDate(
        id: String,
        date: LocalDate,
    ) = dao.updateDate(id, date)

    override fun observe(id: String): Flow<Session?> = dao.observe(id).map { it?.toSession() }

    override fun observeLast(): Flow<Session?> = dao.observeLast().map { it?.toSession() }

    override fun search(filter: SessionFilter): Flow<List<Session>> =
        dao
            .search(
                staffNumberPrefix = filter.staffNumberPrefix,
                airportPrefix = filter.airportPrefix,
                date = filter.date
            ).map { rows -> rows.map { it.toSession() } }

    override suspend fun delete(id: String) = dao.delete(id)
}

private fun Session.toEntity(): SessionEntity =
    SessionEntity(
        id = id,
        createdAt = createdAt,
        date = date,
        cm1StaffNumber = cm1StaffNumber?.digits,
        cm2StaffNumber = cm2StaffNumber?.digits,
        departureCode = departure.letters,
        destinationCode = destination.letters
    )

// Rows are only ever written from valid Sessions, so parsing them back can't fail.
private fun SessionEntity.toSession(): Session =
    Session(
        id = id,
        createdAt = createdAt,
        date = date,
        cm1StaffNumber = cm1StaffNumber?.let { checkNotNull(StaffNumber.parse(it)) },
        cm2StaffNumber = cm2StaffNumber?.let { checkNotNull(StaffNumber.parse(it)) },
        departure = checkNotNull(AirportCode.parse(departureCode)),
        destination = checkNotNull(AirportCode.parse(destinationCode))
    )

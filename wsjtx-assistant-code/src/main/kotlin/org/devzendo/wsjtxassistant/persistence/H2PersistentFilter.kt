package org.devzendo.wsjtxassistant.persistence

import org.apache.commons.lang3.StringUtils
import org.devzendo.wsjtxassistant.data.CallsignState
import org.devzendo.wsjtxassistant.logparse.Callsign
import org.devzendo.wsjtxassistant.logparse.LogEntry
import org.devzendo.wsjtxassistant.logparse.Mode
import org.devzendo.wsjtxassistant.logparse.UTCDateTime
import org.h2.engine.ExistenceChecker
import org.slf4j.LoggerFactory
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.io.File
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle


/**
 * Copyright (C) 2008-2017 Matt Gumbley, DevZendo.org http://devzendo.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class H2PersistentFilter(storeDir: File) : PersistentFilter {
    val logger = LoggerFactory.getLogger(H2PersistentFilter::class.java)!!

    var publisher: ((logEntry: LogEntry) -> Unit)? = null

    data class StoredLogEntryWithCallsignState(val logEntry: LogEntry, val callsignState: CallsignState)

    private var dbFile: File = File(storeDir, "persistence")
    private var template: SimpleJdbcTemplate
    private var dataSource: SingleConnectionDataSource
    private var rowMapper: RowMapper<StoredLogEntryWithCallsignState>
    private val utc = ZoneId.of("UTC")

    init {
        val needToCreate = !exists()
        val dbURL = "jdbc:h2:" + dbFile.getAbsolutePath() + ";AUTOCOMMIT=ON"
        logger.debug("Opening database at {}", dbFile.getAbsolutePath())
        dataSource = SingleConnectionDataSource(dbURL, "sa", "", false)
        template = SimpleJdbcTemplate(dataSource)
        if (needToCreate) {
            logger.debug("Database not initialised; creating tables...")
            create()
        } else {
            logger.debug("Database open")
        }
        rowMapper = object : RowMapper<StoredLogEntryWithCallsignState> {
            override fun mapRow(rs: ResultSet, rowNum: Int): StoredLogEntryWithCallsignState {
                val callsign = rs.getString("callsign")
                val dxCallsign = rs.getString("dxCallsign")
                val utcDateTime = rs.getTimestamp("utcDateTime")
                val power = rs.getInt("power")
                val offset = rs.getInt("offsetFrequency")
                val mode = rs.getString("mode")
                val grid = rs.getString("grid")
                val state = rs.getString("state")
                return StoredLogEntryWithCallsignState(LogEntry(UTCDateTime(ZonedDateTime.ofInstant(utcDateTime.toInstant(), utc)), power, offset, Mode.valueOf(mode), callsign, dxCallsign, grid), CallsignState.valueOf(state))
            }
        }
    }

    companion object H2PersistentFilter {
        private val ISO_LOCAL_DATE_TIME = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .appendOffset("+HH:MM", "+00:00")
                .toFormatter()

        // as per H2 docs p33, "Date and Time" YYYY-MM-DD HH:MM:SS+00:00
        // not no 'T' separating date and time, as per default ISO8601 formatters in DateTimeFormatterBuilder.
        fun toISO8601DateTime(utcDateTime: UTCDateTime): String {
            return utcDateTime.value.format(ISO_LOCAL_DATE_TIME)
        }
    }

    private fun exists(): Boolean {
        return ExistenceChecker.exists(dbFile.getAbsolutePath())
    }

    private fun create() {
        logger.info("Creating database...")
        val ddls = arrayOf("CREATE TABLE LogEntries (callsign VARCHAR(25), dxCallsign VARCHAR(25), utcDateTime TIMESTAMP, power INT, offsetFrequency INT, mode VARCHAR(10), grid VARCHAR(10), state VARCHAR(15), PRIMARY KEY(callsign))")
        for (ddl in ddls) {
            template.getJdbcOperations().execute(ddl)
        }
    }

    // Set the callback that is called by incoming, if an incoming entry has not been marked as ignorable.
    override fun publish(publisher: (logEntry: LogEntry) -> Unit) {
        this.publisher = publisher
    }

    override fun recordExists(callsign: Callsign): Boolean {
        logger.debug("Does record {} exist?", callsign)
        val exists = template.queryForInt("SELECT COUNT(*) FROM LogEntries WHERE callsign = ?", callsign) === 1
        logger.debug("Record {} {}", callsign, if (exists) "exists" else "does not exist")
        return exists
    }

    // The user has chosen that this LogEntry has this CallsignState; persist the entry.
    override fun record(logEntry: LogEntry, state: CallsignState) {
        logger.info("Storing record " + logEntry + " with state " + state)
        val sqlDateTime = toISO8601DateTime(logEntry.utcDateTime)

        if (recordExists(logEntry.callsign)) {
            val sql = "UPDATE LogEntries SET dxCallsign = ?, utcDateTime = ?, power = ?, offsetFrequency = ?, mode = ?, grid = ?, state = ? WHERE callsign = ?"
            template.jdbcOperations.update(
                    sql,
                    StringUtils.defaultString(logEntry.dxCallsign),
                    StringUtils.defaultString(sqlDateTime.toString()),
                    logEntry.power,
                    logEntry.offset,
                    StringUtils.defaultString(logEntry.mode.toString()),
                    StringUtils.defaultString(logEntry.grid),
                    StringUtils.defaultString(state.toString()),
                    StringUtils.defaultString(logEntry.callsign))
        } else {
            val sql = "INSERT INTO LogEntries (callsign, dxCallsign, utcDateTime, power, offsetFrequency, mode, grid, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            template.jdbcOperations.update(
                    sql,
                    StringUtils.defaultString(logEntry.callsign),
                    StringUtils.defaultString(logEntry.dxCallsign),
                    StringUtils.defaultString(sqlDateTime.toString()),
                    logEntry.power,
                    logEntry.offset,
                    StringUtils.defaultString(logEntry.mode.toString()),
                    StringUtils.defaultString(logEntry.grid),
                    StringUtils.defaultString(state.toString()))
        }
    }

    // An entry has been received from the tailer; if it is not ignored by a recording (see record), it is given to
    // the publisher (if it has been set by publish)
    override fun incoming(logEntry: LogEntry) {
        val state: Optional<CallsignState> = getRecordedStateForCallsign(logEntry.callsign)
        val filterOut = (state.isPresent && (canIgnore(state.get())))
        if (!filterOut) {
            publisher?.invoke(logEntry)
        }
    }

    private fun canIgnore(state: CallsignState): Boolean {
        return when(state) {
            CallsignState.WORKEDALREADY -> true
            CallsignState.DOESNTQSL -> true
            CallsignState.IGNOREFORNOW -> true
            else -> false
        }
    }

    override fun getRecordedStateForCallsign(callsign: Callsign): Optional<CallsignState> {
        logger.info("Searching for entry from callsign " + callsign)
        val sql = "SELECT * FROM LogEntries WHERE callsign = ?"
        try {
            val queriedObject: StoredLogEntryWithCallsignState = template.queryForObject(sql, rowMapper, callsign)
            logger.debug("queriedObject is " + queriedObject)
            val state = queriedObject.callsignState
            logger.info("Callsign " + callsign + " has state " + state)
            return Optional.of(state)
        } catch (e: IncorrectResultSizeDataAccessException) {
            logger.info("No entry stored for callsign " + callsign)
            return Optional.empty()
        }
    }

    override fun getLogEntryForCallsign(callsign: Callsign): Optional<LogEntry> {
        logger.info("Searching for entry from callsign " + callsign)
        val sql = "SELECT * FROM LogEntries WHERE callsign = ?"
        try {
            val queriedObject: StoredLogEntryWithCallsignState = template.queryForObject(sql, rowMapper, callsign)
            logger.debug("queriedObject is " + queriedObject)
            val logEntry = queriedObject.logEntry
            logger.info("Callsign " + callsign + " has log entry " + logEntry)
            return Optional.of(logEntry)
        } catch (e: IncorrectResultSizeDataAccessException) {
            logger.info("No entry stored for callsign " + callsign)
            return Optional.empty()
        }
    }

    override fun close() {
        logger.info("Closing db")
        try {
            DataSourceUtils.getConnection(dataSource).close()
        } catch (e: CannotGetJdbcConnectionException) {
            logger.warn("Failed to close db: {}", e.message)
        } catch (e: SQLException) {
            logger.warn("Failed to close db: {}", e.message)
        }
    }
}
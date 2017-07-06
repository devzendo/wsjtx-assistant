package org.devzendo.wsjtxassistant.persistence

import org.devzendo.wsjtxassistant.data.CallsignState
import org.devzendo.wsjtxassistant.logparse.LogEntry
import org.springframework.jdbc.core.RowMapper
import java.io.File
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import com.sun.tools.corba.se.idl.Util.getAbsolutePath
import org.h2.engine.ExistenceChecker
import com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER
import com.sun.tools.corba.se.idl.Util.getAbsolutePath
import java.sql.ResultSet
import java.sql.SQLException
import com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER
import org.devzendo.wsjtxassistant.logparse.LogFileParser
import org.devzendo.wsjtxassistant.logparse.Mode
import org.slf4j.LoggerFactory


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

    init {
        val needToCreate = !exists()
        val dbURL = "jdbc:h2:" + dbFile.getAbsolutePath()
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
                val localDateTime = rs.getTimestamp("localDateTime")
                val power = rs.getInt("power")
                val offset = rs.getInt("offsetFrequency")
                val mode = rs.getString("mode")
                val grid = rs.getString("grid")
                val state = rs.getString("state")
                return StoredLogEntryWithCallsignState(LogEntry(localDateTime.toLocalDateTime(), power, offset, Mode.valueOf(mode), callsign, dxCallsign, grid), CallsignState.valueOf(state))
            }
        }

    }
    private fun exists(): Boolean {
        return ExistenceChecker.exists(dbFile.getAbsolutePath())
    }

    private fun create() {
        LOGGER.info("Creating database...")
        val ddls = arrayOf("CREATE TABLE LogEntries(callsign VARCHAR(25), dxCallsign VARCHAR(25), localDateTime TIMESTAMP, power INT, offsetFrequency INT, mode VARCHAR(10), grid VARCHAR(10), state VARCHAR(15), PRIMARY KEY(callsign))")
        for (ddl in ddls) {
            template.getJdbcOperations().execute(ddl)
        }
    }

    // Set the callback that is called by incoming, if an incoming entry has not been marked as ignorable.
    override fun publish(publisher: (logEntry: LogEntry) -> Unit) {
        this.publisher = publisher
    }

    // The user has chosen that this LogEntry has this CallsignState; persist the entry.
    override fun record(logEntry: LogEntry, state: CallsignState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // An entry has been received from the tailer; if it is not ignored by a recording (see record), it is given to
    // the publisher (if it has been set by publish)
    override fun incoming(logEntry: LogEntry) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        // TODO pass things that get past filter to publisher
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
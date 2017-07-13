package org.devzendo.wsjtxassistant.persistence

import com.spotify.hamcrest.optional.OptionalMatchers
import com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional
import com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue
import org.devzendo.commoncode.concurrency.ThreadUtils
import org.devzendo.wsjtxassistant.data.CallsignState
import org.devzendo.wsjtxassistant.logging.ConsoleLoggingUnittestCase
import org.devzendo.wsjtxassistant.logparse.LogEntry
import org.devzendo.wsjtxassistant.logparse.Mode
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

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
class TestH2PersistentFilter : ConsoleLoggingUnittestCase() {

    val logger = LoggerFactory.getLogger(TestH2PersistentFilter::class.java)!!

    @get:Rule
    var thrown = ExpectedException.none()!!

    @Rule
    @JvmField
    val tempDir = TemporaryFolder()

    var persistFilter: PersistentFilter? = null
    var published: Optional<LogEntry> = Optional.empty()

    @Before
    fun before() {
        persistFilter = H2PersistentFilter(tempDir.getRoot())
        persistFilter!!.publish { logEntry -> published = Optional.of(logEntry) }
    }

    @After
    fun after() {
        persistFilter?.close()
    }

    @Test
    fun entryIsNotIgnoredSoPassItThrough() {
        val logEntry = sampleLogEntry()
        persistFilter!!.incoming(logEntry)
        assertThat(published, optionalWithValue<LogEntry?>(equalTo(logEntry)))
    }

    @Test
    fun entryIsIgnoredSoDontPassItThrough() {
        val logEntry = sampleLogEntry()
        persistFilter!!.record(logEntry, CallsignState.IGNOREFORNOW)
        persistFilter!!.incoming(logEntry)
        assertThat(published, emptyOptional())
    }

    @Test
    fun entryDoesNotQSLSoDontPassItThrough() {
        val logEntry = sampleLogEntry()
        persistFilter!!.record(logEntry, CallsignState.DOESNTQSL)
        persistFilter!!.incoming(logEntry)
        assertThat(published, emptyOptional())
    }

    @Test
    fun entryAlreadyWorkedSoDontPassItThrough() {
        val logEntry = sampleLogEntry()
        persistFilter!!.record(logEntry, CallsignState.WORKEDALREADY)
        persistFilter!!.incoming(logEntry)
        assertThat(published, emptyOptional())
    }

    @Test
    fun entryUsesEQSLSoPassItThrough() {
        val logEntry = sampleLogEntry()
        persistFilter!!.record(logEntry, CallsignState.QSLVIAEQSL)
        persistFilter!!.incoming(logEntry)
        assertThat(published, optionalWithValue<LogEntry?>(equalTo(logEntry)))
    }

    @Test
    fun entryUsesBureauSoPassItThrough() {
        val logEntry = sampleLogEntry()
        persistFilter!!.record(logEntry, CallsignState.QSLVIABURO)
        persistFilter!!.incoming(logEntry)
        assertThat(published, optionalWithValue<LogEntry?>(equalTo(logEntry)))
    }

    private fun sampleLogEntry(): LogEntry {
        return LogEntry(LocalDateTime.now(), -18, 980, Mode.JT65, "M0CUV", "CQ", "IO80")
    }
}

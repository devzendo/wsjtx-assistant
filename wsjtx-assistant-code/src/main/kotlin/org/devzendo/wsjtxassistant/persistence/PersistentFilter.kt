package org.devzendo.wsjtxassistant.persistence

import org.devzendo.wsjtxassistant.data.CallsignState
import org.devzendo.wsjtxassistant.logparse.Callsign
import org.devzendo.wsjtxassistant.logparse.LogEntry
import java.io.Closeable
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
interface PersistentFilter: Closeable {
    fun incoming(logEntry: LogEntry) // from the tailer, everything
    fun record(logEntry: LogEntry, state: CallsignState) // store user choice
    fun publish(publisher: (logEntry: LogEntry) -> Unit) // incoming that's not filtered out

    fun recordExists(callsign: Callsign): Boolean // used internally, mostly
    fun getLogEntryForCallsign(callsign: Callsign): Optional<LogEntry> // used internally, mostly
    fun getRecordedStateForCallsign(callsign: Callsign): Optional<CallsignState> // diagnostic, for tests
}
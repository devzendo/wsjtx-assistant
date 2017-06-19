package org.devzendo.wsjtxassistant.logparse

import org.devzendo.wsjtxassistant.logging.ConsoleLoggingUnittestCase
import org.devzendo.wsjtxassistant.logparse.LogFileParser.LogFileParser.defaultLogFile
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Files.exists
import java.nio.file.Files.isRegularFile
import java.nio.file.Paths

private val TEST_FILE = Paths.get("src/test/resources/test-all.txt")

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

class TestLoadFiles: ConsoleLoggingUnittestCase() {
    @get:Rule
    var thrown = ExpectedException.none()

    @Test
    fun defaultPathContainsAFile() {
        val defaultLogFile = defaultLogFile()
        assertThat(defaultLogFile, notNullValue())
        assertThat(exists(defaultLogFile), equalTo(true))
        assertThat(isRegularFile(defaultLogFile), equalTo(true))
    }

    @Test
    fun defaultConstructorScansDefaultPathIfNotGivenAPath() {
        val parser = LogFileParser()
        parser.use {
            val file = parser.file()
            assertThat(file, equalTo(defaultLogFile()))
        }
    }

    @Test
    fun defaultConstructorThrowsIfNonExistentFileGiven() {
        thrown.expect(FileNotFoundException::class.java)
        LogFileParser(Paths.get("/does/not/exist.txt"))
    }

    @Test
    fun specificExistingFileCanBeParsed() {
        val parser = LogFileParser(TEST_FILE)
        parser.use {
            val file = parser.file()
            assertThat(file, not(equalTo(defaultLogFile())))
            val callsigns = mutableListOf<Callsign>()
            val grids = mutableListOf<Grid>()
            parser.parseForBand(Band.BAND_20M, { logEntry ->
                run {
                    callsigns.add(logEntry.callsign)
                    grids.add(logEntry.grid)
                }
            })
            assertThat(callsigns, hasSize(4))
            assertThat(grids, hasSize(4))
            assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG", "AE4DR", "RN6MG"))
            assertThat(grids, Matchers.contains("KN12", "EM82", "EM85", "LN08"))
        }
    }

}
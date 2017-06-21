package org.devzendo.wsjtxassistant.logparse

import org.devzendo.commoncode.concurrency.ThreadUtils
import org.devzendo.wsjtxassistant.logging.ConsoleLoggingUnittestCase
import org.devzendo.wsjtxassistant.logparse.LogFileParser.LogFileParser.defaultLogFile
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
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
    val logger = LoggerFactory.getLogger(TestLoadFiles::class.java)

    @get:Rule
    var thrown = ExpectedException.none()

    @get:Rule
    val tempDir = TemporaryFolder()

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
            val dxCallsigns = mutableListOf<Callsign>()
            val callsigns = mutableListOf<Callsign>()
            val grids = mutableListOf<Grid>()
            parser.parseForBand(Band.BAND_20M, { logEntry ->
                run {
                    callsigns.add(logEntry.callsign)
                    dxCallsigns.add(logEntry.dxCallsign)
                    grids.add(logEntry.grid)
                }
            })
            assertThat(callsigns, hasSize(4))
            assertThat(grids, hasSize(4))
            assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG", "AE4DR", "RN6MG"))
            assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO", "SP7EOY", "KW4PL"))
            assertThat(grids, Matchers.contains("KN12", "EM82", "EM85", "LN08"))
        }
    }

    @Test
    fun specificExistingFileCanBeTailed() {
        val logFile = File(tempDir.root, "tailfile.txt")
        logFile.deleteOnExit()
        logger.info("log file is " + logFile.absolutePath)
        assertThat(logFile.createNewFile(), equalTo(true))
        assertThat(logFile.toPath(), not(equalTo(defaultLogFile())))

        val parser = LogFileParser(logFile.toPath())
        logger.info("got parser")
        parser.use {
            val dxCallsigns = mutableListOf<Callsign>()
            val callsigns = mutableListOf<Callsign>()
            val grids = mutableListOf<Grid>()

            fun assertIsEmpty() {
                ThreadUtils.waitNoInterruption(250)
                assertThat(callsigns, hasSize(0))
                assertThat(dxCallsigns, hasSize(0))
                assertThat(grids, hasSize(0))
            }

            logger.info("creating tailer")
            val tailer = parser.tailForBand(Band.BAND_20M, { logEntry ->
                run {
                    logger.info("called back with callsign " + logEntry.callsign + " dx callsign " + logEntry.dxCallsign)
                    callsigns.add(logEntry.callsign)
                    dxCallsigns.add(logEntry.dxCallsign)
                    grids.add(logEntry.grid)
                }
            })
            logger.info("created tailer")
            tailer.use {
                // returns immediately, calls back when new data comes in
                assertIsEmpty()

                // append to log file
                val fw = FileWriter(logFile.absolutePath, true)
                val bw = BufferedWriter(fw)
                logger.info("writing to log file")
                bw.use {
                    fun flushLine(line: String) {
                        logger.info("appending line " + line)
                        bw.appendln(line)
                        bw.flush()
                        ThreadUtils.waitNoInterruption(250)
                    }

                    // append a transmission for 20m. There has been no date change yet, so it won't be called back to us.
                    flushLine("2032  -3  0.0  839 @ CQ LZ1UBO KN12")
                    assertIsEmpty()

                    // append a date change. no callback, but next transmission will be.
                    flushLine("2016-May-13 20:32  14.078 MHz  JT9")
                    assertIsEmpty()

                    // transmit, receive
                    flushLine("2032  -3  0.0  839 @ CQ LZ1UBO KN12")
                    assertThat(callsigns, Matchers.contains("LZ1UBO"))
                    assertThat(dxCallsigns, Matchers.contains("CQ"))
                    assertThat(grids, Matchers.contains("KN12"))

                    // other line added but doesn't match our pattern, no change
                    flushLine("2032 -15 -0.3 1007 @ KW4PL 9H1KR 73")
                    assertThat(callsigns, Matchers.contains("LZ1UBO"))
                    assertThat(dxCallsigns, Matchers.contains("CQ"))
                    assertThat(grids, Matchers.contains("KN12"))

                    // next transmission
                    flushLine("2033 -15 -0.4  838 @ LZ1UBO WA4RG EM82")
                    assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG"))
                    assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO"))
                    assertThat(grids, Matchers.contains("KN12", "EM82"))
                }
            }
            logger.info("end of test")
        }
    }
}
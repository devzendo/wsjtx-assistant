package org.devzendo.wsjtxassistant.logparse

import com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional
import com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue
import org.devzendo.commoncode.concurrency.ThreadUtils
import org.devzendo.wsjtxassistant.logging.ConsoleLoggingUnittestCase
import org.devzendo.wsjtxassistant.logparse.LogFileParser.LogFileParser.defaultLogFile
import org.devzendo.wsjtxassistant.logparse.LogFileParser.LogFileParser.parseDateBandChange
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.assertj.core.api.Assertions.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.nio.file.Files.exists
import java.nio.file.Files.isRegularFile
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

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

class TestLogFileParser : ConsoleLoggingUnittestCase() {
    val logger = LoggerFactory.getLogger(TestLogFileParser::class.java)

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
        val file = parser.file()
        assertThat(file, equalTo(defaultLogFile()))
    }

    @Test
    fun defaultConstructorThrowsIfNonExistentFileGiven() {
        thrown.expect(FileNotFoundException::class.java)
        LogFileParser(Paths.get("/does/not/exist.txt"))
    }

    val dxCallsigns = mutableListOf<Callsign>()
    val callsigns = mutableListOf<Callsign>()
    val grids = mutableListOf<Grid>()

    fun recordEntry(logEntry: LogEntry): Unit {
        synchronized(logger) {
            logger.info("called back with callsign " + logEntry.callsign + " dx callsign " + logEntry.dxCallsign + " grid " + logEntry.grid)
            callsigns.add(logEntry.callsign)
            dxCallsigns.add(logEntry.dxCallsign)
            grids.add(logEntry.grid)
        }
    }

    private val recordEntryProperty: (logEntry: LogEntry) -> Unit = { logEntry: LogEntry ->
        synchronized(logger) {
            logger.info("called back with callsign " + logEntry.callsign + " dx callsign " + logEntry.dxCallsign + " grid " + logEntry.grid)
            callsigns.add(logEntry.callsign)
            dxCallsigns.add(logEntry.dxCallsign)
            grids.add(logEntry.grid)
        }
    }

    private fun assertIsEmpty() {
        ThreadUtils.waitNoInterruption(250)
        synchronized(logger) {
            assertThat(callsigns, hasSize(0))
            assertThat(dxCallsigns, hasSize(0))
            assertThat(grids, hasSize(0))
        }
    }

    @Test
    fun specificExistingFileCanBeParsed() {
        val parser = LogFileParser(TEST_FILE)
        val file = parser.file()
        assertThat(file, not(equalTo(defaultLogFile())))

        // can't just say ::recordEntry here. Have to wrap it in a block.
        // or use the appalling recordEntryProperty, which has to have its type signature spelled out. ugh.
        parser.parseForBand(Band.BAND_20M, {recordEntry(it)})
        synchronized(logger) {
            assertThat(callsigns, hasSize(4))
            assertThat(grids, hasSize(4))
            assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG", "AE4DR", "RN6MG"))
            assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO", "SP7EOY", "KW4PL"))
            assertThat(grids, Matchers.contains("KN12", "EM82", "EM85", "LN08"))
        }
    }

    @Test
    fun specificExistingFileCanBeTailedForAGivenBand() {
        val logFile = createTailFile()

        val parser = LogFileParser(logFile.toPath())
        logger.info("got parser")

        logger.info("creating tailer")
        val tailer = parser.tailForBand(Band.BAND_20M, recordEntryProperty)
        logger.info("created tailer")
        tailer.use {
            // returns immediately, calls back when new data comes in
            assertIsEmpty()

            // append to log file
            val fw = FileWriter(logFile.absolutePath, true)
            val bw = BufferedWriter(fw)

            fun flushLine(line: String) {
                synchronized(logger) {
                    logger.info("appending line " + line)
                    bw.appendln(line)
                    bw.flush()
                }
                ThreadUtils.waitNoInterruption(500)
            }

            logger.info("writing to log file")
            bw.use {
                // append a transmission for 20m. There has been no date change yet, so it won't be called back to us.
                flushLine("2032  -3  0.0  839 @ CQ LZ1UBO KN12")
                assertIsEmpty()

                // append a date change. no callback, but next transmission will be.
                flushLine("2016-May-13 20:32  14.078 MHz  JT9")
                assertIsEmpty()

                // transmit, receive callback
                flushLine("2032  -3  0.0  839 @ CQ LZ1UBO KN12")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO"))
                    assertThat(dxCallsigns, Matchers.contains("CQ"))
                    assertThat(grids, Matchers.contains("KN12"))
                }

                // other line added but doesn't match our pattern, no change
                flushLine("2032 -15 -0.3 1007 @ KW4PL 9H1KR 73")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO"))
                    assertThat(dxCallsigns, Matchers.contains("CQ"))
                    assertThat(grids, Matchers.contains("KN12"))
                }

                // next transmission
                flushLine("2033 -15 -0.4  838 @ LZ1UBO WA4RG EM82")
                synchronized(logger){
                    assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG"))
                    assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO"))
                    assertThat(grids, Matchers.contains("KN12", "EM82"))
                }

                // append a date and frequency change. no callback, and next transmissino does not match filtered band,
                // so no callback
                flushLine("2016-May-13 20:38  10.14 MHz  JT65")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG"))
                    assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO"))
                    assertThat(grids, Matchers.contains("KN12", "EM82"))
                }

                // next transmission
                flushLine("2040 -15 -0.4  838 @ G3XMR K1DDA LM82")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG"))
                    assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO"))
                    assertThat(grids, Matchers.contains("KN12", "EM82"))
                }
            }
        }
        logger.info("end of test")
        // wait to see if the thread stops
        ThreadUtils.waitNoInterruption(500)
    }

    @Test
    fun specificExistingFileCanBeTailedForAnyBand() {
        val logFile = createTailFile()

        val parser = LogFileParser(logFile.toPath())
        logger.info("got parser")

        logger.info("creating tailer")
        val tailer = parser.tail(recordEntryProperty)
        logger.info("created tailer")
        tailer.use {
            // returns immediately, calls back when new data comes in
            assertIsEmpty()

            // append to log file
            val fw = FileWriter(logFile.absolutePath, true)
            val bw = BufferedWriter(fw)

            fun flushLine(line: String) {
                logger.info("appending line " + line)
                bw.appendln(line)
                bw.flush()
                ThreadUtils.waitNoInterruption(250)
            }

            logger.info("writing to log file")
            bw.use {

                // append a transmission for 20m. There has been no date change yet, so it won't be called back to us.
                flushLine("2032  -3  0.0  839 @ CQ LZ1UBO KN12")
                assertIsEmpty()

                // append a date change. no callback, but next transmission will be.
                flushLine("2016-May-13 20:32  14.078 MHz  JT9")
                assertIsEmpty()

                // transmit, receive callback
                flushLine("2032  -3  0.0  839 @ CQ LZ1UBO KN12")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO"))
                    assertThat(dxCallsigns, Matchers.contains("CQ"))
                    assertThat(grids, Matchers.contains("KN12"))
                }

                // other line added but doesn't match our pattern, no change
                flushLine("2032 -15 -0.3 1007 @ KW4PL 9H1KR 73")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO"))
                    assertThat(dxCallsigns, Matchers.contains("CQ"))
                    assertThat(grids, Matchers.contains("KN12"))
                }

                // next transmission
                flushLine("2033 -15 -0.4  838 @ LZ1UBO WA4RG EM82")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG"))
                    assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO"))
                    assertThat(grids, Matchers.contains("KN12", "EM82"))
                }

                // append a date and frequency change. no callback, next transmissino will be.
                flushLine("2016-May-13 20:38  10.14 MHz  JT65")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG"))
                    assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO"))
                    assertThat(grids, Matchers.contains("KN12", "EM82"))
                }

                // next transmission on new band
                flushLine("2040 -15 -0.4  838 @ G3XMR K1DDA LM82")
                synchronized(logger) {
                    assertThat(callsigns, Matchers.contains("LZ1UBO", "WA4RG", "K1DDA"))
                    assertThat(dxCallsigns, Matchers.contains("CQ", "LZ1UBO", "G3XMR"))
                    assertThat(grids, Matchers.contains("KN12", "EM82", "LM82"))
                }
            }
        }
        logger.info("end of test")
        // wait to see if the thread stops
        ThreadUtils.waitNoInterruption(500)
    }

    private fun createTailFile(): File {
        val logFile = File(tempDir.root, "tailfile.txt")
        logFile.deleteOnExit()
        logger.info("log file is " + logFile.absolutePath)
        assertThat(logFile.createNewFile(), equalTo(true))
        assertThat(logFile.toPath(), not(equalTo(defaultLogFile())))
        return logFile
    }

    @Test
    fun invalidParseDateBandChange() {
        assertThat(parseDateBandChange("2045  Transmitting 3.576 MHz  JT65:  CQ M0CUV JO01"), emptyOptional())
    }

    internal class DateBandChangeMatcher(private val dateAndBand: LogFileParser.DateBandChange) : TypeSafeDiagnosingMatcher<LogFileParser.DateBandChange>() {

        override fun matchesSafely(item: LogFileParser.DateBandChange,
                                   mismatchDescription: Description): Boolean {
            if (item == dateAndBand) {
                return true
            } else {
                val mismatchTexts = mutableListOf<String>()
                if (item.band != dateAndBand.band) {
                    mismatchTexts.add("band is not " + dateAndBand.band)
                }
                if (item.utcDate != dateAndBand.utcDate) {
                    mismatchDescription.appendText("UTC date is not " + dateAndBand.utcDate)
                }
                mismatchDescription.appendText(mismatchTexts.joinToString())
                return false
            }
        }

        override fun describeTo(description: Description) {
            description.appendText("a DateBandChange with a value of " + dateAndBand)
        }
    }

    fun dateAndBandEqualTo(dateAndBand: LogFileParser.DateBandChange): Matcher<LogFileParser.DateBandChange> {
        return DateBandChangeMatcher(dateAndBand)
    }

    @Test
    fun dateChangeFormatter() {
        val april18th = LocalDate.parse("2015-Apr-18", LogFileParser.dateChangeFormatter)
        assertThat(april18th).isEqualTo("2015-04-18")
    }

    @Test
    fun parseDateBandChangeTo80m() {
        val dateBandChange = parseDateBandChange("2015-Apr-18 20:40  3.576 MHz  JT9+JT65")
        val april18th = LocalDate.parse("2015-Apr-18", LogFileParser.dateChangeFormatter)

        val expected = LogFileParser.DateBandChange(april18th, Band.BAND_80M)
        assertThat(dateBandChange, optionalWithValue(dateAndBandEqualTo(expected)))
    }

    @Test
    fun validUTCDateTime() {
        UTCDateTime(ZonedDateTime.now(ZoneId.of("UTC")))
    }

    @Test
    fun invalidUTCDateTime() {
        thrown.expectMessage("does not have the UTC zone")
        UTCDateTime(ZonedDateTime.now(ZoneId.of("CET")))
    }
}
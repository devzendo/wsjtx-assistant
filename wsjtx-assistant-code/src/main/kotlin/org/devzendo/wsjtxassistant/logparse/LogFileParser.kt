package org.devzendo.wsjtxassistant.logparse

import org.devzendo.commoncode.os.OSTypeDetect
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.format.SignStyle


import java.time.temporal.ChronoField.DAY_OF_MONTH
import java.time.temporal.ChronoField.DAY_OF_WEEK
import java.time.temporal.ChronoField.DAY_OF_YEAR
import java.time.temporal.ChronoField.HOUR_OF_DAY
import java.time.temporal.ChronoField.MINUTE_OF_HOUR
import java.time.temporal.ChronoField.MONTH_OF_YEAR
import java.time.temporal.ChronoField.NANO_OF_SECOND
import java.time.temporal.ChronoField.SECOND_OF_MINUTE
import java.time.temporal.ChronoField.YEAR
import java.util.HashMap

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

typealias Power = Int
typealias Offset = Int
enum class Mode { JT65, JT9 }
typealias Callsign = String
typealias Grid = String
enum class Band { BAND_2200M, BAND_630M, BAND_160M, BAND_80M, BAND_60M, BAND_40M, BAND_30M, BAND_20M, BAND_17M,
    BAND_15M, BAND_12M, BAND_10M, BAND_6M, BAND_4M, BAND_2M }
data class LogEntry(val date: LocalDateTime, val power: Power, val offset: Offset, val mode: Mode,
                    val callsign: Callsign, val dxCallsign: Callsign, val grid: Grid)

// 2015-Apr-15 20:13  14.076 MHz  JT9
private val DATE_CHANGE_REGEX = "^(\\d{4}-\\S{3}-\\d{2}) \\d{2}:\\d{2}\\s+(\\d+\\.\\d+) MHz\\s+\\S+\\s*$".toRegex()
private var DATE_CHANGE_FORMATTER: DateTimeFormatter = initialiseDateChangeFormatter()
private fun initialiseDateChangeFormatter(): DateTimeFormatter {
    val moy = HashMap<Long, String>()
    moy.put(1L, "Jan")
    moy.put(2L, "Feb")
    moy.put(3L, "Mar")
    moy.put(4L, "Apr")
    moy.put(5L, "May")
    moy.put(6L, "Jun")
    moy.put(7L, "Jul")
    moy.put(8L, "Aug")
    moy.put(9L, "Sep")
    moy.put(10L, "Oct")
    moy.put(11L, "Nov")
    moy.put(12L, "Dec")
    return DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendText(MONTH_OF_YEAR, moy)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .toFormatter()
}

// 0001  -8  0.2  560 # KC0EFQ WA3ETR FN10
//                            0001        -8         0.2         560      @#      KC0EFQ    WA3ETR     FN10
private val REPORT_REGEX = "^(\\d{4})\\s+(-\\d+)\\s+[-\\d.]+\\s+(\\d+)\\s([#@])\\s(\\w+)\\s+(\\w+)\\s+([A-Z]{2}\\d{2})\\s*$".toRegex()
private var TIME_FORMATTER = initialiseTimeFormatter()
private fun initialiseTimeFormatter(): DateTimeFormatter {
    return DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2)
            .appendValue(MINUTE_OF_HOUR, 2)
            .toFormatter()

}
private val FREQ_TO_BAND = HashMap<String, Band>()

class LogFileParser(val logFile: Path = defaultLogFile()): Closeable {
    val logger = LoggerFactory.getLogger(LogFileParser::class.java)

    override fun close() {

    }

    init {
        if (!Files.exists(logFile) || !Files.isRegularFile(logFile)) {
            throw FileNotFoundException(logFile.toString() + " is not a regular file, or does not exist")
        }

        initialiseFrequencyToBandTable()
    }

    private fun initialiseFrequencyToBandTable() {
        FREQ_TO_BAND.put("144.491", Band.BAND_2M) // +2
        FREQ_TO_BAND.put("144.489", Band.BAND_2M)
        FREQ_TO_BAND.put("70.093", Band.BAND_4M) // +2
        FREQ_TO_BAND.put("70.091", Band.BAND_4M)
        FREQ_TO_BAND.put("50.278", Band.BAND_6M) // +2
        FREQ_TO_BAND.put("50.276", Band.BAND_6M)
        FREQ_TO_BAND.put("28.078", Band.BAND_10M) // +2
        FREQ_TO_BAND.put("28.076", Band.BAND_10M)
        FREQ_TO_BAND.put("24.919", Band.BAND_12M) // +2
        FREQ_TO_BAND.put("24.917", Band.BAND_12M)
        FREQ_TO_BAND.put("21.078", Band.BAND_15M) // +2
        FREQ_TO_BAND.put("21.076", Band.BAND_15M)
        FREQ_TO_BAND.put("18.104", Band.BAND_17M) // +2
        FREQ_TO_BAND.put("18.102", Band.BAND_17M)
        FREQ_TO_BAND.put("14.078", Band.BAND_20M) // +2
        FREQ_TO_BAND.put("14.076", Band.BAND_20M)
        FREQ_TO_BAND.put("10.14", Band.BAND_30M) // +2
        FREQ_TO_BAND.put("10.138", Band.BAND_30M)
        FREQ_TO_BAND.put("7.078", Band.BAND_40M) // +2
        FREQ_TO_BAND.put("7.076", Band.BAND_40M)
        FREQ_TO_BAND.put("5.359", Band.BAND_60M) //  +2
        FREQ_TO_BAND.put("5.357", Band.BAND_60M)
        FREQ_TO_BAND.put("3.578", Band.BAND_80M) // +2
        FREQ_TO_BAND.put("3.576", Band.BAND_80M)
        FREQ_TO_BAND.put("1.84", Band.BAND_160M) // +2
        FREQ_TO_BAND.put("1.838", Band.BAND_160M)
        FREQ_TO_BAND.put("0.4762", Band.BAND_630M) // +2
        FREQ_TO_BAND.put("0.4742", Band.BAND_630M)
        FREQ_TO_BAND.put("0.13813", Band.BAND_2200M) // +2
        FREQ_TO_BAND.put("0.13613", Band.BAND_2200M)
    }


    companion object LogFileParser {
        fun defaultLogFile(): Path {
            when (OSTypeDetect.getInstance().osType) {
                OSTypeDetect.OSType.MacOSX -> {
                    // povaX' ALLmon suggests >= v1.4 uses standard OSX location:
                    val homeAll = Paths.get(System.getenv("HOME") + "/Library/Application Support/WSJT-X/ALL.TXT")
                    val globalAll = Paths.get("/Applications/WSJT-X/ALL.TXT") // povaX' ALLmon suggests it was here in WSJT-X <= v1.3
                    if (Files.exists(homeAll) && Files.isRegularFile(homeAll)) return homeAll
                    if (Files.exists(globalAll) && Files.isRegularFile(globalAll)) return globalAll
                    throw IllegalStateException("Could not find default ALL.TXT")
                }
                else -> throw UnsupportedOperationException("Only OSX is supported for now")
            }
        }
    }


    fun file(): Path = logFile
    fun parseForBand(band: Band, callback: (logEntry: LogEntry) -> Unit) {
        var currentBand: Band? = null
        var currentDate: LocalDate? = null
        Files.lines(file()).forEach {
            logger.debug("Line [{}]", it)
            // Only interested in data from a specific band, and the indicator for changing band/mode looks like:
            // 2015-Apr-15 20:13  14.076 MHz  JT9
            // So extract the frequency, and look up the band. This also gives us the date. Records like this are always
            // written at startup, mode change, and at midnight.
            val result = DATE_CHANGE_REGEX.matchEntire(it)
            if (result != null) {
                val groups = result.groups
                val dateString = groups[1]!!.value
                val freqString = groups[2]!!.value
                currentDate = LocalDate.parse(dateString, DATE_CHANGE_FORMATTER)
                currentBand = FREQ_TO_BAND.get(freqString)
                logger.debug("Detected changed date {} and band {}", currentDate, currentBand)
            }
            // Time/Power/Freq offset/Mode/Call/Square can be extracted from records like these:
            // 0000  -9  1.5 1259 # CQ TI4DJ EK70
            // 0001  -1  0.5  404 # DX K1RI FN41
            // 0001  -8  0.2  560 # KC0EFQ WA3ETR FN10
            // 0001 -15  0.1  628 # KK7X K8MDA EN80
            // 0002 -13  1.1 1322 # CQ YV5FRD FK60
            // 0003  -3  0.5 1002 # TF2MSN K1RI FN41
            val reportResult = REPORT_REGEX.matchEntire(it)
            if (reportResult != null) {
                val groups = reportResult.groups
                val ctime = groups[1]!!.value
                val cpower = groups[2]!!.value
                val coffset = groups[3]!!.value
                val cmode = groups[4]!!.value
                val cdxcallsign = groups[5]!!.value // could be CQ
                val ccallsign = groups[6]!!.value
                val cgrid = groups[7]!!.value
                // callsigns must have at least one digit.
                if (ccallsign.contains("\\d".toRegex())) {
                    if (currentDate != null && band == currentBand) {
                        val entryLocalTime = LocalTime.parse(ctime, TIME_FORMATTER)!!
                        val dateTime = LocalDateTime.of(currentDate, entryLocalTime)
                        val logEntry = LogEntry(dateTime,
                                cpower.toInt(),
                                coffset.toInt(),
                                if (cmode == "@") Mode.JT9 else Mode.JT65,
                                ccallsign,
                                cdxcallsign,
                                cgrid)
                        logger.debug(logEntry.toString())
                        callback(logEntry)
                    }
                }
            }
        }
    }

/*

        local *F;
        unless (open F, "<$file") {
            die "Cannot open $file: $!\n";
        }
        my $currentBand = undef;
        my $currentDate = undef;
        while (<F>) {
            chomp;
            #print "line [$_]\n";
            # Only interested in data from a specific band, and the indicator for changing band/mode looks like:
            # 2015-Apr-15 20:13  14.076 MHz  JT9
            # So extract the frequency, and look up the band. This also gives us the date. Records like this are always
            # written at startup, mode change, and at midnight.
            if (/^(\d{4}-\S{3}-\d{2}) \d{2}:\d{2}\s+(\d+\.\d+) MHz\s+\S+\s*$/) {
                $currentDate = $1;
                my $frequency = $2;
                $currentBand = $freqToBand{$frequency};
                #print "data being received for $currentBand (filtering on $bandOfInterest)\n";
                next;
            }
            # Time/Power/Freq offset/Mode/Call/Square can be extracted from records like these:
            # 0000  -9  1.5 1259 # CQ TI4DJ EK70
            # 0001  -1  0.5  404 # DX K1RI FN41
            # 0001  -8  0.2  560 # KC0EFQ WA3ETR FN10
            # 0001 -15  0.1  628 # KK7X K8MDA EN80
            # 0002 -13  1.1 1322 # CQ YV5FRD FK60
            # 0003  -3  0.5 1002 # TF2MSN K1RI FN41
            if (/^(\d{4})\s+(-\d+)\s+[-\d.]+\s+(\d+)\s([#@])\s\w+\s+(\w+)\s+([A-Z]{2}\d{2})\s*$/) {
                my $ctime = $1;
                my $cpower = $2;
                my $coffset = $3;
                my $cmode = $4;
                my $ccallsign = $5;
                my $cgrid = $6;
                # callsigns must have at least one digit.
                next unless ($ccallsign =~ /\d/);
                if (defined $currentDate && $bandOfInterest eq $currentBand) {
                    $callback->($currentDate, $ctime, $cpower, $coffset, $cmode, $ccallsign, $cgrid);
                }
                next;
            }
        }
        close F;

 */
}
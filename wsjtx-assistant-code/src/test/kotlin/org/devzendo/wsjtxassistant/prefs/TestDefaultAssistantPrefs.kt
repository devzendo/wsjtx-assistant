package org.devzendo.wsjtxassistant.prefs

import org.devzendo.wsjtxassistant.logging.ConsoleLoggingUnittestCase
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory
import java.io.File

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
class TestDefaultAssistantPrefs: ConsoleLoggingUnittestCase() {
    val logger = LoggerFactory.getLogger(TestDefaultAssistantPrefs::class.java)

    @get:Rule
    val tempDir = TemporaryFolder()

    var iniFile: File? = null

    private fun createPrefs(): AssistantPrefs {
        iniFile = File(tempDir.root, "prefs.ini")
        iniFile!!.deleteOnExit()
        logger.info("ini file is " + iniFile!!.absolutePath)
        return DefaultAssistantPrefs(iniFile!!)
    }
}

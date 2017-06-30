package org.devzendo.wsjtxassistant.prefs

import org.devzendo.commoncode.file.INIFile
import org.devzendo.wsjtxassistant.logparse.Band
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

class DefaultAssistantPrefs(prefsFile: File): AssistantPrefs
{
    val logger = LoggerFactory.getLogger(DefaultAssistantPrefs::class.java)

    // The section names and preference items
    private val SECTION_UI = "ui"
    private val UI_GEOMETRY = "geometry"

    private val SECTION_ASSISTANT = "assistant"
    private val ASSISTANT_BAND = "band"

    val iniFile = INIFile(prefsFile.absolutePath)
    init {
        logger.debug("Preferences are stored at " + prefsFile)
    }

    override fun getWindowGeometry(windowName: String): String {
        return iniFile.getValue(SECTION_UI, formWindowGeometryKey(windowName), "")
    }

    override fun setWindowGeometry(windowName: String, geometry: String) {
        iniFile.setValue(SECTION_UI, formWindowGeometryKey(windowName), geometry)
    }

    private fun formWindowGeometryKey(windowName: String): String {
        return UI_GEOMETRY + "_" + windowName
    }
}
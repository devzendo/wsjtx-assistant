package org.devzendo.wsjtxassistant.prefs

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


import java.io.File

import org.devzendo.commoncode.string.StringUtils

class PrefsFactory(homeDir: File, prefsSubDir: String, prefsFile: String) {
    val prefsDir: File
    val prefsFile: File

    init {
        prefsDir = File(StringUtils.slashTerminate(homeDir.absolutePath) + prefsSubDir)
        this.prefsFile = File(StringUtils.slashTerminate(prefsDir.absolutePath) + prefsFile)
    }

    constructor(prefsSubDir: String, prefsFile: String) : this(File(System.getProperty("user.home")), prefsSubDir, prefsFile) {}

    fun prefsDirectoryExists(): Boolean {
        return prefsDir.exists()
    }

    fun createPrefsDirectory(): Boolean {
        return prefsDir.mkdir()
    }
}

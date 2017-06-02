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

import org.devzendo.commonapp.gui.GUIUtils
import org.devzendo.commoncode.string.StringUtils
import org.slf4j.LoggerFactory
import java.io.File
import javax.swing.JOptionPane

/**
 * Warns the user of prefs dir creation failure via a dialog.

 * @author matt
 */
/**
 * @param prefsFactory the factory in which to store it.
 * *
 * @param prefsInstantiator the instantiator of prefs
 */
class PrefsStartupHelper(val prefsFactory: PrefsFactory, val prefsInstantiator: (File) -> Prefs) {
    val logger = LoggerFactory.getLogger(PrefsStartupHelper::class.java)

    /**
     * Warn the user either via GUI or log output of a failure to create the
     * prefs directory.
     */
    fun warnUserOfPrefsDirCreationFailure() {
        GUIUtils.runOnEventThread { showPrefsDirCreationFailureMessage() }
    }

    private fun showPrefsDirCreationFailureMessage() {
        val errorMessage = StringUtils.join(createErrorMessage(), "")
        JOptionPane.showMessageDialog(null,
                // NOTE user-centric message
                // I18N
                errorMessage,
                "Could not create settings folder",
                JOptionPane.ERROR_MESSAGE)

        System.exit(0)
    }

    /**
     * Initialise the prefs. If this fails, the user wil be notified, and the
     * program will exit. You can't continue without prefs.
     */
    fun initialisePrefs(): Prefs {
        logger.debug("Prefs directory is " + prefsFactory.prefsDir.absolutePath)
        logger.debug("Prefs file is " + prefsFactory.prefsFile.absolutePath)
        if (!prefsFactory.prefsDirectoryExists()) {
            logger.info(String.format("Prefs directory %s does not exist - creating it",
                    prefsFactory.prefsDir.absolutePath))
            if (!prefsFactory.createPrefsDirectory()) {
                logger.warn("Failed to create prefs directory")
                warnUserOfPrefsDirCreationFailure()

            } else {
                logger.info("Created prefs directory OK")
            }
        }
        return prefsInstantiator(prefsFactory.prefsFile)
    }

    /**
     * @return an error message explaining the problem
     */
    fun createErrorMessage(): Array<String> {
        return arrayOf(String.format("The '%s' folder cannot be created - the application cannot continue.\n",
                prefsFactory.prefsDir.absolutePath), "This folder would be used to remember your options and settings.\n\n",
                "Failure to create this folder may be be due to security permissions, or a full disk.")
    }
}
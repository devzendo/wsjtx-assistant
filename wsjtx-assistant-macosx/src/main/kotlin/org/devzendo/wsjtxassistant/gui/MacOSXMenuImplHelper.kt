package org.devzendo.wsjtxassistant.gui

import org.devzendo.commonapp.gui.menu.MenuWiring
import org.slf4j.LoggerFactory


import com.apple.eawt.*

import com.apple.eawt.AppEvent.QuitEvent
import com.apple.eawt.AppEvent.PreferencesEvent
import com.apple.eawt.AppEvent.AboutEvent

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
class MacOSXMenuImplHelper(val wiring: MenuWiring) {
    val logger = LoggerFactory.getLogger(MacOSXMenuImplHelper::class.java)

    val macApp = Application.getApplication()
    init {
        logger.info("Initialising MacOSX-specific menu")

        macApp.setAboutHandler(object: AboutHandler {
            override fun handleAbout(ae: AboutEvent) {
                logger.info("Handling About menu event")
                wiring.triggerActionListener(AssistantMenuIdentifiers.HELP_ABOUT)
            }
        })

        macApp.setPreferencesHandler(object: PreferencesHandler {
            override fun handlePreferences(pe: PreferencesEvent) {
                logger.info("Handling Preferences menu event")
                wiring.triggerActionListener(AssistantMenuIdentifiers.TOOLS_OPTIONS)
            }
        })

        macApp.setQuitHandler(object: QuitHandler {
            override fun handleQuitRequestWith(pe: QuitEvent, pr: QuitResponse) {
                logger.info("Handling Quit menu event " + pe)
                wiring.triggerActionListener(AssistantMenuIdentifiers.FILE_EXIT)
            }
        })
    }
}
package org.devzendo.wsjtxassistant.gui

import org.devzendo.commonapp.gui.CursorManager
import org.devzendo.commonapp.gui.SwingWorker
import org.devzendo.commonapp.gui.menu.MenuWiring
import org.slf4j.LoggerFactory
import java.awt.AWTEvent
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import javax.swing.JFrame

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
class StartupAWTEventListener(val mainFrame: JFrame,
                              val cursorManager: CursorManager,
                              val menuWiring: MenuWiring,
                              val startRunnable: Runnable): AWTEventListener {
    val logger = LoggerFactory.getLogger(StartupAWTEventListener::class.java)

    override fun eventDispatched(event: AWTEvent) {
        logger.debug("Event received " + event.toString())
        if (event.source.equals(mainFrame)) {
            if (event.id == WindowEvent.WINDOW_OPENED) {
                startLifecycle()
            }
            if (event.id == WindowEvent.WINDOW_CLOSING) {
                menuWiring.triggerActionListener(AssistantMenuIdentifiers.FILE_EXIT)
            }
        }
    }

    private fun startLifecycle() {
        cursorManager.hourglass(this.javaClass.simpleName)
        val worker = object: SwingWorker() {
            override fun construct(): Object {
                Thread.currentThread().setName("App Startup")
                logger.info("App startup...")
                startRunnable.run();
                logger.info("...end of app startup")
                return Object()
            }

            override fun finished() {
                cursorManager.normal(this.javaClass.simpleName)
            }
        }
        worker.start()
    }

}
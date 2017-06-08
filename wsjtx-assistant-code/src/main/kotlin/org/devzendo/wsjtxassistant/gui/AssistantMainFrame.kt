package org.devzendo.wsjtxassistant.gui

import org.devzendo.commonapp.gui.WindowGeometryStore
import org.devzendo.commonapp.gui.menu.MenuWiring
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

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
class AssistantMainFrame(
        val windowGeometryStore: WindowGeometryStore,
        val menuWiring: MenuWiring,
        mainPanel: JPanel): JFrame() {
    val logger = LoggerFactory.getLogger(AssistantMainFrame::class.java)
    val MAIN_FRAME_NAME = "main"

    init {
        logger.info("Initialising main frame")
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)

        setName(MAIN_FRAME_NAME)
        setLayout(BorderLayout())

        add(mainPanel, BorderLayout.CENTER)

        loadInitialGeometry()
        triggerFileExitOnWindowClose()
        logger.info("Main frame initialised")
    }

    private fun loadInitialGeometry() {
        logger.info("Loading initial geometry")
        if (!windowGeometryStore.hasStoredGeometry(this)) {
            logger.info("No geometry stored; packing main frame")
            pack()
        }
        logger.info("Loading geometry")
        windowGeometryStore.loadGeometry(this)
    }

    private fun triggerFileExitOnWindowClose() {
        logger.info("Requesting menu redirection on window close")
        setResizable(false)
        addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                logger.debug("Detected window closing; triggering action listener for FileExit")
                menuWiring.triggerActionListener(AssistantMenuIdentifiers.FILE_EXIT)
            }
            override fun windowClosed(e: WindowEvent ) {
                logger.debug("Detected window closed")
            }})
    }
}
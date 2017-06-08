package org.devzendo.wsjtxassistant.gui

import org.devzendo.commonapp.gui.CursorManager
import org.devzendo.commonapp.gui.SwingWorker
import org.devzendo.commonapp.gui.WindowGeometryStore
import org.slf4j.LoggerFactory
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
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
class MainFrameCloseActionListener(val windowGeometryStore: WindowGeometryStore,
                                   val mainFrame: JFrame,
                                   val cursorManager: CursorManager): ActionListener {
    val logger = LoggerFactory.getLogger(MainFrameCloseActionListener::class.java)

    override fun actionPerformed(e: ActionEvent?) {
        cursorManager.hourglass(MainFrameCloseActionListener::class.java.simpleName)

        val worker = object: SwingWorker() {
            override fun construct() = Object() // not needed really

            override fun finished() {
                logger.info("Saving main frame geometry")
                windowGeometryStore.saveGeometry(mainFrame)
                cursorManager.normal(MainFrameCloseActionListener::class.java.simpleName)
                logger.info("Disposing main frame")
                mainFrame.dispose()
            }
        }
        worker.start()
    }
}
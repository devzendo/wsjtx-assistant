package org.devzendo.wsjtxassistant.main

import org.devzendo.commonapp.gui.*
import org.devzendo.commonapp.gui.GUIUtils.runOnEventThread
import org.devzendo.commonapp.gui.menu.MenuWiring
import org.devzendo.commoncode.logging.Logging
import org.devzendo.wsjtxassistant.concurrency.DaemonThreadFactory
import org.devzendo.wsjtxassistant.gui.*
import org.devzendo.wsjtxassistant.logparse.LogFileParser
import org.devzendo.wsjtxassistant.persistence.H2PersistentFilter
import org.devzendo.wsjtxassistant.persistence.PersistentFilter
import org.devzendo.wsjtxassistant.prefs.AssistantPrefs
import org.devzendo.wsjtxassistant.prefs.DefaultAssistantPrefs
import org.devzendo.wsjtxassistant.prefs.PrefsFactory
import org.devzendo.wsjtxassistant.prefs.PrefsStartupHelper
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities


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

// Just for the logger
class Main

fun main(args: Array<String>) {
    SLF4JBridgeHandler.install()
    val logger = LoggerFactory.getLogger(Main::class.java)!!
    val logging = Logging.getInstance()
    val finalArgList = logging.setupLoggingFromArgs(args.toMutableList())

    logger.info("Starting WSJTX Assistant")

    ThreadCheckingRepaintManager.initialise()

    val wsjtxTailer = LogFileParser()
    var tailer: Closeable? = null // initialised in the startup helper, closed in window closer
    var persistentFilter: PersistentFilter? = null // initialised in the startup helper, closed in window closer

    val prefsFactory = PrefsFactory(".wsjtxassistant", "wsjtxassistant.ini")

    val executor = ThreadPoolExecutor(5, 100, 2000L, TimeUnit.MILLISECONDS,
            ArrayBlockingQueue<Runnable>(100),
            DaemonThreadFactory("executor-thread-"))

    // Sun changed their recommendations and now recommends the UI be built
    // on the EDT, so I think flagging creation on non-EDT is OK.
    // "We used to say that you could create the GUI on the main thread as
    // long as you didn't modify components that had already been realized.
    // While this worked for most applications, in certain situations it
    // could cause problems."
    // http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html
    // So let's create it on the EDT anyway
    //
    runOnEventThread {
        try {
            // Process command line
            for (i in finalArgList.indices) {
                logger.debug("arg " + i + " = " + finalArgList[i] + "'")
            }

            Beautifier.makeBeautiful()

            val cursorManager = DefaultCursorManager()

            val prefsStartupHelper = PrefsStartupHelper(prefsFactory) { DefaultAssistantPrefs(it) }

            val prefs: AssistantPrefs = prefsStartupHelper.initialisePrefs()

            class DefaultWindowGeometryStorePersistence: WindowGeometryStorePersistence {
                override fun getWindowGeometry(windowName: String): String {
                    return prefs.getWindowGeometry(windowName)
                }

                override fun setWindowGeometry(windowName: String, geometry: String) {
                    prefs.setWindowGeometry(windowName, geometry)
                }
            }

            val windowPersistence = DefaultWindowGeometryStorePersistence()
            val windowGeometryStore = WindowGeometryStore(windowPersistence)
            val menuWiring = MenuWiring()
            val mainPanel = AssistantMainPanel()
            val mainFrame = AssistantMainFrame(windowGeometryStore, menuWiring, mainPanel)
            cursorManager.mainFrame = mainFrame
            // this is triggered when the window has actually closed (after this was invoked via menu and the shutdown
            // has run) - NOT when close is requested.
            mainFrame.addWindowListener(object: WindowAdapter() {
                override fun windowClosed(e: WindowEvent) {
                    logger.info("Detected window closed")
                    tailer?.close()
                    persistentFilter?.close()
                    System.exit(0)
                }})

            val fileMenu = FileMenu(menuWiring)
            val menu = MenuImpl(menuWiring, fileMenu)
            mainFrame.jMenuBar = menu.menuBar

            val closeAL = MainFrameCloseActionListener(windowGeometryStore, mainFrame, cursorManager)
            menuWiring.setActionListener(AssistantMenuIdentifiers.FILE_EXIT, closeAL)

            // also handles the OSX close window red dot, by triggering file/exit
            val startupListener = StartupAWTEventListener(mainFrame, cursorManager, menuWiring, Runnable {
                persistentFilter = H2PersistentFilter(prefsFactory.prefsDir)

                // UI -> filter (store user choice)
                mainPanel.record { logEntry, state ->
                    executor.submit {
                        persistentFilter?.record(logEntry, state)
                    }
                }

                // filter -> UI (new info that's not been filtered out by filter)
                persistentFilter?.publish { logEntry ->
                    SwingUtilities.invokeLater {
                        mainPanel.incomingNew(logEntry)
                    }
                }

                // tail -> filter (incoming tail of everything)
                tailer = wsjtxTailer.tail { logEntry ->
                    persistentFilter?.incoming(logEntry)
                }
            })
            Toolkit.getDefaultToolkit().addAWTEventListener(startupListener, AWTEvent.WINDOW_EVENT_MASK)

            mainFrame.isVisible = true


        } catch (e: Exception) {
            logger.error(e.message)
            System.exit(1)
        }
    }

    logger.debug("End of main thread")
}
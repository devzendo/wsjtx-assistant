package org.devzendo.wsjtxassistant.gui

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


import org.devzendo.commonapp.gui.menu.MenuWiring
import org.devzendo.commoncode.os.OSTypeDetect
import org.slf4j.LoggerFactory
import javax.swing.JMenuBar
import javax.swing.SwingUtilities

class MenuImpl(val wiring: MenuWiring, val fileMenu: FileMenu) {
    val logger = LoggerFactory.getLogger(MenuImpl::class.java)

    val osType = OSTypeDetect.getInstance().getOSType()
    val menuBar: JMenuBar

    init {
        logger.debug("Menu has detected OS type as " + osType)
        assert(SwingUtilities.isEventDispatchThread())
        menuBar = JMenuBar()
        val helperClassName = osType.name + "MenuImplHelper"
        val fullHelperClassName = this.javaClass.getPackage().getName() + "." + helperClassName
        try {
            logger.info("Trying to load platform-specific menu helper " + helperClassName)
            val helperClass = Class.forName(fullHelperClassName)
            helperClass.getConstructor(MenuWiring::class.java).newInstance(wiring) // just instantiate it...
        } catch (cnf: ClassNotFoundException) {
            logger.warn("No platform-specific menu helper " + helperClassName + " available")
        }
        menuBar.add(fileMenu.getJMenu())
    }
}
package org.devzendo.wsjtxassistant.gui

import org.devzendo.commonapp.gui.menu.AbstractRebuildableMenuGroup
import org.devzendo.commonapp.gui.menu.MenuWiring
import org.devzendo.commoncode.os.OSTypeDetect
import javax.swing.JMenu
import javax.swing.JSeparator

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
class FileMenu(val wiring: MenuWiring): AbstractRebuildableMenuGroup(wiring) {
    private val fileMenu: JMenu = JMenu("File")
    private val osType = OSTypeDetect.getInstance().getOSType()

    init {
        fileMenu.setMnemonic('F')
        // Trigger the first build; initially we'll have no recent files list.
        // Need to do an initial rebuild so the menu wiring is initially populated
        rebuildMenuGroup()
    }

    override fun getJMenu(): JMenu = fileMenu

    override fun rebuildMenuGroup() {
        fileMenu.removeAll()

        if (osType != OSTypeDetect.OSType.MacOSX) {
            fileMenu.add(JSeparator())

            createMenuItem(AssistantMenuIdentifiers.FILE_EXIT, "Exit", 'x', fileMenu)
        }
    }
}
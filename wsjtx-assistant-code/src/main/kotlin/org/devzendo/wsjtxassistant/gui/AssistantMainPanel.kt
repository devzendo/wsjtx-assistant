package org.devzendo.wsjtxassistant.gui

import org.devzendo.commonapp.gui.GUIUtils.runOnEventThread
import org.devzendo.wsjtxassistant.data.CallsignState
import org.devzendo.wsjtxassistant.logparse.LogEntry
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener


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

class AssistantMainPanel : JPanel(), ActionListener, ListSelectionListener {
    val logger = LoggerFactory.getLogger(AssistantMainPanel::class.java)!!

    var persister: ((logEntry: LogEntry, state: CallsignState) -> Unit)? = null

    val callsignModel = DefaultListModel<LogEntry>()
    val callsignList = JList(callsignModel)
    val doesntQSLRadio: JButton
    val workedAlreadyRadio: JButton
    val ignoreForNowRadio: JButton
    val qslViaBuroRadio: JButton
    val qslViaEQSLRadio: JButton

    init {
        doesntQSLRadio = JButton("Does not QSL")
        workedAlreadyRadio = JButton("Worked already")
        ignoreForNowRadio = JButton("Ignore for now")
        qslViaBuroRadio = JButton("QSL via Bureau")
        qslViaEQSLRadio = JButton("QSL via eQSL.cc")

        layout = BorderLayout()

        callsignList.visibleRowCount = 30
        callsignList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        callsignList.cellRenderer = CallsignListCellRenderer()
        val listScroller = JScrollPane(callsignList)
        listScroller.preferredSize = Dimension(250, 200)
        add(listScroller, BorderLayout.CENTER)

        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.Y_AXIS)
        buttonPanel.add(doesntQSLRadio)
        buttonPanel.add(workedAlreadyRadio)
        buttonPanel.add(ignoreForNowRadio)
        buttonPanel.add(qslViaBuroRadio)
        buttonPanel.add(qslViaEQSLRadio)

        val controlGroupPanel = JPanel()
        controlGroupPanel.layout = BorderLayout()
        controlGroupPanel.add(buttonPanel, BorderLayout.CENTER)

        add(controlGroupPanel, BorderLayout.SOUTH)

        doesntQSLRadio.addActionListener(this)
        doesntQSLRadio.actionCommand = CallsignState.DOESNTQSL.toString()
        workedAlreadyRadio.addActionListener(this)
        workedAlreadyRadio.actionCommand = CallsignState.WORKEDALREADY.toString()
        ignoreForNowRadio.addActionListener(this)
        ignoreForNowRadio.actionCommand = CallsignState.IGNOREFORNOW.toString()
        qslViaBuroRadio.addActionListener(this)
        qslViaBuroRadio.actionCommand = CallsignState.QSLVIABURO.toString()
        qslViaEQSLRadio.addActionListener(this)
        qslViaEQSLRadio.actionCommand = CallsignState.QSLVIAEQSL.toString()
    }

    internal inner class CallsignListCellRenderer : ListCellRenderer<LogEntry> {
        protected var defaultRenderer = DefaultListCellRenderer()
        override fun getListCellRendererComponent(list: JList<out LogEntry>?, value: LogEntry?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
            val renderer = defaultRenderer.getListCellRendererComponent(list, value?.callsign, index,
                    isSelected, cellHasFocus) as JLabel
            return renderer
        }

    }

    fun addCallsign(logEntry: LogEntry) {
        runOnEventThread {
            callsignModel.addElement(logEntry)
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (!e!!.valueIsAdjusting) {
            val enable = callsignList.selectedIndex != -1
            doesntQSLRadio.isEnabled = enable
            workedAlreadyRadio.isEnabled = enable
            ignoreForNowRadio.isEnabled = enable
            qslViaBuroRadio.isEnabled = enable
            qslViaEQSLRadio.isEnabled = enable
        }
    }


    override fun actionPerformed(e: ActionEvent?) {
//        val callsignState = CallsignState.valueOf(buttonGroup.selection.actionCommand)
        val callsignState = CallsignState.valueOf(e!!.actionCommand)

        // Kotlin: invoking possibly null function
        val selectedValue = callsignList.selectedValue
        persister?.invoke(selectedValue, callsignState)

        val remove = when (callsignState) {
            CallsignState.DOESNTQSL -> true
            CallsignState.WORKEDALREADY -> true
            CallsignState.IGNOREFORNOW -> true
            CallsignState.QSLVIABURO -> false
            CallsignState.QSLVIAEQSL -> false
        }
        if (remove) {
            callsignModel.removeElement(selectedValue)
        }
    }

    fun record(persister: (logEntry: LogEntry, state: CallsignState) -> Unit) {
        this.persister = persister
    }

    // On event thread
    fun incomingNew(logEntry: LogEntry) {
        addCallsign(logEntry);
    }
}
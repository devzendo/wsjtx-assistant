package org.devzendo.wsjtxassistant.gui

import org.devzendo.commonapp.gui.GUIUtils.runOnEventThread
import org.devzendo.wsjtxassistant.data.CallsignState
import org.devzendo.wsjtxassistant.logparse.Band
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
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

class AssistantMainPanel(): JPanel(), ActionListener, ListSelectionListener {
    val logger = LoggerFactory.getLogger(AssistantMainPanel::class.java)

    val callsignModel = DefaultListModel<String>()
    val callsignList = JList(callsignModel)
    val storeButton = JButton("Store")
    val buttonGroup = ButtonGroup()

    init {
        val doesntQSLRadio = JRadioButton("Does not QSL")
        val workedAlreadyRadio = JRadioButton("Worked already")
        val ignoreForNowRadio = JRadioButton("Ignore for now")
        val qslViaBuroRadio = JRadioButton("QSL via Bureau")
        val qslViaEQSLRadio = JRadioButton("QSL via eQSL.cc")

        setLayout(BorderLayout())

        callsignList.visibleRowCount = 10
        callsignList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        val listScroller = JScrollPane(callsignList)
        listScroller.preferredSize = Dimension(250, 80)
        add(listScroller, BorderLayout.CENTER)

        buttonGroup.add(doesntQSLRadio)
        buttonGroup.add(workedAlreadyRadio)
        buttonGroup.add(ignoreForNowRadio)
        buttonGroup.add(qslViaBuroRadio)
        buttonGroup.add(qslViaEQSLRadio)

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
        controlGroupPanel.add(storeButton, BorderLayout.SOUTH)
        storeButton.setEnabled(false) // until something in the list is selected

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

    fun addCallsign(callsign: String) {
        runOnEventThread {
            callsignModel.addElement(callsign)
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (!e!!.getValueIsAdjusting()) {

            if (callsignList.getSelectedIndex() == -1) {
                //No selection, disable store button.
                storeButton.setEnabled(false)

            } else {
                //Selection, enable the store button.
                storeButton.setEnabled(true)
            }
        }
    }


    override fun actionPerformed(e: ActionEvent?) {
        val callsignState = CallsignState.valueOf(buttonGroup.selection.actionCommand)
//        when (callsignState) {
//            CallsignState.DOESNTQSL ->
//        }
    }
}
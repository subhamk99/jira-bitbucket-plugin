package com.subhamk.jiraplugin.ui

import javax.swing.JPanel
import java.awt.FlowLayout
import javax.swing.JComponent

class PanelToolbar(vararg components: JComponent) : JPanel() {

    init {

        layout = FlowLayout(FlowLayout.LEFT)

        components.forEach { add(it) }
    }
}
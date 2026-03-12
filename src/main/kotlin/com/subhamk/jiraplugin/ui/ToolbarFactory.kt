package com.subhamk.jiraplugin.ui

import com.intellij.openapi.actionSystem.*
import javax.swing.JComponent

object ToolbarFactory {

    fun createToolbar(vararg actions: AnAction): JComponent {

        val group = DefaultActionGroup()

        actions.forEach { group.add(it) }

        val toolbar =
            ActionManager.getInstance()
                .createActionToolbar(
                    "DevDashboardToolbar",
                    group,
                    true
                )

        toolbar.targetComponent = null

        return toolbar.component
    }
}
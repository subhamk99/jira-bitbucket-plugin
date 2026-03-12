package com.subhamk.jiraplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.icons.AllIcons

class RefreshAction(
    private val action: () -> Unit
) : AnAction(
    "Refresh",
    "Refresh data",
    AllIcons.Actions.Refresh
) {

    override fun actionPerformed(e: AnActionEvent) {
        action()
    }
}
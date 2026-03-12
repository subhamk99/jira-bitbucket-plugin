package com.subhamk.jiraplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.ui.content.ContentFactory
import com.subhamk.jiraplugin.settings.JiraSettingsState
import com.subhamk.jiraplugin.service.PullRequestService
import service.JiraService
import java.awt.BorderLayout
import java.awt.Desktop
import java.net.URI
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import com.subhamk.jiraplugin.data.PullRequestData
import java.awt.Color
import java.awt.Component

class DevDashboardToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        val tabs = JTabbedPane()

        tabs.addTab("Jira Tickets", createJiraPanel())
        tabs.addTab("My Pull Requests", createPRPanel(false))
        tabs.addTab("PRs To Review", createPRPanel(true))

        val content = ContentFactory.getInstance()
            .createContent(tabs, "", false)

        toolWindow.contentManager.addContent(content)
    }

    // ---------------- JIRA PANEL ----------------

    private fun createJiraPanel(): JPanel {

        val panel = JPanel(BorderLayout())

        val refreshButton = JButton("Load My Tickets")

        val columns = arrayOf("Key","Summary","Status","Feature","Story Points","Sprint")

        val model = object : DefaultTableModel(columns,0){
            override fun isCellEditable(r:Int,c:Int)=false
        }

        val table = JBTable(model)
        table.autoCreateRowSorter = true

        val scroll = JBScrollPane(table)

        refreshButton.addActionListener {

            try {

                val tickets = JiraService().getAssignedTickets()

                model.rowCount = 0

                for(t in tickets){

                    model.addRow(arrayOf(
                        t.issueKey,
                        t.summary,
                        t.status,
                        t.feature,
                        t.storyPoints,
                        t.sprintName
                    ))
                }

            } catch (ex: Exception) {

                JOptionPane.showMessageDialog(
                    panel,
                    "Failed to load Jira tickets:\n${ex.message}",
                    "Jira Plugin Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        table.addMouseListener(object:java.awt.event.MouseAdapter(){

            override fun mousePressed(e:java.awt.event.MouseEvent){

                if(e.clickCount==2 && table.selectedRow != -1){

                    val row = table.convertRowIndexToModel(table.selectedRow)

                    val key = model.getValueAt(row,0).toString()

                    val url = "${JiraSettingsState.getInstance().jiraUrl}/browse/$key"

                    Desktop.getDesktop().browse(URI(url))
                }
            }
        })

        panel.add(refreshButton,BorderLayout.NORTH)
        panel.add(scroll,BorderLayout.CENTER)

        return panel
    }

    // ---------------- PR PANELS ----------------

    private fun createPRPanel(isReviewPanel: Boolean): JPanel {

        val panel = JPanel(BorderLayout())

        val refreshButton =
            if(isReviewPanel) JButton("Load PRs To Review")
            else JButton("Load My PRs")

        val columns =
            if(isReviewPanel)
                arrayOf("ID","Title","Status","Repository","Source","Target","Author","Approvals","Merge")
            else
                arrayOf("ID","Title","Status","Repository","Source","Target","Approvals","Merge")

        val model = object : DefaultTableModel(columns,0){
            override fun isCellEditable(r:Int,c:Int)=false
        }

        val table = JBTable(model)
        table.autoCreateRowSorter = true
        table.setDefaultRenderer(Any::class.java, PRTableRenderer())

        val scroll = JBScrollPane(table)

        val prs = mutableListOf<PullRequestData>()

        refreshButton.addActionListener {

            try {

                val fetched =
                    if(isReviewPanel)
                        PullRequestService().getReviewPullRequests()
                    else
                        PullRequestService().getMyPullRequests()

                prs.clear()
                prs.addAll(fetched)

                model.rowCount = 0

                for(pr in prs){

                    if(isReviewPanel){

                        model.addRow(arrayOf(
                            pr.id,
                            pr.title,
                            pr.status,
                            pr.repo,
                            pr.sourceBranch,
                            pr.targetBranch,
                            pr.author,
                            pr.approvals,
                            pr.mergeStatus
                        ))

                    } else {

                        model.addRow(arrayOf(
                            pr.id,
                            pr.title,
                            pr.status,
                            pr.repo,
                            pr.sourceBranch,
                            pr.targetBranch,
                            pr.approvals,
                            pr.mergeStatus
                        ))
                    }
                }

            } catch (ex: Exception) {

                JOptionPane.showMessageDialog(
                    panel,
                    "Failed to load Pull Requests:\n${ex.message}",
                    "Bitbucket Plugin Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        table.addMouseListener(object:java.awt.event.MouseAdapter(){

            override fun mousePressed(e:java.awt.event.MouseEvent){

                if(e.clickCount==2 && table.selectedRow != -1){

                    val row = table.convertRowIndexToModel(table.selectedRow)

                    val prLink = prs[row].link

                    if(prLink.isNotBlank()){
                        Desktop.getDesktop().browse(URI(prLink))
                    }
                }
            }
        })

        panel.add(refreshButton,BorderLayout.NORTH)
        panel.add(scroll,BorderLayout.CENTER)

        return panel
    }

    // ---------------- CUSTOM TABLE RENDERER ----------------

    class PRTableRenderer : DefaultTableCellRenderer() {

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {

            val c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
            )

            val columnName =
                table.columnModel.getColumn(column).headerValue.toString()

            val isDark = com.intellij.util.ui.UIUtil.isUnderDarcula()

            if (!isSelected) {
                foreground =
                    if (isDark)
                        Color(220,220,220)
                    else
                        Color.BLACK
            }

            if (columnName == "Approvals") {

                val approvals = value?.toString()?.toIntOrNull() ?: 0

                text =
                    if (approvals == 0)
                        "⚠ 0"
                    else
                        "✔ $approvals"

                foreground =
                    if (approvals == 0)
                        if (isDark) Color(255,180,80) else Color(200,120,0)
                    else
                        if (isDark) Color(120,220,120) else Color(0,150,0)

                horizontalAlignment = CENTER
            }

            if (columnName == "Merge") {

                val merge = value?.toString()

                text = when (merge) {

                    "CLEAN" -> "MERGEABLE"

                    "CONFLICTED" -> "CONFLICT"

                    else -> "UNKNOWN"
                }

                foreground = when (merge) {

                    "CLEAN" ->
                        if (isDark) Color(120,220,120) else Color(0,150,0)

                    "CONFLICTED" ->
                        if (isDark) Color(255,120,120) else Color(200,0,0)

                    else ->
                        if (isDark) Color(255,180,80) else Color(200,120,0)
                }

                horizontalAlignment = CENTER
            }

            return c
        }
    }
}
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
import javax.swing.table.DefaultTableModel
import com.subhamk.jiraplugin.data.PullRequestData

class DevDashboardToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        val tabs = JTabbedPane()

        tabs.addTab("Jira Tickets", createJiraPanel())
        tabs.addTab("My Pull Requests", createPRPanel())
        tabs.addTab("PRs To Review", createReviewPRPanel())

        val content = ContentFactory.getInstance()
            .createContent(tabs, "", false)

        toolWindow.contentManager.addContent(content)
    }

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

    private fun createPRPanel(): JPanel {

        val panel = JPanel(BorderLayout())

        val refreshButton = JButton("Load My PRs")

        val columns = arrayOf(
            "ID",
            "Title",
            "Status",
            "Repository",
            "Source",
            "Target",
            "Approvals"
        )

        val model = object : DefaultTableModel(columns,0){
            override fun isCellEditable(r:Int,c:Int)=false
        }

        val table = JBTable(model)
        table.autoCreateRowSorter = true

        val scroll = JBScrollPane(table)

        // store PRs so mouse listener can access them
        val prs = mutableListOf<PullRequestData>()

        refreshButton.addActionListener {

            try {

                val fetched = PullRequestService().getMyPullRequests()

                prs.clear()
                prs.addAll(fetched)

                model.rowCount = 0

                for(pr in prs){

                    model.addRow(arrayOf(
                        pr.id,
                        pr.title,
                        pr.status,
                        pr.repo,
                        pr.sourceBranch,
                        pr.targetBranch,
                        pr.approvals
                    ))
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
    private fun createReviewPRPanel(): JPanel {

        val panel = JPanel(BorderLayout())

        val refreshButton = JButton("Load PRs To Review")

        val columns = arrayOf(
            "ID",
            "Title",
            "Status",
            "Repository",
            "Source",
            "Target",
            "Author",
            "Approvals"
        )

        val model = object : DefaultTableModel(columns,0){
            override fun isCellEditable(r:Int,c:Int)=false
        }

        val table = JBTable(model)
        table.autoCreateRowSorter = true

        val scroll = JBScrollPane(table)

        val prs = mutableListOf<PullRequestData>()

        refreshButton.addActionListener {

            try {

                val fetched = PullRequestService().getReviewPullRequests()

                prs.clear()
                prs.addAll(fetched)

                model.rowCount = 0

                for(pr in prs){

                    model.addRow(arrayOf(
                        pr.id,
                        pr.title,
                        pr.status,
                        pr.repo,
                        pr.sourceBranch,
                        pr.targetBranch,
                        pr.author,
                        pr.approvals
                    ))
                }

            } catch (ex: Exception) {

                JOptionPane.showMessageDialog(
                    panel,
                    "Failed to load Review PRs:\n${ex.message}",
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
}
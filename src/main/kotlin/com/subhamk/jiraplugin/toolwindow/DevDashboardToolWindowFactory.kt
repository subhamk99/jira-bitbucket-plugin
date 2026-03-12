package com.subhamk.jiraplugin.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.UIUtil
import com.subhamk.jiraplugin.actions.RefreshAction
import com.subhamk.jiraplugin.data.PullRequestData
import com.subhamk.jiraplugin.service.PullRequestService
import com.subhamk.jiraplugin.settings.JiraSettingsState
import com.subhamk.jiraplugin.ui.ToolbarFactory
import com.subhamk.jiraplugin.util.BackgroundLoader
import service.JiraService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Desktop
import java.net.URI
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class DevDashboardToolWindowFactory : ToolWindowFactory {

    private var jiraLoaded = false
    private var myPrLoaded = false
    private var reviewPrLoaded = false

    private var jiraLoader: (() -> Unit)? = null
    private var myPrLoader: (() -> Unit)? = null
    private var reviewPrLoader: (() -> Unit)? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        val tabs = JTabbedPane()

        tabs.addTab("📋 Jira Tickets", createJiraPanel(project, tabs))
        tabs.addTab("🔀 My Pull Requests", createPRPanel(project, tabs,1,false))
        tabs.addTab("👀 PRs To Review", createPRPanel(project, tabs,2,true))

        tabs.addChangeListener {

            when (tabs.selectedIndex) {

                0 -> if (!jiraLoaded) {
                    jiraLoaded = true
                    jiraLoader?.invoke()
                }

                1 -> if (!myPrLoaded) {
                    myPrLoaded = true
                    myPrLoader?.invoke()
                }

                2 -> if (!reviewPrLoaded) {
                    reviewPrLoaded = true
                    reviewPrLoader?.invoke()
                }
            }
        }

        val content = ContentFactory.getInstance()
            .createContent(tabs,"",false)

        toolWindow.contentManager.addContent(content)

        addSettingsButton(toolWindow)
    }

    // ---------------- SETTINGS BUTTON ----------------

    private fun addSettingsButton(toolWindow: ToolWindow) {

        val settingsAction = object : AnAction(
            "Settings",
            "Open Plugin Settings",
            AllIcons.General.Settings
        ) {

            override fun actionPerformed(e: AnActionEvent) {

                ShowSettingsUtil.getInstance()
                    .showSettingsDialog(e.project,"Jira Plugin")
            }
        }

        toolWindow.setTitleActions(listOf(settingsAction))
    }

    // ---------------- JIRA PANEL ----------------

    private fun createJiraPanel(project: Project, tabs: JTabbedPane): JPanel {

        val panel = JPanel(BorderLayout())

        val columns =
            arrayOf("Key","Summary","Status","Feature","Story Points","Sprint")

        val model = object : DefaultTableModel(columns,0){
            override fun isCellEditable(r:Int,c:Int)=false
        }

        val table = JBTable(model)
        val scroll = JBScrollPane(table)

        val refreshLogic = {

            BackgroundLoader.run(
                project,
                "Loading Jira Tickets...",
                {
                    JiraService().getAssignedTickets()
                },
                { tickets ->

                    model.rowCount = 0

                    for (t in tickets) {

                        model.addRow(
                            arrayOf(
                                t.issueKey,
                                t.summary,
                                t.status,
                                t.feature,
                                t.storyPoints,
                                t.sprintName
                            )
                        )
                    }

                    updateTabTitle(tabs, 0, "📋 Jira Tickets", tickets.size)
                }
            )
        }

        jiraLoader = refreshLogic

        val toolbar = ToolbarFactory.createToolbar(
            RefreshAction { refreshLogic() }
        )

        table.addMouseListener(object:java.awt.event.MouseAdapter(){

            override fun mousePressed(e:java.awt.event.MouseEvent){

                if(e.clickCount==2 && table.selectedRow!=-1){

                    val row = table.convertRowIndexToModel(table.selectedRow)

                    val key = model.getValueAt(row,0).toString()

                    Desktop.getDesktop().browse(
                        URI("${JiraSettingsState.getInstance().jiraUrl}/browse/$key")
                    )
                }
            }
        })

        panel.add(toolbar,BorderLayout.NORTH)
        panel.add(scroll,BorderLayout.CENTER)

        return panel
    }

    // ---------------- PR PANELS ----------------

    private fun createPRPanel(
        project: Project,
        tabs:JTabbedPane,
        tabIndex:Int,
        review:Boolean
    ): JPanel {

        val panel = JPanel(BorderLayout())

        val columns =
            if(review)
                arrayOf("ID","Title","Status","Repo","Source","Target","Author","Approvals","Merge")
            else
                arrayOf("ID","Title","Status","Repo","Source","Target","Approvals","Merge")

        val model = object:DefaultTableModel(columns,0){
            override fun isCellEditable(r:Int,c:Int)=false
        }

        val table = JBTable(model)
        table.setDefaultRenderer(Any::class.java,PRTableRenderer())

        val scroll = JBScrollPane(table)

        val prs = mutableListOf<PullRequestData>()

        val refreshLogic = {

            BackgroundLoader.run(
                project,
                "Loading Pull Requests...",
                {
                    if (review)
                        PullRequestService().getReviewPullRequests()
                    else
                        PullRequestService().getMyPullRequests()
                },
                { result ->

                    prs.clear()
                    prs.addAll(result)

                    model.rowCount = 0

                    for (pr in prs) {

                        if (review) {

                            model.addRow(
                                arrayOf(
                                    pr.id,
                                    pr.title,
                                    pr.status,
                                    pr.repo,
                                    pr.sourceBranch,
                                    pr.targetBranch,
                                    pr.author,
                                    pr.approvals,
                                    pr.mergeStatus
                                )
                            )

                        } else {

                            model.addRow(
                                arrayOf(
                                    pr.id,
                                    pr.title,
                                    pr.status,
                                    pr.repo,
                                    pr.sourceBranch,
                                    pr.targetBranch,
                                    pr.approvals,
                                    pr.mergeStatus
                                )
                            )
                        }
                    }

                    updateTabTitle(
                        tabs,
                        tabIndex,
                        if (review) "👀 PRs To Review" else "🔀 My Pull Requests",
                        prs.size
                    )
                }
            )
        }

        if(review) reviewPrLoader = refreshLogic else myPrLoader = refreshLogic

        val toolbar = ToolbarFactory.createToolbar(
            RefreshAction { refreshLogic() }
        )

        table.addMouseListener(object:java.awt.event.MouseAdapter(){

            override fun mousePressed(e:java.awt.event.MouseEvent){

                if(e.clickCount==2 && table.selectedRow!=-1){

                    val row = table.convertRowIndexToModel(table.selectedRow)

                    Desktop.getDesktop().browse(
                        URI(prs[row].link)
                    )
                }
            }
        })

        panel.add(toolbar,BorderLayout.NORTH)
        panel.add(scroll,BorderLayout.CENTER)

        return panel
    }

    // ---------------- TABLE RENDERER ----------------

    class PRTableRenderer : DefaultTableCellRenderer(){

        override fun getTableCellRendererComponent(
            table:JTable,
            value:Any?,
            isSelected:Boolean,
            hasFocus:Boolean,
            row:Int,
            column:Int
        ): Component {

            val c = super.getTableCellRendererComponent(
                table,value,isSelected,hasFocus,row,column
            )

            val name = table.columnModel.getColumn(column).headerValue.toString()

            val dark = UIUtil.isUnderDarcula()

            if(name=="Merge"){

                val merge = value?.toString()

                foreground = when(merge){

                    "CLEAN" ->
                        if(dark) Color(120,220,120) else Color(0,150,0)

                    "CONFLICTED" ->
                        if(dark) Color(255,120,120) else Color(200,0,0)

                    else ->
                        if(dark) Color(255,180,80) else Color(200,120,0)
                }
            }

            return c
        }
    }

    private fun updateTabTitle(
        tabs:JTabbedPane,
        index:Int,
        base:String,
        count:Int
    ){

        tabs.setTitleAt(index,"$base ($count)")
    }
}
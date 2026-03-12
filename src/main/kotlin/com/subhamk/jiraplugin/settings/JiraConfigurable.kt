package com.subhamk.jiraplugin.settings

import com.intellij.openapi.options.Configurable
import javax.swing.*
import java.awt.GridLayout

class JiraConfigurable : Configurable {

    private val jiraUrlField = JTextField()
    private val projectKeyField = JTextField()
    private val userIdField = JTextField()
    private val patTokenField = JPasswordField()

    private val panel = JPanel(GridLayout(4, 2))

    init {

        panel.add(JLabel("Jira URL"))
        panel.add(jiraUrlField)

        panel.add(JLabel("Project Keys (comma separated)"))
        panel.add(projectKeyField)

        panel.add(JLabel("User ID"))
        panel.add(userIdField)

        panel.add(JLabel("PAT Token"))
        panel.add(patTokenField)
    }

    override fun getDisplayName(): String {
        return "Jira Plugin"
    }

    override fun createComponent(): JComponent {
        loadSettings()
        return panel
    }

    private fun loadSettings() {

        val settings = JiraSettingsState.getInstance()

        jiraUrlField.text = settings.jiraUrl
        projectKeyField.text = settings.projectKey
        userIdField.text = settings.userId
        patTokenField.text = settings.patToken
    }

    override fun isModified(): Boolean {

        val settings = JiraSettingsState.getInstance()

        return jiraUrlField.text != settings.jiraUrl ||
                projectKeyField.text != settings.projectKey ||
                userIdField.text != settings.userId ||
                String(patTokenField.password) != settings.patToken
    }

    override fun apply() {

        val settings = JiraSettingsState.getInstance()

        settings.jiraUrl = jiraUrlField.text
        settings.projectKey = projectKeyField.text
        settings.userId = userIdField.text
        settings.patToken = String(patTokenField.password)
    }
}
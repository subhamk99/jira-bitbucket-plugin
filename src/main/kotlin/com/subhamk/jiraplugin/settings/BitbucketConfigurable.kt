package com.subhamk.jiraplugin.settings

import com.intellij.openapi.options.Configurable
import javax.swing.*
import java.awt.GridLayout

class BitbucketConfigurable : Configurable {

    private val urlField = JTextField()
    private val projectKeyField = JTextField()
    private val repoKeyField = JTextField()
    private val userField = JTextField()
    private val tokenField = JPasswordField()

    private val panel = JPanel(GridLayout(5,2))

    init {

        panel.add(JLabel("Bitbucket URL"))
        panel.add(urlField)

        panel.add(JLabel("Project Key"))
        panel.add(projectKeyField)

        panel.add(JLabel("Repository Key (optional)"))
        panel.add(repoKeyField)

        panel.add(JLabel("User ID"))
        panel.add(userField)

        panel.add(JLabel("PAT Token"))
        panel.add(tokenField)
    }

    override fun getDisplayName(): String = "Bitbucket Plugin"

    override fun createComponent(): JComponent {
        loadSettings()
        return panel
    }

    private fun loadSettings() {

        val s = BitbucketSettingsState.getInstance()

        urlField.text = s.bitbucketUrl
        projectKeyField.text = s.projectKey
        repoKeyField.text = s.repoKey
        userField.text = s.userId
        tokenField.text = s.patToken
    }

    override fun isModified(): Boolean {

        val s = BitbucketSettingsState.getInstance()

        return urlField.text != s.bitbucketUrl ||
                projectKeyField.text != s.projectKey ||
                repoKeyField.text != s.repoKey ||
                userField.text != s.userId ||
                String(tokenField.password) != s.patToken
    }

    override fun apply() {

        val s = BitbucketSettingsState.getInstance()

        s.bitbucketUrl = urlField.text
        s.projectKey = projectKeyField.text
        s.repoKey = repoKeyField.text
        s.userId = userField.text
        s.patToken = String(tokenField.password)
    }
}
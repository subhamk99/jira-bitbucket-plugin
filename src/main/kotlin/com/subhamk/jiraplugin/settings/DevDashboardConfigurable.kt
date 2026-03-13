package com.subhamk.jiraplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.HideableDecorator
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.subhamk.jiraplugin.service.ConnectionTestService
import java.awt.*
import javax.swing.*

class DevDashboardConfigurable : Configurable {

    private val jiraSettings = JiraSettingsState.getInstance()
    private val bbSettings = BitbucketSettingsState.getInstance()

    private val jiraUrlField = JBTextField()
    private val jiraProjectField = JBTextField()
    private val jiraUserField = JBTextField()
    private val jiraTokenField = JBPasswordField()

    private val bbUrlField = JBTextField()
    private val bbProjectField = JBTextField()
    private val bbRepoField = JBTextField()
    private val bbUserField = JBTextField()
    private val bbTokenField = JBPasswordField()

    private val rootPanel = JPanel(GridBagLayout())

    override fun getDisplayName(): String = "Dev Dashboard"

    override fun createComponent(): JComponent {

        loadSettings()

        rootPanel.border = JBUI.Borders.empty(10)

        val gc = GridBagConstraints()
        gc.gridx = 0
        gc.weightx = 1.0
        gc.fill = GridBagConstraints.HORIZONTAL

        gc.gridy = 0
        rootPanel.add(createJiraSection(), gc)

        gc.gridy = 1
        gc.insets = JBUI.insetsTop(10)
        rootPanel.add(createBitbucketSection(), gc)

        gc.gridy = 2
        gc.weighty = 1.0
        rootPanel.add(JPanel(), gc)

        return rootPanel
    }

    // ---------------- JIRA SECTION ----------------

    private fun createJiraSection(): JComponent {

        val form = createFormPanel()

        form.add(createField("Jira URL", jiraUrlField))
        form.add(createField("Project Keys", jiraProjectField))
        form.add(createField("User ID", jiraUserField))
        form.add(createField("PAT Token", jiraTokenField))

        val testButton = JButton("Test Connection")

        testButton.putClientProperty("JButton.buttonType", "default")
        testButton.isFocusable = false

        testButton.addActionListener {

            val success = ConnectionTestService.testJira(
                jiraUrlField.text,
                String(jiraTokenField.password)
            )

            if (success)
                Messages.showInfoMessage(
                    "Jira connection successful!",
                    "Connection Test"
                )
            else
                Messages.showErrorDialog(
                    "Unable to connect to Jira.",
                    "Connection Test"
                )
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        buttonPanel.border = JBUI.Borders.emptyTop(6)
        buttonPanel.add(testButton)

        form.add(buttonPanel)

        return createCollapsible("Jira Configuration", form)
    }

    // ---------------- BITBUCKET SECTION ----------------

    private fun createBitbucketSection(): JComponent {

        val form = createFormPanel()

        form.add(createField("Bitbucket URL", bbUrlField))
        form.add(createField("Project Key", bbProjectField))
        form.add(createField("Repository Key (Optional)", bbRepoField))
        form.add(createField("User ID", bbUserField))
        form.add(createField("PAT Token", bbTokenField))

        val testButton = JButton("Test Connection")

        testButton.putClientProperty("JButton.buttonType", "default")
        testButton.isFocusable = false

        testButton.addActionListener {

            val success = ConnectionTestService.testBitbucket(
                bbUrlField.text,
                bbUserField.text,
                String(bbTokenField.password)
            )

            if (success)
                Messages.showInfoMessage(
                    "Bitbucket connection successful!",
                    "Connection Test"
                )
            else
                Messages.showErrorDialog(
                    "Unable to connect to Bitbucket.",
                    "Connection Test"
                )
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        buttonPanel.border = JBUI.Borders.emptyTop(6)
        buttonPanel.add(testButton)

        form.add(buttonPanel)

        return createCollapsible("Bitbucket Configuration", form)
    }

    // ---------------- FORM PANEL ----------------

    private fun createFormPanel(): JPanel {

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(6)

        return panel
    }

    // ---------------- FIELD BUILDER ----------------

    private fun createField(label: String, field: JComponent): JComponent {

        val panel = JPanel(BorderLayout())
        panel.maximumSize = Dimension(Int.MAX_VALUE, 55)
        panel.border = JBUI.Borders.emptyBottom(8)

        val labelComponent = JLabel(label)
        labelComponent.border = JBUI.Borders.emptyBottom(3)

        panel.add(labelComponent, BorderLayout.NORTH)
        panel.add(field, BorderLayout.CENTER)

        return panel
    }

    // ---------------- COLLAPSIBLE WRAPPER ----------------

    private fun createCollapsible(title: String, content: JComponent): JComponent {

        val wrapper = JPanel(BorderLayout())

        val decorator = HideableDecorator(wrapper, title, true)

        decorator.setContentComponent(content)
        decorator.setOn(true)

        return wrapper
    }

    // ---------------- SETTINGS ----------------

    private fun loadSettings() {

        jiraUrlField.text = jiraSettings.jiraUrl
        jiraProjectField.text = jiraSettings.projectKey
        jiraUserField.text = jiraSettings.userId
        jiraTokenField.text = jiraSettings.patToken

        bbUrlField.text = bbSettings.bitbucketUrl
        bbProjectField.text = bbSettings.projectKey
        bbRepoField.text = bbSettings.repoKey
        bbUserField.text = bbSettings.userId
        bbTokenField.text = bbSettings.patToken
    }

    override fun isModified(): Boolean {

        return jiraUrlField.text != jiraSettings.jiraUrl ||
                jiraProjectField.text != jiraSettings.projectKey ||
                jiraUserField.text != jiraSettings.userId ||
                String(jiraTokenField.password) != jiraSettings.patToken ||

                bbUrlField.text != bbSettings.bitbucketUrl ||
                bbProjectField.text != bbSettings.projectKey ||
                bbRepoField.text != bbSettings.repoKey ||
                bbUserField.text != bbSettings.userId ||
                String(bbTokenField.password) != bbSettings.patToken
    }

    override fun apply() {

        jiraSettings.jiraUrl = jiraUrlField.text
        jiraSettings.projectKey = jiraProjectField.text
        jiraSettings.userId = jiraUserField.text
        jiraSettings.patToken = String(jiraTokenField.password)

        bbSettings.bitbucketUrl = bbUrlField.text
        bbSettings.projectKey = bbProjectField.text
        bbSettings.repoKey = bbRepoField.text
        bbSettings.userId = bbUserField.text
        bbSettings.patToken = String(bbTokenField.password)
    }
}
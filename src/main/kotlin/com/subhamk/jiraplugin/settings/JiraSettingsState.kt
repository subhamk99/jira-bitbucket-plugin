package com.subhamk.jiraplugin.settings

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "JiraSettingsState",
    storages = [Storage("jira-settings.xml")]
)
class JiraSettingsState : PersistentStateComponent<JiraSettingsState> {

    var jiraUrl: String = ""
    var projectKey: String = ""
    var userId: String = ""
    var patToken: String = ""

    override fun getState(): JiraSettingsState = this

    override fun loadState(state: JiraSettingsState) {
        this.jiraUrl = state.jiraUrl
        this.projectKey = state.projectKey
        this.userId = state.userId
        this.patToken = state.patToken
    }

    companion object {
        fun getInstance(): JiraSettingsState =
            service()
    }
}
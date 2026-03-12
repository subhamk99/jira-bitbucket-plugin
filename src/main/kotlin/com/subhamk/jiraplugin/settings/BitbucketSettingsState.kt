package com.subhamk.jiraplugin.settings

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "BitbucketSettingsState",
    storages = [Storage("bitbucket-settings.xml")]
)
class BitbucketSettingsState : PersistentStateComponent<BitbucketSettingsState> {

    var bitbucketUrl: String = ""
    var projectKey: String = ""
    var repoKey: String = ""
    var userId: String = ""
    var patToken: String = ""

    override fun getState(): BitbucketSettingsState = this

    override fun loadState(state: BitbucketSettingsState) {
        this.bitbucketUrl = state.bitbucketUrl
        this.projectKey = state.projectKey
        this.repoKey = state.repoKey
        this.userId = state.userId
        this.patToken = state.patToken
    }

    companion object {
        fun getInstance(): BitbucketSettingsState =
            service()
    }
}
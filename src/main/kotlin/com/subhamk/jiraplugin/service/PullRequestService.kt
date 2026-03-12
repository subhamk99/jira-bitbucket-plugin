package com.subhamk.jiraplugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.subhamk.jiraplugin.data.PullRequestData
import com.subhamk.jiraplugin.settings.BitbucketSettingsState
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.*

class PullRequestService {

    fun getMyPullRequests(): List<PullRequestData> {

        val settings = BitbucketSettingsState.getInstance()

        disableSSLVerification()

        val projectKey = settings.projectKey
        val repoKey = settings.repoKey
        val user = settings.userId

        val urlString =
            if (repoKey.isNullOrBlank())
                "${settings.bitbucketUrl}/rest/api/1.0/dashboard/pull-requests?state=OPEN&role=AUTHOR"
            else
                "${settings.bitbucketUrl}/rest/api/1.0/projects/$projectKey/repos/$repoKey/pull-requests?state=OPEN"

        val connection = URL(urlString).openConnection() as HttpURLConnection

        connection.setRequestProperty("Authorization", "Bearer ${settings.patToken}")
        connection.setRequestProperty("Accept", "application/json")

        val response = connection.inputStream.bufferedReader().readText()

        return parsePullRequests(response)
    }

    private fun parsePullRequests(response: String): List<PullRequestData> {

        val mapper = ObjectMapper()

        val root = mapper.readTree(response)

        val values = root.get("values") ?: return emptyList()

        val results = mutableListOf<PullRequestData>()

        for (pr in values) {

            val title = pr.get("title")?.asText() ?: ""

            val id = pr.get("id")?.asText() ?: ""

            val state = pr.get("state")?.asText() ?: ""

            val sourceBranch =
                pr.get("fromRef")?.get("displayId")?.asText() ?: ""

            val targetBranch =
                pr.get("toRef")?.get("displayId")?.asText() ?: ""

            val repo =
                pr.get("toRef")?.get("repository")?.get("name")?.asText() ?: ""

            val link =
                pr.get("links")
                    ?.get("self")
                    ?.get(0)
                    ?.get("href")
                    ?.asText() ?: ""

            results.add(
                PullRequestData(
                    id,
                    title,
                    state,
                    sourceBranch,
                    targetBranch,
                    repo,
                    link
                )
            )
        }

        return results
    }

    private fun disableSSLVerification() {

        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }
}
package service

import com.fasterxml.jackson.databind.ObjectMapper
import com.subhamk.jiraplugin.settings.JiraSettingsState
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.*
import java.security.cert.X509Certificate

class JiraService {

    data class JiraIssueData(
        val issueKey: String,
        val feature: String,
        val summary: String,
        val status: String,
        val updated: String,
        val storyPoints: String,
        val sprintId: String,
        val sprintName: String,
        val sprintStartDate: String,
        val sprintEndDate: String
    )

    fun getAssignedTickets(): List<JiraIssueData> {

        val settings = JiraSettingsState.getInstance()

        disableSSLVerification()

        val jql = buildJql(settings.projectKey, settings.userId)

        val encodedJql = URLEncoder.encode(jql, "UTF-8")

        val url = URL(
            "${settings.jiraUrl}/rest/api/2/search?jql=$encodedJql&fields=summary,status,updated,customfield_10007,customfield_10008&maxResults=50"
        )

        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${settings.patToken}")
        connection.setRequestProperty("Accept", "application/json")

        val responseCode = connection.responseCode

        val stream =
            if (responseCode in 200..299)
                connection.inputStream
            else
                connection.errorStream

        val response = stream.bufferedReader().readText()

        return extractIssueData(response)
    }

    private fun extractIssueData(response: String): List<JiraIssueData> {

        val mapper = ObjectMapper()
        val root = mapper.readTree(response)

        val issuesNode = root.get("issues") ?: return emptyList()

        val results = mutableListOf<JiraIssueData>()

        for (issue in issuesNode) {

            try {

                val key = issue.get("key")?.asText() ?: "Unknown"

                val fields = issue.get("fields")

                val summary = fields?.get("summary")?.asText() ?: "No Summary"

                val updated = fields?.get("updated")?.asText() ?: "Unknown"

                val status = fields
                    ?.get("status")
                    ?.get("name")
                    ?.asText() ?: "Unknown"

                // Feature extraction (customfield_10008)
                val featureNode = fields?.get("customfield_10008")

                val feature = when {
                    featureNode == null || featureNode.isNull -> "No Feature"
                    featureNode.has("value") -> featureNode.get("value").asText()
                    featureNode.has("name") -> featureNode.get("name").asText()
                    featureNode.isTextual -> featureNode.asText()
                    else -> featureNode.toString()
                }

                // Story points detection
                val storyPointFields = listOf(
                    "customfield_10005",
                    "customfield_10007",
                    "customfield_10008",
                    "customfield_10016"
                )

                var storyPoints: String? = null

                for (field in storyPointFields) {

                    val value = fields?.get(field)

                    if (value != null && !value.isNull && value.isNumber) {
                        storyPoints = value.asText()
                        break
                    }
                }

                if (storyPoints == null) storyPoints = "None"

                // Sprint parsing
                var sprintId = "No Sprint"
                var sprintName = "No Sprint"
                var sprintStartDate = "No Sprint"
                var sprintEndDate = "No Sprint"

                val sprintField = fields?.get("customfield_10007")

                if (sprintField != null && sprintField.isArray && sprintField.size() > 0) {

                    val sprintData = sprintField.last().asText()

                    val idMatch = Regex("id=(\\d+)").find(sprintData)
                    val nameMatch = Regex("name=([^,\\]]+)").find(sprintData)
                    val startMatch = Regex("startDate=([^,\\]]+)").find(sprintData)
                    val endMatch = Regex("endDate=([^,\\]]+)").find(sprintData)

                    sprintId = idMatch?.groupValues?.get(1) ?: "No Sprint"
                    sprintName = nameMatch?.groupValues?.get(1) ?: "No Sprint"

                    sprintStartDate = startMatch
                        ?.groupValues
                        ?.get(1)
                        ?.takeIf { it.length >= 10 }
                        ?.substring(0, 10) ?: "No Sprint"

                    sprintEndDate = endMatch
                        ?.groupValues
                        ?.get(1)
                        ?.takeIf { it.length >= 10 }
                        ?.substring(0, 10) ?: "No Sprint"
                }

                val issueData = JiraIssueData(
                    issueKey = key,
                    feature = feature,
                    summary = summary,
                    status = status,
                    updated = updated,
                    storyPoints = storyPoints,
                    sprintId = sprintId,
                    sprintName = sprintName,
                    sprintStartDate = sprintStartDate,
                    sprintEndDate = sprintEndDate
                )

                results.add(issueData)

            } catch (e: Exception) {

                println("Error parsing issue: ${e.message}")
            }
        }

        return results
    }

    private fun buildJql(projectKeys: String?, userId: String): String {

        val query = StringBuilder()

        if (!projectKeys.isNullOrBlank()) {

            val formattedProjects = projectKeys
                .split(",")
                .map { it.trim() }
                .joinToString(",") { "\"$it\"" }

            query.append("project in ($formattedProjects) AND ")
        }

        query.append("assignee in ($userId) ")
        query.append("AND statusCategory != Done ")
        query.append("ORDER BY cf[10007] DESC, updated DESC, key DESC")

        return query.toString()
    }

    private fun disableSSLVerification() {

        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }
}
package com.subhamk.jiraplugin.service

import java.net.HttpURLConnection
import java.net.URL

object ConnectionTestService {

    fun testJira(url: String, token: String): Boolean {

        return try {

            val connection = URL("$url/rest/api/2/myself")
                .openConnection() as HttpURLConnection

            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")

            connection.connectTimeout = 5000

            connection.responseCode in 200..299

        } catch (e: Exception) {
            false
        }
    }

    fun testBitbucket(url: String, user: String, token: String): Boolean {

        return try {

            val connection = URL("$url/rest/api/1.0/users/$user")
                .openConnection() as HttpURLConnection

            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")

            connection.connectTimeout = 5000

            connection.responseCode in 200..299

        } catch (e: Exception) {
            false
        }
    }
}
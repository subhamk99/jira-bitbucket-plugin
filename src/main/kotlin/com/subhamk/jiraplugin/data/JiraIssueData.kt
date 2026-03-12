package com.subhamk.jiraplugin.data

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
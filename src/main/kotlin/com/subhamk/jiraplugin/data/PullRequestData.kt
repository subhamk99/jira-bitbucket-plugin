package com.subhamk.jiraplugin.data

data class PullRequestData(
    val id: String,
    val title: String,
    val status: String,
    val sourceBranch: String,
    val targetBranch: String,
    val repo: String,
    val link: String
)
package com.subhamk.jiraplugin.util

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager

object BackgroundLoader {

    fun <T> run(
        project: Project?,
        title: String,
        task: () -> T,
        onSuccess: (T) -> Unit
    ) {

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, false) {

            private var result: T? = null

            override fun run(indicator: ProgressIndicator) {

                indicator.text = title
                result = task()
            }

            override fun onSuccess() {
                result?.let { onSuccess(it) }
            }
        })
    }
}
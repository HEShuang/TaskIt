package com.example.taskit.ui.model

data class Task(
    val id: Int = 0,
    val bucket: Bucket,
    val content: String = "",
    val isChecked: Boolean = false,
    val isChild: Boolean = false,
    val isVisible: Boolean = true,
)

package com.example.taskit.ui.model

data class Task(
    val id: Int = 0,
    val bucket: Bucket,
    val parentId: Int = 0,
    val index: Int = 0,
    val content: String = "",
    val isChecked: Boolean = false,
    )

package com.example.taskit.ui.ext

import com.example.taskit.db.model.Task as DbTask
import com.example.taskit.db.model.Bucket as DbBucket
import com.example.taskit.ui.model.Bucket as UiBucket
import com.example.taskit.ui.model.Task as UiTask


fun DbBucket.toUiBucket() = UiBucket(
    id = id,
    name = name,
)

fun UiBucket.toDbBucket() = DbBucket(
    id = id,
    name = name,
)

fun DbTask.toUiTask(bucket: UiBucket, isVisible: Boolean) = UiTask(
    id = id,
    bucket = bucket,
    content = content,
    isChecked = isChecked,
    isChild = parentId >= 0,
    isVisible = isVisible,
)


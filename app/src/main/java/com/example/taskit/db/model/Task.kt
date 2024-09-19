package com.example.taskit.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Bucket::class,
            parentColumns = ["id"],
            childColumns = ["bucket_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "bucket_id")
    val bucketId: Int, //Each task belongs to a bucket/group

    @ColumnInfo(name = "parent_id")
    val parentId : Int, // parentId = -1 means it is a root task

    @ColumnInfo(name = "task_order")
    val taskOrder: Int = 0, // Index under its parent task

    val content: String = "",

    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,

){
    companion object {
        fun create(id: Int, bucketId: Int, parentId: Int, taskOrder: Int, content:String, isChecked: Boolean): Task {
            require(bucketId >= 0)
            require(parentId >= -1)
            require(taskOrder >= 0)
            return Task(id, bucketId, parentId, taskOrder, content, isChecked)
        }
    }
}

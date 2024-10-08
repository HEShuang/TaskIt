package com.example.taskit.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "buckets",
)

data class Bucket(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String = "new bucket",
)

package com.example.taskit.db.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buckets",
    //indices = [Index(value = ["name"], unique = true)]
)

data class Bucket(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String = "new bucket",
    //val users: List<Int>?,
)

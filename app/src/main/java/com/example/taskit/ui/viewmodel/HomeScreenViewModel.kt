package com.example.taskit.ui.viewmodel

import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
import kotlinx.coroutines.flow.StateFlow

interface HomeScreenViewModel {
    val buckets: StateFlow<List<Bucket>>
    fun buildTasksStateFlow(bucket: Bucket): StateFlow<List<Task>>
    fun getBucket(bucketId: Int, onComplete: (bucket: Bucket?) -> Unit)
    fun addBucket( nTasks: Int = 0, onComplete: (bucket: Bucket) -> Unit)
    fun updateBucket(bucket: Bucket)
    fun deleteBucket(bucket: Bucket)
    fun deleteBuckets(bucketIds: List<Int>)
}
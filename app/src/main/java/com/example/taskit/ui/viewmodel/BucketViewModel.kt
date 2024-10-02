package com.example.taskit.ui.viewmodel

import com.example.taskit.ui.model.Bucket
import kotlinx.coroutines.flow.StateFlow

interface BucketViewModel
{
    val buckets: StateFlow<List<Bucket>>
    fun getBucket(bucketId: Int, onComplete: (bucket: Bucket?) -> Unit)
    fun addBucket( nTasks: Int = 0, onComplete: (bucket: Bucket) -> Unit)
    fun updateBucket(bucket: Bucket)
    fun deleteBucket(bucket: Bucket)
    fun deleteBuckets(bucketIds: List<Int>)
}
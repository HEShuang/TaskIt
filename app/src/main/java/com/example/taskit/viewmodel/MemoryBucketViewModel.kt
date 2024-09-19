package com.example.taskit.viewmodel

import com.example.taskit.ui.viewmodel.BucketViewModel
import com.example.taskit.ui.model.Bucket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryBucketViewModel : BucketViewModel {
    private var id = 0
    private val _buckets = MutableStateFlow(emptyList<Bucket>())

    override val buckets: StateFlow<List<Bucket>> = _buckets.asStateFlow()
    override fun getBucket(bucketId: Int, onComplete: (bucket: Bucket?) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun addBucket(nTasks: Int, onComplete: (bucket: Bucket) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun updateBucket(bucket: Bucket) {
        TODO("Not yet implemented")
    }

    override fun deleteBucket(bucket: Bucket) {
        TODO("Not yet implemented")
    }

}



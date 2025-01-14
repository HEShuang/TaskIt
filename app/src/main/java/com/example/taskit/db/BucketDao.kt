package com.example.taskit.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.taskit.db.model.Bucket
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBucket(bucket: Bucket): Long

    @Update
    suspend fun updateBucket(bucket: Bucket)

    @Query("DELETE FROM buckets WHERE id = :bucketId")
    suspend fun deleteBucket(bucketId: Int)

    @Query("DELETE FROM buckets WHERE id IN (:bucketIds)")
    suspend fun deleteBuckets(bucketIds: List<Int>)

    @Query("SELECT * FROM buckets WHERE id = :bucketId")
    suspend fun getBucket(bucketId: Int): Bucket?

    @Query("SELECT * FROM buckets")
    fun getBucketsFlow(): Flow<List<Bucket>>

}
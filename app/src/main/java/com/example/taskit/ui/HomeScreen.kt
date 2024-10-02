package com.example.taskit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.viewmodel.BucketViewModel
import com.example.taskit.ui.viewmodel.TaskViewModel

@Composable
fun HomeScreen(
    bucketViewModel: BucketViewModel,
    taskViewModel: TaskViewModel,
    onAddBucket: ()->Unit,
    onLoadBucket: (bucketId: Int)->Unit,
    ) {
    val nMaxTasksInPreview = 5
    val buckets by bucketViewModel.buckets.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBucket,
                content = {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            )
        },
        floatingActionButtonPosition = FabPosition.End, // Aligns to the bottom-right
        content = { paddingValues ->
            LazyColumn (modifier = Modifier.padding(paddingValues)){
                items(buckets) {bucket ->
                    HomeBucketItem(
                        bucket = bucket,
                        nMaxTasks = nMaxTasksInPreview,
                        taskViewModel = taskViewModel,
                        onClick = {
                            onLoadBucket(bucket.id)
                        },
                        )
                }
            }
            if(buckets.isEmpty()){
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You don't have any tasks yet",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}


package com.example.taskit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.viewmodel.BucketViewModel
import com.example.taskit.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    bucketViewModel: BucketViewModel,
    taskViewModel: TaskViewModel,
    onAddBucket: ()->Unit,
    onLoadBucket: (bucketId: Int)->Unit,
) {
    val nMaxTasksInPreview = 5
    val buckets by bucketViewModel.buckets.collectAsStateWithLifecycle()
    var selection by remember { mutableStateOf(emptyList<Int>()) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selection.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selection.size}") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                selection = emptyList()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                )
            }
        },
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
                        requestSelect = bucket.id in selection,
                        onSelect = { isSelected ->
                            selection = if(isSelected)
                                selection + bucket.id
                            else
                                selection - bucket.id
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
                        text = "You don't have any notes yet",
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
    )

    if(showDeleteDialog){
        DialogAlertDelete(
            text = "Are you sure to delete selected notes?",
            onDismiss = {
                showDeleteDialog = false
            },
            onDelete = {
                if(selection.isNotEmpty()){
                    bucketViewModel.deleteBuckets(selection)
                    selection = emptyList()
                }
            },
        )
    }
}


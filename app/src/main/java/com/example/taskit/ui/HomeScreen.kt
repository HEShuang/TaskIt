package com.example.taskit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddBucket: ()->Unit,
    onLoadBucket: (bucketId: Int)->Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val nMaxTasksInPreview = 5
    val buckets by viewModel.buckets.collectAsStateWithLifecycle()
    var selection by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    with(sharedTransitionScope){
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = selection.isNotEmpty(),
                    enter = slideInVertically() + expandVertically (expandFrom = Alignment.Top),
                    exit = slideOutVertically() + shrinkVertically()
                ) {
                    TopAppBar(
                        title = { Text("${selection.size}") },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    selection = emptySet()
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
                LazyColumn (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ){
                    items(buckets) {bucket ->
                        val isSelectedState = remember {
                            derivedStateOf {
                                bucket.id in selection
                            }
                        }

                        HomeBucketItem(
                            modifier = Modifier.sharedElement(
                                state = rememberSharedContentState(key = bucket.id.toString()),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                            bucket = bucket,
                            nMaxTasks = nMaxTasksInPreview,
                            viewModel = viewModel,
                            onClick = {
                                onLoadBucket(bucket.id)
                            },
                            isSelectedState = isSelectedState,
                            switchSelection = {
                                selection = if (bucket.id !in selection)
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
    }

    if(showDeleteDialog){
        DialogAlertDelete(
            text = "Are you sure to delete selected notes?",
            onDismiss = {
                showDeleteDialog = false
            },
            onDelete = {
                if(selection.isNotEmpty()){
                    viewModel.deleteBuckets(selection.toList())
                    selection = emptySet()
                }
            },
        )
    }
}


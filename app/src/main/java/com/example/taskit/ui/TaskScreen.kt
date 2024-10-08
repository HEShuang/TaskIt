package com.example.taskit.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.viewmodel.TaskViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    bucket: Bucket,
    updateBucket: (bucket: Bucket) -> Unit,
    onDeleteBucket: () -> Unit,
    onGoBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val tasksStateFlow = remember(bucket) {
        viewModel.buildTasksStateFlow(bucket)
    }
    val tasks by tasksStateFlow.collectAsStateWithLifecycle()
    val isWriting by viewModel.isWriting.collectAsStateWithLifecycle()

    var reorderableTasks by remember(tasks) { mutableStateOf(tasks) }
    var reorderToIndex by remember { mutableIntStateOf(-1) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderToIndex = to.index
        reorderableTasks = reorderableTasks.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    var focusedItemIndex by remember { mutableIntStateOf(-1) }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .sharedElement(
                    state = rememberSharedContentState(key = bucket.id.toString()),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            topBar = {
                TaskScreenTopBar(
                    bucket = bucket,
                    onBucketNameChange = { newName ->
                        updateBucket(bucket.copy(name = newName))
                    },
                    onDeleteBucket = onDeleteBucket,
                    onGoBack = {
                        onGoBack()
                    }
                )
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    LazyColumn(
                        state = lazyListState,
                    ) {
                        itemsIndexed(
                            reorderableTasks,
                            key = { _, task -> task.id }) { index, task ->
                            ReorderableItem(reorderableLazyListState, key = task.id) { isDragging ->
                                if (!task.isVisible) return@ReorderableItem
                                TaskItem(
                                    task = task,
                                    isReordering = isDragging,
                                    onCheckedChange = { isChecked ->
                                        viewModel.updateTaskState(task.id, isChecked)
                                    },
                                    onContentChange = { newText ->
                                        if (newText != task.content) {
                                            viewModel.updateTaskContent(task.id, newText)
                                        }
                                    },
                                    onTaskAdd = {
                                        focusedItemIndex = index + 1
                                        viewModel.insertTask(task.id)
                                    },
                                    onTaskDelete = {
                                        focusedItemIndex = index
                                        if (index == tasks.size - 1 && index != 0) {
                                            focusedItemIndex = index - 1
                                        }
                                        viewModel.deleteTask(task.id)
                                    },
                                    onReorderStart = {
                                        viewModel.onReorderStart(task.id)
                                    },
                                    onReorderEnd = {
                                        viewModel.onReorderEnd(task.id)
                                        if (reorderToIndex in 0..<tasks.size)
                                            viewModel.reorderTask(task.id, tasks[reorderToIndex].id)
                                    },
                                    onMoveToRoot = {
                                        viewModel.moveTaskToRoot(task.id)
                                    },
                                    onMoveToChild = {
                                        if (index in 1..<tasks.size) {
                                            viewModel.moveTaskToChild(task.id, tasks[index - 1].id)
                                        }
                                    },
                                    isFirstTask = index == 0,
                                    requireFocus = index == focusedItemIndex,
                                )
                            }
                        }
                        item {
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                elevation = null,
                                onClick = {
                                    focusedItemIndex = tasks.size
                                    viewModel.addTask(bucket.id)
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Task")
                                Text("Add Task")
                            }
                        }
                    }
                }

                val isWritingAlpha by animateFloatAsState(
                    targetValue = if (isWriting) 1f else 0f,
                    label = "isWritingAlpha",
                )
                if (isWritingAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha((isWritingAlpha - 0.5f).coerceAtLeast(0f) * 2f)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { },
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        )

    }
}


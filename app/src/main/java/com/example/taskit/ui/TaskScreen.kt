package com.example.taskit.ui

import android.util.Log
import androidx.collection.emptyLongSet
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
import com.example.taskit.ui.viewmodel.BucketViewModel
import com.example.taskit.ui.viewmodel.TaskViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class StableState(
    var recomposition: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskScreen(
    taskViewModel: TaskViewModel,
    bucket: Bucket,
    updateBucket: (bucket: Bucket)->Unit,
) {
    var stableState = remember { StableState() }
    stableState.recomposition++
    Log.d("TaskScreen", "taskscreen composition: ${stableState.recomposition}")

    val tasksStateFlow = remember(bucket) {
        Log.d("TaskScreen", "buildTasksStateFlow")
        taskViewModel.buildTasksStateFlow(bucket)
    }
    val tasks by tasksStateFlow.collectAsStateWithLifecycle()
    val isWriting by taskViewModel.isWriting.collectAsStateWithLifecycle()

    var reorderableTasks by remember(tasks) { mutableStateOf(tasks) }
    var reorderToIndex by remember { mutableIntStateOf(-1) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderToIndex = to.index
        reorderableTasks = reorderableTasks.toMutableList().apply {
            Log.d("MoveTask", "onReorder : ${from.index} -> ${to.index}")
            add(to.index, removeAt(from.index))
        }
    }

    var focusedItemIndex by remember { mutableIntStateOf(0) }

    // Edit task list content
    Scaffold(
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                TaskScreenBucket(
                    name = bucket.name,
                    onTextChange = {newName ->
                        updateBucket(bucket.copy(name = newName))
                    }
                )

                LazyColumn (
                    state = lazyListState,
                ){
                    itemsIndexed(reorderableTasks, key = { _, task -> task.id } ) { index, task ->
                        ReorderableItem(reorderableLazyListState, key = task.id) { isDragging ->
                            if(!task.isVisible) return@ReorderableItem
                            TaskItem(
                                task = task,
                                isReordering = isDragging,
                                onCheckedChange = { isChecked ->
                                    taskViewModel.updateTaskState(task.id, isChecked)
                                },
                                onContentChange = { newText ->
                                    if(newText != task.content) {
                                        taskViewModel.updateTaskContent(task.id, newText)
                                    }
                                },
                                onTaskAdd = {
                                    focusedItemIndex = index + 1
                                    taskViewModel.insertTask(task.id)
                                },
                                onTaskDelete = {
                                    focusedItemIndex = index
                                    if (index == tasks.size - 1 && index != 0){
                                        focusedItemIndex = index - 1
                                    }
                                    taskViewModel.deleteTask(task.id)
                                },
                                onReorderStart = {
                                    taskViewModel.onReorderStart(task.id)
                                },
                                onReorderEnd = {
                                    taskViewModel.onReorderEnd(task.id)
                                    if(reorderToIndex in 0..< tasks.size)
                                        taskViewModel.reorderTask(task.id, tasks[reorderToIndex].id)
                                },
                                onMoveToRoot = {
                                    taskViewModel.moveTaskToRoot(task.id)
                                },
                                onMoveToChild = {
                                    if(index in 1..< tasks.size) {
                                        taskViewModel.moveTaskToChild(task.id, tasks[index - 1].id)
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
                                taskViewModel.addTask(bucket.id)
                            }
                        ){
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

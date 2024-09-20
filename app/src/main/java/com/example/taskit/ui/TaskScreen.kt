package com.example.taskit.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun TaskScreen(
    taskViewModel: TaskViewModel,
    bucket: Bucket,
    updateBucket: (bucket: Bucket)->Unit,
) {
    val tasksStateFlow = remember(bucket) {
        taskViewModel.buildTasksStateFlow(bucket)
    }
    val tasks by tasksStateFlow.collectAsStateWithLifecycle()

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
                Log.d("TaskScreen before BucketNameText", "check Bucket: ${bucket.name}")
                TaskScreenBucket(
                    name = bucket.name,
                    onTextChange = {newName ->
                        updateBucket(bucket.copy(name = newName))
                    }
                )
                LazyColumn (){
                    itemsIndexed(tasks) { index, task ->
                        TaskItem(
                            task = task,
                            onCheckedChange = { isChecked ->
                                taskViewModel.updateTask(task.copy(isChecked = isChecked))
                            },
                            onTextChange = { newText ->
                                taskViewModel.updateTask(task.copy(content = newText))
                            },
                            onTaskAdd = {
                                focusedItemIndex = tasks.size
                                taskViewModel.addTask(Task(bucket = bucket))
                            },
                            onTaskDelete = {
                                if (index == tasks.size - 1 && index != 0){
                                    focusedItemIndex = index - 1
                                }
                                taskViewModel.deleteTask(task)
                            },
                            requireFocus = index == focusedItemIndex
                        )
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
                                taskViewModel.addTask(Task(bucket = bucket))
                            }
                        ){
                            Icon(Icons.Filled.Add, contentDescription = "Add Task")
                            Text("Add Task")
                        }
                    }
                }
            }
        }
    )
}

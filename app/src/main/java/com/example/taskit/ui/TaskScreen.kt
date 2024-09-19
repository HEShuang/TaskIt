package com.example.taskit.ui

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
import com.example.taskit.ui.viewmodel.BucketViewModel
import com.example.taskit.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    taskViewModel: TaskViewModel,
    bucket: Bucket,
    updateBucket: (bucket: Bucket)->Unit,
) {
    val tasks by taskViewModel.buildTasksStateFlow(bucket).collectAsStateWithLifecycle()

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
                BucketNameText(
                    name = bucket.name,
                    onTextChange = {newName ->
                        updateBucket(bucket.copy(name = newName))
                    }
                )
                LazyColumn (){
                    itemsIndexed(tasks) { index, task ->

                        EditableTaskItem(
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
                        ButtonAddTask {
                            focusedItemIndex = tasks.size
                            taskViewModel.addTask(Task(bucket = bucket))
                        }
                    }
                }

                // Simple text field for editing tasks

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    // Handle saving the task list or navigate back
                }) {
                    Text("Save Task List")
                }
            }
        }
    )
}

@Composable
fun BucketNameText(
    name: String,
    onTextChange: (String)->Unit,
)
{
    var text by remember { mutableStateOf(name) }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            onTextChange(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onTaskAdd: () -> Unit,
    onTaskDelete:() -> Unit,
    requireFocus: Boolean
) {
    var text by remember { mutableStateOf(task.content) }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(requireFocus) }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox to toggle the checked state
        Checkbox(
            checked = task.isChecked,
            onCheckedChange = onCheckedChange
        )
        // TextField for the user to input or edit the note text
        BasicTextField(
            value = text,
            textStyle = MaterialTheme.typography.bodyMedium,
            onValueChange = {
                text = it
                onTextChange(it)
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onTaskAdd() })
        )
        if(isFocused){
            IconButton(onClick = onTaskDelete) {
                Icon(Icons.Default.Close,contentDescription = "Delete")
            }
        }

    }

    //When creating a new task item, focus on this item
    LaunchedEffect(requireFocus){
        if(requireFocus){
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun ButtonAddTask(onClick: () -> Unit){
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        elevation = null
    ){
        Icon(Icons.Filled.Add, contentDescription = "Add Task")
        Text("Add Task")
    }
}

/*
@Preview(showBackground = true)
@Composable
fun PreviewTask() {
    TaskScreen()
}*/

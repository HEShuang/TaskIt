package com.example.taskit.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.viewmodel.TaskViewModel

@Composable
fun HomeBucketItem(
    bucket: Bucket,
    nMaxTasks: Int,
    taskViewModel: TaskViewModel,
    onClick: ()->Unit,
    requestSelect: Boolean,
    onSelect: (isSelected: Boolean)->Unit,
){
    val tasksStateFlow = remember(bucket) {
        taskViewModel.buildTasksStateFlow(bucket)
    }
    val tasks by tasksStateFlow.collectAsStateWithLifecycle()
    val nTasks = if(nMaxTasks < tasks.size) nMaxTasks else tasks.size
    Log.d("HomeScreen", "show tasks : $nTasks")

    var isSelected by remember(requestSelect) { mutableStateOf(requestSelect) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        isSelected = !isSelected
                        onSelect(isSelected)
                    },
                    onTap = {
                      onClick()
                    }
                )
            },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if(isSelected) Color.Red else Color.LightGray),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .padding(6.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(// Bucket name
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 6.dp),
                text = bucket.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            //List of tasks
            if(nTasks > 0){
                for (i in 0..<nTasks) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        text = tasks[i].content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            //If there are tasks not shown in preview, show the number of the rest tasks
            if(nTasks < tasks.size){
                val nRest = tasks.size - nTasks
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(1.dp)
                ){
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "more items",
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$nRest items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
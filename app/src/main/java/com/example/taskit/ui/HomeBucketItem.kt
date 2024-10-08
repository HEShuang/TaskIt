package com.example.taskit.ui

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.viewmodel.HomeScreenViewModel
import com.example.taskit.ui.viewmodel.TaskViewModel

@Composable
fun HomeBucketItem(
    modifier: Modifier,
    bucket: Bucket,
    nMaxTasks: Int,
    viewModel: HomeScreenViewModel,
    onClick: () -> Unit,
    isSelectedState: State<Boolean>,
    switchSelection: () -> Unit,
){
    val tasksStateFlow = remember(bucket) {
        viewModel.buildTasksStateFlow(bucket)
    }
    val tasks by tasksStateFlow.collectAsStateWithLifecycle()
    val nTasks = if(nMaxTasks < tasks.size) nMaxTasks else tasks.size
    val isSelected by isSelectedState

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Red else Color.LightGray,
        animationSpec = tween(300),
        label = "HomeBucketItem-borderColor"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 10.dp else 0.dp,
        animationSpec = tween(300),
        label = "HomeBucketItem-shadowElevation"
    )

    LaunchedEffect(isSelected) {
        Log.d("aaaa", "launched ${bucket.id} $isSelected")
    }

    Box(modifier = modifier.fillMaxSize()){
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            Log.d("aaaa", "onPress ${bucket.id} $isSelected")
                            switchSelection()
                        },
                        onTap = {
                            onClick()
                        }
                    )
                },
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = shadowElevation,
        ) {
            Column(
                modifier = Modifier
                    .padding(6.dp),
                verticalArrangement = Arrangement.Center,
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
}
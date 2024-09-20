package com.example.taskit.ui

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.taskit.ui.model.Task

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onTaskAdd: () -> Unit,
    onTaskDelete:() -> Unit,
    onTaskMoved:(fromIndex: Int)->Unit,
    requireFocus: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(requireFocus) }

    val dndTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val fromIndex = event.toAndroidDragEvent()
                    .clipData?.getItemAt(0)?.text.toString().toInt()
                onTaskMoved(fromIndex)
                return true
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dragAndDropSource {
                detectTapGestures(onLongPress = {
                    startTransfer(
                        transferData = DragAndDropTransferData(
                            clipData = ClipData.newPlainText("fromTaskIndex", task.index.toString())
                        )
                    )
                })
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes()
                        .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = dndTarget,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = "Drag Drop",
        )
        Checkbox(
            checked = task.isChecked,
            onCheckedChange = onCheckedChange
        )
        BasicTextField(
            value = task.content,
            textStyle = MaterialTheme.typography.bodyMedium,
            onValueChange = {
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
    LaunchedEffect(requireFocus){
        if(requireFocus){
            focusRequester.requestFocus()
        }
    }
}

package com.example.taskit.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.taskit.ui.model.Task
import sh.calvin.reorderable.ReorderableCollectionItemScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableCollectionItemScope.TaskItem(
    uiIndex: Int,
    task: Task,
    isDragging: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onContentChange: (String) -> Unit,
    onTaskAdd: () -> Unit,
    onTaskDelete:() -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    requireFocus: Boolean
) {
    val direction = LocalLayoutDirection.current
    var contentFieldValue by remember (task.id){
        mutableStateOf(task.content.toTextFieldValue(direction))
    }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(requireFocus) }

    LaunchedEffect(contentFieldValue.text) {
        onContentChange(contentFieldValue.text)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDragging) Color.Red else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .draggableHandle(
                    onDragStarted = {
                        onDragStart()
                    },
                    onDragStopped = onDragEnd,
                )
                .padding(6.dp),
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Drag Drop",
        )
        Text(text = uiIndex.toString()  )
        Checkbox(
            checked = task.isChecked,
            onCheckedChange = onCheckedChange
        )
        BasicTextField(
            value = contentFieldValue,
            textStyle = MaterialTheme.typography.bodyMedium,
            onValueChange = {
                contentFieldValue = it
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

fun String.toTextFieldValue(layoutDirection: LayoutDirection): TextFieldValue = TextFieldValue(
    text = this,
    selection = if (layoutDirection == LayoutDirection.Ltr) TextRange(length) else TextRange.Zero
)
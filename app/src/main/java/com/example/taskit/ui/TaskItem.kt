package com.example.taskit.ui

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.taskit.R
import com.example.taskit.ui.model.Task
import sh.calvin.reorderable.ReorderableCollectionItemScope
import kotlin.math.roundToInt

enum class IndentAnchors {
    Root,
    Child,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableCollectionItemScope.TaskItem(
    task: Task,
    isReordering: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onContentChange: (String) -> Unit,
    onTaskAdd: () -> Unit,
    onTaskDelete: () -> Unit,
    onReorderStart: () -> Unit,
    onReorderEnd: () -> Unit,
    onMoveToRoot: () -> Unit,
    onMoveToChild: () -> Unit,
    requireFocus: Boolean
) {
    val direction = LocalLayoutDirection.current
    var contentFieldValue by remember (task.id){
        mutableStateOf(task.content.toTextFieldValue(direction))
    }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(requireFocus) }

    val density = LocalDensity.current
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val indentState = remember (task.isChild){
        AnchoredDraggableState(
            initialValue = if(task.isChild) IndentAnchors.Child else IndentAnchors.Root,
            anchors = DraggableAnchors {
                IndentAnchors.Child at with(density) { 40.dp.toPx() }
                IndentAnchors.Root at 0f
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = decayAnimationSpec,
        )
    }

    LaunchedEffect(contentFieldValue.text) {
        onContentChange(contentFieldValue.text)
    }

    LaunchedEffect(indentState.currentValue) {
        when (indentState.currentValue ) {
            IndentAnchors.Root -> onMoveToRoot()
            IndentAnchors.Child -> onMoveToChild()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            //.background(if (isDragging) Color.Red else Color.Transparent)
            .offset {
                IntOffset(
                    x = indentState
                        .requireOffset()
                        .roundToInt(),
                    y = 0
                )
            }
            .border(
                if (isReordering)
                    BorderStroke(1.dp, Color.LightGray)
                else
                    BorderStroke(1.dp, Color.Transparent)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_drag_size24),
            tint = Color.Unspecified,
            modifier = Modifier
                //vertical drag for reordering task
                .draggableHandle(
                    onDragStarted = {
                        onReorderStart()
                    },
                    onDragStopped = {
                        onReorderEnd()
                    },
                )
                //horizontal drag for changing parent task
                .anchoredDraggable(
                    state = indentState,
                    orientation = Orientation.Horizontal,
                )
                .padding(6.dp),
            contentDescription = "Drag Drop",
        )
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
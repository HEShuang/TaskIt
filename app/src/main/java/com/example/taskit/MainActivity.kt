package com.example.taskit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskit.db.AppDatabase
import com.example.taskit.ui.viewmodel.BucketViewModel
import com.example.taskit.ui.HomeScreen
import com.example.taskit.ui.TaskScreen
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.theme.TaskItTheme
import com.example.taskit.ui.viewmodel.TaskViewModel
import com.example.taskit.viewmodel.MemoryBucketViewModel
import com.example.taskit.viewmodel.MemoryTaskViewModel
import com.example.taskit.viewmodel.RoomBucketViewModel
import com.example.taskit.viewmodel.RoomTaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val bucketDao = AppDatabase.getDatabase(this).bucketDao
        val taskDao = AppDatabase.getDatabase(this).taskDao
        val bucketViewModel = RoomBucketViewModel(
            bucketDao = bucketDao,
            taskDao = taskDao,
            scope = lifecycleScope,
        )
        val taskViewModel = RoomTaskViewModel(
            taskDao = taskDao,
            scope = lifecycleScope,
        )

        setContent {
            TaskItTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp(bucketViewModel, taskViewModel)
                }
            }
        }
    }
}

@Composable
fun MyApp(bucketViewModel: BucketViewModel, taskViewModel: TaskViewModel) {
    val navController = rememberNavController()

    // Set up the navigation between screens
    NavHost(navController = navController, startDestination = Screen.HomeScreen.route) {

        composable(Screen.HomeScreen.route){
            HomeScreen(
                bucketViewModel = bucketViewModel,
                taskViewModel = taskViewModel,
                onAddBucket = {
                    // Navigate to TaskScreen (arg bucketId=-1) when Add button is clicked
                    val bucketId = -1
                    navController.navigate(Screen.TaskScreen.withArgs(bucketId.toString()))
                },
                onLoadBucket = { bucketId ->
                    //Navigate to TaskScreen which loads all tasks of the clicked bucket
                    navController.navigate(Screen.TaskScreen.withArgs(bucketId.toString()))
                },
            )
        }

        composable(
            route = Screen.TaskScreen.route + "/{bucketId}",
            arguments = listOf(
                navArgument("bucketId"){
                    type = NavType.IntType
                    defaultValue = -1
                    nullable = false
                }
            )
        ) {
            entry->
            //Get bucketId passed by arguments
            val bucketId = entry.arguments?.getInt("bucketId")!!
            var bucket by remember { mutableStateOf<Bucket?>(null)}

            //add new bucket (bucketId == -1) or get bucket by bucketId
            LaunchedEffect(Unit) {
                if(bucketId < 0) {
                    bucketViewModel.addBucket {
                        bucket = it
                    }
                }
                else {
                    bucketViewModel.getBucket(bucketId) {
                        bucket = it
                    }
                }
            }
            if(bucket != null){
                TaskScreen(taskViewModel, bucket!!) { newBucket ->
                    bucketViewModel.updateBucket(newBucket)
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewMyApp() {

    val bucketViewModel = MemoryBucketViewModel()
    val taskViewModel = MemoryTaskViewModel()
    for(i in 0..5) {
        bucketViewModel.addBucket(i+1) {}
    }

    HomeScreen(bucketViewModel, taskViewModel, {},{})
}



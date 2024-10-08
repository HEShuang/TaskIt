package com.example.taskit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskit.db.AppDatabase
import com.example.taskit.db.AppRepository
import com.example.taskit.ui.HomeScreen
import com.example.taskit.ui.TaskScreen
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.theme.TaskItTheme
import com.example.taskit.ui.viewmodel.HomeScreenViewModel
import com.example.taskit.ui.viewmodel.TaskScreenViewModel
import com.example.taskit.viewmodel.RoomHomeScreenViewModel
import com.example.taskit.viewmodel.RoomTaskScreenViewModel

class MainActivity : ComponentActivity() {
    //private val bucketDao = AppDatabase.getDatabase(this).bucketDao
    //private val taskDao = AppDatabase.getDatabase(this).taskDao
    //private val bucketViewModel by viewModels<RoomBucketViewModel>()
    //private val taskViewModel by viewModels<RoomTaskViewModel>( )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val repo = AppRepository(AppDatabase.getDatabase(this))
            val homeViewModel = viewModel<RoomHomeScreenViewModel>(
                factory = object  : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return RoomHomeScreenViewModel(repo) as T
                    }
                }
            )
            val taskViewModel = viewModel<RoomTaskScreenViewModel>(
                factory = object  : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return RoomTaskScreenViewModel(repo) as T
                    }
                }
            )

            TaskItTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp(homeViewModel, taskViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MyApp(homeViewModel: HomeScreenViewModel, taskViewModel: TaskScreenViewModel) {
    val navController = rememberNavController()

    SharedTransitionLayout() {
        NavHost(
            navController = navController,
            startDestination = Screen.HomeScreen.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Screen.HomeScreen.route){
                HomeScreen(
                    viewModel = homeViewModel,
                    onAddBucket = {
                        // Navigate to TaskScreen (arg bucketId=-1) when Add button is clicked
                        val bucketId = -1
                        navController.navigate(Screen.TaskScreen.withArgs(bucketId.toString()))
                    },
                    onLoadBucket = { bucketId ->
                        //Navigate to TaskScreen which loads all tasks of the clicked bucket
                        navController.navigate(Screen.TaskScreen.withArgs(bucketId.toString()))
                    },
                    animatedVisibilityScope = this@composable,
                    sharedTransitionScope = this@SharedTransitionLayout,
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
                        homeViewModel.addBucket {
                            bucket = it
                        }
                    }
                    else {
                        homeViewModel.getBucket(bucketId) {
                            bucket = it
                        }
                    }
                }
                if(bucket != null){
                    TaskScreen(
                        viewModel = taskViewModel,
                        bucket = bucket!!,
                        updateBucket = { newBucket ->
                            homeViewModel.updateBucket(newBucket)
                        },
                        onDeleteBucket = {
                            homeViewModel.deleteBucket(bucket!!)
                            navController.popBackStack()
                        },
                        onGoBack = {
                            navController.popBackStack()
                        },
                        animatedVisibilityScope = this@composable,
                        sharedTransitionScope = this@SharedTransitionLayout,
                    )
                }
            }
        }
    }
}



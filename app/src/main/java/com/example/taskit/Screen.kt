package com.example.taskit

sealed class Screen (val route: String){
    data object HomeScreen: Screen("home_screen")
    data object TaskScreen: Screen("task_screen")

    fun withArgs(vararg  args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}
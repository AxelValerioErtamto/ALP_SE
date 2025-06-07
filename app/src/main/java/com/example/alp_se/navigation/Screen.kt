package com.example.alp.navigation

sealed class Screen(val route: String) {
    object LoginRegister : Screen("login_register_screen")
    object Home : Screen("home_screen")
    object CreateMemory : Screen("create_memory_screen")
    object EditMemory : Screen("edit_memory_screen/{memoryId}") {
        fun createRoute(memoryId: Int) = "edit_memory_screen/$memoryId"
    }
}
package com.example.alp_se.enums

// Updated PagesEnum to match the structure implied by your Screen sealed class
enum class PagesEnum(val routeName: String) {
    // Corresponds to Screen.LoginRegister
    LoginRegister("login_register_screen"),

    // Corresponds to Screen.Home
    Home("home_screen"),

    // Corresponds to Screen.CreateMemory
    CreateMemory("create_memory_screen"),

    // Corresponds to Screen.EditMemory (base route without argument)
    // The argument part "{memoryId}" is handled by NavHost when defining the composable.
    // The enum entry represents the base destination.
    EditMemory("edit_memory_screen")
    // Note: The dynamic part with memoryId ("edit_memory_screen/{memoryId}")
    // is typically defined in your NavHost composable route string.
    // The enum helps identify the destination type.
}
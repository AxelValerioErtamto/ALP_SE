package com.example.alp_se.views // Or your appropriate package

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text // For placeholder routes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alp.navigation.Screen // Your Screen sealed class
import com.example.alp.viewmodels.AuthenticationViewModel
import com.example.alp.viewmodels.MemoryViewModel

// Import your actual view composables
// import com.example.alp.views.LoginRegisterView
// import com.example.alp.views.HomeView
// import com.example.alp.views.CreateMemoryView
// import com.example.alp.views.EditMemoryView

@Composable
fun MemoMapApp(
    navController: NavHostController = rememberNavController(),
    authenticationViewModel: AuthenticationViewModel = viewModel(factory = AuthenticationViewModel.Factory),
    memoryViewModel: MemoryViewModel = viewModel(factory = MemoryViewModel.Factory)
) {
    // Observe the token from the AuthenticationViewModel's UserRepository
    val currentUserToken by authenticationViewModel.userRepository.currentUserToken.collectAsState(initial = "Unknown")
    // val currentUserId by authenticationViewModel.userRepository.currentUserId.collectAsState(initial = 0) // If needed globally

    // This LaunchedEffect handles initial navigation based on auth state when the app starts.
    // Actual navigation after login/register actions should be handled within AuthenticationViewModel.
    LaunchedEffect(currentUserToken, navController.currentBackStackEntry) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentUserToken != "Unknown" && currentUserToken.isNotBlank()) {
            // User is logged in
            if (currentRoute == Screen.LoginRegister.route) { // Only navigate if currently on login/register
                // Potentially check if admin here if you have a separate admin start screen
                // For now, assume all authenticated users go to Home.
                // The AuthenticationViewModel's loginUser method should handle admin navigation.
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.LoginRegister.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            // User is not logged in
            if (currentRoute != Screen.LoginRegister.route) { // Only navigate if not already on login/register
                navController.navigate(Screen.LoginRegister.route) {
                    // Pop up to the start of the graph to clear the back stack.
                    // This is important if the user logs out from a deep screen.
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.LoginRegister.route // Always start here; LaunchedEffect will redirect
    ) {
        composable(route = Screen.LoginRegister.route) {
            LoginRegisterView( // Assuming this view handles both login and registration UI
                authenticationViewModel = authenticationViewModel,
                navController = navController
            )
        }

        composable(route = Screen.Home.route) {
            HomeView(
                navController = navController,
                authViewModel = authenticationViewModel,
                memoryViewModel = memoryViewModel
            )
        }

        composable(route = Screen.CreateMemory.route) {
            CreateMemoryView(
                navController = navController,
                authViewModel = authenticationViewModel,
                memoryViewModel = memoryViewModel
            )
        }

        composable(
            route = Screen.EditMemory.route, // This is "edit_memory_screen/{memoryId}"
            arguments = listOf(navArgument("memoryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val memoryId = backStackEntry.arguments?.getInt("memoryId") ?: 0 // Default to 0 or handle error
            if (memoryId > 0) { // Basic validation for memoryId
                EditMemoryView(
                    navController = navController,
                    authViewModel = authenticationViewModel,
                    memoryViewModel = memoryViewModel,
                    memoryId = memoryId
                )
            } else {
                // Handle invalid memoryId, e.g., show an error or navigate back
                Text("Error: Invalid Memory ID", modifier = Modifier.padding(16.dp))
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        // Add other routes from your Screen object or PagesEnum as needed
        // Example for other PagesEnum entries if you still use them for some routes:
        // composable(route = PagesEnum.Admin.routeName) { /* AdminView(...) */ }
    }
}
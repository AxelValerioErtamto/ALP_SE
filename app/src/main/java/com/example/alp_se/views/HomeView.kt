package com.example.alp_se.views

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.example.alp_se.models.MemoryPost
import com.example.alp_se.navigation.Screen // Assuming Screen object is correctly defined
import com.example.alp_se.viewmodels.MemoryViewModel
import android.net.Uri
import com.example.alp_se.viewmodels.AuthenticationViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    navController: NavHostController,
    authViewModel: AuthenticationViewModel,
    memoryViewModel: MemoryViewModel
) {
    val memoriesUiState by memoryViewModel.memoriesUiState.collectAsState()
    // Get user ID and token from the userRepository within AuthenticationViewModel
    val currentUserId by authViewModel.userRepository.currentUserId.collectAsState(initial = 0)
    val currentUserToken by authViewModel.userRepository.currentUserToken.collectAsState(initial = "Unknown")

    val context = LocalContext.current

//    // Fetch memories when a valid user ID is available
//    LaunchedEffect(currentUserId) {
//        if (currentUserId != 0) {
//            memoryViewModel.fetchMemories(currentUserId)
//        }
//    }

    // Navigate to login if user token indicates logged out state
    LaunchedEffect(currentUserToken) { // Fetch based on token availability
        if (currentUserToken != "Unknown" && currentUserToken.isNotBlank()) {
            memoryViewModel.fetchAllMemoriesForFeed() // Fetch all memories for the feed
        } else {
            // Optionally clear memories if user logs out or show "login to view"
//            _memoriesUiState.value = MemoriesUiState(memories = emptyList(), error = "Please log in to view memories.")
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Memories") },
                actions = {
                    IconButton(onClick = {
                        // Call the logout function from AuthenticationViewModel
                        authViewModel.logout(navController)
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreateMemory.route) }) {
                Icon(Icons.Filled.Add, contentDescription = "Create Memory")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (memoriesUiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (memoriesUiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${memoriesUiState.error}", color = MaterialTheme.colorScheme.error)
                }
            } else if (memoriesUiState.memories.isEmpty() && currentUserId != 0) { // Only show "No memories" if logged in
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No memories yet. Create one!")
                }
            } else if (currentUserId == 0 && !memoriesUiState.isLoading) { // Handling the initial state before userId is confirmed
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading user data...") // Or a different placeholder
                }
            }
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memoriesUiState.memories) { memory ->
                        MemoryCard(
                            memory = memory,
                            onEdit = {
                                if (memory.id > 0) {
                                    navController.navigate(Screen.EditMemory.createRoute(memory.id))
                                } else {
                                    Toast.makeText(context, "${memory.id} does not exist", Toast.LENGTH_SHORT).show()
                                }
                            },
                            currentLoggedInUserId = currentUserId // <<< --- THIS LINE IS ADDED/CORRECTED
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryCard(memory: MemoryPost, onEdit: () -> Unit, currentLoggedInUserId: Int // Pass this from HomeView
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Could navigate to a detail view if Screen.MemoryDetail exists */
                // Example: navController.navigate(Screen.MemoryDetail.createRoute(memory.memoryId))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${memory.id}")
            if (memory.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = Uri.parse(memory.imageUrl) // Assumes imageUrl is a parseable URI string (http, content, etc.)
                    ),
                    contentDescription = memory.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(memory.caption, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            memory.location?.let {
                if (it.isNotBlank()) {
                    Text("Location: $it", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (memory.userId == currentLoggedInUserId) { // Show edit button only for owner
                Button(onClick = onEdit, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Edit")
                }
            }
        }
    }
}
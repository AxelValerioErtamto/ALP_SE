package com.example.alp_se.views

import android.Manifest
import android.content.ContextWrapper // For activity fallback
import android.content.Intent // <<< --- ADD THIS IMPORT
import android.net.Uri
import android.util.Log // For logging permission attempts
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.alp_se.viewmodels.AuthenticationViewModel
import com.example.alp_se.viewmodels.MemoryViewModel
import com.example.alp_se.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemoryView(
    navController: NavController,
    authViewModel: AuthenticationViewModel,
    memoryViewModel: MemoryViewModel,
    memoryId: Int
) {
    var caption by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUrlFromDb by remember { mutableStateOf<String?>(null) }
    var locationString by remember { mutableStateOf<String?>(null) }
    var addLocationToggled by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val singleMemoryState by memoryViewModel.singleMemoryUiState.collectAsState()
    val currentUserId by authViewModel.userRepository.currentUserId.collectAsState(initial = 0)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(lifecycleOwner) {
        if (lifecycleOwner is ComponentActivity) {
            lifecycleOwner
        } else {
            var tempContext = context
            while (tempContext is ContextWrapper) {
                if (tempContext is android.app.Activity) {
                    return@remember tempContext
                }
                tempContext = tempContext.baseContext
            }
            null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { selectedUri ->
            if (selectedUri != null) {
                try {
                    // Try to take persistent read permission
                    val contentResolver = context.contentResolver
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
                    imageUri = selectedUri // This is the new URI selected by the user
                    Log.d("EditMemoryView", "Persistent permission taken for new URI: $selectedUri")
                } catch (e: SecurityException) {
                    Log.e("EditMemoryView", "Failed to take persistent permission for new URI: $selectedUri", e)
                    imageUri = selectedUri
                    Toast.makeText(context, "Could not secure long-term image access. Image might not be visible after app restart.", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    val locationPermissionLauncher = rememberLocationPermissionLauncher(
        onPermissionGranted = {
            memoryViewModel.setFetchingLocation(true)
            getCurrentLocation(
                context = context,
                onSuccess = { lat, lon ->
                    locationString = "Lat: %.3f, Lon: %.3f".format(lat, lon)
                    memoryViewModel.setFetchingLocation(false)
                },
                onError = { e ->
                    locationString = singleMemoryState.memory?.location
                    Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
                    memoryViewModel.setFetchingLocation(false)
                    addLocationToggled = singleMemoryState.memory?.location != null
                }
            )
        },
        onPermissionDenied = {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
            addLocationToggled = singleMemoryState.memory?.location != null
        },
        onPermissionPermanentlyDenied = {
            Toast.makeText(context, "Location permission permanently denied. Please enable in settings.", Toast.LENGTH_LONG).show()
            activity?.let { openAppSettings(it) }
            addLocationToggled = singleMemoryState.memory?.location != null
        }
    )

    LaunchedEffect(memoryId) {
        if (memoryId > 0) {
            memoryViewModel.getMemoryById(memoryId)
        }
    }

//    LaunchedEffect(singleMemoryState) {
//        singleMemoryState.memory?.let { memory ->
//            if (!singleMemoryState.isLoading && !singleMemoryState.operationSuccess && caption != memory.caption) {
//                caption = memory.caption
//                currentImageUrlFromDb = memory.imageUrl // This is the URL/URI string from DB
//                locationString = memory.location
//                addLocationToggled = memory.location != null
//            }
//        }
//        if (singleMemoryState.operationSuccess) {
//            Toast.makeText(context, "Operation successful!", Toast.LENGTH_SHORT).show()
//            if (currentUserId != 0) {
//                // Assuming HomeView shows all memories, refresh the global feed
//                memoryViewModel.fetchAllMemoriesForFeed()
//            }
//            navController.popBackStack()
//            memoryViewModel.resetSingleMemoryState()
//        }
//        singleMemoryState.error?.let {
//            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
//            memoryViewModel.resetSingleMemoryState()
//        }
//    }

    LaunchedEffect(singleMemoryState.memory) { // Key on singleMemoryState.memory
        singleMemoryState.memory?.let { loadedMemory ->
            // Only set initial values if they haven't been set yet or if memoryId changes
            // and to avoid overwriting user edits if singleMemoryState recomposes for other reasons.
            if (caption.isEmpty() && currentImageUrlFromDb == null && locationString == null) { // Heuristic: populate if all local states are "empty"
                caption = loadedMemory.caption
                currentImageUrlFromDb = loadedMemory.imageUrl
                locationString = loadedMemory.location
                addLocationToggled = loadedMemory.location != null
            } else if (caption == "" && loadedMemory.caption.isNotEmpty()) { // If user cleared it, but original had value
                // This part is tricky. If user clears caption, do we send "" or original?
                // For "if left empty, keep original", we'll handle this in the onClick.
                // For now, let local state `caption` be empty if user makes it so.
            }
        }
    }

    // This LaunchedEffect handles navigation and Toasts after an operation
    LaunchedEffect(singleMemoryState.operationSuccess, singleMemoryState.error) {
        if (singleMemoryState.operationSuccess) {
            Toast.makeText(context, "Operation successful!", Toast.LENGTH_SHORT).show()
            if (currentUserId != 0) {
                memoryViewModel.fetchAllMemoriesForFeed() // Or fetchMyMemories()
            }
            navController.popBackStack()
            memoryViewModel.resetSingleMemoryState()
        }
        singleMemoryState.error?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            memoryViewModel.resetSingleMemoryState()
        }
    }

    Scaffold(
        topBar = {
                TopAppBar(
                    title = { Text("Edit Memory") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete Memory", tint = MaterialTheme.colorScheme.error)
                    }
                }

            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (singleMemoryState.isLoading && singleMemoryState.memory == null && !singleMemoryState.operationSuccess) {
                CircularProgressIndicator()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // If a new image is selected (imageUri is not null), display it.
                    // Otherwise, display the image from the database (currentImageUrlFromDb).
                    val displayUri = imageUri ?: currentImageUrlFromDb?.let { Uri.parse(it) }

                    if (displayUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = displayUri),
                            contentDescription = "Memory image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, "Change Photo", modifier = Modifier.size(48.dp))
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !singleMemoryState.isLoading || singleMemoryState.operationSuccess
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Current Location:", modifier = Modifier.weight(1f))
                    Switch(
                        checked = addLocationToggled,
                        onCheckedChange = {
                            addLocationToggled = it
                            if (it) {
                                if (!isLocationEnabled(context)) {
                                    Toast.makeText(context, "Please enable location services.", Toast.LENGTH_LONG).show()
                                    activity?.let { act -> requestLocationEnable(act) }
                                    addLocationToggled = false
                                    return@Switch
                                }
                                if (hasLocationPermission(context)) {
                                    memoryViewModel.setFetchingLocation(true)
                                    getCurrentLocation(
                                        context = context,
                                        onSuccess = { lat, lon ->
                                            locationString = "Lat: %.3f, Lon: %.3f".format(lat, lon)
                                            memoryViewModel.setFetchingLocation(false)
                                        },
                                        onError = { e ->
                                            locationString = singleMemoryState.memory?.location
                                            Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
                                            memoryViewModel.setFetchingLocation(false)
                                            addLocationToggled = singleMemoryState.memory?.location != null
                                        }
                                    )
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            } else {
                                locationString = null
                            }
                        },
                        thumbContent = if (addLocationToggled) {
                            { Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null) }
                        } else null,
                        enabled = !singleMemoryState.isLoading || singleMemoryState.operationSuccess
                    )
                }
                if (singleMemoryState.isFetchingLocation) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (addLocationToggled && locationString != null) {
                    Text("Location: $locationString", style = MaterialTheme.typography.bodySmall)
                } else if (!addLocationToggled && singleMemoryState.memory?.location != null && locationString == null) {
                    Text("Original Location: ${singleMemoryState.memory?.location}", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (singleMemoryState.isLoading && !singleMemoryState.operationSuccess) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            // If imageUri (newly selected image) is null, it means the user didn't pick a new image.
                            // In this case, we use currentImageUrlFromDb (the one already stored).
                            // If imageUri is NOT null, it means the user picked a new image, so we use that.
                            val imageToSendUri = imageUri // This is the new Uri if selected
                            val existingImageUrl = currentImageUrlFromDb ?: "" // This is the string from DB

                            val finalImageUri = imageUri
                            val finalImageUrl = currentImageUrlFromDb ?: ""

//                            val userIdForRefresh = if (currentUserId != 0) currentUserId else null


                            memoryViewModel.updateMemory(
                                memoryId = memoryId,
                                caption = caption,
                                imageUri = finalImageUri,
                                currentImageUrl = finalImageUrl,
                                location = if (addLocationToggled) locationString else null,
//                                userIdToRefresh = userIdForRefresh
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !singleMemoryState.isLoading || singleMemoryState.operationSuccess
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Memory") },
            text = { Text("Are you sure you want to delete this memory? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUserId != 0) {
                            // Pass currentUserId to deleteMemory as it's used for refreshing the list later
                            memoryViewModel.deleteMemory(memoryId, currentUserId)
                        } else {
                            Toast.makeText(context, "Cannot delete: User not identified or not owner.", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

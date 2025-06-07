package com.example.alp_se.views

import android.Manifest
import android.content.Context
import android.content.Intent // <<< --- ADD THIS IMPORT
import android.net.Uri
import android.util.Log // For logging permission attempts
import android.widget.Toast
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.alp_se.viewmodels.AuthenticationViewModel
import com.example.alp_se.viewmodels.MemoryViewModel
import com.example.alp_se.utils.*

const val MAX_IMAGE_SIZE_BYTES_CREATE = 15 * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMemoryView(
    navController: NavController,
    authViewModel: AuthenticationViewModel,
    memoryViewModel: MemoryViewModel
) {
    var caption by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var locationString by remember { mutableStateOf<String?>(null) }
    var addLocationToggled by remember { mutableStateOf(false) }

    val currentUserId by authViewModel.userRepository.currentUserId.collectAsState(initial = 0)
    val singleMemoryState by memoryViewModel.singleMemoryUiState.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { selectedUri ->
            if (selectedUri != null) {
                // --- BEGIN FILE SIZE CHECK ---
                var fileSize: Long = -1
                try {
                    context.contentResolver.openFileDescriptor(selectedUri, "r")?.use { parcelFileDescriptor ->
                        fileSize = parcelFileDescriptor.statSize
                    }
                } catch (e: Exception) {
                    Log.e("CreateMemoryView", "Error getting file size for URI: $selectedUri", e)
                    Toast.makeText(context, "Could not determine image size.", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult // Stop processing this URI
                }

                if (fileSize == -1L) { // Should ideally not happen if descriptor was obtained
                    Toast.makeText(context, "Could not determine image size.", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                if (fileSize > MAX_IMAGE_SIZE_BYTES_CREATE) {
                    val sizeInMB = MAX_IMAGE_SIZE_BYTES_CREATE / (1024.0 * 1024.0)
                    Toast.makeText(context, "Image is too large (max ${"%.1f".format(sizeInMB)} MB). Size: ${"%.1f".format(fileSize / (1024.0 * 1024.0))} MB", Toast.LENGTH_LONG).show()
                    return@rememberLauncherForActivityResult // Reject image
                }
                // --- END FILE SIZE CHECK ---

                // If size check passes, proceed with taking persistent permission
                try {
                    val contentResolver = context.contentResolver
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
                    imageUri = selectedUri // Update state with the URI that now has persistent permission
                    Log.d("CreateMemoryView", "Persistent permission taken for URI: $selectedUri. Size: $fileSize bytes.")
                } catch (e: SecurityException) {
                    Log.e("CreateMemoryView", "Failed to take persistent permission for URI: $selectedUri", e)
                    imageUri = selectedUri // Fallback: use the URI without persistent permission
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
                    Toast.makeText(context, "Location acquired", Toast.LENGTH_SHORT).show()
                },
                onError = { e ->
                    locationString = null
                    Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
                    memoryViewModel.setFetchingLocation(false)
                    addLocationToggled = false
                }
            )
        },
        onPermissionDenied = {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
            addLocationToggled = false
        },
        onPermissionPermanentlyDenied = {
            Toast.makeText(context, "Location permission permanently denied. Please enable in settings.", Toast.LENGTH_LONG).show()
            openAppSettings(context)
            addLocationToggled = false
        }
    )

    LaunchedEffect(singleMemoryState) {
        if (singleMemoryState.operationSuccess) {
            Toast.makeText(context, "Memory created successfully!", Toast.LENGTH_SHORT).show()
            if (currentUserId != 0) {
                // Assuming HomeView shows all memories, refresh the global feed
                memoryViewModel.fetchAllMemoriesForFeed()
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
                title = { Text("Create New Memory") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
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
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, "Add Photo", modifier = Modifier.size(48.dp))
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Current Location:", modifier = Modifier.weight(1f))
                Switch(
                    checked = addLocationToggled,
                    onCheckedChange = {
                        addLocationToggled = it
                        if (it) {
                            if (!isLocationEnabled(context)) {
                                Toast.makeText(context, "Please enable location services.", Toast.LENGTH_LONG).show()
                                requestLocationEnable(context)
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
                                        Toast.makeText(context, "Location acquired", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { e ->
                                        locationString = null
                                        Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
                                        memoryViewModel.setFetchingLocation(false)
                                        addLocationToggled = false
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
                    } else null
                )
            }
            if (singleMemoryState.isFetchingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (addLocationToggled && locationString != null) {
                Text("Location: $locationString", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (singleMemoryState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val currentImgUri = imageUri
                        if (currentImgUri == null) {
                            Toast.makeText(context, "Please select an image.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (caption.isBlank()) {
                            Toast.makeText(context, "Caption cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (currentUserId != 0) {
                            memoryViewModel.createMemory(
                                currentUserId,
                                caption,
                                currentImgUri,
                                if (addLocationToggled) locationString else null
                            )
                        } else {
                            Toast.makeText(context, "User not identified. Cannot create memory.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Memory")
                }
            }
        }
    }
}

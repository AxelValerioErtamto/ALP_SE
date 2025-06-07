package com.example.alp_se.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alp_se.MemoMapApplication // Assuming this is your Application class
import com.example.alp_se.models.MemoryPost
import com.example.alp_se.repositories.MemoryRepository
import com.example.alp_se.repositories.UserRepository // Added UserRepository dependency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first // To get the current token
import kotlinx.coroutines.launch

data class MemoriesUiState(
    val isLoading: Boolean = false,
    val memories: List<MemoryPost> = emptyList(),
    val error: String? = null
)

data class SingleMemoryUiState(
    val isLoading: Boolean = false,
    val memory: MemoryPost? = null,
    val error: String? = null,
    val operationSuccess: Boolean = false,
    val isFetchingLocation: Boolean = false
)

class MemoryViewModel(
    private val memoryRepository: MemoryRepository,
    private val userRepository: UserRepository // Added UserRepository
) : ViewModel() {

    private val _memoriesUiState = MutableStateFlow(MemoriesUiState())
    val memoriesUiState: StateFlow<MemoriesUiState> = _memoriesUiState.asStateFlow()

    private val _singleMemoryUiState = MutableStateFlow(SingleMemoryUiState())
    val singleMemoryUiState: StateFlow<SingleMemoryUiState> = _singleMemoryUiState.asStateFlow()


    fun fetchMyMemories() { // Renamed and userId parameter removed
        viewModelScope.launch {
            _memoriesUiState.value = MemoriesUiState(isLoading = true, error = null) // Clear previous error
            try {
                val token = userRepository.currentUserToken.first()
                if (token == "Unknown" || token.isBlank()) {
                    _memoriesUiState.value = MemoriesUiState(error = "User not authenticated")
                    return@launch
                }
                memoryRepository.fetchMyMemories(token) // Call the updated repository method
                    .onSuccess { fetchedMemories ->
                        _memoriesUiState.value = MemoriesUiState(memories = fetchedMemories)
                    }
                    .onFailure { exception ->
                        _memoriesUiState.value = MemoriesUiState(error = exception.message ?: "Failed to fetch your memories")
                    }
            } catch (e: Exception) {
                _memoriesUiState.value = MemoriesUiState(error = e.message ?: "An unexpected error occurred")
            }
        }
    }

    /**
     * Fetches all memories to be displayed in a global feed.
     */
    fun fetchAllMemoriesForFeed() {
        viewModelScope.launch {
            _memoriesUiState.value = MemoriesUiState(isLoading = true, error = null) // Clear previous error
            try {
                val token = userRepository.currentUserToken.first()
                if (token == "Unknown" || token.isBlank()) {
                    // Decide if unauthenticated users can see the feed.
                    // If not, show error. If yes, call without token (if backend allows)
                    // For now, assuming feed requires auth.
                    _memoriesUiState.value = MemoriesUiState(error = "User not authenticated to view feed")
                    return@launch
                }
                memoryRepository.fetchAllMemories(token)
                    .onSuccess { fetchedMemories ->
                        _memoriesUiState.value = MemoriesUiState(memories = fetchedMemories)
                    }
                    .onFailure { exception ->
                        _memoriesUiState.value = MemoriesUiState(error = exception.message ?: "Failed to fetch all memories")
                    }
            } catch (e: Exception) {
                _memoriesUiState.value = MemoriesUiState(error = e.message ?: "An unexpected error occurred")
            }
        }
    }
    fun createMemory(userId: Int, caption: String, imageUri: Uri, location: String?) {
        viewModelScope.launch {
            _singleMemoryUiState.value = SingleMemoryUiState(isLoading = true)
            try {
                val token = userRepository.currentUserToken.first()
                if (token == "Unknown") {
                    _singleMemoryUiState.value = SingleMemoryUiState(error = "User not authenticated")
                    return@launch
                }
                // For backend, you'd upload imageUri here and get back a URL
                // For now, we just use the URI string
                // The repository's createMemory no longer takes userId, it's inferred from token
                memoryRepository.createMemory(token, caption, imageUri.toString(), location)
                    .onSuccess { newMemory ->
                        _singleMemoryUiState.value = SingleMemoryUiState(memory = newMemory, operationSuccess = true)
                        fetchMyMemories() // Refresh list using the original userId
                    }
                    .onFailure { exception ->
                        _singleMemoryUiState.value = SingleMemoryUiState(error = exception.message ?: "Failed to create memory")
                    }
            } catch (e: Exception) {
                _singleMemoryUiState.value = SingleMemoryUiState(error = e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun updateMemory(
        memoryId: Int,
        caption: String,
        imageUri: Uri?,
        currentImageUrl: String,
        location: String?,
// userIdToRefresh is no longer needed if we fetch based on auth state or global feed
    ) {
        viewModelScope.launch {
            _singleMemoryUiState.value = SingleMemoryUiState(isLoading = true, error = null)
            try {
                val token = userRepository.currentUserToken.first()
                if (token == "Unknown" || token.isBlank()) {
                    _singleMemoryUiState.value = SingleMemoryUiState(error = "User not authenticated")
                    return@launch
                }
                val imageUrlToSave = imageUri?.toString() ?: currentImageUrl

                memoryRepository.updateMemory(token, memoryId, caption, imageUrlToSave, location)
                    .onSuccess { updatedMemory ->
                        _singleMemoryUiState.value = SingleMemoryUiState(memory = updatedMemory, operationSuccess = true)
                        // Refresh the appropriate list
                        fetchAllMemoriesForFeed() // Or fetchMyMemories()
                    }
                    .onFailure { exception ->
                        _singleMemoryUiState.value = _singleMemoryUiState.value.copy(isLoading = false, error = exception.message ?: "Failed to update memory")
                    }
            } catch (e: Exception) {
                _singleMemoryUiState.value = _singleMemoryUiState.value.copy(isLoading = false, error = e.message ?: "An unexpected error occurred")
            }
        }
    }


//    fun updateMemory(
//        memoryId: Int,
//        newCaptionInput: String,
//        newImageUri: Uri?,
//        newLocationInput: String?,
//        addLocationToggled: Boolean
//    ) {
//        viewModelScope.launch {
//            val originalMemory = _singleMemoryUiState.value.memory
//            if (originalMemory == null) {
//                _singleMemoryUiState.value = _singleMemoryUiState.value.copy(
//                    isLoading = false, // Stop loading if original data isn't there
//                    error = "Original memory data not found. Cannot update."
//                )
//                return@launch
//            }
//
//            _singleMemoryUiState.value = _singleMemoryUiState.value.copy(isLoading = true, error = null)
//            try {
//                val token = userRepository.currentUserToken.first()
//                if (token == "Unknown" || token.isBlank()) {
//                    _singleMemoryUiState.value = _singleMemoryUiState.value.copy(
//                        isLoading = false,
//                        error = "User not authenticated"
//                    )
//                    return@launch
//                }
//
//                val captionToSend = if (newCaptionInput.isNotBlank()) newCaptionInput else originalMemory.caption
//                val imageUrlToSend = newImageUri?.toString() ?: originalMemory.imageUrl
//                val locationToSend: String? = if (addLocationToggled) {
//                    if (!newLocationInput.isNullOrBlank()) newLocationInput else originalMemory.location
//                } else {
//                    null // User explicitly turned off location, so remove it
//                }
//
//                memoryRepository.updateMemory(token, memoryId, captionToSend, imageUrlToSend, locationToSend)
//                    .onSuccess { updatedMemory ->
//                        _singleMemoryUiState.value = SingleMemoryUiState(
//                            memory = updatedMemory,
//                            operationSuccess = true
//                        )
//                        // Refresh the list that HomeView displays
//                        fetchAllMemoriesForFeed() // Or fetchMyMemories() if HomeView shows user-specific list
//                    }
//                    .onFailure { exception ->
//                        _singleMemoryUiState.value = _singleMemoryUiState.value.copy(
//                            isLoading = false,
//                            error = exception.message ?: "Failed to update memory"
//                        )
//                    }
//            } catch (e: Exception) {
//                _singleMemoryUiState.value = _singleMemoryUiState.value.copy(
//                    isLoading = false,
//                    error = e.message ?: "An unexpected error occurred"
//                )
//            }
//        }
//    }

    fun deleteMemory(memoryId: Int, userId: Int) {
        viewModelScope.launch {
            _singleMemoryUiState.value = SingleMemoryUiState(isLoading = true)
            try {
                val token = userRepository.currentUserToken.first()
                if (token == "Unknown") {
                    _singleMemoryUiState.value = SingleMemoryUiState(error = "User not authenticated")
                    return@launch
                }
                memoryRepository.deleteMemory(token, memoryId)
                    .onSuccess {
                        _singleMemoryUiState.value = SingleMemoryUiState(operationSuccess = true)
                        fetchMyMemories() // Refresh list
                    }
                    .onFailure { exception ->
                        _singleMemoryUiState.value = SingleMemoryUiState(error = exception.message ?: "Failed to delete memory")
                    }
            } catch (e: Exception) {
                _singleMemoryUiState.value = SingleMemoryUiState(error = e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun getMemoryById(memoryId: Int) {
        viewModelScope.launch {
            _singleMemoryUiState.value = SingleMemoryUiState(isLoading = true)
            try {
                val token = userRepository.currentUserToken.first()
                if (token == "Unknown") {
                    _singleMemoryUiState.value = SingleMemoryUiState(error = "User not authenticated")
                    return@launch
                }
                memoryRepository.getMemoryById(token, memoryId)
                    .onSuccess { memory ->
                        _singleMemoryUiState.value = SingleMemoryUiState(memory = memory)
                    }
                    .onFailure { exception ->
                        _singleMemoryUiState.value = SingleMemoryUiState(error = exception.message ?: "Failed to fetch memory details")
                    }
            } catch (e: Exception) {
                _singleMemoryUiState.value = SingleMemoryUiState(error = e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun resetSingleMemoryState() {
        _singleMemoryUiState.value = SingleMemoryUiState()
    }

    fun setFetchingLocation(isFetching: Boolean) {
        _singleMemoryUiState.value = _singleMemoryUiState.value.copy(isFetchingLocation = isFetching)
    }

    // Companion object for ViewModel Factory
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MemoMapApplication)
                val memoryRepository = application.container.memoryRepository // Assuming you have this in your AppContainer
                val userRepository = application.container.userRepository
                MemoryViewModel(memoryRepository, userRepository)
            }
        }
    }
}
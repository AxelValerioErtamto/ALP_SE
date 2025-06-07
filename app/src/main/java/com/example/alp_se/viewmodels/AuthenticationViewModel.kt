package com.example.alp_se.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.example.alp_se.MemoMapApplication
import com.example.alp_se.R
import com.example.alp_se.models.ErrorModel
import com.example.alp_se.models.UserResponse
import com.example.alp_se.repositories.AuthenticationRepository
import com.example.alp_se.repositories.UserRepository
import com.example.alp_se.uistates.AuthenticationStatusUIState
import com.example.alp_se.uistates.AuthenticationUIState
import com.example.alp_se.navigation.Screen
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.alp_se.enums.PagesEnum
import com.example.alp_se.models.GeneralResponseModel
import java.io.IOException


import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import kotlin.coroutines.resume // For awaitResult if used
import kotlinx.coroutines.suspendCancellableCoroutine // For awaitResult if used

class AuthenticationViewModel(
    private val authenticationRepository: AuthenticationRepository,
    val userRepository: UserRepository
) : ViewModel() {
    private val _authenticationUIState = MutableStateFlow(AuthenticationUIState())

    val authenticationUIState: StateFlow<AuthenticationUIState>
        get() {
            return _authenticationUIState.asStateFlow()
        }

    var dataStatus: AuthenticationStatusUIState by mutableStateOf(AuthenticationStatusUIState.Start)
        private set

    var usernameInput by mutableStateOf("")
        private set

    var passwordInput by mutableStateOf("")
        private set

    fun changeUsernameInput(usernameInput: String) {
        this.usernameInput = usernameInput
    }

    fun changePasswordInput(passwordInput: String) {
        this.passwordInput = passwordInput
    }

    fun changePasswordVisibility() {
        _authenticationUIState.update { currentState ->
            if (currentState.showPassword) {
                currentState.copy(
                    showPassword = false,
                    passwordVisibility = PasswordVisualTransformation(),
                    passwordVisibilityIcon = R.drawable.ic_password_visible
                )
            } else {
                currentState.copy(
                    showPassword = true,
                    passwordVisibility = VisualTransformation.None,
                    passwordVisibilityIcon = R.drawable.ic_password_invisible
                )
            }
        }
    }

    fun changeConfirmPasswordVisibility() {
        _authenticationUIState.update { currentState ->
            if (currentState.showConfirmPassword) {
                currentState.copy(
                    showConfirmPassword = false,
                    confirmPasswordVisibility = PasswordVisualTransformation(),
                    confirmPasswordVisibilityIcon = R.drawable.ic_password_visible
                )
            } else {
                currentState.copy(
                    showConfirmPassword = true,
                    confirmPasswordVisibility = VisualTransformation.None,
                    confirmPasswordVisibilityIcon = R.drawable.ic_password_invisible
                )
            }
        }
    }

    fun checkLoginForm() {
        if (usernameInput.isNotEmpty() && passwordInput.isNotEmpty()) {
            _authenticationUIState.update { currentState ->
                currentState.copy(
                    buttonEnabled = true
                )
            }
        } else {
            _authenticationUIState.update { currentState ->
                currentState.copy(
                    buttonEnabled = false
                )
            }
        }
    }

    fun checkRegisterForm() {
        if (usernameInput.isNotEmpty() && passwordInput.isNotEmpty() && usernameInput.isNotEmpty()) {
            _authenticationUIState.update { currentState ->
                currentState.copy(
                    buttonEnabled = true
                )
            }
        } else {
            _authenticationUIState.update { currentState ->
                currentState.copy(
                    buttonEnabled = false
                )
            }
        }
    }

    fun checkButtonEnabled(isEnabled: Boolean): Color {
        if (isEnabled) {
            return Color.Blue
        }

        return Color.LightGray
    }

    fun registerUser(navController: NavHostController) {
        viewModelScope.launch {
            dataStatus = AuthenticationStatusUIState.Loading

            try {
                val call = authenticationRepository.register(usernameInput,  passwordInput)
//                dataStatus = UserDataStatusUIState.Success(registerResult)

                call.enqueue(object: Callback<UserResponse>{
                    override fun onResponse(call: Call<UserResponse>, res: Response<UserResponse>) {
                        if (res.isSuccessful) {
                            Log.d("response-data", "RESPONSE DATA: ${res.body()}")

                            //saveUsernameToken(res.body()!!.data.token!!, res.body()!!.data.username)
                            saveUsernameTokenId(res.body()!!.data.token!!, res.body()!!.data.username, res.body()!!.data.id)

                            dataStatus = AuthenticationStatusUIState.Success(res.body()!!.data)

                            resetViewModel()

                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.LoginRegister.route) {
                                    inclusive = true
                                }
                            }
                        } else {
                            // get error message
                            val errorMessage = Gson().fromJson(
                                res.errorBody()!!.charStream(),
                                ErrorModel::class.java
                            )

                            Log.d("error-data", "ERROR DATA: ${errorMessage}")
                            dataStatus = AuthenticationStatusUIState.Failed(errorMessage.errors)
                        }
                    }

                    override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                        Log.d("error-data", "ERROR DATA: ${t.localizedMessage}")
                        dataStatus = AuthenticationStatusUIState.Failed(t.localizedMessage)
                    }

                })
            } catch (error: IOException) {
                dataStatus = AuthenticationStatusUIState.Failed(error.localizedMessage)
                Log.d("register-error", "REGISTER ERROR: ${error.localizedMessage}")
            }
        }
    }

    fun loginUser(
        navController: NavHostController
    ) {
        viewModelScope.launch {
            dataStatus = AuthenticationStatusUIState.Loading
            try {
                val call = authenticationRepository.login(usernameInput, passwordInput)
                call.enqueue(object: Callback<UserResponse> {
                    override fun onResponse(call: Call<UserResponse>, res: Response<UserResponse>) {
                        if (res.isSuccessful) {
                            //saveUsernameToken(res.body()!!.data.token!!, res.body()!!.data.username)
                            saveUsernameTokenId(res.body()!!.data.token!!, res.body()!!.data.username, res.body()!!.data.id)

                            dataStatus = AuthenticationStatusUIState.Success(res.body()!!.data)
                            Log.d("THISNEW", "Username: $usernameInput")
                            Log.d("THISCODEISRAN", "Username: $usernameInput")
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.LoginRegister.route) {
                                    inclusive = true
                                }
                            }
                            resetViewModel()

                        } else {
                            val errorMessage = Gson().fromJson(
                                res.errorBody()!!.charStream(),
                                ErrorModel::class.java
                            )

                            Log.d("error-data", "ERROR DATA: ${errorMessage.errors}")
                            dataStatus = AuthenticationStatusUIState.Failed(errorMessage.errors)
                        }
                    }

                    override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                        dataStatus = AuthenticationStatusUIState.Failed(t.localizedMessage)
                    }

                })
            } catch (error: IOException) {
                dataStatus = AuthenticationStatusUIState.Failed(error.localizedMessage)
                Log.d("register-error", "LOGIN ERROR: ${error.toString()}")
            }
        }
    }

    fun logout(navController: NavHostController) {
        viewModelScope.launch {
            // 1. Get the current token to invalidate it on the server (optional but good practice)
            val currentToken = userRepository.currentUserToken.first() // Get the latest token

            if (currentToken != "Unknown" && currentToken.isNotBlank()) {
                try {
                    // 2. Call the backend API to logout/invalidate the token
                    // We need a way to handle Call<GeneralResponseModel> in a coroutine
                    // Let's assume an awaitResult extension function exists or adapt
                    val call = userRepository.logout(currentToken)
                    // This is a simplified way to handle the call without a full awaitResult here
                    // for brevity. In a real app, you'd use awaitResult or similar.
                    call.enqueue(object : Callback<GeneralResponseModel> {
                        override fun onResponse(call: Call<GeneralResponseModel>, response: Response<GeneralResponseModel>) {
                            if (response.isSuccessful) {
                                Log.d("AuthVM_Logout", "Server logout successful: ${response.body()?.data}")
                            } else {
                                Log.w("AuthVM_Logout", "Server logout failed: ${response.errorBody()?.string()}")
                            }
                            // Proceed to clear local data regardless of server response for robustness
                            clearLocalSessionAndNavigate(navController)
                        }

                        override fun onFailure(call: Call<GeneralResponseModel>, t: Throwable) {
                            Log.e("AuthVM_Logout", "Server logout API call failed: ${t.message}")
                            // Proceed to clear local data regardless of server response
                            clearLocalSessionAndNavigate(navController)
                        }
                    })
                } catch (e: Exception) {
                    Log.e("AuthVM_Logout", "Error during server logout call: ${e.message}")
                    // Proceed to clear local data even if API call setup fails
                    clearLocalSessionAndNavigate(navController)
                }
            } else {
                // No valid token, just clear local session and navigate
                clearLocalSessionAndNavigate(navController)
            }
        }
    }

    private fun clearLocalSessionAndNavigate(navController: NavHostController) {
        viewModelScope.launch {
            // 3. Clear local user data from DataStore
            userRepository.saveUserToken("Unknown") // Or ""
            userRepository.saveUsername("Unknown")  // Or ""
            userRepository.saveUserId(0)            // Or -1

            Log.d("AuthVM_Logout", "Local session cleared.")

            // 4. Navigate to the login screen
            // The LaunchedEffect in MemoMapApp should also pick up the token change,
            // but explicit navigation here ensures immediate redirection.
            navController.navigate(Screen.LoginRegister.route) {
                popUpTo(navController.graph.startDestinationId) { // Pop up to the start of the graph
                    inclusive = true
                }
                launchSingleTop = true // Avoid multiple copies of login screen
            }
            // Reset any ViewModel state related to authentication if necessary
            resetViewModel() // Your existing resetViewModel method
            dataStatus = AuthenticationStatusUIState.Start // Reset dataStatus
        }
    }

    //    fun saveUsernameToken(token: String, username: String) {
//        viewModelScope.launch {
//            userRepository.saveUserToken(token)
//            userRepository.saveUsername(username)
//        }
//    }
    fun saveUsernameTokenId(token: String, username: String, id: Int) {
        viewModelScope.launch {
            userRepository.saveUserToken(token)
            userRepository.saveUsername(username)
            userRepository.saveUserId(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MemoMapApplication)
                val authenticationRepository = application.container.authenticationRepository
                val userRepository = application.container.userRepository
                AuthenticationViewModel(authenticationRepository, userRepository)
            }
        }
    }

    fun resetViewModel() {
        changePasswordInput("")
        changeUsernameInput("")
        _authenticationUIState.update { currentState ->
            currentState.copy(
                showConfirmPassword = false,
                showPassword = false,
                passwordVisibility = PasswordVisualTransformation(),
                confirmPasswordVisibility = PasswordVisualTransformation(),
                passwordVisibilityIcon = R.drawable.ic_password_visible,
                confirmPasswordVisibilityIcon = R.drawable.ic_password_visible,
                buttonEnabled = false
            )
        }
        dataStatus = AuthenticationStatusUIState.Start
    }

    fun clearErrorMessage() {
        dataStatus = AuthenticationStatusUIState.Start
    }
}
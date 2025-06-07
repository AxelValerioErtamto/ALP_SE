package com.example.alp_se.views


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource // Required for painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Required for viewModel()
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.alp_se.repositories.MockUserRepository
import com.example.alp_se.viewmodels.AuthenticationViewModel // Using the provided ViewModel
import com.example.alp_se.uistates.AuthenticationStatusUIState // Using the provided UI state
// PagesEnum will be used by the ViewModel for navigation, no direct import needed here unless for type safety in NavController args if any.

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.alp_se.models.User
import com.example.alp_se.models.UserResponse
import com.example.alp_se.repositories.AuthenticationRepository
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterView(
    navController: NavHostController,
    // Use the provided AuthenticationViewModel and its factory
    authenticationViewModel: AuthenticationViewModel = viewModel(factory = AuthenticationViewModel.Factory)
) {
    // Collect states from AuthenticationViewModel
    val authUiState by authenticationViewModel.authenticationUIState.collectAsState()
    // dataStatus is a mutableStateOf property in the ViewModel, not a Flow to be collected with collectAsState directly here.
    // We access it directly: authenticationViewModel.dataStatus
    val dataStatus = authenticationViewModel.dataStatus

    // Use usernameInput and passwordInput from the ViewModel
    val usernameInput = authenticationViewModel.usernameInput
    val passwordInput = authenticationViewModel.passwordInput

    val context = LocalContext.current

    // Effect to handle data status changes (Success, Error messages)
    // Navigation is handled within the ViewModel's loginUser/registerUser methods.
    LaunchedEffect(dataStatus) {
        when (dataStatus) {
            is AuthenticationStatusUIState.Success -> {
                // ViewModel handles navigation and resetting state.
                // We can show a generic success message or rely on navigation as feedback.
                // The original view showed a specific message, let's adapt that.
                Toast.makeText(context, "Operation Successful for ${dataStatus.userModelData.username}", Toast.LENGTH_SHORT).show()
                // ViewModel calls resetViewModel() internally on success.
            }
            is AuthenticationStatusUIState.Failed -> {
                Toast.makeText(context, "Error: ${dataStatus.errorMessage}", Toast.LENGTH_LONG).show()
                authenticationViewModel.clearErrorMessage() // Reset error message to allow user to try again
            }
            else -> Unit // Idle (Start), Loading. Loading is handled by UI below.
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MemoMap", style = MaterialTheme.typography.headlineMedium) // App name or title
        Spacer(modifier = Modifier.height(32.dp))

        // Username TextField
        OutlinedTextField(
            value = usernameInput,
            onValueChange = {
                authenticationViewModel.changeUsernameInput(it)
                // ViewModel's checkLoginForm/checkRegisterForm updates buttonEnabled state.
                // Both checks are similar; using checkLoginForm for simplicity as it covers non-empty username/password.
                authenticationViewModel.checkLoginForm()
            },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Password TextField
        OutlinedTextField(
            value = passwordInput,
            onValueChange = {
                authenticationViewModel.changePasswordInput(it)
                authenticationViewModel.checkLoginForm()
            },
            label = { Text("Password") },
            visualTransformation = authUiState.passwordVisibility, // From ViewModel's UI state
            trailingIcon = {
                IconButton(onClick = { authenticationViewModel.changePasswordVisibility() }) {
                    Icon(
                        painter = painterResource(id = authUiState.passwordVisibilityIcon),
                        contentDescription = if (authUiState.showPassword) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Loading Indicator or Buttons
        if (dataStatus == AuthenticationStatusUIState.Loading) {
            CircularProgressIndicator()
        } else {
            // Login Button
            Button(
                onClick = {
                    // ViewModel's loginUser method handles the logic and navigation
                    authenticationViewModel.loginUser(navController)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authUiState.buttonEnabled // Use buttonEnabled from ViewModel's UI state
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Register Button
            OutlinedButton(
                onClick = {
                    // ViewModel's registerUser method handles the logic and navigation
                    authenticationViewModel.registerUser(navController)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authUiState.buttonEnabled // Use buttonEnabled from ViewModel's UI state
                // As checkRegisterForm is similar to checkLoginForm,
                // buttonEnabled should work for both.
            ) {
                Text("Register")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleLoginRegisterPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MemoMap", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Register")
                }
            }
        }
    }
}

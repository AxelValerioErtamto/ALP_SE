package com.example.alp_se.uistates

import com.example.alp_se.models.User

// exhaustive (covers all scenario possibilities) interface
sealed interface AuthenticationStatusUIState {
    data class Success(val userModelData: User): AuthenticationStatusUIState
    object Loading: AuthenticationStatusUIState
    object Start: AuthenticationStatusUIState
    data class Failed(val errorMessage: String): AuthenticationStatusUIState
}

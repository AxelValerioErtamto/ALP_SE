package com.example.alp_se.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.alp_se.models.GeneralResponseModel
import com.example.alp_se.services.UserAPIService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okio.Timeout
import retrofit2.Call

interface UserRepository {
    val currentUserToken: Flow<String>
    val currentUsername: Flow<String>
    val currentUserId: Flow<Int>

    fun logout(token: String): Call<GeneralResponseModel>

    suspend fun saveUserToken(token: String)

    suspend fun saveUsername(username: String)

    suspend fun saveUserId(id: Int)

    suspend fun clearUserSession() // Add this if you added the method to NetworkUserRepository

}

class NetworkUserRepository(
    private val userDataStore: DataStore<Preferences>,
    private val userAPIService: UserAPIService
): UserRepository {
    private companion object {
        val USER_TOKEN = stringPreferencesKey("token")
        val USERNAME = stringPreferencesKey("username")
        val USER_ID = stringPreferencesKey("id")
    }

    override val currentUserToken: Flow<String> = userDataStore.data.map { preferences ->
        preferences[USER_TOKEN] ?: "Unknown"
    }

    override val currentUsername: Flow<String> = userDataStore.data.map { preferences ->
        preferences[USERNAME] ?: "Unknown"
    }

    override val currentUserId: Flow<Int> = userDataStore.data.map { preferences ->
        preferences[USER_ID]?.toIntOrNull() ?: 0
    }

    override suspend fun saveUserToken(token: String) {
        userDataStore.edit { preferences ->
            preferences[USER_TOKEN] = token
        }
    }

    override suspend fun saveUsername(username: String) {
        userDataStore.edit { preferences ->
            preferences[USERNAME] = username
        }
    }

    override suspend fun saveUserId(id: Int) {
        userDataStore.edit { preferences ->
            preferences[USER_ID] = id.toString()
        }
    }

    override fun logout(token: String): Call<GeneralResponseModel> {
        return userAPIService.logout(token)
    }

    override suspend fun clearUserSession() {
        saveUserToken("Unknown")
        saveUsername("Unknown")
        saveUserId(0)
    }
}

class MockUserRepository : UserRepository {
    private var mockUserToken: String = "mockToken"
    private var mockUsername: String = "mockUsername"
    private var mockUserId: Int = 1

    override val currentUserToken: Flow<String> = flow {
        emit(mockUserToken)
    }

    override val currentUsername: Flow<String> = flow {
        emit(mockUsername)
    }

    override val currentUserId: Flow<Int> = flow {
        emit(mockUserId)
    }

    override suspend fun saveUserToken(token: String) {
        mockUserToken = token
    }

    override suspend fun saveUsername(username: String) {
        mockUsername = username
    }

    override suspend fun saveUserId(id: Int) {
        mockUserId = id
    }

    override fun logout(token: String): Call<GeneralResponseModel> {
        val mockResponse = GeneralResponseModel("Logged out successfully")
        return object : Call<GeneralResponseModel> {
            override fun enqueue(callback: retrofit2.Callback<GeneralResponseModel>) {
                callback.onResponse(this, retrofit2.Response.success(mockResponse))
            }

            override fun isExecuted(): Boolean = false
            override fun clone(): Call<GeneralResponseModel> = this
            override fun execute(): retrofit2.Response<GeneralResponseModel> {
                return retrofit2.Response.success(mockResponse)
            }

            override fun cancel() {}
            override fun isCanceled(): Boolean = false
            override fun request(): okhttp3.Request = okhttp3.Request.Builder().url("http://example.com").build()
            override fun timeout(): Timeout {
                return Timeout.NONE
            }
        }
    }

    // Implement the missing clearUserSession method
    override suspend fun clearUserSession() {
        // Simulate clearing the session by resetting mock values to defaults
        // or to "logged out" states.
        mockUserToken = "Unknown" // Or whatever your logged-out default is
        mockUsername = "Unknown"  // Or ""
        mockUserId = 0            // Or -1
        // Since the Flows emit the current mock values, changing these values
        // will effectively "clear" the session for any collectors of these mock flows
        // on subsequent emissions (though these simple flows only emit once).
        // For more dynamic mock flows, you might re-emit these new values.
    }
}
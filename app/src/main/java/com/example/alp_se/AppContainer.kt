package com.example.alp_se

import android.content.Context // Import Context if not already present
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.alp_se.repositories.AuthenticationRepository
import com.example.alp_se.repositories.MemoryRepository // Added import
import com.example.alp_se.repositories.NetworkAuthenticationRepository
import com.example.alp_se.repositories.NetworkUserRepository
import com.example.alp_se.repositories.UserRepository
import com.example.alp_se.services.AuthenticationAPIService
import com.example.alp_se.services.MemoryAPIService // Added import
import com.example.alp_se.services.UserAPIService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface AppContainer {
    val authenticationRepository: AuthenticationRepository
    val userRepository: UserRepository
    val memoryRepository: MemoryRepository // Added MemoryRepository
}

class DefaultAppContainer(
    private val userDataStore: DataStore<Preferences>
    // Consider passing Context here if needed for other initializations,
    // though not strictly required for what's shown.
    // private val context: Context
) : AppContainer {
    //    private val baseUrl = "http://192.168.18.252:3000/"
    //    private val baseUrl = "http://192.168.232.233:3000/"
    //    private val basexxzxzzzzzzzUrl = "http://192.168.56.1:3000" // For emulator to host machine
    private val baseUrl = "http://192.168.213.70:3000/" // Standard emulator loopback for localhost

    private val retrofit: Retrofit by lazy { createRetrofit() }

    private val authenticationAPIService: AuthenticationAPIService by lazy {
        retrofit.create(AuthenticationAPIService::class.java)
    }

    private val userAPIService: UserAPIService by lazy {
        retrofit.create(UserAPIService::class.java)
    }

    // Added MemoryAPIService
    private val memoryAPIService: MemoryAPIService by lazy {
        retrofit.create(MemoryAPIService::class.java)
    }

    override val authenticationRepository: AuthenticationRepository by lazy {
        NetworkAuthenticationRepository(authenticationAPIService)
    }

    override val userRepository: UserRepository by lazy {
        NetworkUserRepository(userDataStore, userAPIService)
    }

    // Added MemoryRepository
    override val memoryRepository: MemoryRepository by lazy {
        MemoryRepository(memoryAPIService)
    }

    private fun createRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Logs request and response lines and their respective headers and bodies (if present).
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // You might want to add timeouts here
            // .connectTimeout(30, TimeUnit.SECONDS)
            // .readTimeout(30, TimeUnit.SECONDS)
            // .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create()) // Ensure Gson is correctly handling your models
            .client(client)
            .build()
    }
}
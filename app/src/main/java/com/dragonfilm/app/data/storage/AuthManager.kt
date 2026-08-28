package com.dragonfilm.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dragonfilm_auth", Context.MODE_PRIVATE)
    private val gson = ApiClient.gson

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    val isLoggedIn: Boolean
        get() = _token.value != null && _currentUser.value != null

    init {
        loadStoredSession()
    }

    private fun loadStoredSession() {
        val savedToken = prefs.getString("auth_token", null)
        val savedUserJson = prefs.getString("auth_user", null)
        _token.value = savedToken
        if (!savedUserJson.isNullOrEmpty()) {
            _currentUser.value = try {
                gson.fromJson(savedUserJson, User::class.java)
            } catch (_: Exception) { null }
        }
    }

    fun setSession(token: String, user: User) {
        _token.value = token
        _currentUser.value = user
        prefs.edit()
            .putString("auth_token", token)
            .putString("auth_user", gson.toJson(user))
            .apply()
    }

    fun updateProfile(user: User) {
        _currentUser.value = user
        prefs.edit().putString("auth_user", gson.toJson(user)).apply()
    }

    fun logout() {
        _token.value = null
        _currentUser.value = null
        prefs.edit().clear().apply()
    }

    suspend fun refreshProfile(): Boolean {
        val currentToken = _token.value ?: return false
        return try {
            val resp = ApiClient.service.getProfile("Bearer $currentToken")
            if (resp.ok && resp.user != null) {
                updateProfile(resp.user)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}

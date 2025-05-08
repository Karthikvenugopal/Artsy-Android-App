package com.example.artsyandroid.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

object AuthManager {
    private const val PREFS = "auth_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_PROFILE_IMAGE = "profile_image_url"

    fun saveToken(context: Context, token: String) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit() {
                putString(KEY_TOKEN, token)
            }
    }

    fun clearToken(context: Context) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit() {
                remove(KEY_TOKEN)
            }
    }

    fun getToken(context: Context): String? =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

    fun saveProfileImage(context: Context, url: String) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_PROFILE_IMAGE, url) }
    }
    fun getProfileImage(context: Context): String? =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE_IMAGE, null)
    fun clearProfileImage(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_PROFILE_IMAGE)
        }
    }
    @Composable
    fun isLoggedIn(): Boolean {
        val ctx = LocalContext.current
        return rememberUpdatedState(
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null) != null
        ).value
    }

}


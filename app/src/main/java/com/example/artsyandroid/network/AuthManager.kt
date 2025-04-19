// AuthManager.kt
package com.example.artsyandroid.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

// in AuthManager.kt
object AuthManager {
    private const val PREFS = "auth_prefs"
    private const val KEY_TOKEN = "jwt_token"

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

    /**
     * A Compose‑friendly way to observe login state.
     * It just re‑reads the prefs on every recomposition,
     * so after you call saveToken()/clearToken() the UI will update.
     */
    // in AuthManager.kt
    @Composable
    fun isLoggedIn(): Boolean {
        val ctx = LocalContext.current
        // This will re‑read prefs whenever the composition invalidates
        return rememberUpdatedState(
            ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("jwt_token", null) != null
        ).value
    }

}


package com.dhan.app.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps Google Sign-In just enough to obtain an OAuth token scoped to
 * drive.appdata — Dhan never requests full Drive access, only the hidden
 * per-app folder. Requires the user's own Google Cloud project (Drive API enabled, this
 * app's SHA-1 registered) — see PROJECT_STATUS.md for that one-time setup, which only the
 * user's Google account can do.
 */
class GoogleAuthManager(private val context: Context) {
    private val client: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(SCOPE_DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(): Intent = client.signInIntent

    fun currentAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    @Throws(ApiException::class)
    fun accountFromSignInResult(data: Intent?): GoogleSignInAccount =
        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)

    suspend fun signOut() = withContext(Dispatchers.IO) {
        Tasks.await(client.signOut())
    }

    /** Network + AccountManager I/O — must run off the main thread. */
    suspend fun getAccessToken(account: GoogleSignInAccount): String = withContext(Dispatchers.IO) {
        val androidAccount = account.account
            ?: throw IllegalStateException("Signed-in Google account is missing its Android Account handle")
        GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$SCOPE_DRIVE_APPDATA")
    }

    /** Call after a 401 from Drive so the next getAccessToken() fetches a fresh token
     *  instead of handing back the same expired one from cache. */
    fun invalidateToken(token: String) {
        GoogleAuthUtil.clearToken(context, token)
    }

    companion object {
        private const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    }
}

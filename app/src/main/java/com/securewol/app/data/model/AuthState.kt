package com.securewol.app.data.model

/**
 * AuthState: Represents the lifecycle state of user authentication.
 */
sealed class AuthState {
    object SetupRequired : AuthState()
    object Unauthenticated : AuthState()
    data class LockedOut(val remainingSeconds: Long) : AuthState()
    object Authenticated : AuthState()
}

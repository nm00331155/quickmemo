package com.quickmemo.app.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthenticator @Inject constructor() {

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        onResult: (Boolean) -> Unit,
    ) {
        val allowedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = BiometricManager.from(activity)
            .canAuthenticate(allowedAuthenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(false)
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    onResult(false)
                }
            },
        )

        biometricPrompt.authenticate(promptInfo)
    }
}

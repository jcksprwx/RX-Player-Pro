package com.rxplayer.pro.core.security

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun canAuthenticate(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
            }
        } catch (e: Throwable) {
            Log.w("BiometricAuthManager", "Biometric check not supported or threw exception", e)
            false
        }
    }

    fun promptAuthenticate(
        activity: FragmentActivity,
        title: String = "RX Player Pro Lock",
        subtitle: String = "Authenticate to access media library",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Please try again.")
                }
            })

            val promptBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                promptBuilder.setNegativeButtonText("Cancel")
            }

            prompt.authenticate(promptBuilder.build())
        } catch (e: Throwable) {
            Log.e("BiometricAuthManager", "Failed to launch biometric prompt", e)
            // If biometrics fail unexpectedly, fallback to allowing user in
            onSuccess()
        }
    }
}

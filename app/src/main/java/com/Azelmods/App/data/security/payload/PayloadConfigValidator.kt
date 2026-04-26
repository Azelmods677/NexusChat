package com.Azelmods.App.data.security.payload

import android.util.Patterns
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validator for payload configuration
 * 
 * Validates:
 * - LHOST format (IP address or hostname)
 * - LPORT range [1, 65535]
 * - Custom package name format
 * - msfvenom binary existence
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
@Singleton
class PayloadConfigValidator @Inject constructor(
    private val msfvenomWrapper: MsfvenomWrapper
) {
    
    companion object {
        private const val MIN_PORT = 1
        private const val MAX_PORT = 65535
        private val PACKAGE_NAME_REGEX = Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$""")
    }
    
    /**
     * Validates payload configuration
     * 
     * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
     */
    fun validate(config: PayloadConfig): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate LHOST
        val lhostError = validateLhost(config.lhost)
        if (lhostError != null) {
            errors.add(lhostError)
        }
        
        // Validate LPORT
        val lportError = validateLport(config.lport)
        if (lportError != null) {
            errors.add(lportError)
        }
        
        // Validate custom package name if provided
        if (config.customPackageName != null) {
            val packageError = validatePackageName(config.customPackageName)
            if (packageError != null) {
                errors.add(packageError)
            }
        }
        
        // Validate msfvenom binary
        val msfvenomValidation = msfvenomWrapper.validateMsfvenomBinary()
        if (msfvenomValidation is ValidationResult.Invalid) {
            errors.addAll(msfvenomValidation.errors)
        }
        
        // Validate template APK if high obfuscation is enabled
        if (config.obfuscationLevel == ObfuscationLevel.HIGH || 
            config.obfuscationLevel == ObfuscationLevel.EXTREME) {
            if (config.templateApkPath == null) {
                errors.add(
                    ValidationError(
                        field = "templateApk",
                        message = "Template APK is required for high obfuscation levels"
                    )
                )
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validates LHOST format
     * 
     * Accepts:
     * - IPv4 addresses (e.g., 192.168.1.100)
     * - IPv6 addresses (e.g., ::1)
     * - Hostnames (e.g., attacker.example.com)
     * 
     * Requirement: 8.1
     */
    private fun validateLhost(lhost: String): ValidationError? {
        if (lhost.isBlank()) {
            return ValidationError(
                field = "lhost",
                message = "LHOST cannot be empty"
            )
        }
        
        // Check if it's a valid IP address
        val isValidIp = try {
            InetAddress.getByName(lhost)
            true
        } catch (e: Exception) {
            false
        }
        
        // Check if it's a valid hostname
        val isValidHostname = Patterns.DOMAIN_NAME.matcher(lhost).matches()
        
        if (!isValidIp && !isValidHostname) {
            return ValidationError(
                field = "lhost",
                message = "LHOST must be a valid IP address or hostname"
            )
        }
        
        // Warn about localhost/loopback addresses
        if (lhost == "localhost" || lhost == "127.0.0.1" || lhost == "::1") {
            return ValidationError(
                field = "lhost",
                message = "Warning: Using localhost address. This will only work on the same device."
            )
        }
        
        return null
    }
    
    /**
     * Validates LPORT range
     * 
     * Port must be in range [1, 65535]
     * 
     * Requirement: 8.2
     */
    private fun validateLport(lport: Int): ValidationError? {
        if (lport < MIN_PORT || lport > MAX_PORT) {
            return ValidationError(
                field = "lport",
                message = "LPORT must be between $MIN_PORT and $MAX_PORT"
            )
        }
        
        // Warn about privileged ports (< 1024)
        if (lport < 1024) {
            return ValidationError(
                field = "lport",
                message = "Warning: Port $lport requires root privileges on most systems"
            )
        }
        
        // Warn about commonly used ports
        val commonPorts = mapOf(
            80 to "HTTP",
            443 to "HTTPS",
            22 to "SSH",
            21 to "FTP",
            25 to "SMTP",
            3306 to "MySQL",
            5432 to "PostgreSQL"
        )
        
        if (lport in commonPorts) {
            return ValidationError(
                field = "lport",
                message = "Warning: Port $lport is commonly used for ${commonPorts[lport]}"
            )
        }
        
        return null
    }
    
    /**
     * Validates custom package name format
     * 
     * Package name must follow Android package naming conventions:
     * - Lowercase letters, digits, underscores
     * - At least two segments separated by dots
     * - Each segment must start with a letter
     * 
     * Requirement: 8.3
     */
    private fun validatePackageName(packageName: String): ValidationError? {
        if (packageName.isBlank()) {
            return ValidationError(
                field = "packageName",
                message = "Package name cannot be empty"
            )
        }
        
        if (!PACKAGE_NAME_REGEX.matches(packageName)) {
            return ValidationError(
                field = "packageName",
                message = "Invalid package name format. Must be lowercase with at least two segments (e.g., com.example.app)"
            )
        }
        
        // Check for reserved package names
        val reservedPrefixes = listOf(
            "android.",
            "com.android.",
            "java.",
            "javax."
        )
        
        if (reservedPrefixes.any { packageName.startsWith(it) }) {
            return ValidationError(
                field = "packageName",
                message = "Package name cannot start with reserved prefixes (android., java., javax.)"
            )
        }
        
        return null
    }
    
    /**
     * Quick validation for UI feedback
     * Returns first error found or null if valid
     */
    fun quickValidate(config: PayloadConfig): String? {
        val result = validate(config)
        return if (result is ValidationResult.Invalid && result.errors.isNotEmpty()) {
            result.errors.first().message
        } else {
            null
        }
    }
}

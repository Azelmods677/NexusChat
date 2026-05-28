package com.Azelmods.App.ui.screens.auth

import com.Azelmods.App.data.model.User
import com.Azelmods.App.domain.usecase.auth.GoogleLoginUseCase
import com.Azelmods.App.domain.usecase.auth.LoginUseCase
import com.Azelmods.App.util.Resource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain


/**
 * Unit tests for LoginViewModel.
 *
 * Validates:
 * - Initial state
 * - Email/password validation logic
 * - Successful login
 * - Login failure
 * - clearError() behavior
 */
class LoginViewModelTest : StringSpec({

    val loginUseCase = mockk<LoginUseCase>(relaxed = true)
    val googleLoginUseCase = mockk<GoogleLoginUseCase>(relaxed = true)

    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    afterTest {
        unmockkAll()
        Dispatchers.resetMain()
    }

    "initial state should be empty" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.state.value.let { state ->
            state.email shouldBe ""
            state.password shouldBe ""
            state.isLoading shouldBe false
            state.error shouldBe null
            state.isSuccess shouldBe false
        }
    }

    "onEmailChange should update email and clear error" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.onEmailChange("test@example.com")
        viewModel.state.value.email shouldBe "test@example.com"
        viewModel.state.value.error shouldBe null
    }

    "onPasswordChange should update password and clear error" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.onPasswordChange("password123")
        viewModel.state.value.password shouldBe "password123"
        viewModel.state.value.error shouldBe null
    }

    "login with blank email should show error" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.login()
        viewModel.state.value.error shouldBe "Email is required"
        viewModel.state.value.isLoading shouldBe false
    }

    "login with invalid email should show error" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.onEmailChange("invalid")
        viewModel.login()
        viewModel.state.value.error shouldBe "Invalid email format"
    }

    "login with blank password should show error" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.onEmailChange("test@example.com")
        viewModel.login()
        viewModel.state.value.error shouldBe "Password is required"
    }

    "login with short password should show error" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("12345")
        viewModel.login()
        viewModel.state.value.error shouldBe "Password must be at least 6 characters"
    }

    "login with valid credentials should call loginUseCase" {
        runTest {
            val mockUser = mockk<User>(relaxed = true)
            coEvery { loginUseCase.invoke("test@example.com", "password123") } returns Resource.Success(mockUser)

            val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

            viewModel.onEmailChange("test@example.com")
            viewModel.onPasswordChange("password123")
            viewModel.login()

            coVerify { loginUseCase.invoke("test@example.com", "password123") }
        }
    }

    "login success should set isSuccess to true" {
        runTest {
            val mockUser = mockk<User>(relaxed = true)
            coEvery { loginUseCase.invoke(any(), any()) } returns Resource.Success(mockUser)

            val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

            viewModel.onEmailChange("test@example.com")
            viewModel.onPasswordChange("password123")
            viewModel.login()

            viewModel.state.value.isSuccess shouldBe true
            viewModel.state.value.isLoading shouldBe false
        }
    }

    "login failure should set error message" {
        runTest {
            coEvery { loginUseCase.invoke(any(), any()) } returns Resource.Error("Invalid credentials")

            val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

            viewModel.onEmailChange("test@example.com")
            viewModel.onPasswordChange("wrongpass")
            viewModel.login()

            viewModel.state.value.error shouldBe "Invalid credentials"
            viewModel.state.value.isSuccess shouldBe false
        }
    }

    "clearError should reset error to null" {
        val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

        viewModel.onEmailChange("test@example.com")
        viewModel.login()
        viewModel.state.value.error shouldNotBe null

        viewModel.clearError()
        viewModel.state.value.error shouldBe null
    }

    "error state should include login failure type" {
        runTest {
            coEvery { loginUseCase.invoke(any(), any()) } returns Resource.Error("Network error")

            val viewModel = createViewModel(loginUseCase, googleLoginUseCase)

            viewModel.onEmailChange("test@example.com")
            viewModel.onPasswordChange("password123")
            viewModel.login()

            viewModel.state.value.let { state ->
                state.error shouldBe "Network error"
                state.isLoading shouldBe false
                state.isSuccess shouldBe false
            }
        }
    }
})

/**
 * Factory helper to create a LoginViewModel with mocked dependencies.
 */
private fun createViewModel(
    loginUseCase: LoginUseCase,
    googleLoginUseCase: GoogleLoginUseCase
): LoginViewModel {
    val credentialManager = mockk<androidx.credentials.CredentialManager>(relaxed = true)
    val context = mockk<android.content.Context>(relaxed = true)

    return LoginViewModel(
        loginUseCase = loginUseCase,
        googleLoginUseCase = googleLoginUseCase,
        credentialManager = credentialManager,
        googleClientId = "test-client-id",
        context = context
    )
}

package com.example.study_mentor.ui

import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {
    @Test fun emptyEmailIsRejected() {
        assertNotNull(AuthValidation.emailError(""))
    }

    @Test fun invalidEmailIsRejected() {
        assertNotNull(AuthValidation.emailError("not-an-email"))
    }

    @Test fun validEmailIsAccepted() {
        assertNull(AuthValidation.emailError("student@example.com"))
    }

    @Test fun emailWithSurroundingSpacesIsAccepted() {
        assertNull(AuthValidation.emailError(" student@example.com "))
    }

    @Test fun blankNameIsRejected() {
        assertNotNull(AuthValidation.nameError("   "))
    }

    @Test fun shortNameIsRejected() {
        assertNotNull(AuthValidation.nameError("A"))
    }

    @Test fun validNameIsAccepted() {
        assertNull(AuthValidation.nameError("Study Mentor"))
    }

    @Test fun emptyLoginPasswordIsRejected() {
        assertNotNull(AuthValidation.loginPasswordError(""))
    }

    @Test fun existingShortLoginPasswordIsNotBlocked() {
        assertNull(AuthValidation.loginPasswordError("abc"))
    }

    @Test fun weakRegistrationPasswordIsRejected() {
        assertNotNull(AuthValidation.registrationPasswordError("123456"))
    }

    @Test fun strongRegistrationPasswordIsAccepted() {
        assertNull(AuthValidation.registrationPasswordError("Learn2026!"))
    }

    @Test fun missingPasswordConfirmationIsRejected() {
        assertNotNull(AuthValidation.confirmPasswordError("Learn2026!", ""))
    }

    @Test fun mismatchedPasswordsAreRejected() {
        assertNotNull(AuthValidation.confirmPasswordError("Learn2026!", "Other2026!"))
    }

    @Test fun matchingPasswordsAreAccepted() {
        assertNull(AuthValidation.confirmPasswordError("Learn2026!", "Learn2026!"))
    }

    @Test fun passwordStrengthIsBounded() {
        assertEquals(0, AuthValidation.passwordStrength(""))
        assertEquals(4, AuthValidation.passwordStrength("Learn2026!"))
    }
}

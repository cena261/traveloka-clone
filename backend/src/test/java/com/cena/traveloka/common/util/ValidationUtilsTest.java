package com.cena.traveloka.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for ValidationUtils common validation patterns.
 * Tests email validation, phone number validation, password strength, and input sanitization.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * ValidationUtils implementation does not exist yet.
 */
class ValidationUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "test@example.com",
        "user.name@domain.co.uk",
        "firstname+lastname@example.com",
        "email@123.123.123.123", // IP address
        "user@subdomain.example.com",
        "user_name@example-domain.com"
    })
    void shouldValidateCorrectEmailFormats(String email) {
        // When: Validating correct email formats
        boolean isValid = ValidationUtils.isValidEmail(email);

        // Then: Should return true for valid emails
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid.email",
        "@example.com",
        "user@",
        "user..name@example.com", // double dot
        "user@.example.com", // domain starts with dot
        "user@example..com", // double dot in domain
        "user name@example.com", // space in local part
        "user@example.com.", // trailing dot
        ""
    })
    @NullAndEmptySource
    void shouldRejectInvalidEmailFormats(String email) {
        // When: Validating invalid email formats
        boolean isValid = ValidationUtils.isValidEmail(email);

        // Then: Should return false for invalid emails
        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "+84901234567", // Vietnam mobile
        "+84912345678", // Vietnam mobile
        "+84123456789", // Vietnam landline
        "+1234567890", // International format
        "+442071234567", // UK format
        "0901234567", // Vietnam domestic mobile
        "0123456789" // Vietnam domestic landline
    })
    void shouldValidateCorrectPhoneNumbers(String phoneNumber) {
        // When: Validating correct phone number formats
        boolean isValid = ValidationUtils.isValidPhoneNumber(phoneNumber);

        // Then: Should return true for valid phone numbers
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123", // too short
        "abcdefghij", // letters only
        "123-456-789", // contains hyphens
        "+", // just plus sign
        "++84901234567", // double plus
        "084901234567", // starts with 0 but has country code length
        "12345678901234567890", // too long
        ""
    })
    @NullAndEmptySource
    void shouldRejectInvalidPhoneNumbers(String phoneNumber) {
        // When: Validating invalid phone number formats
        boolean isValid = ValidationUtils.isValidPhoneNumber(phoneNumber);

        // Then: Should return false for invalid phone numbers
        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "Password123!, STRONG",
        "MySecurePass1!, STRONG",
        "ComplexP@ssw0rd, STRONG",
        "SimplePass123, MEDIUM",
        "password123, MEDIUM",
        "PASSWORD123, MEDIUM",
        "Pass123, WEAK",
        "password, WEAK",
        "12345678, WEAK",
        "abc, WEAK"
    })
    void shouldEvaluatePasswordStrength(String password, String expectedStrength) {
        // When: Evaluating password strength
        ValidationUtils.PasswordStrength strength = ValidationUtils.evaluatePasswordStrength(password);

        // Then: Should return correct strength level
        assertThat(strength.toString()).isEqualTo(expectedStrength);
    }

    @Test
    void shouldValidatePasswordRequirements() {
        // Given: Password requirements
        String strongPassword = "MySecurePass123!";
        String weakPassword = "123";

        // When: Checking password requirements
        boolean strongMeetsLength = ValidationUtils.meetsMinimumLength(strongPassword, 8);
        boolean strongHasUpper = ValidationUtils.hasUppercaseLetters(strongPassword);
        boolean strongHasLower = ValidationUtils.hasLowercaseLetters(strongPassword);
        boolean strongHasDigits = ValidationUtils.hasDigits(strongPassword);
        boolean strongHasSpecial = ValidationUtils.hasSpecialCharacters(strongPassword);

        boolean weakMeetsLength = ValidationUtils.meetsMinimumLength(weakPassword, 8);

        // Then: Strong password meets all requirements
        assertThat(strongMeetsLength).isTrue();
        assertThat(strongHasUpper).isTrue();
        assertThat(strongHasLower).isTrue();
        assertThat(strongHasDigits).isTrue();
        assertThat(strongHasSpecial).isTrue();

        // And weak password fails requirements
        assertThat(weakMeetsLength).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "'Hello World', 'Hello World'", // no special chars
        "'<script>alert(\"xss\")</script>', '&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;'", // XSS attempt
        "'John & Jane', 'John &amp; Jane'", // ampersand
        "'Price: $100 < $200', 'Price: $100 &lt; $200'", // less than
        "'5 > 3', '5 &gt; 3'", // greater than
        "'\"Hello\"', '&quot;Hello&quot;'", // quotes
        "'', ''" // empty string
    })
    void shouldSanitizeHtmlInput(String input, String expected) {
        // When: Sanitizing HTML input
        String sanitized = ValidationUtils.sanitizeHtml(input);

        // Then: Should escape HTML special characters
        assertThat(sanitized).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "'SELECT * FROM users', 'SELECT * FROM users'", // simple select (allowed)
        "'Robert'); DROP TABLE students;--', 'Robert''); DROP TABLE students;--'", // SQL injection attempt
        "'1'' OR ''1''=''1', '1'''' OR ''''1''''=''''1'", // single quote escape
        "'admin\"\"--', 'admin\"\"--'", // comment injection
        "'user; DELETE FROM accounts;', 'user; DELETE FROM accounts;'" // semicolon injection
    })
    void shouldSanitizeSqlInput(String input, String expected) {
        // When: Sanitizing SQL input
        String sanitized = ValidationUtils.sanitizeSql(input);

        // Then: Should escape SQL special characters
        assertThat(sanitized).isEqualTo(expected);
    }

    @Test
    void shouldValidateIdFormats() {
        // Given: Various ID formats
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";
        String validNumericId = "12345";
        String invalidUuid = "invalid-uuid-format";
        String invalidId = "";

        // When: Validating ID formats
        boolean isValidUuid = ValidationUtils.isValidUuid(validUuid);
        boolean isValidNumericId = ValidationUtils.isValidNumericId(validNumericId);
        boolean isInvalidUuid = ValidationUtils.isValidUuid(invalidUuid);
        boolean isInvalidId = ValidationUtils.isValidNumericId(invalidId);

        // Then: Should correctly validate ID formats
        assertThat(isValidUuid).isTrue();
        assertThat(isValidNumericId).isTrue();
        assertThat(isInvalidUuid).isFalse();
        assertThat(isInvalidId).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com", // valid email
        "User@Example.Com", // mixed case
        "  user@example.com  ", // with spaces
        "USER@EXAMPLE.COM" // uppercase
    })
    void shouldNormalizeEmailAddresses(String email) {
        // When: Normalizing email addresses
        String normalized = ValidationUtils.normalizeEmail(email);

        // Then: Should return lowercase, trimmed email
        assertThat(normalized).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "+84901234567", // international format
        "0901234567", // domestic format
        "+84 90 123 4567", // with spaces
        "+84-90-123-4567", // with hyphens
        "(+84) 90 123 4567" // with parentheses
    })
    void shouldNormalizePhoneNumbers(String phoneNumber) {
        // When: Normalizing phone numbers
        String normalized = ValidationUtils.normalizePhoneNumber(phoneNumber);

        // Then: Should return digits only with country code
        assertThat(normalized).isEqualTo("+84901234567");
    }

    @Test
    void shouldValidateRequiredFields() {
        // Given: Various input scenarios
        String validInput = "Valid Input";
        String emptyInput = "";
        String whitespaceInput = "   ";
        String nullInput = null;

        // When: Validating required fields
        boolean validIsRequired = ValidationUtils.isRequired(validInput);
        boolean emptyIsRequired = ValidationUtils.isRequired(emptyInput);
        boolean whitespaceIsRequired = ValidationUtils.isRequired(whitespaceInput);
        boolean nullIsRequired = ValidationUtils.isRequired(nullInput);

        // Then: Should correctly identify required field violations
        assertThat(validIsRequired).isTrue();
        assertThat(emptyIsRequired).isFalse();
        assertThat(whitespaceIsRequired).isFalse();
        assertThat(nullIsRequired).isFalse();
    }

    @Test
    void shouldValidateStringLength() {
        // Given: Various string lengths
        String shortString = "Hi";
        String validString = "Valid Length";
        String longString = "This is a very long string that exceeds the maximum allowed length";

        // When: Validating string lengths
        boolean shortIsValid = ValidationUtils.isValidLength(shortString, 3, 50);
        boolean validIsValid = ValidationUtils.isValidLength(validString, 3, 50);
        boolean longIsValid = ValidationUtils.isValidLength(longString, 3, 50);

        // Then: Should correctly validate string length constraints
        assertThat(shortIsValid).isFalse(); // too short
        assertThat(validIsValid).isTrue(); // within range
        assertThat(longIsValid).isFalse(); // too long
    }

    @Test
    void shouldValidateNumericRanges() {
        // Given: Various numeric values
        int validValue = 50;
        int belowMin = 5;
        int aboveMax = 150;

        // When: Validating numeric ranges
        boolean validInRange = ValidationUtils.isInRange(validValue, 10, 100);
        boolean belowInRange = ValidationUtils.isInRange(belowMin, 10, 100);
        boolean aboveInRange = ValidationUtils.isInRange(aboveMax, 10, 100);

        // Then: Should correctly validate numeric ranges
        assertThat(validInRange).isTrue();
        assertThat(belowInRange).isFalse();
        assertThat(aboveInRange).isFalse();
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        // When: Validating null inputs
        boolean emailValid = ValidationUtils.isValidEmail(null);
        boolean phoneValid = ValidationUtils.isValidPhoneNumber(null);
        boolean requiredValid = ValidationUtils.isRequired(null);

        // Then: Should handle null inputs without throwing exceptions
        assertThat(emailValid).isFalse();
        assertThat(phoneValid).isFalse();
        assertThat(requiredValid).isFalse();
    }

    @Test
    void shouldValidateCustomPatterns() {
        // Given: Custom validation patterns
        String vietnameseIdCard = "123456789012"; // 12 digits
        String passportNumber = "A12345678"; // Letter followed by 8 digits
        String licenseNumber = "B2-123-456-789"; // Vietnam license format

        // When: Validating against custom patterns
        boolean idCardValid = ValidationUtils.matchesPattern(vietnameseIdCard, ValidationUtils.VIETNAMESE_ID_PATTERN);
        boolean passportValid = ValidationUtils.matchesPattern(passportNumber, ValidationUtils.PASSPORT_PATTERN);
        boolean licenseValid = ValidationUtils.matchesPattern(licenseNumber, ValidationUtils.VIETNAMESE_LICENSE_PATTERN);

        // Then: Should validate custom patterns correctly
        assertThat(idCardValid).isTrue();
        assertThat(passportValid).isTrue();
        assertThat(licenseValid).isTrue();
    }

    @Test
    void shouldProvideValidationMessages() {
        // When: Getting validation messages
        String emailMessage = ValidationUtils.getValidationMessage("email", "INVALID_FORMAT");
        String phoneMessage = ValidationUtils.getValidationMessage("phoneNumber", "INVALID_FORMAT");
        String requiredMessage = ValidationUtils.getValidationMessage("name", "REQUIRED");

        // Then: Should provide appropriate validation messages
        assertThat(emailMessage).isEqualTo("Email format is invalid");
        assertThat(phoneMessage).isEqualTo("Phone number format is invalid");
        assertThat(requiredMessage).isEqualTo("Name is required");
    }

    @Test
    void shouldValidateFileExtensions() {
        // Given: Various file names
        String imageFile = "photo.jpg";
        String documentFile = "document.pdf";
        String executableFile = "malware.exe";
        String noExtensionFile = "filename";

        // When: Validating file extensions
        boolean imageAllowed = ValidationUtils.isAllowedFileExtension(imageFile,
            ValidationUtils.ALLOWED_IMAGE_EXTENSIONS);
        boolean documentAllowed = ValidationUtils.isAllowedFileExtension(documentFile,
            ValidationUtils.ALLOWED_DOCUMENT_EXTENSIONS);
        boolean executableAllowed = ValidationUtils.isAllowedFileExtension(executableFile,
            ValidationUtils.ALLOWED_IMAGE_EXTENSIONS);
        boolean noExtensionAllowed = ValidationUtils.isAllowedFileExtension(noExtensionFile,
            ValidationUtils.ALLOWED_IMAGE_EXTENSIONS);

        // Then: Should correctly validate file extensions
        assertThat(imageAllowed).isTrue();
        assertThat(documentAllowed).isTrue();
        assertThat(executableAllowed).isFalse();
        assertThat(noExtensionAllowed).isFalse();
    }

    @Test
    void shouldThrowExceptionForInvalidValidationParameters() {
        // When/Then: Should throw exception for invalid parameters
        assertThatThrownBy(() -> ValidationUtils.isValidLength("test", -1, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Minimum length cannot be negative");

        assertThatThrownBy(() -> ValidationUtils.isValidLength("test", 10, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Maximum length cannot be less than minimum length");

        assertThatThrownBy(() -> ValidationUtils.isInRange(50, 100, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Maximum value cannot be less than minimum value");
    }
}
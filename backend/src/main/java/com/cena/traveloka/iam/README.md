# IAM Module - Identity & Access Management

## Overview

The IAM (Identity and Access Management) module provides comprehensive authentication, authorization, and user management capabilities for the Traveloka platform.

## Features

### Authentication
- ✅ Email/password registration and login
- ✅ JWT-based authentication (1-hour access token, 7-day refresh token)
- ✅ Email verification
- ✅ Password reset flow
- ✅ Two-factor authentication (TOTP)
- ✅ OAuth2 social login (Google, Facebook)
- ✅ Session management with Redis
- ✅ Account lockout after 5 failed attempts

### Authorization
- ✅ Role-based access control (RBAC)
- ✅ Permission-based authorization
- ✅ Integration with Keycloak for identity management

### User Management
- ✅ Extended user profiles
- ✅ Vietnamese phone number validation
- ✅ Profile updates
- ✅ Admin user management

### Security Features
- ✅ Rate limiting on auth endpoints
- ✅ Password complexity validation
- ✅ Automatic account lockout expiration (scheduled job)
- ✅ Login history tracking
- ✅ Session limit enforcement (5 concurrent sessions)

## Architecture

```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Entity Layer (Database Models)
```

## API Endpoints

### Authentication Endpoints

#### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecureP@ss123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+84901234567"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "code": "USER_REGISTERED",
  "message": "User registered successfully. Please verify your email.",
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "username": "johndoe",
    "emailVerified": false
  },
  "timestamp": "2025-01-05T10:30:00Z"
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecureP@ss123"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "code": "LOGIN_SUCCESS",
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "username": "johndoe"
    }
  },
  "timestamp": "2025-01-05T10:30:00Z"
}
```

#### Logout
```http
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
```

#### Refresh Token
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}
```

#### Forgot Password
```http
POST /api/v1/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### Reset Password
```http
POST /api/v1/auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-here",
  "newPassword": "NewSecureP@ss123"
}
```

#### Verify Email
```http
POST /api/v1/auth/verify-email
Content-Type: application/json

{
  "token": "verification-token-here"
}
```

#### Change Password
```http
POST /api/v1/auth/change-password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "SecureP@ss123",
  "newPassword": "NewSecureP@ss123"
}
```

### User Management Endpoints

#### Get Current User
```http
GET /api/v1/users/me
Authorization: Bearer {accessToken}
```

#### Update Current User
```http
PUT /api/v1/users/me
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+84901234567",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE"
}
```

#### Get All Users (Admin Only)
```http
GET /api/v1/users?page=0&size=20
Authorization: Bearer {adminAccessToken}
```

#### Get User by ID (Admin Only)
```http
GET /api/v1/users/{userId}
Authorization: Bearer {adminAccessToken}
```

#### Lock User Account (Admin Only)
```http
POST /api/v1/users/{userId}/lock?reason=Suspicious activity
Authorization: Bearer {adminAccessToken}
```

#### Unlock User Account (Admin Only)
```http
POST /api/v1/users/{userId}/unlock
Authorization: Bearer {adminAccessToken}
```

### Session Management Endpoints

#### Get Active Sessions
```http
GET /api/v1/sessions/active
Authorization: Bearer {accessToken}
```

#### Delete Session
```http
DELETE /api/v1/sessions/{sessionId}
Authorization: Bearer {accessToken}
```

#### Delete All Sessions Except Current
```http
DELETE /api/v1/sessions/all-except-current
Authorization: Bearer {accessToken}
```

### Two-Factor Authentication Endpoints

#### Setup 2FA
```http
POST /api/v1/users/me/2fa/setup
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "secret": "BASE32SECRET",
    "qrCodeUrl": "otpauth://totp/Traveloka:user@example.com?secret=...",
    "backupCodes": ["12345678", "87654321", ...]
  }
}
```

#### Verify 2FA
```http
POST /api/v1/users/me/2fa/verify
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "code": "123456"
}
```

#### Disable 2FA
```http
POST /api/v1/users/me/2fa/disable
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "password": "SecureP@ss123"
}
```

## Configuration

### Application Properties

```yaml
# application-iam.yml
iam:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiry: 3600000 # 1 hour in milliseconds
    refresh-token-expiry: 604800000 # 7 days in milliseconds

  session:
    max-concurrent: 5
    timeout: 1800 # 30 minutes in seconds

  account-lockout:
    max-attempts: 5
    duration-minutes: 30

  rate-limit:
    login: 5 # per minute
    register: 3 # per minute
    password-reset: 3 # per minute

keycloak:
  realm: traveloka
  auth-server-url: ${KEYCLOAK_URL}
  resource: traveloka-backend
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}
```

## Password Requirements

- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one number (0-9)
- At least one special character (!@#$%^&*)

## Vietnamese Phone Number Format

- Must start with +84
- Followed by 9-10 digits
- Example: `+84901234567`

## Rate Limiting

Rate limits are enforced per IP address:

| Endpoint | Limit |
|----------|-------|
| `/auth/login` | 5 requests/minute |
| `/auth/register` | 3 requests/minute |
| `/auth/forgot-password` | 3 requests/minute |
| `/auth/reset-password` | 3 requests/minute |
| `/auth/verify-email` | 5 requests/minute |

When rate limit is exceeded:
- HTTP 429 (Too Many Requests)
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After`

## Account Lockout

After 5 failed login attempts:
- Account is locked for 30 minutes
- HTTP 423 (Locked) response
- Automatic unlock via scheduled job (runs every 5 minutes)

## Session Management

- Maximum 5 concurrent sessions per user
- Sessions stored in Redis
- 30-minute inactivity timeout
- Session tracking includes: IP address, user agent, login time

## Email Templates

The module includes professionally designed email templates:

1. **Email Verification** (`templates/email/verification-email.html`)
   - Welcome message
   - Verification link (24-hour expiry)
   - Security notice

2. **Password Reset** (`templates/email/password-reset-email.html`)
   - Reset link (30-minute expiry)
   - Request details (time, IP, device)
   - Password requirements
   - Security tips

## Scheduled Jobs

### Account Lockout Expiration
- **Frequency**: Every 5 minutes
- **Purpose**: Automatically unlock accounts after lockout period expires
- **Class**: `AccountLockoutScheduler`

## Security Best Practices

1. **JWT Tokens**
   - Store in httpOnly cookies or secure storage
   - Never expose in URLs
   - Implement token refresh before expiry

2. **Password Management**
   - Enforce strong password policy
   - Use bcrypt hashing (strength 12)
   - Never log or expose passwords

3. **Session Security**
   - Limit concurrent sessions
   - Track session activities
   - Implement session timeout

4. **Rate Limiting**
   - Protect against brute force attacks
   - Monitor for suspicious patterns
   - Block abusive IPs

## Testing

Run IAM module tests:
```bash
mvn test -Dtest=**/iam/**/*Test
```

Run specific test suites:
```bash
# Repository tests
mvn test -Dtest=**/iam/repository/**/*Test

# Service tests
mvn test -Dtest=**/iam/service/**/*Test

# Controller tests
mvn test -Dtest=**/iam/controller/**/*Test

# Integration tests
mvn test -Dtest=**/iam/integration/**/*Test
```

## Dependencies

- Spring Security
- Spring Data JPA
- Spring Data Redis
- JWT (io.jsonwebtoken)
- Keycloak
- BCrypt (password hashing)
- TOTP (two-factor auth)
- Thymeleaf (email templates)
- MapStruct (DTO mapping)

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `USER_NOT_FOUND` | 404 | User does not exist |
| `INVALID_CREDENTIALS` | 401 | Invalid email/password |
| `ACCOUNT_LOCKED` | 423 | Account is locked |
| `ACCOUNT_SUSPENDED` | 403 | Account is suspended |
| `EMAIL_NOT_VERIFIED` | 403 | Email verification required |
| `TOKEN_EXPIRED` | 401 | JWT token expired |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INVALID_TOKEN` | 400 | Invalid or malformed token |
| `SESSION_LIMIT_EXCEEDED` | 403 | Max sessions reached |
| `PASSWORD_COMPLEXITY` | 400 | Password doesn't meet requirements |

## Support

For issues or questions:
- Documentation: `/docs/iam`
- Swagger UI: `/swagger-ui.html`
- Contact: iam-team@traveloka.com

## Version

**Module Version**: 1.0.0
**Last Updated**: 2025-01-05

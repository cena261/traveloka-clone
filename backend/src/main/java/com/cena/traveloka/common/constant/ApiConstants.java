package com.cena.traveloka.common.constant;

import org.springframework.http.HttpStatus;

public final class ApiConstants {

    private ApiConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }


    public static final int DEFAULT_PAGE_NUMBER = 0;

    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final int MAX_PAGE_SIZE = 100;

    public static final int MIN_PAGE_SIZE = 1;


    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    public static final String HEADER_TOTAL_COUNT = "X-Total-Count";

    public static final String HEADER_PAGE_NUMBER = "X-Page-Number";

    public static final String HEADER_PAGE_SIZE = "X-Page-Size";

    public static final String HEADER_API_VERSION = "X-API-Version";


    public static final String PARAM_PAGE = "page";

    public static final String PARAM_SIZE = "size";

    public static final String PARAM_SORT = "sort";

    public static final String PARAM_SEARCH = "search";


    public static final String API_VERSION = "v1";

    public static final String API_BASE_PATH = "/api/" + API_VERSION;


    public static final class ResponseCodes {
        public static final String SUCCESS = "SUCCESS";
        public static final String CREATED = "CREATED";
        public static final String UPDATED = "UPDATED";
        public static final String DELETED = "DELETED";
        public static final String RETRIEVED = "RETRIEVED";
        public static final String PROCESSED = "PROCESSED";
        public static final String ACCEPTED = "ACCEPTED";

        private ResponseCodes() {
            throw new UnsupportedOperationException("Constants class cannot be instantiated");
        }
    }


    public static final class ErrorCodes {
        public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
        public static final String BAD_REQUEST = "BAD_REQUEST";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
        public static final String CONFLICT = "CONFLICT";
        public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
        public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";

        public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
        public static final String FIELD_REQUIRED = "FIELD_REQUIRED";
        public static final String FIELD_INVALID = "FIELD_INVALID";
        public static final String FIELD_TOO_LONG = "FIELD_TOO_LONG";
        public static final String FIELD_TOO_SHORT = "FIELD_TOO_SHORT";

        public static final String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";
        public static final String ENTITY_ALREADY_EXISTS = "ENTITY_ALREADY_EXISTS";
        public static final String ENTITY_CREATION_FAILED = "ENTITY_CREATION_FAILED";
        public static final String ENTITY_UPDATE_FAILED = "ENTITY_UPDATE_FAILED";
        public static final String ENTITY_DELETE_FAILED = "ENTITY_DELETE_FAILED";

        public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
        public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
        public static final String INVALID_OPERATION = "INVALID_OPERATION";
        public static final String OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED";

        public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";
        public static final String EXTERNAL_SERVICE_UNAVAILABLE = "EXTERNAL_SERVICE_UNAVAILABLE";
        public static final String EXTERNAL_SERVICE_TIMEOUT = "EXTERNAL_SERVICE_TIMEOUT";

        public static final String DATABASE_ERROR = "DATABASE_ERROR";
        public static final String DATABASE_CONNECTION_ERROR = "DATABASE_CONNECTION_ERROR";
        public static final String OPTIMISTIC_LOCKING_FAILURE = "OPTIMISTIC_LOCKING_FAILURE";

        public static final String CACHE_ERROR = "CACHE_ERROR";
        public static final String CACHE_MISS = "CACHE_MISS";

        public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
        public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
        public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
        public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
        public static final String TOKEN_INVALID = "TOKEN_INVALID";
        public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
        public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";

        public static final String BOOKING_NOT_FOUND = "BOOKING_NOT_FOUND";
        public static final String BOOKING_ALREADY_EXISTS = "BOOKING_ALREADY_EXISTS";
        public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";
        public static final String BOOKING_EXPIRED = "BOOKING_EXPIRED";
        public static final String BOOKING_NOT_MODIFIABLE = "BOOKING_NOT_MODIFIABLE";

        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String PAYMENT_METHOD_INVALID = "PAYMENT_METHOD_INVALID";
        public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
        public static final String PAYMENT_ALREADY_PROCESSED = "PAYMENT_ALREADY_PROCESSED";

        public static final String NOT_AVAILABLE = "NOT_AVAILABLE";
        public static final String INSUFFICIENT_CAPACITY = "INSUFFICIENT_CAPACITY";

        private ErrorCodes() {
            throw new UnsupportedOperationException("Constants class cannot be instantiated");
        }
    }


    public static final class Messages {
        public static final String SUCCESS = "Operation completed successfully";
        public static final String CREATED = "Resource created successfully";
        public static final String UPDATED = "Resource updated successfully";
        public static final String DELETED = "Resource deleted successfully";
        public static final String RETRIEVED = "Resource retrieved successfully";

        public static final String USER_CREATED = "User created successfully";
        public static final String USER_UPDATED = "User updated successfully";
        public static final String USER_DELETED = "User deleted successfully";
        public static final String USER_RETRIEVED = "User retrieved successfully";
        public static final String USER_LOGGED_IN = "User logged in successfully";
        public static final String USER_LOGGED_OUT = "User logged out successfully";

        public static final String BOOKING_CREATED = "Booking created successfully";
        public static final String BOOKING_UPDATED = "Booking updated successfully";
        public static final String BOOKING_CANCELLED = "Booking cancelled successfully";
        public static final String BOOKING_RETRIEVED = "Booking retrieved successfully";

        public static final String PAYMENT_PROCESSED = "Payment processed successfully";
        public static final String PAYMENT_REFUNDED = "Payment refunded successfully";

        public static final String REVIEW_CREATED = "Review submitted successfully";
        public static final String REVIEW_UPDATED = "Review updated successfully";
        public static final String REVIEW_DELETED = "Review deleted successfully";

        private Messages() {
            throw new UnsupportedOperationException("Constants class cannot be instantiated");
        }
    }


    public static final class ErrorMessages {
        public static final String INTERNAL_SERVER_ERROR = "An internal server error occurred";
        public static final String BAD_REQUEST = "Invalid request parameters";
        public static final String UNAUTHORIZED = "Authentication required";
        public static final String FORBIDDEN = "Access denied";
        public static final String NOT_FOUND = "Resource not found";
        public static final String CONFLICT = "Resource conflict detected";

        public static final String VALIDATION_FAILED = "Input validation failed";
        public static final String FIELD_REQUIRED = "This field is required";
        public static final String FIELD_INVALID = "This field has an invalid value";

        public static final String ENTITY_NOT_FOUND = "Entity not found";
        public static final String ENTITY_ALREADY_EXISTS = "Entity already exists";

        public static final String USER_NOT_FOUND = "User not found";
        public static final String INVALID_CREDENTIALS = "Invalid username or password";
        public static final String TOKEN_EXPIRED = "Authentication token has expired";
        public static final String SESSION_EXPIRED = "Session has expired";

        public static final String BOOKING_NOT_FOUND = "Booking not found";
        public static final String BOOKING_CANCELLED = "Cannot modify cancelled booking";

        public static final String PAYMENT_FAILED = "Payment processing failed";
        public static final String INSUFFICIENT_FUNDS = "Insufficient funds for transaction";

        private ErrorMessages() {
            throw new UnsupportedOperationException("Constants class cannot be instantiated");
        }
    }


    public static final class HttpStatuses {
        public static final int OK = HttpStatus.OK.value();
        public static final int CREATED = HttpStatus.CREATED.value();
        public static final int ACCEPTED = HttpStatus.ACCEPTED.value();
        public static final int NO_CONTENT = HttpStatus.NO_CONTENT.value();

        public static final int BAD_REQUEST = HttpStatus.BAD_REQUEST.value();
        public static final int UNAUTHORIZED = HttpStatus.UNAUTHORIZED.value();
        public static final int FORBIDDEN = HttpStatus.FORBIDDEN.value();
        public static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();
        public static final int METHOD_NOT_ALLOWED = HttpStatus.METHOD_NOT_ALLOWED.value();
        public static final int CONFLICT = HttpStatus.CONFLICT.value();
        public static final int UNPROCESSABLE_ENTITY = HttpStatus.UNPROCESSABLE_ENTITY.value();
        public static final int TOO_MANY_REQUESTS = HttpStatus.TOO_MANY_REQUESTS.value();

        public static final int INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR.value();
        public static final int BAD_GATEWAY = HttpStatus.BAD_GATEWAY.value();
        public static final int SERVICE_UNAVAILABLE = HttpStatus.SERVICE_UNAVAILABLE.value();
        public static final int GATEWAY_TIMEOUT = HttpStatus.GATEWAY_TIMEOUT.value();

        private HttpStatuses() {
            throw new UnsupportedOperationException("Constants class cannot be instantiated");
        }
    }


    public static final class DateTimeFormats {
        public static final String ISO_DATETIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        public static final String ISO_DATE = "yyyy-MM-dd";
        public static final String STANDARD_DATETIME = "dd/MM/yyyy HH:mm:ss";
        public static final String STANDARD_DATE = "dd/MM/yyyy";
        public static final String STANDARD_TIME = "HH:mm:ss";

        private DateTimeFormats() {
            throw new UnsupportedOperationException("Constants class cannot be instantiated");
        }
    }


    public static final class ContentTypes {
        public static final String JSON = "application/json";
        public static final String XML = "application/xml";
        public static final String TEXT = "text/plain";
        public static final String HTML = "text/html";
        public static final String PDF = "application/pdf";
        public static final String CSV = "text/csv";

        private ContentTypes() {
            throw new UnsupportedOperationException("Constants class cannot be instantiated");
        }
    }
}
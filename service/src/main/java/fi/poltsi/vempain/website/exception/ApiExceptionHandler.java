package fi.poltsi.vempain.website.exception;

import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Every failure is reported as an {@link ApiErrorResponse} body, which is the shape the frontend expects.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
		return ResponseEntity.status(exception.getStatus())
							 .body(error(exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
							 .body(error(exception.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
							 .body(error(exception.getMessage()));
	}

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class
	})
	ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception) {
		return ResponseEntity.badRequest()
							 .body(error("Malformed request"));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
		log.error("Unhandled failure while serving a request", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							 .body(error("Internal server error"));
	}

	private static ApiErrorResponse error(String message) {
		return ApiErrorResponse.builder()
							   .error(message)
							   .build();
	}
}

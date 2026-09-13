package fi.poltsi.vempain.website.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
		return ResponseEntity.status(exception.getStatus())
		                     .body(Map.of("error", exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
		                     .body(Map.of("error", exception.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
		                     .body(Map.of("error", exception.getMessage()));
	}

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class
	})
	ResponseEntity<Map<String, String>> handleMalformedRequest(Exception exception) {
		return ResponseEntity.badRequest()
		                     .body(Map.of("error", exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
		LOG.error("Unhandled failure while serving a request", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		                     .body(Map.of("error", "Internal server error"));
	}
}

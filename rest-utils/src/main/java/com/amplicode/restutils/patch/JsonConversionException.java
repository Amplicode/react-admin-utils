package com.amplicode.restutils.patch;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;

import java.util.Locale;

/**
 * Similar to {@link HttpMessageNotReadableException}.
 * Thrown when parsing input JSON request argument or body fails.
 */
public class JsonConversionException extends RuntimeException implements ErrorResponse {

	private final ProblemDetail body;

	public JsonConversionException(String message) {
		super(message);
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), message);
	}

	public JsonConversionException(String message, Throwable cause) {
		super(message, cause);
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), message);
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	public ProblemDetail getBody() {
		return body;
	}

	@Override
	public HttpHeaders getHeaders() {
		return ErrorResponse.super.getHeaders();
	}

	@Override
	public String getDetailMessageCode() {
		return ErrorResponse.super.getDetailMessageCode();
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return ErrorResponse.super.getDetailMessageArguments();
	}

	@Override
	public Object[] getDetailMessageArguments(MessageSource messageSource, Locale locale) {
		return ErrorResponse.super.getDetailMessageArguments(messageSource, locale);
	}

	@Override
	public String getTitleMessageCode() {
		return ErrorResponse.super.getTitleMessageCode();
	}

	@Override
	public ProblemDetail updateAndGetBody(MessageSource messageSource, Locale locale) {
		return ErrorResponse.super.updateAndGetBody(messageSource, locale);
	}
}

/*
 * Copyright 2024 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.amplicode.rautils.patch;

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

package com.convotis.converter.model;

/**
 * Exception class to encapsulate Exception thrown in ConverterService
 */
public class ConverterException extends Exception {

	public ConverterException() {
		super();
	}

	public ConverterException(String message) {
		super(message);
	}

	public ConverterException(String message, Throwable cause) {
		super(message, cause);
	}


}

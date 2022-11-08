package com.convotis.converter.model;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface ConverterIntf {

	public String convert(String fromValue) throws ConverterException;
}

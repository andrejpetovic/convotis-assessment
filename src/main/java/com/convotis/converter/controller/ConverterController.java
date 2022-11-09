package com.convotis.converter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.convotis.converter.model.ConvertableFileType;
import com.convotis.converter.model.ConverterException;
import com.convotis.converter.service.ConverterService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ConverterController {

	@Autowired
	private ConverterService converterService;



	@RequestMapping(value = "/converter/json2csv", method = RequestMethod.POST)
	@ApiOperation(value = "Converts entered JSON String into CSV", notes = "This API operation takes json String as input parameter fromValue and converts it into csv string returned in response. Returns CSV output with delimiter as comma separator")
	public @ResponseBody String convertFromJsonToCsv(@RequestParam @ApiParam(name = "fromValue", value = "Valid Json String to be converter") String fromValue){
		String csvReturnValue = null;
		try {
			csvReturnValue = converterService.convert(fromValue, ConvertableFileType.JSON, ConvertableFileType.CSV);
		}catch (ConverterException e){
			log.error(e.getMessage() +". Cause:"+ e.getCause().getMessage());
			return e.getMessage() +". Cause:"+ e.getCause().getMessage();
		}
		return  csvReturnValue;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/api/javainuse")
	public String sayHello() {
		return "Swagger Hello World";
	}


}

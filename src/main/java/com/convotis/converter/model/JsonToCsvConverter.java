package com.convotis.converter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Implementation class containing logic for specified conversion from JSON to CSV
 */
public class JsonToCsvConverter implements ConverterIntf {

	private static final String COMMA_SEPARATOR = ";";

	public String convert(String jsonValue) throws ConverterException {
		try {
			String csvValue = null;

			//1. validate json String for valid json format
			if (!isJsonValid(jsonValue)){
				throw new ConverterException("Json Input Parameter is not a valid Json String");
			}

			//2. get CSV headers as Json labels of last level
			List<String> csvHeaders = getCsvHeaders(jsonValue);

			//3. remove parents of nested json elements
			jsonValue = removeParentNestedElements(jsonValue);

			//4. convert JSON to CSV
			JsonNode jsonTree = new ObjectMapper().readTree(jsonValue);
			CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
			csvSchemaBuilder.setColumnSeparator((char) COMMA_SEPARATOR.codePointAt(0));
			for (String csvHeader : csvHeaders) {
				csvSchemaBuilder.addColumn(csvHeader);
			}
			CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
			CsvMapper csvMapper = new CsvMapper();
			csvValue = csvMapper.writerFor(JsonNode.class)
					.with(csvSchema)
					.writeValueAsString(jsonTree);


			return csvValue;
		} catch (JsonMappingException e) {
			throw new ConverterException("Json mapping error occured. ", e);
		} catch (JsonProcessingException e) {
			throw new ConverterException("Json processing error occured", e);
		}

	}

	/**
	 * removes nested parent elements (they are not relevant for csv format)
	 * @param jsonValue
	 * @return
	 */
	private String removeParentNestedElements(String jsonValue) throws JsonProcessingException {
		StringBuilder sb = new StringBuilder();
		ObjectMapper mapper = new  ObjectMapper();


		if (isJsonList(jsonValue)){
			//JSON is an array
			sb.append("[");
			List<Map<String, Object>> jsonElements =
					mapper.readValue(jsonValue , new TypeReference<List<Map<String, Object>>>(){});
			removeJsonNestedElements(jsonElements, sb);
			sb.deleteCharAt(sb.lastIndexOf(","));
			sb.append("]");
		}else{
			//JSON is not an array but an object
			Map<String, Object> jsonElements =
					mapper.readValue(jsonValue , new TypeReference<Map<String, Object>>() {});
			sb.append("{");
			recreateJsonStringWithoutNestedElements(jsonElements, sb);
			sb.deleteCharAt(sb.lastIndexOf(",")); //remove last comma
			sb.append("}");
		}




		return sb.toString();
	}

	private void removeJsonNestedElements(List<Map<String, Object>> jsonElements, StringBuilder modifiedJsonValue) {
		for (Map<String, Object> jsonElementMap : jsonElements){
			modifiedJsonValue.append("{");
			recreateJsonStringWithoutNestedElements(jsonElementMap, modifiedJsonValue);
			modifiedJsonValue.deleteCharAt(modifiedJsonValue.lastIndexOf(",")); //remove last comma
			modifiedJsonValue.append("},");
		}
	}

	/**
	 * iterates elements of original json string and recreated a new json string without parents of nested elements
	 * @param jsonElements
	 * @param modifiedJsonValue
	 */
	private void recreateJsonStringWithoutNestedElements(Map<String,Object> jsonElements, StringBuilder modifiedJsonValue) {
		Set<Map.Entry<String, Object>> entries = jsonElements.entrySet();
		for (Map.Entry<String, Object> entry : entries) {
			if (isValidCsvElement(entry)) {
				modifiedJsonValue.append("\"" + entry.getKey() + "\" : \"" + entry.getValue() + "\",");
			}
			if (entry.getValue() instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) entry.getValue();
				recreateJsonStringWithoutNestedElements(map, modifiedJsonValue);
			} else if (entry.getValue() instanceof List) {
				List<?> list = (List<?>) entry.getValue();
				for (Object listEntry : list) {
					if (listEntry instanceof Map) {
						Map<String, Object> map = (Map<String, Object>) listEntry;
						recreateJsonStringWithoutNestedElements(map, modifiedJsonValue);
					}
				}
			}
		}
	}


	/**
	 * Method return csv headers after parsing the json labels
	 * @param json
	 * @return
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	private List<String> getCsvHeaders(String json) throws JsonMappingException, JsonProcessingException {
		List<String> keys = new ArrayList<>();
		ObjectMapper mapper = new  ObjectMapper();

		if (isJsonList(json)){
			List<Map<String, Object>> jsonElements =
					mapper.readValue(json , new TypeReference<List<Map<String, Object>>>(){});
			getJsonRelevantLabelsAsCsvHeaders(jsonElements, keys);
		}else{
			//JSON is not an array but an object
			Map<String, Object> jsonElements = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			getJsonRelevantLabelsAsCsvHeaders(jsonElements, keys);
		}


		return keys;
	}

	private void getJsonRelevantLabelsAsCsvHeaders(List<Map<String, Object>> jsonElements, List<String> keys) {
		for (Map<String, Object> jsonElement : jsonElements){
			getJsonRelevantLabelsAsCsvHeaders(jsonElement, keys);
		}
	}

	/**
	 * Method reads all the relevant json labels (labels containing sub-objects are ignored)
	 *
	 * @param jsonElements
	 * @param keys
	 */
	private void getJsonRelevantLabelsAsCsvHeaders(Map<String, Object> jsonElements, List<String> keys) {

		jsonElements.entrySet()
				.forEach(entry -> {
					if (isValidCsvElement(entry) && !keys.contains(entry.getKey())){
						keys.add(entry.getKey());
					}
					if (entry.getValue() instanceof Map) {
						Map<String, Object> map = (Map<String, Object>) entry.getValue();
						getJsonRelevantLabelsAsCsvHeaders(map, keys);
					} else if (entry.getValue() instanceof List) {
						List<?> list = (List<?>) entry.getValue();
						list.forEach(listEntry -> {
							if (listEntry instanceof Map) {
								Map<String, Object> map = (Map<String, Object>) listEntry;
								getJsonRelevantLabelsAsCsvHeaders(map, keys);
							}
						});
					}
				});
	}

	/**
	 * As CSV Headers only 'last level' labels qualify. No parents of nested elements in csv
	 * @param entry
	 * @return
	 */
	private boolean isValidCsvElement(Map.Entry<String,Object> entry) {
		return !(entry.getValue() instanceof Map) && !(entry.getValue() instanceof List) ;
	}

	/**
	 * validates if JSON input param is in valid format
	 * @param json
	 * @return
	 */
	private boolean isJsonValid(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(json);
		} catch (JacksonException e) {
			return false;
		}
		return true;
	}

	/**
	 * checks if Json String is Object or Array representation
	 * @param json
	 * @return
	 */
	private boolean isJsonList(String json){
		ObjectMapper mapper = new  ObjectMapper();
		try{
			List<Map<String, Object>> jsonElements =
					mapper.readValue(json , new TypeReference<List<Map<String, Object>>>(){});
		} catch (JsonMappingException e) {
			return false;
		} catch (JsonProcessingException e) {
			return false;
		}
		return true;
	}

}

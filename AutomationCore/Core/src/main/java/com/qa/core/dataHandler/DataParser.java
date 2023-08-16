package com.qa.core.dataHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface DataParser {
    DataParser getInstance(String filePath, String sheetName);
    List<LinkedHashMap<String, String>> read();
    List<LinkedHashMap<String, String>> read(Map<String, String> mFilters);
    List<LinkedHashMap<String, String>> read(String[] arFilters);
    List<String[]> readAsList();
    List<LinkedHashMap<String, String>> read(String sColumn, String sValue);
    List<LinkedHashMap<String, String>> read(String sColumn, String sValue, String exclude_column, String exclude_value);
    List<LinkedHashMap<String, String>> readUniqueColumnValues(String column);
    LinkedHashMap<String, String> readRandomRow(String column, String value);
    LinkedHashMap<String, String> readRandomRow(String column, String value, String exclude_column, String exclude_value);
    List<LinkedHashMap<String, String>> readUniqueColumnValues(String filterColumn, String filterValue, String column);
    void clear();
    void write(Map<String, String> sData, int iRow);
    void write(List<Map<String, String>> sData);
    void write(Map<String, String> sData);
    void setProcessData(Boolean flag);
    Map<String, String> get_as_headers(String columnName, String key);
    Map<String, String> get_as_query_params(String columnName, String key);
    boolean checkForFilters(Map<String, String> mFilters);
}

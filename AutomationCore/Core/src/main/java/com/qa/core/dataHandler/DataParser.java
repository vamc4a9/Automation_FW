package com.qa.core.dataHandler;

import com.qa.core.context.RunConfiguration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface DataParser extends BaseDataParser {

    DataParser getInstance(String filePath, String sheetName);

    List<LinkedHashMap<String, String>> read();

    String getWorkbookPath();

    String getWorksheetPath();

    RunConfiguration getRunConfigurationObj();

    void clear();

    void write(Map<String, String> sData, int iRow);

    void write(List<Map<String, String>> sData);

    void write(Map<String, String> sData);

    void setProcessData(Boolean flag);

    List<String[]> readAsList();

    /***
     * Reads the sheet data into a collection based on filter provided
     *
     * @param mFilters - map containing column name and values, used for filtering
     *                 Excel sheet to read data
     * @return collection of rows where each row is represented as a map of column
     *         name and column value
     * @author vamsikrishna.kayyala
     */
    default List<LinkedHashMap<String, String>> read(Map<String, String> mFilters) {
        return processData(filterData(read(), mFilters));
    }

    /***
     * Reads the sheet data into a collection based on filter provided
     *
     * @param arFilters - array containing column name and values separated by ==, used for filtering
     *                 Excel sheet to read data
     * @return collection of rows where each row is represented as a map of column
     *         name and column value
     * @author vamsikrishna.kayyala
     */
    default List<LinkedHashMap<String, String>> read(String[] arFilters) {
        return processData(filterData(read(), arFilters));
    }

    /***
     * Reads the sheet data into a collection based on filter provided
     *
     * @param sColumn - Column name to put a filter on
     * @param sValue  - Filter value
     * @return collection of rows where each row is represented as a map of column
     *         name and column value
     * @author vamsikrishna.kayyala
     */
    default List<LinkedHashMap<String, String>> read(String sColumn, String sValue) {
        return processData(filterData(read(), sColumn, sValue));
    }

    /***
     * Reads the sheet data into a collection based on filter provided
     *
     * @param sColumn - Column name to put a filter on
     * @param sValue  - Filter value
     * @param exclude_column - column name for putting negative filter
     * @param exclude_value - column value for putting negative filter
     * @return collection of rows where each row is represented as a map of column
     *         name and column value
     * @author vamsikrishna.kayyala
     */
    default List<LinkedHashMap<String, String>> read(String sColumn, String sValue, String exclude_column, String exclude_value) {
        var completeData = read(sColumn, sValue);
        Predicate<LinkedHashMap<String, String>> filterCondition =
                map -> map.containsKey(exclude_column) && !map.get(exclude_column).equals(exclude_value);
        return processData(filterData(completeData, filterCondition));
    }

    /***
     * Reads a single column values with no duplicates
     *
     * @param column - Column name to put a filter on
     * @return returns a single row in a hashmap format
     * @author vamsikrishna.kayyala
     */
    default List<LinkedHashMap<String, String>> readUniqueColumnValues(String column) {
        var results = read();
        var uniqueData = getUniqueColumnValues(column, results);
        return processData(uniqueData);
    }

    /***
     * Reads a single column values with no duplicates
     *
     * @param column - Column name to put a filter on
     * @param value  - Filter value
     * @return returns a single row in a hashmap format
     * @author vamsikrishna.kayyala
     */
    default LinkedHashMap<String, String> readRandomRow(String column, String value) {
        var row = readRandomRow(read(column, value));
        if (row.isEmpty()) {
            throw new RuntimeException("Looks like there are no matching rows for column " +
                    "" + column + " and value " + value + " in " + getWorkbookPath());
        } else {
            return row.get();
        }
    }

    /***
     * Reads a single column values with no duplicates
     *
     * @param column - Column name to put a filter on
     * @param value  - Filter value
     * @param exclude_column - column name for putting negative filter
     * @param exclude_value - column value for putting negative filter
     * @return returns a single row in a hashmap format
     *
     * @author vamsikrishna.kayyala
     */
    default LinkedHashMap<String, String> readRandomRow(String column, String value, String exclude_column, String exclude_value) {
        var row = readRandomRow(read(column, value, exclude_column, exclude_value));
        if (row.isEmpty()) {
            throw new RuntimeException("Looks like there are no matching rows for column " +
                    "" + column + " and value " + value + " in " + getWorkbookPath());
        } else {
            return row.get();
        }
    }

    /***
     * Reads a single column values with no duplicates
     *
     * @param filterColumn - column to put filter
     * @param filterValue - Value to use for the filter
     * @param column - column name to read unique values from
     * @return returns the list of unique column values with key as column name
     * @author vamsikrishna.kayyala
     */
    default List<LinkedHashMap<String, String>> readUniqueColumnValues(String filterColumn,
                                                                      String filterValue,
                                                                      String column) {
        var results = read(filterColumn, filterValue);
        var uniqueData = getUniqueColumnValues(column, results);
        return processData(uniqueData);
    }

    /***
     * Reads a combination of data provided in _keys and _values format and return it as a Map.
     * Key is going to be the value from _keys row and value is from _values row
     *
     * @param columnName - column to put filter
     * @return returns the list of unique column values with key as column name
     * @param key - Value to use for the filter (we should have two rows with this key name,
     *            one will have key_keys and other one will be key_values
     * @author vamsikrishna.kayyala
     */
    default Map<String, String> get_as_headers(String columnName, String key) {
        LinkedHashMap<String, String> keys = read(columnName, key + "_keys").get(0);
        LinkedHashMap<String, String> values = read(columnName, key + "_values").get(0);
        Map<String, String> headers = new HashMap<>();
        for (String sKey : keys.keySet()) {
            if (!sKey.contentEquals(columnName)) {
                if (keys.get(sKey).contentEquals("")) {
                    break;
                }
                if (!getRunConfigurationObj().resolveString(values.get(sKey)).contentEquals("!IGNORE!"))
                    headers.put(getRunConfigurationObj().resolveString(keys.get(sKey)),
                            getRunConfigurationObj().resolveString(values.get(sKey)));
            }
        }
        return headers;
    }

    /***
     * Reads a combination of data provided in _keys and _values format and return it as a Map.
     * Key is going to be the value from _keys row and value is from _values row
     *
     * @param columnName - column to put filter
     * @return returns the list of unique column values with key as column name
     * @param key - Value to use for the filter (we should have two rows with this key name,
     *            one will have key_keys and other one will be key_values
     * @author vamsikrishna.kayyala
     */
    default Map<String, String> get_as_query_params(String columnName, String key) {
        return get_as_headers(columnName, key);
    }

}

package com.qa.core.dataHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface BaseDataParser {
    static Map<String, DataParser> DataParsers() {
        return BaseDataParserImpl.staticDataParsers.get();
    }

    static void storeDataParser(String key, DataParser value) {
        Map<String, DataParser> excels = DataParsers();
        excels.put(key, value);
        BaseDataParserImpl.staticDataParsers.set(excels);
    }

    int getRandomNumber(int min, int max);

    List<LinkedHashMap<String, String>> processData(List<LinkedHashMap<String, String>> data);

    List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data,
                                                   Map<String, String> filters);

    List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data, String[] filters);

    List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data, String column, String value);

    List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data,
                                                   Predicate<LinkedHashMap<String, String>> filter);

    List<LinkedHashMap<String, String>> getUniqueColumnValues(String column, List<LinkedHashMap<String, String>> results);

    Optional<LinkedHashMap<String, String>> readRandomRow(List<LinkedHashMap<String, String>> data);

    void saveData(String sKey, List<LinkedHashMap<String, String>> data);

    List<LinkedHashMap<String, String>> getSavedData(String sKey);

    boolean isDataSaved(String sKey);

    boolean containsKey(String key);
}

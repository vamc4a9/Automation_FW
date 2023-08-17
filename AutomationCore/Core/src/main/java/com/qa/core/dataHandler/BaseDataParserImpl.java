package com.qa.core.dataHandler;

import com.qa.core.context.RunConfiguration;
import com.qa.core.dataLib.DataProcessor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class BaseDataParserImpl<T extends BaseDataParserImpl<T>> implements BaseDataParser {

    protected static ThreadLocal<Map<String, DataParser>> staticDataParsers = ThreadLocal.withInitial(HashMap::new);

    protected static ThreadLocal<Map<String, Map<String, List<LinkedHashMap<String, String>>>>> staticDataParserData
            = ThreadLocal.withInitial(HashMap::new);

    public String sWBPath;
    public String sWSName;
    protected Boolean processData = true;

    private final RunConfiguration config;
    private final DataProcessor dp;

    BaseDataParserImpl(RunConfiguration config, DataProcessor dp) {
        this.config = config;
        this.dp = dp;
    }

    @Override
    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    @Override
    public List<LinkedHashMap<String, String>> processData(List<LinkedHashMap<String, String>> data) {
        if (processData) {
            List<LinkedHashMap<String, String>> processedData = new ArrayList<>();
            for (LinkedHashMap<String, String> row : data) {
                LinkedHashMap<String, String> newRow = new LinkedHashMap<String, String>();
                for (String key : row.keySet()) {
                    dp.init(row.get(key));
                    newRow.put(key, config.resolveString(dp.parse()));
                }
                processedData.add(newRow);
            }
            return processedData;
        } else {
            return data;
        }
    }

    @Override
    public List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data,
                                                          Map<String, String> filters) {
        Predicate<LinkedHashMap<String, String>> filterCondition = map -> {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                if (!map.containsKey(entry.getKey()) || !map.get(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
            }
            return true;
        };
        return filterData(data, filterCondition);
    }

    @Override
    public List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data, String[] filters) {
        Predicate<LinkedHashMap<String, String>> filterCondition = map -> {
            for (String filter : filters) {
                String[] keyValue = filter.split("==");
                if (keyValue.length != 2) {
                    throw new IllegalArgumentException("Invalid filter format: " + filter);
                }
                String key = keyValue[0];
                String value = keyValue[1];
                if (!map.containsKey(key) || !map.get(key).equals(value)) {
                    return false;
                }
            }
            return true;
        };
        return filterData(data, filterCondition);
    }

    @Override
    public List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data, String column, String value) {
        Predicate<LinkedHashMap<String, String>> filterCondition =
                map -> map.containsKey(column) && map.get(column).equals(value);
        return filterData(data, filterCondition);
    }

    @Override
    public List<LinkedHashMap<String, String>> filterData(List<LinkedHashMap<String, String>> data,
                                                          Predicate<LinkedHashMap<String, String>> filter) {
        return data.stream().filter(filter).collect(Collectors.toList());
    }

    @Override
    public List<LinkedHashMap<String, String>> getUniqueColumnValues(String column, List<LinkedHashMap<String, String>> results) {
        List<LinkedHashMap<String, String>> lReturn =
                new ArrayList<>();
        for(LinkedHashMap<String, String> row : results) {
            LinkedHashMap<String, String> newRow = new LinkedHashMap<>();
            newRow.put(column, row.get(column));
            if (!lReturn.contains(newRow)) {
                lReturn.add(newRow);
            }
        }
        return lReturn;
    }
    
    @Override
    public Optional<LinkedHashMap<String, String>> readRandomRow(List<LinkedHashMap<String, String>> data) {
        if (data.size() == 0) {
            return Optional.empty();
        }
        int randNumber = getRandomNumber(0, data.size() - 1);
        if (randNumber < 0 || randNumber > data.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.get(randNumber));
    }

    @Override
    public void saveData(String sKey, List<LinkedHashMap<String, String>> data) {
        Map<String, Map<String, List<LinkedHashMap<String, String>>>> excelData = staticDataParserData.get();
        if (excelData.containsKey(sWBPath + sWSName)) {
            excelData.get(sWBPath + sWSName).put(sKey, data);
        } else {
            Map<String, List<LinkedHashMap<String, String>>> mNewData = new HashMap<>();
            mNewData.put(sKey, data);
            excelData.put(sWBPath + sWSName, mNewData);
        }
        staticDataParserData.set(excelData);
    }

    @Override
    public List<LinkedHashMap<String, String>> getSavedData(String sKey) {
        if (staticDataParserData.get().containsKey(sWBPath + sWSName)) {
            return staticDataParserData.get().get(sWBPath + sWSName).get(sKey);
        } else {
            return null;
        }
    }

    @Override
    public boolean isDataSaved(String sKey) {
        if (staticDataParserData.get().containsKey(sWBPath + sWSName)) {
            return staticDataParserData.get().get(sWBPath + sWSName).containsKey(sKey);
        } else {
            return false;
        }
    }

    @Override
    public boolean containsKey(String key) {
        return staticDataParserData.get().containsKey(key);
    }

}

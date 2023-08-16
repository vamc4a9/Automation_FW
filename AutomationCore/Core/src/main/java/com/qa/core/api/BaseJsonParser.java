package com.qa.core.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@Scope("prototype")
public class BaseJsonParser {

    private Object document;

    public BaseJsonParser getInstance(String json) {
        this.document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        return this;
    }

    public LinkedHashMap readList(String json,String jPath) {
        return readList(Configuration.defaultConfiguration().jsonProvider().parse(json), jPath);
    }

    public LinkedHashMap readList(String jPath) {
        return readList(document, jPath);
    }

    private LinkedHashMap readList(Object document, String jPath) {
        Object read = JsonPath.read(document, jPath);
        if (read instanceof JSONArray) {
            return null;
        } else if (read instanceof LinkedHashMap) {
            return ((LinkedHashMap) read);
        } else {
            return null;
        }
    }

    public String readNode(String json, String jPath) {
        return readNode(Configuration.defaultConfiguration().jsonProvider().parse(json), jPath, 0);
    }

    public String readNode(String jPath) {
        return readNode(document, jPath, 0);
    }

    public String readNode(String json, String jPath, int index) {
        return readNode(Configuration.defaultConfiguration().jsonProvider().parse(json), jPath, index);
    }

    public String readNode(String jPath, int index) {
        return readNode(document, jPath, index);
    }

    private String readNode(Object document, String jPath, int index) {
        try {
            Object read = JsonPath.read(document, jPath);
            if (read instanceof JSONArray) {
                return ((net.minidev.json.JSONArray) read).get(index).toString();
            }
            return read.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String readJsonString(String response, String jPath) {
        return readJsonString(Configuration.defaultConfiguration().jsonProvider().parse(response), jPath);
    }

    public String readJsonString(String jPath) {
        return readJsonString(document, jPath);
    }

    private String readJsonString(Object document, String jPath) {
        Object read = JsonPath.read(document, jPath);
        if (read instanceof JSONArray) {
            if (((JSONArray) read).get(0) instanceof LinkedHashMap) {
                Gson gson = new Gson();
                String json = gson.toJson(((JSONArray) read).get(0), LinkedHashMap.class);
                return json;
            }
            return ((JSONArray) read).get(0).toString();
        }
        return read.toString();
    }

    public int readArraySize(String json, String jPath) {
        return readNodeSize(Configuration.defaultConfiguration().jsonProvider().parse(json), jPath);
    }

    public int readArraySize(String jPath) {
        return readNodeSize(document, jPath);
    }

    public JSONArray readArray(String json, String jPath) {
        return readArray(Configuration.defaultConfiguration().jsonProvider().parse(json), jPath);
    }

    public JSONArray readArray(String jPath) {
        return readArray(document, jPath);
    }

    private JSONArray readArray(Object document, String jPath) {
        Object read = JsonPath.read(document, jPath);
        try {
            return (JSONArray) read;
        } catch (Exception e) {
            throw e;
        }
    }

    public int readNodeSize(String json, String jPath) {
        return readNodeSize(Configuration.defaultConfiguration().jsonProvider().parse(json), jPath);
    }

    public int readNodeSize(String jPath) {
        return readNodeSize(document, jPath);
    }

    private int readNodeSize(Object document, String jPath) {
        Object read = JsonPath.read(document, jPath);
        if (read instanceof JSONArray) {
            return ((JSONArray) read).size();
        } else if (read instanceof LinkedHashMap) {
            return ((LinkedHashMap) read).size();
        } else {
            return 0;
        }
    }

    public boolean checkNode(String json, String jpath) {
        return checkNode(Configuration.defaultConfiguration().jsonProvider().parse(json), jpath);
    }

    public boolean checkNode(String jpath) {
        return checkNode(document, jpath);
    }

    private boolean checkNode(Object document, String jpath) {
        try {
            Object read = JsonPath.read(document, jpath);
            if (read instanceof JSONArray) {
                if (((net.minidev.json.JSONArray) read).size() > 0) {
                    return true;
                } else {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getAllValues(String json, String key) throws JsonProcessingException {
        List<String> allValues = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonTree = objectMapper.readTree(json);
        for (int i = 0; i <= jsonTree.size() - 1; i++) {
            allValues.add(jsonTree.get(i).get(key).asText());
        }
        return allValues;
    }

}

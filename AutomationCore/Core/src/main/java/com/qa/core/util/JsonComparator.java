package com.qa.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.core.report.ReportManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
@Lazy
@Scope("prototype")
public class JsonComparator {
    private final ReportManager reporter;
    private boolean isPassed = true;
    private String sourceCol;
    private String targetCol;

    public JsonComparator(ReportManager reporter) {
        this.reporter = reporter;
    }

    public void compareJsonFilesAndCreateHtml(String file1Path, String file2Path, String sourceCol, String targetCol,
                                              List<String> nodesToIgnore) throws IOException {
        this.sourceCol = sourceCol;
        this.targetCol = targetCol;
        isPassed = true;
        compareJsonFiles(new File(file1Path), new File(file2Path), nodesToIgnore);
    }

    private void compareJsonFiles(File json1, File json2, List<String> nodesToIgnore) throws IOException {
        reporter.startNode("Expand the table to view the differences");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node1 = objectMapper.readTree(json1);
        JsonNode node2 = objectMapper.readTree(json2);
        List<Map<String, String>> diffMap = new ArrayList<>();
        compareJsonNodes(diffMap, node1, node2, "", nodesToIgnore);
        if (isPassed) {
            reporter.report("pass", "source and target jsons are matching");
            if (diffMap.size() > 0) {
                reporter.reportAsTable("pass", diffMap);
            }
        } else {
            reporter.reportAsTable("fail", diffMap);
        }
        reporter.endNode();
    }

    private void compareJsonNodes(List<Map<String, String>> diffMap, JsonNode node1,
                                                       JsonNode node2, String path, List<String> nodesToIgnore) {
        if (node1.isObject() && node2.isObject()) {
            Set<String> fieldNames = new HashSet<>();
            node1.fieldNames().forEachRemaining(fieldNames::add);
            node2.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode childNode1 = node1.get(fieldName);
                JsonNode childNode2 = node2.get(fieldName);
                String childPath = path + "/" + fieldName;
                checkJsonNodes(diffMap, childNode1, childNode2, childPath, nodesToIgnore);
            }
        } else if (node1.isArray() && node2.isArray()) {
            int size = Math.max(node1.size(), node2.size());
            for (int i = 0; i < size; i++) {
                JsonNode childNode1 = i < node1.size() ? node1.get(i) : null;
                JsonNode childNode2 = i < node2.size() ? node2.get(i) : null;
                String childPath = path + "[" + i + "]";
                checkJsonNodes(diffMap, childNode1, childNode2, childPath, nodesToIgnore);
            }
        } else {
            if (!node1.equals(node2)) {
                getDiffInformation(diffMap, node1.toString(), node2.toString(), path,
                        "Value Mismatch", "Fail");
            }
        }
    }

    private void checkJsonNodes(List<Map<String, String>> diffMap, JsonNode childNode1,
                                   JsonNode childNode2, String childPath, List<String> nodesToIgnore) {
        String nodeName = getNodeNameFromPath(childPath);
        if (nodesToIgnore.contains(nodeName)) {
            getDiffInformation(diffMap, "", "", childPath, "Validation Ignored",
                    "Pass");
        } else if (childNode1 == null) {
            getDiffInformation(diffMap, "", childNode2.toString(), childPath,
                    "Missing in Source", "Fail");
        } else if (childNode2 == null) {
            getDiffInformation(diffMap, childNode1.toString(), "", childPath,
                    "Missing in Target", "Fail");
        } else {
            compareJsonNodes(diffMap, childNode1, childNode2, childPath, nodesToIgnore);
        }
    }

    private void getDiffInformation(List<Map<String, String>> diffMap, String sourceVal, String targetVal,
                                    String childPath, String validationNote, String status) {
        if (status.equalsIgnoreCase("fail"))
            isPassed = false;
        Map<String, String> row = new HashMap<>();
        row.put("path", childPath);
        row.put(sourceCol, sourceVal);
        row.put(targetCol, targetVal);
        row.put("Status", status);
        row.put("Note", validationNote);
        diffMap.add(row);
    }

    private String getNodeNameFromPath(String path) {
        int lastIndex = path.lastIndexOf("/");
        return lastIndex >= 0 ? path.substring(lastIndex + 1) : path;
    }
}

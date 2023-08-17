package com.qa.core.dataHandler;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.qa.core.context.RunConfiguration;
import com.qa.core.dataLib.DataProcessor;
import com.qa.core.util.BeanUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Lazy
@Scope("prototype")
public class CsvParser extends BaseDataParserImpl<CsvParser> implements DataParser {

    public List<String> headers;
    private List<LinkedHashMap<String, String>> data;
    private List<String[]> allLines;

    private final RunConfiguration config;
    private final DataProcessor dp;
    private final BeanUtil beanUtil;

    public CsvParser(RunConfiguration config, DataProcessor dp, BeanUtil beanUtil) {
        super(config, dp);
        this.config = config;
        this.dp = dp;
        this.beanUtil = beanUtil;
    }

    public synchronized CsvParser getInstance(URL url) {
        if (BaseDataParser.DataParsers().containsKey(url.toString())) {
            return (CsvParser) BaseDataParser.DataParsers().get(url.toString());
        } else {
            var csv = getNewInstance();
            csv.sWBPath = url.toString();
            csv.sWSName = "";
            csv.headers = new ArrayList<>();
            csv.data = new ArrayList<>();
            try {
                URLConnection urlConn = url.openConnection();
                InputStreamReader inputCSV = new InputStreamReader((urlConn).getInputStream());
                var csvReader = new CSVReaderBuilder(inputCSV).build();
                readCsv(csv, csvReader);
            } catch (Exception e) {
                throw new RuntimeException("Unable to read the csv file: " + sWBPath, e);
            }
            BaseDataParser.storeDataParser(url.toString(), csv);
            return csv;
        }
    }

    public synchronized CsvParser getInstance(String filePath, String sheetName) {
        if (BaseDataParser.DataParsers().containsKey(filePath + sheetName)) {
            return (CsvParser) BaseDataParser.DataParsers().get(filePath + sheetName);
        } else {
            var csv = getNewInstance();
            csv.sWBPath = filePath;
            csv.sWSName = sheetName;
            csv.headers = new ArrayList<>();
            csv.data = new ArrayList<>();
            try {
                var csvReader = CsvStatic.getWorkbook(filePath);
                readCsv(csv, csvReader);
            } catch (Exception e) {
                throw new RuntimeException("Unable to read the csv file: " + filePath, e);
            }
            BaseDataParser.storeDataParser(filePath + sheetName, csv);
            return csv;
        }
    }

    private static synchronized void readCsv(CsvParser csv, CSVReader csvReader) throws Exception {
        int i = 0;
        var allLines = csvReader.readAll();
        for(String[] line : allLines) {
            if (i == 0) {
                for (String value : line) {
                    value = value.replace("\uFEFF", "");
                    csv.headers.add(value.trim());
                }
                i++;
                continue;
            }
            LinkedHashMap<String, String> rowData = new LinkedHashMap<>();
            int index = 0;
            for (String value : line) {
                value = value.replace("\uFEFF", "");
                rowData.put(csv.headers.get(index), value.trim());
                index++;
            }
            rowData.put("ROW_NUMBER", String.valueOf(i));
            csv.data.add(rowData);
            csv.allLines = allLines;
        }
    }

    private CsvParser getNewInstance() {
        return beanUtil.getBeanClass(CsvParser.class);
    }

    @Override
    public void setProcessData(Boolean flag) {
        processData = flag;
    }

    @Override
    public List<String[]> readAsList() {
        return allLines;
    }

    @Override
    public List<LinkedHashMap<String, String>> read() {
        return processData(data);
    }

    @Override
    public String getWorkbookPath() {
        return sWBPath;
    }

    @Override
    public String getWorksheetPath() {
        return sWSName;
    }

    @Override
    public RunConfiguration getRunConfigurationObj() {
        return config;
    }

    @Override
    public void clear() {
        try {
            File file = new File(sWBPath);
            List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
            if (lines.size() > 1) {
                lines.subList(1, lines.size()).clear();
            }
            FileUtils.writeLines(file, lines);
            readCSVFile();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while reading " + sWBPath);
        }
    }

    @Override
    public void write(Map<String, String> sData, int iRow) {
        write_to_file(getCsvRow(sData));
        readCSVFile();
    }

    @Override
    public void write(List<Map<String, String>> sData) {
        clear();
        List<String> newLines = new ArrayList<>();
        for(Map<String, String> rowData: sData) {
            newLines.add(getCsvRow(rowData));
        }
        write_to_file(newLines);
        readCSVFile();
    }

    @Override
    public void write(Map<String, String> sData) {
        write_to_file(getCsvRow(sData));
        readCSVFile();
    }

    private String getCsvRow(Map<String, String> sData) {
        List<String> values = new ArrayList<>();
        for (String header : headers) {
            String value = sData.getOrDefault(header, "");
            values.add(escapeCSVValue(value));
        }
        return String.join(",", values);
    }

    private static String escapeCSVValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        } else {
            return value;
        }
    }

    private synchronized void write_to_file(String data) {
        try {
            File file = new File(sWBPath);
            List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
            lines.add(data);
            FileUtils.writeLines(file, lines);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while writing " +
                    data + " to a csv file named " + sWBPath);
        }
    }

    private synchronized void write_to_file(List<String> data) {
        try {
            File file = new File(sWBPath);
            List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
            lines.addAll(data);
            FileUtils.writeLines(file, lines);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while writing " +
                    data + " to a csv file named " + sWBPath);
        }
    }

    private synchronized void write_to_file(String data, int iRow) {
        try {
            File file = new File(sWBPath);
            List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
            for (int i = lines.size() - 1; i >= iRow; i--) {
                lines.add(i + 1, lines.get(i));
            }
            lines.set(iRow, data);
            FileUtils.writeLines(file, lines);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while writing " +
                    data + " to a csv file named " + sWBPath);
        }
    }

    private void readCSVFile() {
        this.headers = new ArrayList<>();
        this.data = new ArrayList<>();
        try {
            CSVReader csvReader = new CSVReaderBuilder(new FileReader(sWBPath)).build();
            String[] nextLine;
            int i = 0;
            while ((nextLine = csvReader.readNext()) != null) {
                if (i == 0) {
                    for (String value : nextLine) {
                        value = value.replace("\uFEFF", "");
                        headers.add(value.trim());
                    }
                    i++;
                    continue;
                }
                LinkedHashMap<String, String> rowData = new LinkedHashMap<>();
                int index = 0;
                for (String value : nextLine) {
                    value = value.replace("\uFEFF", "");
                    rowData.put(headers.get(index), value.trim());
                    index++;
                }
                data.add(rowData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to read the csv file: " + sWBPath, e);
        }
    }
}
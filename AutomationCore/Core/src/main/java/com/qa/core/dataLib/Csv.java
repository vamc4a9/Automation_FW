package com.qa.core.dataLib;

import com.qa.core.context.RunConfiguration;
import com.qa.core.report.ReportManager;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Component
@Lazy
@Scope("singleton")
public class Csv {

    private final ReportManager reporter;
    private final RunConfiguration configuration;

    private static Map<String, Integer> csvFileCounter = new HashMap<>();
    private static Map<String, String> csvFileNames = new HashMap<>();
    private static Map<String, String> csvHeaders = new HashMap<>();
    private static Integer no_of_records_per_csv;

    public Csv(ReportManager reporter, RunConfiguration configuration) {
        this.reporter = reporter;
        this.configuration = configuration;
    }

    @PostConstruct
    public void init() {
        no_of_records_per_csv = Integer.parseInt(configuration.getProperty("number_of_records_per_csv"));
    }

    public List<LinkedHashMap<String, String>> read(String url) throws IOException {
        List<LinkedHashMap<String, String>> lReturn = new ArrayList<>();
        if (!url.contentEquals("")) {
            URL uri = new URL(url);
            URLConnection urlConn = uri.openConnection();
            InputStreamReader inputCSV = new InputStreamReader((urlConn).getInputStream());
            BufferedReader br = new BufferedReader(inputCSV);
            String line;
            int iRow = 1;
            List<String> sHeaders = new ArrayList<String>();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(","); // separator
                if (iRow == 1) {
                    for (String header : values) {
                        sHeaders.add(header);
                    }
                } else {
                    LinkedHashMap<String, String> row = new LinkedHashMap<>();
                    int index = 0;
                    for (String data : values) {
                        row.put(sHeaders.get(index), data);
                        index++;
                    }
                    lReturn.add(row);
                }
                iRow++;
            }
        }
        return lReturn;
    }

    public synchronized void create(String fileName) {
        try {
            fileName = getFileName(fileName);
            File file = new File(ReportManager.resultsFolder + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            reporter.report("Error occurred while creating a csv file named " + fileName, e);
        }
    }

    public synchronized void create(String fileName, String headers) {
        try {
            if (csvHeaders.containsKey(fileName)) {
                headers = csvHeaders.get(fileName);
            } else {
                csvHeaders.put(fileName, headers);
            }

            fileName = getFileName(fileName);

            File file = new File(ReportManager.resultsFolder + fileName);
            if (!file.exists()) {
                if (file.createNewFile())
                    FileUtils.writeLines(file, Collections.singleton(headers));
            }
        } catch (Exception e) {
            reporter.report("Error occurred while creating a csv file named " + fileName, e);
        }
    }

    public synchronized void write(String fileName, String data) {
        try {
            String originalFileName = fileName;
            fileName = getFileName(fileName);
            File file = new File(ReportManager.resultsFolder + fileName);
            List<String> lines = FileUtils.readLines(file);
            if (lines.size() > no_of_records_per_csv) {
                incrementFileCounter(originalFileName);
                updateFileName(originalFileName);
                create(originalFileName, "");
                fileName = getFileName(originalFileName);
                file = new File(ReportManager.resultsFolder + fileName);
                lines = FileUtils.readLines(file);
            }
            lines.add(data);
            FileUtils.writeLines(file, lines);
        } catch (Exception e) {
            reporter.report("Error occurred while writing " +
                    data + " to a csv file named " + fileName, e);
        }
    }

    public synchronized void write(String fileName, List<String> data) {
        try {
            String originalFileName = fileName;
            fileName = getFileName(fileName);
            File file = new File(ReportManager.resultsFolder + fileName);
            List<String> lines = FileUtils.readLines(file, (Charset) null);
            if (lines.size() > no_of_records_per_csv) {
                incrementFileCounter(originalFileName);
                updateFileName(originalFileName);
                create(originalFileName, "");
                fileName = getFileName(originalFileName);
                file = new File(ReportManager.resultsFolder + fileName);
                lines = FileUtils.readLines(file, (Charset) null);
            }
            lines.addAll(data);
            FileUtils.writeLines(file, lines);
        } catch (Exception e) {
            reporter.report("Error occurred while writing " +
                    data + " to a csv file named " + fileName, e);
        }
    }

    public List<String> readHeaders(String url) throws IOException {
        List<String> sHeaders = new ArrayList<>();
        if (!url.contentEquals("")) {
            URL uri = new URL(url);
            URLConnection urlConn = uri.openConnection();
            InputStreamReader inputCSV = new InputStreamReader(urlConn.getInputStream());
            BufferedReader br = new BufferedReader(inputCSV);
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(","); // separator
                for (String header : values) {
                    sHeaders.add(header.replace("\"", ""));
                }
                break;
            }
        }
        return sHeaders;
    }

    public String saveAsFile(String url) {
        try {
            InputStream inputStream = new URL(url).openStream();
            String filePath = reporter.screenshotsFolder;
            String fileName = Calendar.getInstance().getTimeInMillis() + ".csv";
            Files.copy(inputStream, Paths.get(filePath  + fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            return filePath  + fileName;
        } catch (Exception e) {
            reporter.report("failed to save downloaded csv url into file", e);
        }
        return null;
    }

    private static void incrementFileCounter(String fileName) {
        if (csvFileCounter.containsKey(fileName)) {
            int fileCounter = csvFileCounter.get(fileName);
            csvFileCounter.put(fileName, ++fileCounter);
        } else {
            csvFileCounter.put(fileName, 1);
        }
    }

    private static void updateFileName(String fileName) {
        int fileCounter = csvFileCounter.get(fileName);
        String newFileName = fileName.replace(".csv", "_" + fileCounter + ".csv");
        csvFileNames.put(fileName, newFileName);
    }

    private static String getFileName(String fileName) {
        if (csvFileNames.containsKey(fileName))
            return csvFileNames.get(fileName);
        else
            csvFileNames.put(fileName,fileName);
        return fileName;
    }

}

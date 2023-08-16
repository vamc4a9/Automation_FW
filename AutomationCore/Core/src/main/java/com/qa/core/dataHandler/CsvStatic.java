package com.qa.core.dataHandler;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class CsvStatic {

    public static synchronized CSVReader getWorkbook(String filePath) throws FileNotFoundException {
        return new CSVReaderBuilder(new FileReader(filePath)).build();
    }
}

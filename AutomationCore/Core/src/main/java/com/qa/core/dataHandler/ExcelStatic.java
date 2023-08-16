package com.qa.core.dataHandler;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class ExcelStatic {
    public static synchronized Workbook getWorkbook(String sWBPath) throws IOException {
        FileInputStream oFis = new FileInputStream(sWBPath);
        // Instantiating the Workbook class based on Excel extension
        if (sWBPath.contains(".xlsx")) {
            return new XSSFWorkbook(oFis);
        } else {
            return new HSSFWorkbook(oFis);
        }
    }

    public static synchronized Sheet getWorksheet(Workbook wb, String sheetName) {
        return wb.getSheet(sheetName);
    }
}

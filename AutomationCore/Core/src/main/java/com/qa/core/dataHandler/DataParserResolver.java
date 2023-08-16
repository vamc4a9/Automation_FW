package com.qa.core.dataHandler;

import com.qa.core.context.RunConfiguration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
@Lazy
@Scope("prototype")
public class DataParserResolver {

    private final ExcelParser excel;
    private final CsvParser csv;
    private final RunConfiguration config;
    private DataParser catalogParser = null;

    public DataParserResolver(ExcelParser excel, CsvParser csv,
                              RunConfiguration config) {
        this.excel = excel;
        this.csv = csv;
        this.config = config;
    }

    public DataParser getInstance(String name) {
        var sheetDetails = getWorksheetDetailsFromCatalogFile(name);
        return getDataParser(sheetDetails);
    }

    private LinkedHashMap<String, String> getWorksheetDetailsFromCatalogFile(String sheetName) {
        catalogParser = getCatalogDataParser(ParserUtils.getAbsolutePathOfFile(config.getProperty("catalog_path")));
        return  readSheetDetailsFromCatalogFile(catalogParser, sheetName);
    }

    private synchronized LinkedHashMap<String, String> readSheetDetailsFromCatalogFile(DataParser data, String sheetName) {
        var sheetData = data.read("Name", sheetName);
        if (!isSheetNamePartOfCatalog(sheetData))
            throw new IllegalArgumentException("There is no record available in the catalog sheet for the Name: "
                    + sheetName);
        return sheetData.get(0);
    }

    private DataParser getCatalogDataParser(String catalogFilePath) {
        if (catalogParser == null)
            catalogParser = getDataParser(catalogFilePath, "Data");
        return catalogParser;
    }

    private boolean isValidFile(String filePath) {
        return isCsvFile(filePath) || isExcelFile(filePath);
    }

    private boolean isCsvFile(String filePath) {
        return filePath.toLowerCase().endsWith(".csv");
    }

    private CsvParser getCsvInstance(String filePath, String name) {
        return csv.getInstance(filePath, name);
    }

    private boolean isExcelFile(String filePath) {
        return filePath.toLowerCase().endsWith(".xls") || filePath.toLowerCase().endsWith(".xlsx");
    }

    private ExcelParser getExcelInstance(String filePath, String name) {
        return excel.getInstance(filePath, name);
    }

    private DataParser getDataParser(LinkedHashMap<String, String> sheetDetails) {
        return getDataParser(ParserUtils.getAbsolutePathOfFile(sheetDetails.get("WorkbookPath")),
                sheetDetails.get("SheetName"));
    }

    private boolean isSheetNamePartOfCatalog(List<LinkedHashMap<String, String>> details) {
        return details.size() > 0;
    }

    private DataParser getDataParser(String filePath, String sheetName) {
        if (isValidFile(filePath)) {
            if (isCsvFile(filePath))
                return getCsvInstance(filePath, sheetName);
            else
                return getExcelInstance(filePath, sheetName);
        } else {
            throw new IllegalArgumentException("Given file path is not supported to be passed to DataParser."
                    + filePath);
        }
    }
}
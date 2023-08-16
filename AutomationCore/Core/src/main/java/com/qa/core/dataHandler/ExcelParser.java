package com.qa.core.dataHandler;

import com.qa.core.context.RunConfiguration;
import com.qa.core.dataLib.DataProcessor;
import com.qa.core.util.BeanUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

@Component
@Lazy
@Scope("prototype")
public class ExcelParser extends BaseDataParser<ExcelParser> implements DataParser {
    public List<String> sHeaders = new ArrayList<>();
    private final DataProcessor dp;
    private final RunConfiguration config;
    private final BeanUtil beanUtil;
    private Workbook xBook;
    private Sheet xSheet;
    private Row xRow;
    private int iCurrentRow = 0;
    private FormulaEvaluator evaluator;

    public ExcelParser(DataProcessor dp, RunConfiguration config, BeanUtil beanUtil) {
        super(config, dp);
        this.dp = dp;
        this.config = config;
        this.beanUtil = beanUtil;
    }

    /**
     * a flag can be set in order to specify whether data need to be processed for any parameter
     * resolution before returning it to consumer function.
     * Note: by default, it is marked to process the data,
     * using this function, the user can make it false when needed.
     * @param processData - true or false
     *
     * @author vamsikrishna.kayyala
     */
    public void setProcessData(Boolean processData) {
        this.processData = processData;
    }

    /***
     * Get the Excel instance object to perform actions on Excel files
     *
     * @param sWBPath  - Excel workbook path
     * @param sWSSheet - Worksheet name
     * @return returns the Excel instance object
     * @author vamsikrishna.kayyala
     */
    public synchronized ExcelParser getInstance(String sWBPath, String sWSSheet) {
        try {
            if (DataParsers().containsKey(sWBPath + sWSSheet)) {
                // Returning the Excel instance object if it was already created
                return (ExcelParser) DataParsers().get(sWBPath + sWSSheet);
            } else {
                // Creating a new instance of Excel object
                ExcelParser oExcel = getNewInstance();
                oExcel.sWBPath = sWBPath;
                oExcel.sWSName = sWSSheet;

                oExcel.xBook = ExcelStatic.getWorkbook(sWBPath);

                // to get environment specific worksheet
                String sheetName = getSheetName(oExcel.xBook, sWSSheet);

                // Accessing the Excel worksheet
                oExcel.xSheet = ExcelStatic.getWorksheet(oExcel.xBook, sheetName);
                oExcel.sWSName = sheetName;

                // Setting the current row to 0
                oExcel.xRow = oExcel.xSheet.getRow(oExcel.iCurrentRow);

                // Creating a cell iterator to loop through all header cells
                Iterator<Cell> cellIterator = oExcel.xRow.cellIterator();

                // Accessing all the header cells into a list
                while (cellIterator.hasNext()) {
                    oExcel.sHeaders.add(cellIterator.next().getStringCellValue());
                }

                // Incrementing current row by 1 and updating to the worksheet
                oExcel.xSheet.getRow(oExcel.iCurrentRow++);

                // Creating a formula evaluator
                oExcel.evaluator = oExcel.xBook.getCreationHelper().createFormulaEvaluator();
                storeDataParser(sWBPath + sWSSheet, oExcel);
                return oExcel;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while accessing " + sWSSheet + " from " + sWBPath);
        }
    }

    private ExcelParser getNewInstance() {
        return beanUtil.getBeanClass(ExcelParser.class);
    }

    /***
     * Returns the environment specific sheet name. For example, if we pass Login as
     * a sheet name to this method, and workbook contains a sheet like Login_dev or
     * Login_staging, then it will return the appropriate sheet name based on the
     * current environment
     *
     * @param book  - Workbook object
     * @param sheet - Worksheet name
     * @return if the workbook contains any environment specific worksheet, then it
     *         will return that name.
     * @author vamsikrishna.kayyala
     */
    private String getSheetName(Workbook book, String sheet) {
        String env = config.getProperty("env");
        return (book.getSheet(sheet + "_" + env) == null) ? sheet : sheet + "_" + env;
    }

    public Map<String, String> get_as_headers(String columnName, String key) {
        LinkedHashMap<String, String> keys = read(columnName, key + "_keys").get(0);
        LinkedHashMap<String, String> values = read(columnName, key + "_values").get(0);

        Map<String, String> headers = new HashMap<>();
        for (String sKey : keys.keySet()) {
            if (!sKey.contentEquals(columnName)) {
                if (keys.get(sKey).contentEquals("")) {
                    break;
                }
                if (!config.resolveString(values.get(sKey)).contentEquals("!IGNORE!"))
                    headers.put(config.resolveString(keys.get(sKey)), config.resolveString(values.get(sKey)));
            }
        }
        return headers;
    }

    public Map<String, String> get_as_query_params(String columnName, String key) {
        return get_as_headers(columnName, key);
    }

    /***
     * Reads the entire sheet data into a collection
     *
     * @return collection of rows where each row is represented as a map of column
     *         name and column value
     * @author vamsikrishna.kayyala
     */
    public List<LinkedHashMap<String, String>> read() {
        if (isDataSaved("TOTAL_DATA")) {
            return processData(getSavedData("TOTAL_DATA"));
        } else {
            List<LinkedHashMap<String, String>> lReturn = new ArrayList<>();
            this.iCurrentRow = 0;
            while (HasNext()) {
                LinkedHashMap<String, String> lRow = new LinkedHashMap<>();
                for (String sHeader : sHeaders)
                    lRow.put(sHeader, getValue(sHeader));
                lRow.put("ROW_NUMBER", String.valueOf(this.iCurrentRow));
                lReturn.add(lRow);
            }
            saveData("TOTAL_DATA", lReturn);
            return processData(lReturn);
        }
    }

    public List<String[]> readAsList() {
        throw new RuntimeException("TO BE IMPLEMENTED FOR EXCEL, USE CSV FILE TO USE THIS METHOD");
    }

    /***
     * Reads the sheet data into a collection based on filter provided
     *
     * @param mFilters - map containing column name and values, used for filtering
     *                 Excel sheet to read data
     * @return collection of rows where each row is represented as a map of column
     *         name and column value
     * @author vamsikrishna.kayyala
     */
    public List<LinkedHashMap<String, String>> read(Map<String, String> mFilters) {
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
    public List<LinkedHashMap<String, String>> read(String[] arFilters) {
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
    public List<LinkedHashMap<String, String>> read(String sColumn, String sValue) {
        return processData(filterData(read(), sColumn, sValue));
    }

    public List<LinkedHashMap<String, String>> read(String sColumn, String sValue, String exclude_column, String exclude_value) {
        var completeData = read(sColumn, sValue);
        Predicate<LinkedHashMap<String, String>> filterCondition =
                map -> map.containsKey(exclude_column) && !map.get(exclude_column).equals(exclude_value);
        return processData(filterData(completeData, filterCondition));
    }

    /***
     * Reads a single column values with no duplicates
     *
     * @param column - column name to read from
     * @return returns the list of unique column values with key as column name
     * @author vamsikrishna.kayyala
     */
    public List<LinkedHashMap<String, String>> readUniqueColumnValues(String column) {
        var results = read();
        var uniqueData = getUniqueColumnValues(column, results);
        return processData(uniqueData);
    }

    public LinkedHashMap<String, String> readRandomRow(String column, String value) {
        var row = readRandomRow(read(column, value));
        if (row.isEmpty()) {
            throw new RuntimeException("Looks like there are no matching rows for column " +
                    "" + column + " and value " + value + " in " + sWBPath);
        } else {
            return row.get();
        }
    }

    public LinkedHashMap<String, String> readRandomRow(String column, String value, String exclude_column, String exclude_value) {
        var row = readRandomRow(read(column, value, exclude_column, exclude_value));
        if (row.isEmpty()) {
            throw new RuntimeException("Looks like there are no matching rows for column " +
                    "" + column + " and value " + value + " in " + sWBPath);
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
    public List<LinkedHashMap<String, String>> readUniqueColumnValues(String filterColumn,
                                                                      String filterValue,
                                                                      String column) {
        var results = read(filterColumn, filterValue);
        var uniqueData = getUniqueColumnValues(column, results);
        return processData(uniqueData);
    }

    private boolean HasNext() {
        if (xSheet.getLastRowNum() > iCurrentRow) {
            MoveNext();
            return true;
        } else {
            return false;
        }
    }

    private void MoveNext() {
        iCurrentRow++;
        xRow = xSheet.getRow(iCurrentRow);
    }

    private String getValue(String sHeader) {
        try {
            CellValue cell = evaluator.evaluate(xRow.getCell(sHeaders.indexOf(sHeader)));
            if (cell == null) {
                return "";
            } else {
                switch (cell.getCellType()) {
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanValue());
                    case STRING:
                        return cell.getStringValue();
                    case NUMERIC:
                        return String.valueOf(Math.round(cell.getNumberValue()));
                    default:
                        return "";
                }
            }
        } catch (Exception e) {
            return "";
        }
    }

    /***
     * Clears the Excel data by excluding the header row alone
     *
     * @author vamsikrishna.kayyala
     */
    public synchronized void clear() {
        int totalRows = xSheet.getLastRowNum();
        for (int i = 1; i <= totalRows; i++) {
            xSheet.removeRow(xSheet.getRow(i));
        }
        try (FileOutputStream outputStream = new FileOutputStream(sWBPath)) {
            xBook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * writes data into any given row based on a map
     *
     * @param sData - Map containing write data, keys are column names and values
     *              are column values to write
     * @param iRow  - Row number
     * @author vamsikrishna.kayyala
     *
     * NOTE: This method is not thread safe, if you try to write to same
     * excel across different threads, this method does not work as intended.
     */
    public synchronized void write(Map<String, String> sData, int iRow) {
        Row row;
        if (xSheet.getRow(iRow) != null) {
            row = xSheet.getRow(iRow);
        } else {
            row = xSheet.createRow(iRow);
        }
        Set<String> keySet = sData.keySet();
        for (String column : keySet) {
            Cell createCell = row.createCell(sHeaders.indexOf(column));
            createCell.setCellValue(sData.get(column));
        }

        try (FileOutputStream outputStream = new FileOutputStream(sWBPath)) {
            xBook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * writes list of data into Excel sheet using SXSSF class
     * This method can be used when writing any large number of records to excel, it takes very little time to process
     *
     * @param sData - List of Map containing write data, keys are column names and values
     *              are column values to write
     * @author vamsikrishna.kayyala
     *
     * NOTE: This method is not thread safe, if you try to write to same
     * excel across different threads, this method does not work as intended.
     */
    public synchronized void write(List<Map<String, String>> sData) {

        //first clears the data automatically before writing
        clear();

        SXSSFWorkbook wb;
        if (sWBPath.endsWith("xlsx")) {
            wb = new SXSSFWorkbook((XSSFWorkbook) xBook);
        } else {
            throw new RuntimeException("Cannot write content to a non xlsx file: " + sWBPath);
        }

        wb.setCompressTempFiles(true);
        SXSSFSheet sh = wb.getSheet(sWSName);

        int iRow = 1;

        for(Map<String, String> data: sData) {
            Row row;
            if (sh.getRow(iRow) != null) {
                row = sh.getRow(iRow);
            } else {
                row = sh.createRow(iRow);
            }
            Set<String> keySet = data.keySet();
            for (String column : keySet) {
                Cell createCell = row.createCell(sHeaders.indexOf(column));
                createCell.setCellValue(data.get(column));
            }
            iRow++;
        }

        try (FileOutputStream outputStream = new FileOutputStream(sWBPath)) {
            wb.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        var excels = DataParsers();
        excels.remove(sWBPath + sWSName);
        staticDataParsers.set(excels);
    }

    /***
     * writes data into last row based on a map
     *
     * @param sData - Map containing write data, keys are column names and values
     *              are column values to write
     * @author vamsikrishna.kayyala
     *
     * NOTE: This method is not thread safe, if you try to write to same
     * excel across different threads, this method does not work as intended.
     */
    public synchronized void write(Map<String, String> sData) {

        Row row;
        int iRow = xSheet.getLastRowNum() + 1;
        if (xSheet.getRow(iRow) != null) {
            row = xSheet.getRow(iRow);
        } else {
            row = xSheet.createRow(iRow);
        }
        Set<String> keySet = sData.keySet();
        for (String column : keySet) {
            Cell createCell = row.createCell(sHeaders.indexOf(column));
            createCell.setCellValue(sData.get(column));
        }

        try (FileOutputStream outputStream = new FileOutputStream(sWBPath)) {
            xBook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkForFilters(Map<String, String> mFilters) {
        Set<String> keySet = mFilters.keySet();
        for (String key : keySet) {
            if (!getValue(key).contentEquals(mFilters.get(key))) {
                return false;
            }
        }
        return true;
    }

}

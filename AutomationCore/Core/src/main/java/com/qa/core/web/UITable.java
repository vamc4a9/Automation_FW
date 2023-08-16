package com.qa.core.web;

import org.openqa.selenium.WebElement;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
@Lazy
public class UITable {

    private final ObjectHandler objectHandler;

    public UITable(ObjectHandler objectHandler) {
        this.objectHandler = objectHandler;
    }

    private String table_xpath;
    private String row_xpath;
    private String cell_xpath;

    // Assigning the default header row number as 1, user can change it using
    // setHeaderRowNum method
    private int header_row_num = 1;

    public void setHeaderRowNum(int iRow) {
        this.header_row_num = iRow;
    }

    public void setTableXPath(String table_identification) {
        this.table_xpath = table_identification;
    }

    public void setRowXPath(String row_identification) {
        this.row_xpath = row_identification;
    }

    public void setCellXPath(String cell_identification) {
        this.cell_xpath = cell_identification;
    }

    public WebElement getTable() throws Exception {
        return objectHandler.getObject("xpath", table_xpath);
    }

    public List<WebElement> getRows() {
        return objectHandler.getObjects("xpath", table_xpath + row_xpath);
    }

    public List<WebElement> getCells(int iRow) {
        return objectHandler.getObjects("xpath", "(" + table_xpath + row_xpath + ")[" + iRow + "]" + cell_xpath);
    }

    public WebElement getCell(int iRow, int iCol) throws Exception {
        return objectHandler.getObject("xpath",
                "(" + table_xpath + row_xpath + ")[" + iRow + "]" + cell_xpath + "[" + iCol + "]");
    }

    public List<String> getHeaders() {
        List<WebElement> cells = getCells(header_row_num);
        List<String> sHeaders = new ArrayList<String>();
        int null_index = 0;
        for (WebElement cell : cells) {
            if (cell.getText().trim().equals("")) {
                sHeaders.add("null" + null_index);
                null_index++;
            } else {
                sHeaders.add(cell.getText().trim());
            }
        }
        return sHeaders;
    }

    public int getRowCount() {
        return objectHandler.getObjects("xpath", table_xpath + row_xpath).size();
    }

    public void waitTillDisplayed(Duration timeout) {
        var startTime = System.currentTimeMillis();
        var endTime = startTime + timeout.toMillis();
        while (Calendar.getInstance().getTimeInMillis() < endTime) {
            if (objectHandler.getObjects("xpath", table_xpath).size() > 1)
                break;
        }
    }

    public Optional<List<String[]>> readTable(int numberOfRowsToRead) {
        List<WebElement> rows = getRows();
        if (rows.size() == 0) {
            return Optional.empty();
        }
        numberOfRowsToRead = Math.min(rows.size(), numberOfRowsToRead);
        List<String[]> actualTblData = new ArrayList<>();
        for (int i = 1; i <= numberOfRowsToRead; i++) {
            List<WebElement> cells = getCells(i);
            String[] rowData = new String[cells.size()];
            int index = 0;
            for (WebElement cell : cells) {
                rowData[index] = cell.getText().trim();
                index++;
            }
            actualTblData.add(rowData);
        }
        return Optional.of(actualTblData);
    }

}

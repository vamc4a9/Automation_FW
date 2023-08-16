package com.qa.core.dataLib;

import java.lang.reflect.Method;
import java.util.*;

import com.qa.core.dataHandler.DataParserResolver;
import com.qa.core.util.BeanUtil;
import com.qa.core.report.ReportManager;
import org.testng.annotations.DataProvider;

public class GenericDataProvider {

    ReportManager reportManager = BeanUtil.getBean(ReportManager.class);

    DataParserResolver excel = BeanUtil.getBean(DataParserResolver.class);

    /***
     * This method is called from @Test annotation to read data from Excel sheet
     * In order to pass the sheet name and filter parameters, user must use
     * @DataProviderArgs annotation along with @Test annotation
     *
     * @param method - it is automatically assigned from test class
     * @return - Map object containing row information from Excel sheet
     *
     * @author vamsikrishna.kayyala
     */
    @DataProvider(name = "excel-dp", parallel = true)
    public Object[][] getData(Method method) {
        return asTwoDimensionalArray(getDataFromExcelData(method));
    }

    public List<LinkedHashMap<String, String>> getDataFromExcelData(Method method) {
        DataProviderArgs annotation = method.getAnnotation(DataProviderArgs.class);
        String column = annotation.column();
        String value = annotation.value();
        String filters = annotation.filters();
        String sheetName = annotation.name();
        var oExcel = excel.getInstance(sheetName);
        List<LinkedHashMap<String, String>> results;

        if (!filters.contentEquals("")) {
            Map<String, String> mapFilters = getFilters(filters);
            results = oExcel.read(mapFilters);
        } else if (value.contentEquals("") && (!column.contentEquals(""))) {
            results = oExcel.readUniqueColumnValues(column);
        } else if (value.contentEquals("")) {
            results = oExcel.read();
        } else {
            if (value.equalsIgnoreCase("empty"))
                value = "";
            results = oExcel.read(column,value);
        }
        return results;
    }

    private Map<String, String> getFilters(String filters) {
        Map<String, String> filterMap = new HashMap<>();
        String[] arFilters = filters.split(";;");
        try {
            for(String filter : arFilters) {
                if (!filter.contentEquals(""))
                    filterMap.put(filter.split("==")[0], filter.split("==")[1]);
            }
        } catch (Exception e) {
            reportManager.report(e);
        }
        return filterMap;
    }

    private Object[][] asTwoDimensionalArray(List<LinkedHashMap<String, String>> mapData) {
        Object[][] results = new Object[mapData.size()][1];
        int index = 0;
        for (LinkedHashMap<String, String> data : mapData) {
            results[index++] = new Object[] {data};
        }
        return results;
    }

}
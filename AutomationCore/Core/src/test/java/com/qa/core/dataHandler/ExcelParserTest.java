package com.qa.core.dataHandler;

import com.qa.core.context.CoreParameters;
import com.qa.core.context.RunConfiguration;
import com.qa.core.dataLib.DataProcessor;
import com.qa.core.util.BeanUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class ExcelParserTest {
    @Mock
    List<String> sHeaders;
    @Mock
    DataProcessor dp;
    @Mock
    RunConfiguration config;
    @Mock
    BeanUtil beanUtil;
    @Mock
    Workbook xBook;
    @Mock
    Sheet xSheet;
    @Mock
    Row xRow;
    @Mock
    FormulaEvaluator evaluator;
    @InjectMocks
    ExcelParser excelParser;

    ExcelParser excelInstance;

    LinkedHashMap<String, String> expected_row = new LinkedHashMap<>();

    @BeforeMethod
    public void setUp() {
        expected_row.put("Test_ID", "TC_01");
        expected_row.put("Header1", "Value");
        expected_row.put("Header2", "Value");
        expected_row.put("Header3", "Value");
        expected_row.put("Header4", "Value");
        expected_row.put("Header5", "Value");
        expected_row.put("ROW_NUMBER", "1");
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetInstance() {
        var workbookPath = System.getProperty("user.dir")+"/build/resources/test/data/TestExcel.xlsx";
        var worksheet = "TestExcel";
        when(config.getProperty("env")).thenReturn("test");
        when(beanUtil.getBeanClass(ExcelParser.class))
                .thenReturn(new ExcelParser(new DataProcessor(),
                        new RunConfiguration(new CoreParameters()),
                        new BeanUtil()));
        excelInstance = excelParser.getInstance(workbookPath, worksheet);
        Assert.assertEquals(excelInstance.sHeaders, List.of("Test_ID", "Header1", "Header2", "Header3", "Header4", "Header5"));
        Assert.assertEquals(excelInstance.sWBPath, workbookPath);
        Assert.assertEquals(excelInstance.sWSName, worksheet);
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void testGet_as_headers() {
        Map<String, String> result = excelInstance.get_as_headers("Test_ID", "TC_Header");
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get("Key1"), "Value1");
        Assert.assertEquals(result.get("Key2"), "Value2");
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void testGet_as_query_params() {
        Map<String, String> result = excelInstance.get_as_query_params("Test_ID", "TC_Header");
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get("Key1"), "Value1");
        Assert.assertEquals(result.get("Key2"), "Value2");
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_full_read() {
        List<LinkedHashMap<String, String>> result = excelInstance.read();
        Assert.assertEquals(result.size(), 7);
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_read_using_column_and_value() {
        List<LinkedHashMap<String, String>> result = excelInstance.read("Test_ID", "TC_01");
        Assert.assertEquals(result, List.of(expected_row));
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_read_using_array_filter() {
        List<LinkedHashMap<String, String>> result = excelInstance.read(new String[]{"Test_ID==TC_01"});
        Assert.assertEquals(result, List.of(expected_row));
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_read_using_map_filter() {
        List<LinkedHashMap<String, String>> result = excelInstance.read(Map.of("Test_ID", "TC_01"));
        Assert.assertEquals(result, List.of(expected_row));
    }
}
package com.qa.core.dataHandler;

import com.qa.core.context.CoreParameters;
import com.qa.core.context.RunConfiguration;
import com.qa.core.dataLib.DataProcessor;
import com.qa.core.util.BeanUtil;
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

public class CsvParserTest {
    @Mock
    List<String> headers;
    @Mock
    List<LinkedHashMap<String, String>> data;
    @Mock
    RunConfiguration config;
    @Mock
    DataProcessor dp;
    @Mock
    BeanUtil beanUtil;
    @InjectMocks
    CsvParser csvParser;
    CsvParser csvInstance;
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

        var workbookPath = System.getProperty("user.dir")+"/build/resources/test/data/TestCsv.csv";
        var worksheet = "TestExcel";
        when(config.getProperty("env")).thenReturn("test");
        when(beanUtil.getBeanClass(CsvParser.class))
                .thenReturn(new CsvParser(new RunConfiguration(new CoreParameters()),
                        new DataProcessor(), new BeanUtil()));
        csvInstance = csvParser.getInstance(workbookPath, worksheet);
        Assert.assertEquals(csvInstance.headers, List.of("Test_ID", "Header1", "Header2", "Header3", "Header4", "Header5"));
        Assert.assertEquals(csvInstance.sWBPath, workbookPath);
        Assert.assertEquals(csvInstance.sWSName, worksheet);
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void testGet_as_headers() {
        Map<String, String> result = csvInstance.get_as_headers("Test_ID", "TC_Header");
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get("Key1"), "Value1");
        Assert.assertEquals(result.get("Key2"), "Value2");
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void testGet_as_query_params() {
        Map<String, String> result = csvInstance.get_as_query_params("Test_ID", "TC_Header");
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get("Key1"), "Value1");
        Assert.assertEquals(result.get("Key2"), "Value2");
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_full_read() {
        List<LinkedHashMap<String, String>> result = csvInstance.read();
        Assert.assertEquals(result.size(), 7);
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_read_using_column_and_value() {
        List<LinkedHashMap<String, String>> result = csvInstance.read("Test_ID", "TC_01");
        Assert.assertEquals(result, List.of(expected_row));
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_read_using_array_filter() {
        List<LinkedHashMap<String, String>> result = csvInstance.read(new String[]{"Test_ID==TC_01"});
        Assert.assertEquals(result, List.of(expected_row));
    }

    @Test(enabled = true, dependsOnMethods = "testGetInstance")
    public void test_read_using_map_filter() {
        List<LinkedHashMap<String, String>> result = csvInstance.read(Map.of("Test_ID", "TC_01"));
        Assert.assertEquals(result, List.of(expected_row));
    }
}


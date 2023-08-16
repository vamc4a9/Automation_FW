package com.qa.core.listeners;

import com.qa.core.util.BeanUtil;
import com.qa.core.dataLib.Csv;
import com.qa.core.util.TestUtil;
import com.qa.core.report.ReportManager;
import org.testng.*;

/**
 * The listener interface for receiving report events. The class that is
 * interested in processing a report event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addReportListener<code> method. When the report event
 * occurs, that object's appropriate method is invoked.
 */
public class TestListener implements ITestListener {

    public static int passed_tests = 0;
    public static int failed_tests = 0;
    public static int skipped_tests = 0;

    @Override
    public void onTestStart(ITestResult result) {
        ReportManager reporter = BeanUtil.getBean(ReportManager.class);
        TestUtil testUtil = BeanUtil.getBean(TestUtil.class);
        Csv csv = BeanUtil.getBean(Csv.class);
        csv.create("ExecutionSummary.csv", "TestClass,TestName,Status,FailReason," +
                "HtmlReportName,ExecutionTime(ms),Parameters...");
        testUtil.init(result);
        String name = testUtil.getUpdatedTestName();
        reporter.startTest(name, testUtil.getTestDescription(),
                result.getTestClass().getRealClass().getSimpleName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ReportManager reporter = BeanUtil.getBean(ReportManager.class);
        WriteToExecutionSummary(result,"Pass");
        reporter.endTest();
        passed_tests++;
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ReportManager reporter = BeanUtil.getBean(ReportManager.class);
        Throwable t = result.getThrowable();
        t.printStackTrace();
        Reporter.setCurrentTestResult(result);
        WriteToExecutionSummary(result,"Fail");
        reporter.endTest();
        failed_tests++;
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ReportManager reporter = BeanUtil.getBean(ReportManager.class);
        WriteToExecutionSummary(result,"Skip");
        reporter.endTest();
        skipped_tests++;
    }

    private void WriteToExecutionSummary(ITestResult result, String status) {
        Csv csv = BeanUtil.getBean(Csv.class);
        TestUtil testUtil = BeanUtil.getBean(TestUtil.class);
        testUtil.init(result);
        String failReason = getFailureReason(result, status);
        csv.write("ExecutionSummary.csv", "\"" + result.getMethod().getTestClass().getName() + "\"" +
                ",\"" + testUtil.getUpdatedTestName() + "\"" +
                ",\"" + status + "\"" +
                ",\"" + failReason + "\"" +
                ",\"" + ReportManager.htmlFileName + "\"" +
                ",\"" + (result.getEndMillis() - result.getStartMillis()) + "\"" +
                "," + testUtil.getParameters());
    }

    private String getFailureReason(ITestResult result, String status) {
        if (status.equalsIgnoreCase("fail")) {
            try {
                return result.getThrowable().getMessage();
            } catch (Exception e) {
                return "";
            }
        } else {
            return "";
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        ReportManager reporter = BeanUtil.getBean(ReportManager.class);
        WriteToExecutionSummary(result,"Fail");
        reporter.endTest();
    }

    @Override
    public void onStart(ITestContext context) {
    }

    @Override
    public void onFinish(ITestContext context) {
    }

}
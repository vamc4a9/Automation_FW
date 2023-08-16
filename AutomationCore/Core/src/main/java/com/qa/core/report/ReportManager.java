package com.qa.core.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.model.Category;
import com.aventstack.extentreports.model.Log;
import com.aventstack.extentreports.model.Test;
import com.aventstack.extentreports.model.context.NamedAttributeContextManager;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.qa.core.baseTest.BaseSpringTest;
import com.qa.core.context.CoreParameters;
import com.qa.core.context.RunConfiguration;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.StepResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.testng.Assert;
import org.testng.asserts.Assertion;
import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;
import org.testng.collections.Maps;

import javax.annotation.PostConstruct;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Lazy
public class ReportManager extends Assertion {

    private final static Logger LOGGER =
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final Map<AssertionError, IAssert<?>> m_errors = Maps.newLinkedHashMap();

    public static String resultsFolder = "";
    public static String screenshotsFolder = "";
    public static String htmlReportPath;
    public static String htmlFileName;
    private String initialHtmlName;
    private String reportOutputDirectory;
    private String resultsFolderName;
    private static ExtentReports report = new ExtentReports();
    private final ThreadLocal<Stack<ExtentTest>> testNodes = ThreadLocal.withInitial(Stack::new);
    private final ThreadLocal<Stack<String>> allureNodes = ThreadLocal.withInitial(Stack::new);
    private final ThreadLocal<ExtentTest> mainTest = new ThreadLocal<>();
    private final ThreadLocal<Map<String, String>> screenshots = ThreadLocal.withInitial(HashMap::new);
    private int resultHtmlCounter = 0;
    private final CoreParameters coreParameters;
    private final RunConfiguration config;
    private final String extentConfigPath;
    private final String browser;
    private final String environment;
    private final String appName;
    private final String abortFlag;
    private final String numberOfTestsPerHtml;
    private final boolean isReportingEnabled;

    public ReportManager(RunConfiguration config, CoreParameters coreParameters) {
        this.config = config;
        this.coreParameters = coreParameters;
        initialHtmlName = config.getProperty("html_report_name");
        reportOutputDirectory = config.getProperty("report_output_directory");
        resultsFolderName = config.getProperty("results_folder_name");
        extentConfigPath = config.getProperty("extent_report_config_path");
        numberOfTestsPerHtml = config.getProperty("number_of_tests_per_html");
        browser = config.getProperty("browser");
        environment = config.getProperty("env");
        appName = config.getProperty("application_name");
        abortFlag = config.getProperty("abort_flag");
        isReportingEnabled = Boolean.parseBoolean(config.getProperty("is_reporting_enabled", "true"));
    }

    @PostConstruct
    public void init() {
        if (!isReportingEnabled)
            return;
        if (resultsFolder.contentEquals("")) {
            String REPORT_FOLDER_NAME = getResultsFolderName();
            String REPORT_DIRECTORY = coreParameters.getWorkingDirectory() +
                    reportOutputDirectory;
            resultsFolder = REPORT_DIRECTORY + REPORT_FOLDER_NAME + "/";
            screenshotsFolder = REPORT_DIRECTORY + REPORT_FOLDER_NAME + "/Screenshots/";
            createFolder(screenshotsFolder);
            report = createReportHtml(initialHtmlName);
        }
    }

    private static void createFolder(String folder) {
        File file = new File(folder);
        file.mkdirs();
    }

    public static boolean containsHtml(String input) {
        return input.contains("<style>") && input.contains("</style>");
    }

    private static synchronized Test getNotCompletedTest(int size) {
        try {
            for (int i = 0; i < size; i++) {
                Test test = report.getReport().getTestList().get(i);
                if (test.getName().contains("Started::>>")) {
                    return report.getReport().getTestList().get(i);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static synchronized ExtentTest setLog(Test oldTest, ExtentTest newTest) {
        int iSize = oldTest.getChildren().size();
        int logSize = oldTest.getLogs().size();
        if (iSize == 0 && logSize == 0) {
            return newTest;
        }
        for (Test subTest : oldTest.getChildren()) {
            ExtentTest subNode = newTest.createNode(subTest.getName(), subTest.getDescription());
            subNode.getModel().setParent(newTest.getModel());
            setLog(subTest, subNode);
        }
        for (Log log : oldTest.getLogs()) {
            newTest.getModel().addLog(log);
        }
        return newTest;
    }

    private String getResultsFolderName() {
            if (resultsFolderName.contentEquals("")) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd' T 'HH.mm.ss");
                df.setTimeZone(TimeZone.getTimeZone("IST"));
                return df.format(new Date());
            } else {
                return resultsFolderName;
            }
    }

    private synchronized ExtentReports createReportHtml(String name) {
        ExtentReports report = new ExtentReports();
        ExtentSparkReporter spark;

        if (name.contentEquals("")) {
            if (resultHtmlCounter == 0) {
                htmlFileName = initialHtmlName;
            } else {
                htmlFileName = initialHtmlName.replace(".html",
                        "_" + resultHtmlCounter + ".html");
            }
            htmlReportPath = resultsFolder + htmlFileName;
        } else {
            htmlFileName = name;
            htmlReportPath = resultsFolder + name;
        }
        spark = new ExtentSparkReporter(htmlReportPath);
        try {
            spark.loadXMLConfig(new File(coreParameters.getTargetFolderPath() + extentConfigPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        report.attachReporter(spark);
        try {
            report.setSystemInfo("HostName", InetAddress.getLocalHost().getHostName());
            report.setSystemInfo("Browser", browser);
            report.setSystemInfo("Environment", environment);
            report.setSystemInfo("Application Name", appName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return report;
    }

    public void startTest(String sName, String sDescription, String sTag) {
        if (!isReportingEnabled)
            return;
        synchronized (this) {
            String testsCount = numberOfTestsPerHtml;
            if (!testsCount.equalsIgnoreCase("all")) {
                if (report.getReport().getTestList().size() >= Integer.parseInt(testsCount)) {
                    endResults(testsCount);
                }
            }
            ExtentTest test = report.createTest(sName, sDescription);
            test.getModel().setStartTime(Calendar.getInstance().getTime());

            List<Label> labels = new ArrayList<>();
            labels.add(new Label().setName(sTag));

            test.getModel().setName("Started::>>" + sName + "<<<<" + System.currentTimeMillis() + "_" + Thread.currentThread().getId());
            test.assignCategory(sTag);
            setTestNode(test);
            mainTest.set(test);
        }
    }

    public void endTest() {
        if (!isReportingEnabled)
            return;
        synchronized (this) {
            ExtentTest test = mainTest.get();
            test.getModel().setEndTime(Calendar.getInstance().getTime());
            String name = test.getModel().getName();
            name = name.replace("Started::>>", "");
            name = name.split("<<<<")[0];
            test.getModel().setName(name);
            testNodes.remove();
            allureNodes.remove();
        }
    }

    //To create an expandable test node inside the main test in html file
    public void startNode(String details) {
        if (!isReportingEnabled)
            return;
        try {
            ExtentTest test = getCurrentTest();
            ExtentTest node = test.createNode(details);
            setTestNode(node);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Executed outside the test hence, went into exception" +
                    e.getMessage());
        } finally {
            startAllureNode(details);
        }
    }

    private void startAllureNode(String details) {
        try {
            final String uuid = UUID.randomUUID().toString();
            Allure.getLifecycle().startStep(uuid, new StepResult().setName(details));
            setAllureNode(uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAllureNode(String details, String status) {
        try {
            final String uuid = UUID.randomUUID().toString();
            Allure.getLifecycle().startStep(uuid, new StepResult().setName(details).setStatus(getAllureStatus(status)));
            setAllureNode(uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //To create an expandable test node inside the main test in html file
    public void startNode(String name, String details) {
        if (!isReportingEnabled)
            return;
        ExtentTest test = getCurrentTest();
        ExtentTest node = test.createNode(name, details);

        final String uuid = UUID.randomUUID().toString();
        Allure.getLifecycle().startStep(uuid, new StepResult().setName(details).setDescription(details));

        setAllureNode(uuid);
        setTestNode(node);
    }

    // To start the node as a 2-column table where the first column contains the key and the second one is row
    public void startNode(Map<String, String> map) {
        if (!isReportingEnabled)
            return;
        String sTblStyle = "<style>\n" + "tr:nth-child(even) {background-color: #f2f2f2;}\n" + "table {\n"
                + "  width: 100%;\n" + "}\n" + "tbody tr td:first-child {\n" + "  width: 8em;\n" + "  min-width: 8em;\n"
                + "  max-width: 25em;\n" + "  word-break: break-all;\n" + "}\n" + "</style>";
        ExtentTest test = getCurrentTest();
        ExtentTest node = test.createNode(sTblStyle + MarkupHelper.createTable(mapToArray(map)).getMarkup());

        final String uuid = UUID.randomUUID().toString();
        Allure.getLifecycle().startStep(uuid, new StepResult().setName(map.toString()));

        setAllureNode(uuid);
        setTestNode(node);
    }

    public String getStatus() {
        return getCurrentTest().getStatus().toString();
    }

    public void endNode() {
        if (!isReportingEnabled)
            return;
        try {
            Stack<ExtentTest> nodes = getTestNodes();
            nodes.pop();
            testNodes.set(nodes);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Executed outside the test hence, went into exception" +
                    e.getMessage());
        } finally {
            endAllureNode();
        }
    }

    private void endAllureNode() {
        var nodes = getAllureNodes();
        Allure.getLifecycle().stopStep(nodes.peek());
        nodes.pop();
        allureNodes.set(nodes);
    }

    private Stack<ExtentTest> getTestNodes() {
        return testNodes.get();
    }

    private Stack<String> getAllureNodes() {
        return allureNodes.get();
    }

    private void setTestNode(ExtentTest node) {
        Stack<ExtentTest> nodes = testNodes.get();
        nodes.push(node);
        testNodes.set(nodes);
    }

    private void setAllureNode(String uuid) {
        var nodes = allureNodes.get();
        nodes.push(uuid);
        allureNodes.set(nodes);
    }

    public void removeTest() {
        if (!isReportingEnabled)
            return;
        report.removeTest(mainTest.get().getModel().getName());
    }

    public synchronized ExtentTest getCurrentTest() {
        if (!isReportingEnabled)
            return null;
        try {
            return getTestNodes().peek();
        } catch (Exception e) {
            return null;
        }
    }

    private synchronized SoftAssert getAssert() {
        return BaseSpringTest.getSoftAssert();
    }

    /***
     * This report step should only be used between startNode() and endNode() methods.
     * Otherwise, it will display the report details at the top the html file.
     *
     * @param status - pass/fail/warning/info
     * @param details - report details
     * @author vamsikrishna.kayyala
     */
    public void report(String status, String details) {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status))
            report_step(status, details, null);
    }

    /***
     * This report step is used when user wants to create a separate expandable node in html file
     * and display the details under it.
     * if user has already created a node using startNode() method, then using this method will
     * create a sub node under the parent node
     * @param status - pass/fail/warning/info
     * @param sMessage - message to create a node
     * @param details - report details
     * @author vamsikrishna.kayyala
     */
    public void report(String status, String sMessage, String details) {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status))
            report_step(status, sMessage, details, null);
    }

    /***
     * This report step should only be used between startNode() and endNode() methods.
     * Otherwise, it will display the report details at the top the html file.
     *
     * @param status - pass/fail/warning/info
     * @param details - report details
     * @param driver - WebDriver to take screenshot
     * @author vamsikrishna.kayyala
     */
    public void report(String status, String details, WebDriver driver) {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status))
            report_step(status, details, driver);
    }

    /***
     * This report step is used when user wants to create a separate expandable node in html file
     * and display the details under it.
     * if user has already created a node using startNode() method, then using this method will
     * create a sub node under the parent node
     *
     * @param status - pass/fail/warning/info
     * @param sMessage - message to create a node
     * @param driver - WebDriver to take screenshot
     * @param details - report details
     * @author vamsikrishna.kayyala
     */
    public void report(String status, String sMessage, String details, WebDriver driver) {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status))
            report_step(status, sMessage, details, driver);
    }

    public void report_extent(String status, String sMessage, String details, WebDriver driver) {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status))
            report_step_extent(status, sMessage, details, driver);
    }

    public void report(String status, List<String> list) {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status))
            report(status, "", MarkupHelper.createUnorderedList(list).getMarkup(), null);
    }

    public void report(String status, String[][] array) {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status))
            report(status, "", MarkupHelper.createTable(array).getMarkup(), null);
    }

    public void report(String status, Map<String, String> map) {
        if (!isReportingEnabled)
            return;
        String sTblStyle = "<style>\n" + "tr:nth-child(even) {background-color: #f2f2f2;}\n" + "table {\n"
                + "  width: 100%;\n" + "}\n" + "tbody tr td:first-child {\n" + "  width: 8em;\n" + "  min-width: 8em;\n"
                + "  max-width: 25em;\n" + "  word-break: break-all;\n" + "}\n" + "</style>";
        report(status, "", sTblStyle + MarkupHelper.createTable(mapToArray(map)).getMarkup(), null);
    }

    public void report(String status, String message, Map<String, String> map) {
        if (!isReportingEnabled)
            return;
        String sTblStyle = "<style>\n" + "tr:nth-child(even) {background-color: #f2f2f2;}\n" + "table {\n"
                + "  width: 100%;\n" + "}\n" + "tbody tr td:first-child {\n" + "  width: 8em;\n" + "  min-width: 8em;\n"
                + "  max-width: 25em;\n" + "  word-break: break-all;\n" + "}\n" + "</style>";
        report(status, message, sTblStyle + MarkupHelper.createTable(mapToArray(map)).getMarkup(), null);
    }

    public void report_extent(String status, String message, Map<String, String> map) {
        if (!isReportingEnabled)
            return;
        String sTblStyle = "<style>\n" + "tr:nth-child(even) {background-color: #f2f2f2;}\n" + "table {\n"
                + "  width: 100%;\n" + "}\n" + "tbody tr td:first-child {\n" + "  width: 8em;\n" + "  min-width: 8em;\n"
                + "  max-width: 25em;\n" + "  word-break: break-all;\n" + "}\n" + "</style>";
        report_extent(status, message, sTblStyle + MarkupHelper.createTable(mapToArray(map)).getMarkup(), null);
    }

    @Override
    public void onAssertSuccess(IAssert<?> assertCommand) {
        if (!isReportingEnabled)
            return;
        if (assertCommand.getExpected() != null)
            report("pass", "Expected: " + assertCommand.getExpected() +
                    ", Actual: " + assertCommand.getActual());
        else
            report("pass", assertCommand.getMessage());
    }

    @Override
    public void onAssertFailure(IAssert<?> assertCommand, AssertionError ex) {
        if (!isReportingEnabled)
            return;
        if (assertCommand.getExpected() != null)
            report("fail", "Expected: " + assertCommand.getExpected() +
                    ", Actual: " + assertCommand.getActual());
        else
            report("fail", assertCommand.getMessage());
        m_errors.put(ex, assertCommand);
    }

    @Override
    public void onBeforeAssert(IAssert<?> assertCommand) {
        if (!isReportingEnabled)
            return;
        if (assertCommand.getMessage().trim().contentEquals(""))
            startNode("Expand to view the assertion");
        else
            startNode(assertCommand.getMessage());
    }

    @Override
    public void onAfterAssert(IAssert<?> assertCommand) {
        if (!isReportingEnabled)
            return;
        endNode();
    }

    public void report(String message, Exception exception) {
        if (!isReportingEnabled)
            return;
        boolean bFlag = createConditionalNode(message);
        try {
            ExtentTest test = getCurrentTest();
            test.fail(exception);
            addExceptionToAllure(message, exception);
            doSoftAssert("fail", message + "->" + exception.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Executed outside the test hence, went into exception" +
                    exception.getMessage());
        } finally {
            if (bFlag)
                endNode();
        }
    }

    private void addExceptionToAllure(String message, Exception exception) {
        if (!isReportingEnabled)
            return;
        startAllureNode(message, "fail");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String sStackTrace = sw.toString();
        Allure.addAttachment("Exception", sStackTrace);
        endAllureNode();
    }

    public void report(Exception exception) {
        if (!isReportingEnabled)
            return;
        boolean bFlag = createConditionalNode(exception.getMessage());
        try {
            ExtentTest test = getCurrentTest();
            test.fail(exception);
            doSoftAssert("fail", exception.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Executed outside the test hence, went into exception" +
                    exception.getMessage());
        } finally {
            addExceptionToAllure(exception.getMessage(), exception);
            if (bFlag)
                endNode();
        }
    }

    private void report_step(String status, String sMessage, String details, WebDriver driver) {
        boolean bFlag = createConditionalNode(sMessage);
        var path = "";
        try {
            path = reportExtent(status, details, driver);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, status + " -> Executed outside the test hence, went into exception -> "
                    + sMessage + " -> " + details);
        } finally {
            reportAllure(status, sMessage, details, path);
            if (bFlag)
                endNode();
            doSoftAssert(status, sMessage + "->" + details);
        }
    }

    private void report_step_extent(String status, String sMessage, String details, WebDriver driver) {
        boolean bFlag = createConditionalNode(sMessage);
        try {
            reportExtent(status, details, driver);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, status + " -> Executed outside the test hence, went into exception -> "
                    + sMessage + " -> " + details);
        } finally {
            if (bFlag)
                endNode();
            doSoftAssert(status, sMessage + "->" + details);
        }
    }

    private void reportAllure(String status, String sMessage, String details, String screenshotPath) {
        try {
            if (containsHtml(details)) {
                attachHtmlToAllure(status, sMessage, details);
            } else {
                Allure.step(details, getAllureStatus(status));
            }
            if (!screenshotPath.contentEquals("")) {
                addScreenshotToAllure(sMessage, screenshotPath);
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    public void reportAsTable(String status, List<Map<String, String>> data) {
        if (!isReportingEnabled)
            return;
        var htmlContent = generateHtmlTable(data);
        report(status, htmlContent);
    }

    private String generateHtmlTable(List<Map<String, String>> data) {
        StringBuilder html = new StringBuilder();
        if (!data.isEmpty()) {
            Map<String, String> firstMap = data.get(0);
            Set<String> columns = firstMap.keySet();
            html.append("<table class=\"my-table\">");
            html.append("<tr class=\"header\">");
            for (String column : columns) {
                html.append("<th>").append(column).append("</th>");
            }
            html.append("</tr>");
            int rowIndex = 0;
            for (Map<String, String> map : data) {
                String status = map.getOrDefault("status", null);
                status = (status == null) ? (map.getOrDefault("Status", null)) : null;

                String rowClass = rowIndex % 2 == 0 ? "even" : "odd";

                if (status != null && !status.isEmpty()) {
                    if ("pass".equalsIgnoreCase(status)) {
                        rowClass = "pass";
                    } else if ("fail".equalsIgnoreCase(status)) {
                        rowClass = "fail";
                    }
                }

                html.append("<tr class=\"").append(rowClass).append("\">");
                for (String column : columns) {
                    String value = map.get(column);
                    html.append("<td>").append(value).append("</td>");
                }
                html.append("</tr>");
                rowIndex++;
            }
            html.append("</table>");
            html.append("<style>");
            html.append(".my-table tr.header { background-color: #cdd2ff; }");
            html.append(".my-table tr.even { background-color: #f2f2f2; }");
            html.append(".my-table tr.pass { background-color: #c8e6c9; }");
            html.append(".my-table tr.fail { background-color: #ffcdd2; }");
            html.append("</style>");
        }
        return html.toString();
    }

    private void attachHtmlToAllure(String status, String sMessage, String details) throws IOException {
        startAllureNode(sMessage, status);
        try (InputStream stream = Files.newInputStream(Paths.get(writeToHtmlFile(details, "html")))) {
            Allure.addAttachment(sMessage, null, stream, "html");
        }
        endAllureNode();
    }

    public void addAttachmentToAllure(String status, String sMessage, String filePath) throws IOException {
        if (!isReportingEnabled)
            return;
        if (is_status_to_be_reported(status)) {
            startAllureNode(sMessage, status);
            try (InputStream stream = Files.newInputStream(Paths.get(filePath))) {
                Allure.addAttachment("Expand to view the attachment", null, stream, null);
            }
            endAllureNode();
        }
    }

    private void addScreenshotToAllure(String message, String path) {
        try {
            if (path.startsWith("./")) {
                path = path.replace("./", resultsFolder);
            }
            if (message.contentEquals(""))
                message = "Screenshot";
            startAllureNode(message);
            Allure.addAttachment("Screenshot", new FileInputStream(path));
            endAllureNode();
        } catch (IOException e) {
            /* Do nothing */
        }
    }

    private String reportExtent(String status, String details, WebDriver driver) {
        var path = "";
        ExtentTest test = getCurrentTest();
        if (driver != null) {
            path = getScreenshot(driver);
            assert path != null;
            test.log(getStatus(status), details,
                    MediaEntityBuilder.createScreenCaptureFromPath(path).build());
        } else {
            test.log(getStatus(status), details);
        }
        return path;
    }

    private void report_step(String status, String details, WebDriver driver) {
        boolean bFlag = createConditionalNode(details);
        var path = "";
        try {
            path = reportExtent(status, details, driver);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, status + " -> Executed outside the test hence, went into exception -> "
                    + " -> " + details);
        } finally {
            reportAllure(status, "", details, path);
            if (bFlag)
                endNode();
            doSoftAssert(status, details);
        }
    }

    public void markTestStatusAsSkip() {
        if (!isReportingEnabled)
            return;
        markTestStatusAsSkip("SKIPPED TEST");
    }

    public void markTestStatusAsSkip(String details) {
        if (!isReportingEnabled)
            return;
        try {
            ExtentTest test = getCurrentTest();
            String desc = test.getModel().getDescription();
            desc = desc.contentEquals("") ? "<b><u><i>" + details + "</b></u></i>" : desc + "<br><b><u><i>" + details + "</b></u></i>";
            test.getModel().setDescription(desc);
            test.getModel().setStatus(getStatus("skip"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Executed outside the test hence, went into exception");
        }
    }

    private boolean createConditionalNode(String message) {
        boolean bFlag = false;
        try {
            // if the current test already has a children's node,
            // then we need to start one child node to display the results appropriately
            if (getCurrentTest().getModel().getChildren().size() > 0) {
                startNode(message);
                bFlag = true;
            }
        } catch (Exception ignored) {
        }
        return bFlag;
    }

    public void doSoftAssert(String status, String message) {
        if (!isReportingEnabled)
            return;
        try {
            SoftAssert sa = getAssert();
            if (status.equalsIgnoreCase("fail")) {
                sa.fail(message);
                if (abortFlag.equalsIgnoreCase("true")) {
                    Assert.fail(message);
                }
            } else {
                sa.assertTrue(true, message);
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    public void captureScreenshot(WebDriver driver) {
        if (!isReportingEnabled)
            return;
        report_step("info", "screenshot", "", driver);
    }

    public void captureScreenshot(String sMessage, WebDriver driver) {
        if (!isReportingEnabled)
            return;
        try {
            report_step("info", sMessage, "", driver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endResults() {
        if (!isReportingEnabled)
            return;
        endResults("all");
    }

    private synchronized void endResults(String count) {
        int finishedCount = 0;
        int test_cases_per_html = 0;
        int testSize = report.getReport().getTestList().size();

        if (!count.contentEquals("all")) {
            test_cases_per_html = Integer.parseInt(count);
        }

        for (int i = 0; i < testSize; i++) {
            if (!report.getReport().getTestList().get(i).getName().startsWith("Started::>>")) {
                finishedCount++;
            }
        }

        if (finishedCount >= test_cases_per_html) {
            resultHtmlCounter++;
            ExtentReports reports = createReportHtml("");
            NamedAttributeContextManager<Category> categoryTests = report.getReport().getCategoryCtx();
            for (int i = 0; i < testSize - finishedCount; i++) {
                Test test = getNotCompletedTest(testSize);
                assert test != null;
                ExtentTest test2 = reports.createTest(test.getName(), test.getDescription());
                Set<Category> categories = test.getCategorySet();
                for (Category category : categories) {
                    test2.assignCategory(category.getName());
                }
                report.removeTest(test.getName());
                categoryTests.removeTest(test);
                setLog(test, test2);
            }
            report.getReport().refresh();
            report.flush();
            report = reports;
        }
    }

    private String writeToHtmlFile(String content, String name) {
        String file_name = name + "_" + Calendar.getInstance().getTimeInMillis() + ".html";
        String absPath = screenshotsFolder + file_name;

        try (FileWriter fileWriter = new FileWriter(absPath)) {
            fileWriter.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return absPath;
    }

    private Status getStatus(String status) {
        switch (status.toLowerCase()) {
            case "pass":
                return Status.PASS;

            case "fail":
                return Status.FAIL;

            case "warning":
                return Status.WARNING;

            case "skip":
                return Status.SKIP;

            default:
                return Status.INFO;
        }
    }

    private io.qameta.allure.model.Status getAllureStatus(String status) {
        switch (status.toLowerCase()) {
            case "fail":
                return io.qameta.allure.model.Status.FAILED;

            case "warning":
                return io.qameta.allure.model.Status.BROKEN;

            case "skip":
                return io.qameta.allure.model.Status.SKIPPED;

            default:
                return io.qameta.allure.model.Status.PASSED;
        }
    }

    private String getScreenshot(WebDriver driver) {
        try {
            String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            if (checkScreenshot(base64))
                return getScreenshot(base64);
            else {
                String file_name = Calendar.getInstance().getTimeInMillis() + ".png";
                String absPath = screenshotsFolder + file_name;
                String relativePath = absPath.replace(resultsFolder, "./");
                convertBase64ToImg(base64, absPath);
                setScreenshot(base64, relativePath);
                return relativePath;
            }
        } catch (Exception e) {
            report(e);
            return null;
        }
    }

    private void convertBase64ToImg(String base64, String path) {
        byte[] data = DatatypeConverter.parseBase64Binary(base64);
        File file = new File(path);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkScreenshot(String base64) {
        return screenshots.get().containsKey(base64);
    }

    private String getScreenshot(String base64) {
        return screenshots.get().get(base64);
    }

    private void setScreenshot(String base64, String file) {
        Map<String, String> map = screenshots.get();
        map.put(base64, file);
        screenshots.set(map);
    }

    private String[][] mapToArray(Map<String, String> map) {
        return map.entrySet().stream().map(e -> new String[]{e.getKey(), e.getValue()})
                .toArray(String[][]::new);
    }

    private boolean is_status_to_be_reported(String status) {
        status = status.toLowerCase();
        var statusesToReport = config.getProperty("statuses_to_report", "all").toLowerCase();
        return statusesToReport.contentEquals("all")
                || status.equals("fail")
                || statusesToReport.contains(status);
    }
}
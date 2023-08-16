package com.qa.core.network;

import com.qa.core.web.WebDriverProvider;
import com.qa.core.report.ReportManager;
import com.qa.core.util.UtilityLib;
import com.qa.core.web.UiLib;
import org.openqa.selenium.WebDriver;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * This class contains all the reusable methods needed for automation
 */

@Component
@Lazy
public class NetworkLogLib {

    private final NetworkLog network;
    private final ReportManager reporter;
    private final WebDriverProvider webDriverProvider;
    private final UiLib uilib;
    private final UtilityLib utilLib;

    public NetworkLogLib(NetworkLog network, ReportManager reporter,
                         WebDriverProvider webDriverProvider, UiLib uilib, UtilityLib utilLib) {
        this.network = network;
        this.reporter = reporter;
        this.webDriverProvider = webDriverProvider;
        this.uilib = uilib;
        this.utilLib = utilLib;
    }
    /***
     * get the active web driver from WebDriverProvider
     *
     * @return WebDriver
     * @author vamsikrishna.kayyala
     */
    public WebDriver driver() {
        try {
            return webDriverProvider.getActiveWebDriver();
        } catch (Exception e) {
            reporter.report(e);
            return null;
        }
    }

    /**
     * Waits for all the requests and responses to be received in the application. This
     * method logic is faulty as the number of requests logged is not always equal to
     * the number of responses logged. Instead of this method, we can use
     * {@link #waitForServiceResponse(String sTitle, String sEndUrl) waitForServiceResponse}
     * or
     * {@link #waitForServiceResponse(String sTitle, String sEndUrl, Duration timeout_seconds) waitForServiceResponse}
     *
     * @deprecated
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public void waitUntilAllTheResonsesAreReceived() {
        final long timeInMillis = Calendar.getInstance().getTimeInMillis();
        while (timeInMillis + 15000 > Calendar.getInstance().getTimeInMillis()) {
            try {
                List<ResponseLog> responseLog = network.getResponseLog(getDriverHandle(), driver().getTitle());
                List<RequestLog> requestLog = network.getRequestLog(getDriverHandle(), driver().getTitle());

                if (responseLog.size() == requestLog.size())
                    return;

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Waits for a given EndPoint URL to be available in the response log. Max wait
     * time is 15 seconds
     *
     * @param sTitle  - Title of the page in which the endpoint URL has to be verified
     * @param sEndUrl - part of the endpoint URL that needs to be checked
     * @return true if the response is available in the given timeout, else false
     * @author vamsikrishna.kayyala
     */
    public boolean waitForServiceResponse(String sTitle, String sEndUrl) {
        return waitForServiceResponse(sTitle, sEndUrl, Duration.ofSeconds(15));
    }

    /**
     * Waits for a given EndPoint URL to be available in the response log. Max wait
     * time is 15 seconds
     *
     * @param sEndUrl - part of the endpoint URL that needs to be checked
     * @return true if the response is available in the given timeout, else false
     * @author vamsikrishna.kayyala
     */
    public boolean waitForServiceResponse(String sEndUrl) {
        return waitForServiceResponse(driver().getTitle(), sEndUrl, Duration.ofSeconds(15));
    }


    /**
     * Waits for a given EndPoint URL to be available in the response log. Max wait
     * time is 15 seconds
     *
     * @param sEndUrl - part of the endpoint URL that needs to be checked
     * @param duration - duration object representing wait time
     * @return true if the response is available in the given timeout, else false
     * @author vamsikrishna.kayyala
     */
    public boolean waitForServiceResponse(String sEndUrl, Duration duration) {
        return waitForServiceResponse(driver().getTitle(), sEndUrl, duration);
    }

    /***
     * Waits for a given EndPoint url to be available in the respose log.
     *
     * @param sTitle          - Title of the page in which end point url has to be
     *                        verified
     * @param sEndUrl         - part of end point url that needs to checked
     * @param duration        - Max wait time out
     * @return true if response is available in given timeout else false
     * @author vamsikrishna.kayyala
     */
    public boolean waitForServiceResponse(String sTitle, String sEndUrl, Duration duration) {
        reporter.startNode("Wait for "+ sEndUrl +" response");
        final long startTime = Calendar.getInstance().getTimeInMillis();
        final long endTime = startTime + duration.toMillis();
        while (Calendar.getInstance().getTimeInMillis() < endTime) {
            try {
                List<ResponseLog> log = network.getResponseLog(getDriverHandle(), sTitle);
                for (int i = log.size() - 1; i >= 0; i--) {
                    ResponseLog response = log.get(i);
                    if (isMatchingResponse(response, sEndUrl)) {
                        long elapsedTime = (Calendar.getInstance().getTimeInMillis() - startTime)/1000;
                        reporter.report("pass", "Service response is retrieved in " + elapsedTime + " seconds");
                        reporter.report("pass", "Response",
                                response.getResponse());
                        reporter.endNode();
                        response.setIsVerified(true);
                        return true;
                    }
                }
                utilLib.fn_wait(Duration.ofSeconds(2));
            } catch (Exception ignored) {
            }
        }
        reporter.report("info", "Wait for service response",
                sEndUrl + " service response is not available even after waiting for "
                        + duration.toSecondsPart() + " seconds");
        reporter.endNode();
        return false;
    }

    private boolean isMatchingResponse(ResponseLog response, String sEndUrl) {
        return response.getUrl().endsWith(sEndUrl.trim())
                && response.getStatusCode().startsWith("2")
                && !response.getIsVerified();
    }

    /***
     * clear the network log activity for any page title
     *
     * @param sPage - page title {@link NetworkLog}
     * @author vamsikrishna.kayyala
     */
    public void clearNetworkLog(String sPage) {
        network.clearLog(sPage);
        uilib.Refresh();
    }

    private String getDriverHandle() {
        try {
            return webDriverProvider.getActiveWebDriver().getWindowHandle();
        } catch (Exception e) {
            reporter.report(e);
            return null;
        }
    }

    /**
     * Verify if there are any 5XX status code API calls during the code execution
     * from AUT for all the pages.
     *
     * @see NetworkLog
     * @author vamsikrishna.kayyala
     */
    public void checkFor5XXerrors() {
        reporter.startNode("Checking 5XX error across all the pages");
        // Get the response logs
        Map<String, LinkedList<ResponseLog>> log = network.getResponseLogs(getDriverHandle());

        // Access all the pages from the respones logs
        Set<String> pages = log.keySet();

        boolean bFlag = true;

        // Loop through all the pages
        for (String page : pages) {

            boolean flag = true;
            int iterator = 1;

            // This while loop is added to handle concurrentModificationException
            while (flag) {

                try {

                    List<ResponseLog> responseLog = network.getResponseLog(getDriverHandle(), page);

                    // Loop through all the response instances from each page
                    for (ResponseLog response : responseLog) {

                        // Verify if Response instance was already verified during the execution of this
                        // function at a different place in test execution
                        if (!network.getReportedResponses(webDriverProvider.getActiveWebDriver().getWindowHandle()).contains(response)) {

                            // Check if the status code is starting with 5
                            if (response.getStatusCode().startsWith("5")) {

                                // Get response into a map
                                Map<String, String> map1 = response.toMap();
                                map1.remove("URL");

                                // Get the corresponding request for the response using request id
                                RequestLog request = network.getRequestLog(getDriverHandle(), driver().getTitle(),
                                        response.getRequestId());

                                // Get the request instance into a map
                                Map<String, String> map2 = request.toMap();
                                map2.putAll(map1);
                                map2.put("Page Title", page);

                                // Report the failed API call as a table in the extent report
                                reporter.report("fail", map2);

                                // Maintain a flag to know if any failure is reported
                                bFlag = false;
                            }
                        }

                        // once the request is iterated, then store it so that same cant be used for
                        // verification again
                        network.setReportedResponses(response, webDriverProvider.getActiveWebDriver().getWindowHandle());

                    }

                    // To exit the while loop if no exceptions occured
                    flag = false;
                } catch (Exception e) {

                    // To continue the while loop if any exception occured
                    flag = true;

                    // To break the while if loop has already run for 5 times
                    if (iterator == 5)
                        break;
                }
                iterator++;
            }

        }

        // Pass the function if no failure is reported
        if (bFlag)
            reporter.report("pass", "There are no 5XX errors");
        reporter.endNode();
    }

    /**
     * Verify if there are any 5XX status code API calls during the code execution
     * from AUT for a specific page.
     *
     * @param sTitle - Title of the page for which we are verifying 5XX errors
     * @return true if no 5XX errors found for the page, else false
     * @see NetworkLog
     * @author vamsikrishna.kayyala
     */
    public boolean checkFor5XXerrors(String sTitle) {

        // Get the response log for given page title
        List<ResponseLog> log = network.getResponseLog(getDriverHandle(), sTitle);
        reporter.startNode("Checking 5XX error in " + driver().getTitle() + " page");
        boolean bFlag = true;

        boolean flag = true;
        int iterator = 1;

        // This while loop is added to handle concurrentModificationException
        while (flag) {
            bFlag = true;
            try {
                // Iterate through all the response instances from the page
                for (ResponseLog response : log) {

                    // Verify if Response instance was already verified during the execution of this
                    // function at a different place in test execution
                    if (!network.getReportedResponses(webDriverProvider.getActiveWebDriver().getWindowHandle()).contains(response)) {

                        // Check if the status code is starting with 5
                        if (response.getStatusCode().startsWith("5")) {

                            // Get response into a map
                            Map<String, String> map1 = response.toMap();
                            map1.remove("URL");

                            // Get the corresponding request for the response using request id
                            RequestLog request = network.getRequestLog(getDriverHandle(), driver().getTitle(), response.getRequestId());

                            // Get the request instance into a map
                            Map<String, String> map2 = request.toMap();
                            map2.putAll(map1);
                            map2.put("Page Title", sTitle);

                            // Report the failed API call as a table in the extent report
                            reporter.report("fail", map2);

                            // Maintain a flag to know if any failure is reported
                            bFlag = false;
                        }

                        // once the request is iterated, then store it so that same cant be used for
                        // verification again
                        network.setReportedResponses(response, webDriverProvider.getActiveWebDriver().getWindowHandle());
                    }
                }
                flag = false;
            } catch (Exception e) {
                flag = true;
                if (iterator == 5)
                    break;
            }
            iterator++;
        }

        // Pass the function if no failure is reported
        if (bFlag)
            reporter.report("pass", "There are no 5XX errors");
        reporter.endNode();
        return bFlag;

    }

    /**
     * read the response from NetworkLog given the endpoint URL in a specific page
     *
     * @param sTitle - page title to filter out network log
     * @param sEP    - Part of the endpoint URL (verifies the contains logic)
     * @return response data for the given endpoint URL, empty string if no response found
     * @author vamsikrishna.kayyala
     */
    public String getResponse(String sTitle, String sEP) {
        try {
            return network.getResponseByUrlAndResponseContent(getDriverHandle(), sTitle, sEP).getResponse();
        } catch (Exception e) {
            reporter.report("fail", "Get Service Response",
                    "There is no response available for the endpoint '" + sEP + "' from + " + sTitle + " page");
            return "";
        }
    }

    public ResponseLog getResponseByUrl(String page_title, String sUrl) {
        return network.getResponseByUrl(getDriverHandle(), page_title, sUrl);
    }

    public ResponseLog getResponse(String page_title, String sUrl, String response) {
        return network.getResponse(getDriverHandle(), page_title, sUrl, response);
    }

    public RequestLog getRequest(String page_title, String sUrl, String post_data) {
        return network.getRequest(getDriverHandle(), page_title, sUrl, post_data);
    }

    public ResponseLog getResponseById(String page_title, String requestId) {
        return network.getResponseById(getDriverHandle(), page_title, requestId);
    }

    /**
     * Waits for all the requests and responses to be received in the application. This
     * method logic is faulty as the number of requests logged is not always equal to
     * the number of responses logged. Instead of this method, we can use
     * {@link #waitForServiceResponse(String sTitle, String sEndUrl) waitForServiceResponse}
     * or
     * {@link #waitForServiceResponse(String sTitle, String sEndUrl, Duration timeout_seconds) waitForServiceResponse}
     *
     * @deprecated
     * @param pageTitle      - Title of the page
     * @param sUrl           - URL to wait for response
     * @param posConditions  - Positive conditions to consider the response received
     * @param negConditions  - Negative conditions to consider the response not received
     * @param timeOutSeconds - Max wait time in seconds
     * @return true if the response is available in the given timeout, else false
     * @see NetworkLog
     */
    @Deprecated
    public boolean waitForResponse(String pageTitle, String sUrl, String[] posConditions, String[] negConditions,
                                   int timeOutSeconds) {
        return network.waitForResponse(getDriverHandle(),
                pageTitle, sUrl, posConditions, negConditions, timeOutSeconds);
    }

    public ResponseLog getResponseByUrlAndResponseContent(String page_title, String sUrl) {
        return network.getResponseByUrlAndResponseContent(getDriverHandle(), page_title, sUrl);
    }

}


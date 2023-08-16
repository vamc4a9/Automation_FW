package com.qa.core.retryAnalyzer;

import com.qa.core.util.BeanUtil;
import com.qa.core.context.RunConfiguration;
import com.qa.core.report.ReportManager;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class RetryTest implements IRetryAnalyzer {

    private static Map<LinkedList<Object>, Integer> RETRY_COUNTER = new HashMap<>();
    private static final Duration RETRY_DELAY = Duration.ofSeconds(15);

    ReportManager reporter = BeanUtil.getBean(ReportManager.class);

    static RunConfiguration configuration = BeanUtil.getBean(RunConfiguration.class);

    private static int retryLimit = Integer.parseInt(configuration.getProperty("retry_limit_for_failed_test", "0"));;

    @Override
    public boolean retry(ITestResult result) {
        //Added this list object to find the unique test object and assign the retry counter
        LinkedList<Object> testObj = new LinkedList<>();
        testObj.add(result.getMethod().getMethodName());
        testObj.add(result.getTestClass().getName());
        testObj.add(getParameters(result));
        testObj.add(result.getName());
        if (retryLimit > 0 && isRetryEnabled(result)) {
            if (RETRY_COUNTER.containsKey(testObj)) {
                if (RETRY_COUNTER.get(testObj) < retryLimit) {
                    reporter.markTestStatusAsSkip("Skipped the test as we are going to retry the execution");
                    RETRY_COUNTER.put(testObj, RETRY_COUNTER.get(testObj) + 1);
                    Instant retryTime = Instant.now().plus(RETRY_DELAY);
                    while (Instant.now().isBefore(retryTime)) {}
                    return true;
                } else {
                    RETRY_COUNTER.remove(testObj);
                    return false;
                }
            } else {
                RETRY_COUNTER.put(testObj, 1);
                Instant retryTime = Instant.now().plus(RETRY_DELAY);
                while (Instant.now().isBefore(retryTime)) {}
                reporter.markTestStatusAsSkip("Skipped the test as we are going to retry the execution");
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean isRetryEnabled(ITestResult testResult) {
        try {
            var method = testResult
                    .getMethod()
                    .getConstructorOrMethod()
                    .getMethod();
            if (method.isAnnotationPresent(EnableRetry.class))
                return method.getAnnotation(EnableRetry.class).retry();
            else
                return true;
        } catch (Exception e) {
            return true;
        }
    }

    public String getParameters(ITestResult result) {
        Object[] data = result.getParameters();
        String parameters = "";
        for(Object o : data) {
            parameters = (parameters.contentEquals("")) ? o + "" : parameters + o;
        }
        return parameters;
    }
}

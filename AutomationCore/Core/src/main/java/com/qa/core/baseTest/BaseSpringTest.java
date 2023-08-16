package com.qa.core.baseTest;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.IHookCallBack;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.asserts.SoftAssert;

public class BaseSpringTest<T extends BaseSpringTest<T>> extends AbstractTestNGSpringContextTests {

    private static final String SOFT_ASSERT = "softAssert";

    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        SoftAssert softAssert = new SoftAssert();
        testResult.setAttribute(SOFT_ASSERT, softAssert);
        callBack.runTestMethod(testResult);
        performSoftAssert(softAssert, testResult);
    }

    private void performSoftAssert(SoftAssert softAssert, ITestResult testResult) {
        try {
            softAssert.assertAll();
        } catch (AssertionError e) {
            handleSoftAssertionFailure(testResult, e);
        }
    }

    private void handleSoftAssertionFailure(ITestResult testResult, AssertionError e) {
        testResult.setThrowable(e);
        testResult.setStatus(ITestResult.FAILURE);
    }

    public static SoftAssert getSoftAssert() {
        return getSoftAssertFromTestResult(Reporter.getCurrentTestResult());
    }

    public static ITestResult getITestResult() {
        return Reporter.getCurrentTestResult();
    }

    private static SoftAssert getSoftAssertFromTestResult(ITestResult result) {
        Object object = result.getAttribute(SOFT_ASSERT);
        if (object instanceof SoftAssert) {
            return (SoftAssert) object;
        }
        throw new IllegalStateException("Could not find a soft assertion object");
    }
}

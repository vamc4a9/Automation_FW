package com.qa.core.listeners;

import com.qa.core.web.WebDriverProvider;
import com.qa.core.util.BeanUtil;
import com.qa.core.context.RunConfiguration;
import com.qa.core.report.ReportManager;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.WebDriverListener;

import java.lang.reflect.Method;

public class WebDriverListenerImpl implements WebDriverListener {

    WebDriverProvider webDriverProvider = BeanUtil.getBean(WebDriverProvider.class);
    RunConfiguration config = BeanUtil.getBean(RunConfiguration.class);
    ReportManager reporter = BeanUtil.getBean(ReportManager.class);

    @Override
    public void beforeAnyCall(Object target, Method method, Object[] args) {
        boolean abort_flag = false;
        if (config.checkProperty("abort_flag")) {
            abort_flag = Boolean.valueOf(config.getProperty("abort_flag"));
        }

        if ((reporter.getCurrentTest() != null)
                && (reporter.getStatus().equalsIgnoreCase("fail") && abort_flag)) {
            Assert.fail("Ending the test execution as there is a failure");
            reporter.report(new Exception("Ending the test execution as there is a failure"));
        }
    }

    @Override
    public void afterClose(WebDriver driver) {
        try {
            switchToLastWindow(driver);
            webDriverProvider.initializeNetworkCapture(driver);
        } catch (Exception e) {
            reporter.report(e);
        }
    }

    @Override
    public void afterQuit(WebDriver driver) {
        try {
            webDriverProvider.removeActiveDriver();
        } catch (Exception e) {
            reporter.report(e);
        }
    }

    public void switchToLastWindow(WebDriver driver) {
        Object[] array = driver.getWindowHandles().toArray();
        if (array.length > 0)
            if (!getWindowHandle(driver).equals(array[array.length - 1].toString()))
                driver.switchTo().window(array[array.length - 1].toString());
    }

    public String getWindowHandle(WebDriver driver) {
        try {
            return driver.getWindowHandle();
        } catch (Exception e) {
            return "";
        }
    }
}

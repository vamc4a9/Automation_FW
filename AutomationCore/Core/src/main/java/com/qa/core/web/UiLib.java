package com.qa.core.web;

import com.qa.core.basePage.Core;
import com.qa.core.dataHandler.DataParser;
import com.qa.core.dataHandler.DataParserResolver;
import com.qa.core.util.BeanUtil;
import com.qa.core.context.CoreParameters;
import com.qa.core.context.RunConfiguration;
import com.qa.core.report.ReportManager;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.testng.Assert;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * This class contains all the reusable methods needed for automation
 */

@Component
@Lazy
public class UiLib {

    private final UiWait oWait;

    private final ReportManager reporter;

    private final DataParserResolver dataParserResolver;

    private final WebDriverProvider webDriverProvider;

    private final RunConfiguration config;

    public UiLib(UiWait oWait, ReportManager reporter, DataParserResolver dataParserResolver,
                 WebDriverProvider webDriverProvider, RunConfiguration config) {
        this.oWait = oWait;
        this.reporter = reporter;
        this.dataParserResolver = dataParserResolver;
        this.webDriverProvider = webDriverProvider;
        this.config = config;
    }

    /**
     * To open the application url in WebDriver
     *
     * @param sAppName - Application name from test.properties
     * @author vamsikrishna.kayyala
     */
    public void openUrl(String sAppName) {
        openUrl(sAppName, "default");
    }

    /**
     * To open the application url in WebDriver
     *
     * @param sAppName - Application name from test.properties
     * @param tagName - a custom tag that can be used to identify the webDriver using webDriverProvider
     * @author vamsikrishna.kayyala
     */
    public void openUrl(String sAppName, String tagName) {
        try {
            reporter.startNode("open " + sAppName + " application");
            webDriverProvider.initializeDriver(tagName);
            String sUrl = config.getProperty(sAppName + "_application_url");
            driver().get(sUrl);
            reporter.report("info", "open " + sAppName + " application", sUrl + " is launched", null);
            reporter.endNode();
        } catch (Exception e) {
            reporter.report("open " + sAppName + " application", e);
        }
    }

    /***
     * Close the active webDriver window
     *
     * @author vamsikrishna.kayyala
     */
    public void closeWindow() {
        driver().close();
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

    public void quitDriver() {
        driver().quit();
    }

    /***
     * loads a different url onto already opened webdriver
     *
     * @param sUrl - URL that needs to be loaded
     * @author vamsikrishna.kayyala
     */
    public void redirectUrl(String sUrl) {
        driver().get(sUrl);
        reporter.report("info", "redirect to a new url", sUrl + " is opened", null);
    }

    /***
     * Take a screenshot
     *
     * @param message - message to display along with screenshot
     * @author vamsikrishna.kayyala
     */
    public void captureScreenshot(String message) {
        reporter.report("info", message + " - screenshot", "", driver());
    }

    /**
     * This class verifies the page existence by checking the core controls. This
     * method automatically gets called whenever a new instance of any page is
     * created.
     *
     * @param oCls - Class name of the page instance
     * @author vamsikrishna.kayyala
     */
    public boolean waitForPageLoad(Class<?> oCls) {

        Field[] fields = oCls.getDeclaredFields();

        boolean fFlag = false;
        List<String> sFailedControls = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Core.class)) {
                try {
                    Object newInstance = BeanUtil.getBean(oCls);
                    FluentWait<WebDriver> waiter = oWait.page_waiter();
                    waiter.until(ExpectedConditions.visibilityOf((WebElement) field.get(newInstance)));
                } catch (Exception e) {
                    sFailedControls.add(field.getName());
                    fFlag = true;
                    e.printStackTrace();
                }
            }
        }

        if (fFlag) {
            reporter.report("fail", oCls.getSimpleName() + " Verification", oCls.getSimpleName()
                            + " is not displayed as expected, following core controls are not available " + sFailedControls,
                    driver());
            Assert.fail(oCls.getSimpleName()
                    + " is not displayed as expected, following core controls are not available " + sFailedControls);
            return false;
        } else {
            reporter.report("info", oCls.getSimpleName() + " Verification",
                    oCls.getSimpleName() + " is displayed as expected");
            return true;
        }
    }

    /**
     * This class verifies the page existence by checking the core controls. This
     * method automatically gets called whenever a new instance of any page is
     * created.
     *
     * use waitForPageLoad method instead
     *
     * @param oCls - Class name of the page instance
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public boolean WaitForPageLoad(Class<?> oCls) {
        return waitForPageLoad(oCls);
    }

    public WebElement getPublicWebElement(Class<?> oCls, String element) {
        Field[] fields = oCls.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().contentEquals(element)) {
                try {
                    return (WebElement) field.get(BeanUtil.getBean(oCls));
                } catch (Exception e) {
                    reporter.report("Error Accessing " + element + " from " + oCls.getName(), e);
                }
            }
        }
        reporter.report("fail", "There is no field named " + element + " in " + oCls.getName());
        return null;
    }

    /***
     * Switch to last available window in webdriver for example, if user clicks on a
     * link and a new window or a tab opens, then user can call this method to shift
     * focus to new window
     *
     * @author vamsikrishna.kayyala
     */
    public void switchToLastWindow() {
        Object[] array = driver().getWindowHandles().toArray();
        if (array.length > 0)
            if (!driver().getWindowHandle().equals(array[array.length - 1].toString()))
                driver().switchTo().window(array[array.length - 1].toString());
    }

    /***
     * Refreshes the application
     * use refresh method instead
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public void Refresh() {
        refresh();
    }

    /***
     * Refreshes the application
     *
     * @author vamsikrishna.kayyala
     */
    public void refresh() {
        reporter.startNode("Refresh the page");
        driver().navigate().refresh();
        waitForPageLoad(CoreParameters.getPage());
        reporter.endNode();
    }

    /***
     * Verify if a given element is available or not in application
     *
     * use exist method instead
     *
     * @param oEle - WebElement
     * @return - true/false
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public boolean Exist(WebElement oEle) {
        return exist(oEle);
    }

    /***
     * Verify if a given element is available or not in application
     *
     * @param oEle - WebElement
     * @return - true/false
     * @author vamsikrishna.kayyala
     */
    public boolean exist(WebElement oEle) {
        try {
            oWait.toBeVisible(oEle);
            return oEle.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Deprecated
    public boolean Exist(By locator) {
        try {
            oWait.toBeVisible(locator);
            return driver().findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean exist(By locator) {
        try {
            oWait.toBeVisible(locator);
            return driver().findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /***
     * Verify if the given WebElement is enabled or not in application, this method
     * doesn't capture screenshot upon verification
     *
     * @param oEle - WebElement object
     * @param name - Logical name of WebElement for reporting
     * @author vamsikrishna.kayyala
     */
    public void isEnabled(WebElement oEle, String name) {
        isEnabled(oEle, name, false);
    }

    /***
     * Verify if the given WebElement is disabled or not in application, this method
     * doesn't capture screenshot upon verification
     *
     * @param oEle - WebElement object
     * @param name - Logical name of WebElement for reporting
     * @author vamsikrishna.kayyala
     */
    public void isDisabled(WebElement oEle, String name) {
        isDisabled(oEle, name, false);
    }

    private WebDriver getScreenshotDriver(boolean screenshot) {
        if (screenshot) {
            return driver();
        } else {
            return null;
        }
    }

    /***
     * Verify if the given WebElement is enabled or not in application and capture
     * screenshot upon verification
     *
     * @param oEle - WebElement object
     * @param name - Logical name of WebElement for reporting
     * @author vamsikrishna.kayyala
     */
    public void isEnabled(WebElement oEle, String name, boolean screenshot) {
        try {
            oWait.toBeVisible(oEle);
            if (oEle.isEnabled())
                reporter.report("pass", "Verify that " + name + " is enabled", name + " is enabled as expected",
                        getScreenshotDriver(screenshot));
        } catch (Exception e) {
            reporter.report(e);
        }
    }

    /***
     * Verify if the given WebElement is disabled or not in application and capture
     * screenshot upon verification
     *
     * @param oEle - WebElement object
     * @param name - Logical name of WebElement for reporting
     * @author vamsikrishna.kayyala
     */
    public void isDisabled(WebElement oEle, String name, boolean screenshot) {
        try {
            oWait.toBeVisible(oEle);
            if (!oEle.isEnabled())
                reporter.report("pass", "Verify that " + name + " is disabled", name + " is disabled as expected",
                        getScreenshotDriver(screenshot));
        } catch (Exception e) {
            reporter.report(e);
        }
    }

    /***
     * Verify if the given list of WebElements are displayed or not in application,
     * screenshots gets captured only while verifying first element in the list
     *
     * @param oEle    - Map of WebElement objects where key is the WebElement and
     *                Value is the logical name of webElement
     * @param message - message that needs to be displayed while verifying the
     *                elements in report
     * @author vamsikrishna.kayyala
     */
    public void isDisplayed(Map<WebElement, String> oEle, String message) {
        int index = 1;
        reporter.startNode(message);
        for (WebElement webElement : oEle.keySet()) {
            try {
                oWait.toBeVisible(webElement);

                boolean bFlag = !webElement.isDisplayed();

                if (index == 1) {
                    if (!bFlag)
                        reporter.report("pass", oEle.get(webElement) + " is displayed", driver());
                    else
                        reporter.report("fail", oEle.get(webElement) + " is not displayed", driver());
                } else if (!bFlag) {
                    reporter.report("pass", oEle.get(webElement) + " is displayed");
                } else {
                    reporter.report("fail", oEle.get(webElement) + " is not displayed");
                }

                index++;

            } catch (Exception e) {
                reporter.report(e);
            }
        }
        reporter.endNode();
    }

    /***
     * Verify if the given list of WebElements are displayed or not in application,
     * screenshots gets captured only while verifying first element in the list
     *
     * use verifyText instead
     *
     * @param oEle            - WebElement object
     * @param sEleName         - Logical Name of the WebElement for reporting
     * @param sValidationText - Expected text to be displayed for WebElement
     * @param validationType - Type of validation, possible values: equals/contains
     *
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public void VerifyText(WebElement oEle, String sEleName, String sValidationText, String validationType) {
        verifyText(oEle, sEleName, sValidationText, validationType);
    }

    /***
     * Verify if the given list of WebElements are displayed or not in application,
     * screenshots gets captured only while verifying first element in the list
     *
     * @param oEle            - WebElement object
     * @param sEleName         - Logical Name of the WebElement for reporting
     * @param sValidationText - Expected text to be displayed for WebElement
     * @param validationType - Type of validation, possible values: equals/contains
     *
     * @author vamsikrishna.kayyala
     */
    public void verifyText(WebElement oEle, String sEleName, String sValidationText, String validationType) {

        try {
            oWait.toBeVisible(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Verify text for " + sEleName,
                        sEleName + " is not displayed to check that it " + validationType + " " + sValidationText);
                return;
            }
            String sActualText = oEle.getText();
            switch (validationType.toLowerCase()) {
                case "contains":
                    if (sActualText.contains(sValidationText))
                        reporter.report("pass", "Verify text for " + sEleName,
                                sValidationText + " is available in application as expected", null);
                    else
                        reporter.report("fail", "Verify text for " + sEleName,
                                "Expected Text: " + sValidationText + "; Actual Text:" + sActualText, driver());
                    break;
                default:
                    if (sActualText.trim().contentEquals(sValidationText.trim()))
                        reporter.report("pass", "Verify text for " + sEleName,
                                sValidationText + " is matched with application as expected", null);
                    else
                        reporter.report("fail", "Verify text for " + sEleName,
                                "Expected Text: " + sValidationText + "; Actual Text:" + sActualText, driver());
                    break;
            }

        } catch (Exception e) {
            reporter.report("Verify text for " + sEleName, e);
        }
    }

    /***
     * Retrieves element's text from application
     *
     * @param oEle     - WebElement object
     * @param sEleName - Logical name of WebElement
     * @return returns the text of webElement
     * @author vamsikrishna.kayyala
     */
    public String getText(WebElement oEle, String sEleName) {
        try {
            oWait.toBeVisible(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "get text for " + sEleName, sEleName + " is not displayed");
                return "";
            }
            String sActualText = oEle.getText().trim();
            reporter.report("info", "get text for " + sEleName, sActualText + " is retrieved from " + sEleName);
            return sActualText;
        } catch (Exception e) {
            reporter.report("get text for " + sEleName, e);
            return "";
        }
    }

    /***
     * Clicks element using selenium's default click method
     *
     * use click method instead
     *
     * @param oEle  - WebElement object
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public void Click(WebElement oEle, String sName) {
        click(oEle, sName);
    }

    /***
     * Clicks element using selenium's default click method
     *
     * @param oEle  - WebElement object
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    public void click(WebElement oEle, String sName) {
        try {
            oWait.toBeClickable(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Click " + sName + " Element", sName + " is not displayed to click");
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Click " + sName + " Element", sName + " is not enabled to click");
            } else {
                oEle.click();
                reporter.report("Pass", "Click " + sName + " Element", sName + " is clicked");
            }
        } catch (Exception e) {
            reporter.report("Click " + sName + " Element", e);
        }
    }

    /**
     * Selects an option from the dropdown based on its value attribute.
     *
     * @param oEle        The WebElement representing the dropdown.
     * @param data        The value of the option to be selected.
     * @param objectName  A descriptive name for the dropdown (used for reporting purposes).
     */
    public void selectByValue(WebElement oEle, String data, String objectName) {
        try {
            oWait.toBeClickable(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Select " + data + " from " + objectName + " Element", objectName + " is not displayed to click");
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Select " + data + " from " + objectName + " Element", objectName + " is not enabled to click");
            } else {
                Select dropdown = new Select(oEle);
                dropdown.selectByValue(data);
                reporter.report("Pass", "Select " + data + " from " + objectName + " Element", data + " is selected");
            }
        } catch (Exception e) {
            reporter.report("Select " + data + " from " + objectName + " Element", e);
        }
    }

    /**
     * Selects an option from the dropdown based on its visible text.
     *
     * @param oEle        The WebElement representing the dropdown.
     * @param data        The visible text of the option to be selected.
     * @param objectName  A descriptive name for the dropdown (used for reporting purposes).
     */
    public void selectByText(WebElement oEle, String data, String objectName) {
        try {
            oWait.toBeClickable(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Select " + data + " from " + objectName + " Element", objectName + " is not displayed to click");
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Select " + data + " from " + objectName + " Element", objectName + " is not enabled to click");
            } else {
                Select dropdown = new Select(oEle);
                dropdown.selectByVisibleText(data);
                reporter.report("Pass", "Select " + data + " from " + objectName + " Element", data + " is selected");
            }
        } catch (Exception e) {
            reporter.report("Select " + data + " from " + objectName + " Element", e);
        }
    }

    /**
     * Selects the first option from the dropdown.
     *
     * @param oEle        The WebElement representing the dropdown.
     * @param objectName  A descriptive name for the dropdown (used for reporting purposes).
     */
    public void selectFirstOption(WebElement oEle, String objectName) {
        try {
            oWait.toBeClickable(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Select from " + objectName + " Element", objectName + " is not displayed to click");
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Select  from " + objectName + " Element", objectName + " is not enabled to click");
            } else {
                Select dropdown = new Select(oEle);
                if (dropdown.getOptions().size()>0)
                    dropdown.selectByIndex(1);
                reporter.report("Pass", "Select from " + objectName + " Element", "selected");
            }
        } catch (Exception e) {
            reporter.report("Select  from " + objectName + " Element", e);
        }
    }

    /***
     * Click on element using {@link JavascriptExecutor}
     *
     * use clickByJs method instead
     *
     * @param oEle  - WebElement object
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public void ClickByJs(WebElement oEle, String sName) {
        clickByJs(oEle, sName);
    }

    /***
     * Click on element using {@link JavascriptExecutor}
     *
     * @param oEle  - WebElement object
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    public void clickByJs(WebElement oEle, String sName) {
        try {
//			oWait.toBeClickable(oEle);
            JavascriptExecutor executor = (JavascriptExecutor) driver();
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Click " + sName + " Element", sName + " is not displayed to click");
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Click " + sName + " Element", sName + " is not enabled to click");
            } else {
                executor.executeScript("arguments[0].click();", oEle);
                reporter.report("Pass", "Click " + sName + " Element", sName + " is clicked");
            }
        } catch (Exception e) {
            reporter.report("Click " + sName + " Element", e);
        }
    }

    /***
     * Click on element using {@link Actions}
     *
     * use clickByAction method instead
     *
     * @param oEle  - WebElement object
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public void ClickByAction(WebElement oEle, String sName) {
        clickByAction(oEle, sName);
    }

    /***
     * Click on element using {@link Actions}
     *
     * @param oEle  - WebElement object
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    public void clickByAction(WebElement oEle, String sName) {
        try {
            oWait.toBeClickable(oEle);
            Actions oAction = new Actions(driver());
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Click " + sName + " Element", sName + " is not displayed to click");
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Click " + sName + " Element", sName + " is not enabled to click");
            } else {
                oAction.click(oEle).perform();
                reporter.report("Pass", "Click " + sName + " Element", sName + " is clicked");
            }
        } catch (Exception e) {
            reporter.report("Click " + sName + " Element", e);
        }
    }

    /***
     * Enters data into given WebElement using selenium's default SendKeys, before
     * setting the value, we use selenium's clear method and then enter the data
     *
     * @param oEle  - WebElement object
     * @param sData - Data to be passed to the WebElement
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    public void set(WebElement oEle, String sData, String sName) {
        try {
            oWait.toBeVisible(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Click " + sName + " Element",
                        sName + " element is not displayed to enter " + sData);
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Click " + sName + " Element",
                        sName + " element is not enabled to enter " + sData);
            } else {
                oEle.clear();
                oEle.sendKeys(sData);
                reporter.report("Pass", "Set data into " + sName + " Element", sData + " is passed to " + sName);
            }
        } catch (Exception e) {
            reporter.report("Set " + sData + " into " + sName, e);
        }
    }

    /***
     * Enters data into given WebElement using selenium's default SendKeys, before
     * setting the value, we use a custom implementation of clear method and then
     * enter the data
     *
     * @param oEle  - WebElement object
     * @param sData - Data to be passed to the WebElement
     * @param sName - Logical name of WebElement
     * @author vamsikrishna.kayyala
     */
    public void clearAndSet(WebElement oEle, String sData, String sName) {
        try {
            oWait.toBeVisible(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Click " + sName + " Element",
                        sName + " element is not displayed to enter " + sData);
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Click " + sName + " Element",
                        sName + " element is not enabled to enter " + sData);
            } else {
                clear(oEle);
                oEle.sendKeys(sData);
                reporter.report("Pass", "Set data into " + sName + " Element", sData + " is passed to " + sName);
            }
        } catch (Exception e) {
            reporter.report("Set " + sData + " into " + sName, e);
        }
    }

    /***
     * Custom implementation of clear method if in case default clear is not working
     *
     * @param oEle - WebElement
     * @author vamsikrishna.kayyala
     */
    private void clear(WebElement oEle) {
        final long timeInMillis = Calendar.getInstance().getTimeInMillis();
        while (timeInMillis + 20000 > Calendar.getInstance().getTimeInMillis()) {
            try {
                if (oEle.getAttribute("value").equals(""))
                    return;
                else {
                    oEle.sendKeys(Keys.BACK_SPACE);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /***
     * Use to set any confidential data into application this will mask the data in
     * the extent report
     *
     * @param oEle  - WebElement object
     * @param sData - Data to be passed onto oEle
     * @param sName - logical name of the WebElement for reporting
     *
     * @author vamsikrishna.kayyala
     */
    public void setPw(WebElement oEle, String sData, String sName) {
        try {
            oWait.toBeVisible(oEle);
            if (!oEle.isDisplayed()) {
                reporter.report("Fail", "Set password into " + sName + " Element",
                        sName + " element is not displayed to enter " + sData);
            } else if (!oEle.isEnabled()) {
                reporter.report("Fail", "Set password into " + sName + " Element",
                        sName + " element is not enabled to enter " + sData);
            } else {
                oEle.clear();
                oEle.sendKeys(sData);
                reporter.report("Pass", "Set password into " + sName + " Element", "******* is passed to " + sName);
            }
        } catch (Exception e) {
            reporter.report("Set password into " + sName, e);
        }
    }


    /***
     * bring focus to any WebElement
     *
     * @param element - WebElement object
     * @author vamsikrishna.kayyala
     */
    public void focusElement(WebElement element) {
        Actions oActs = new Actions(driver());
        oActs.moveToElement(element).perform();
    }

    /***
     * provide a pop-up so that execution can be kept on hold until user manually
     * dismisses the popup
     *
     * @param title - message that needs to be displayed in the popup
     * @author vamsikrishna.kayyala
     */
    public void popup(String title) {
        JFrame oFrame = new JFrame("Message!");
        oFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        JOptionPane.showMessageDialog(oFrame, title, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    /***
     * Read the application title
     *
     * @return returns the application title
     * @author vamsikrishna.kayyala
     */
    public String getTitle() {
        return driver().getTitle();
    }

    /***
     * Read the application url
     *
     * @return returns the application url
     * @author vamsikrishna.kayyala
     */
    public String getUrl() {
        return driver().getCurrentUrl();
    }

    /***
     * Return the Excel object for workbook specified in test.properties file
     *
     * @param sheetName - Worksheet name
     * @return {@link DataParser} instance
     * @author vamsikrishna.kayyala
     */
    private DataParser getDataSheet(String sheetName) {
        try {
            return dataParserResolver.getInstance(sheetName);
        } catch (Exception e) {
            reporter.report(e);
            return null;
        }
    }

    /***
     * Collect all the table data in the form of list of LinkedHashMap
     *
     * @param oTbl    - UITable object
     * @param tblName - table name for reporting
     * @return collection of entire table data
     * @author vamsikrishna.kayyala
     */
    public List<LinkedHashMap<String, String>> getTableData(UITable oTbl, String tblName) {

        // Initializing the empty return collection
        List<LinkedHashMap<String, String>> tableData = new ArrayList<LinkedHashMap<String, String>>();

        try {
            // Get application table object from UItable
            WebElement table = oTbl.getTable();

            // Fail if the application table is not available
            if (table == null) {
                reporter.report("fail", "Collect Table Data from " + tblName, "Table is not displayed", driver());
            } else {

                // Getting all the header names from table
                List<String> headers = oTbl.getHeaders();

                // Getting the rows size from table
                int row_size = oTbl.getRowCount();

                // Looping through all the rows
                for (int i = 1; i <= row_size; i++) {

                    // Getting the individual cells rom
                    List<WebElement> cells = oTbl.getCells(i);

                    // Initializing the empty hashmap to store row data
                    LinkedHashMap<String, String> rowData = new LinkedHashMap<String, String>();
                    for (String header : headers) {

                        // Excluding the columns where the header is blank
                        if (!header.startsWith("null"))
                            rowData.put(header, cells.get(headers.indexOf(header)).getText().trim());
                    }

                    // Adding the row data to table
                    tableData.add(rowData);
                }
            }

            reporter.report("pass", "Collect Table Data from " + tblName,
                    "Collected " + tableData.size() + " row(s) of data");
        } catch (Exception e) {
            reporter.report("Collect Table Data from " + tblName, e);
        }

        return tableData;
    }

    /***
     * Verifies if there is any duplicate data available in the table.
     *
     * @param data          - table data collected using
     *                      {@link #getTableData(UITable, String)}
     * @param excluded_cols - Columns to exclude during verification
     * @param tblName       - table name for reporting
     * @author cb-it-01-1492
     */
    public void check_duplicates(List<LinkedHashMap<String, String>> data, String[] excluded_cols, String tblName) {

        int index = 0;
        int iFail = 0;

        // Getting the copy of main data
        List<LinkedHashMap<String, String>> tempData = new ArrayList<>(data);

        // Initializing a variable to find the pass/fail status
        boolean pFlag = true;

        // Iterating through all the rows from temp dataset
        for (LinkedHashMap<String, String> row : tempData) {

            // Removing the current row data to avoid comparing with the same
            data.clear();
            data.addAll(tempData);
            data.remove(index);

            // Iterating through all the rows data from main dataset
            for (LinkedHashMap<String, String> row2 : data) {

                // Removing the excluded columns for validation
                for (String column : excluded_cols) {
                    row2.remove(column);
                    row.remove(column);
                }

                // Initializing a variable to find row validation status
                boolean bFlag = true;
                for (String key : row2.keySet()) {
                    if (!row2.get(key).contentEquals(row.get(key))) {
                        bFlag = false;
                        break;
                    }
                }

                if (bFlag) {
                    reporter.report("fail", "Checking duplicate records",
                            row + " is available more than once in " + tblName);
                    pFlag = false;
                    iFail++;
                    break;
                }
            }

            // Exits the validation if there are more than 5 duplicate records in the table
            // Added this condition to avoid checking all the records for verification
            if (iFail > 5)
                break;

            index++;
        }

        if (pFlag)
            reporter.report("pass", "Checking duplicate records", "There are no duplicate records in " + tblName);

    }

    /***
     * Checks if the application table data is matching with the data available in
     * TableValidation sheet
     *
     * @param oTbl    - {@link UITable} object that represents application table
     * @param tblName - Logical name of table for reporting
     * @param test_id - Test_Id column value from TableValidation sheet in workbook
     * @author vamsikrishna.kayyala
     */
    public void tableValidation(UITable oTbl, String tblName, String test_id) {

        reporter.startNode("Table validation for " + tblName);

        try {

            // Get application table object from UItable
            WebElement table = oTbl.getTable();

            // Fail if the application table is not available
            if (table == null) {
                reporter.report("fail", tblName + " is not displayed in application", driver());
            } else {
                // Access the TableValidation sheet
                var dataSheet = getDataSheet("TableValidation");

                // Read data from TableValidation sheet where Test_Id is equal to whatever is
                // passed to this method
                assert dataSheet != null;
                List<LinkedHashMap<String, String>> data = dataSheet.read("Test_Id", test_id);

                // Iterate through all the rows from the data
                for (LinkedHashMap<String, String> row : data) {

                    // Initialize a row_num variable to take screenshot for first row validation
                    int row_num = 1;

                    // Remove Test_Id and ROW_NUMBER columns from excel data object since we don't
                    // need them for validation
                    row.remove("Test_Id");
                    row.remove("ROW_NUMBER");

                    // Convert row cell values into a list
                    List<String> expRowData = convertToList(row);

                    // Access all the rows from application table
                    List<WebElement> rows = oTbl.getRows();

                    // Failing if the no.of rows from application is equal to 0
                    if (rows.size() == 0) {
                        reporter.report("fail", tblName + " has no rows in the application");
                        reporter.endNode();
                        return;
                    }

                    // Maintain a flag to identify the pass/fail
                    boolean bFlag = false;

                    // Initialize a list to store the rows that are already verified in application
                    List<Integer> comparedRows = new ArrayList<>();

                    // Iterate through all the rows in application table
                    for (int i = 1; i <= rows.size(); i++) {

                        // Check if the given row is already used for validation
                        if (!comparedRows.contains(i)) {

                            // Access all the cells from a given row
                            List<WebElement> cells = oTbl.getCells(i);

                            // Convert all the cells data into a list
                            List<String> appRowData = new LinkedList<>();
                            for (WebElement cell : cells) {
                                appRowData.add(cell.getText().trim());
                            }

                            // Check the Excel data is matched with application data
                            if (compareRows(expRowData, appRowData)) {

                                // take screenshot only if we are verifying first row
                                if (row_num == 1)
                                    reporter.report("pass", row + " is displayed in application", driver());
                                else
                                    reporter.report("pass", row + " is displayed in application");

                                // Keep the application row number added to ComparedRows
                                comparedRows.add(i);

                                // Mark the flag to say that row validation is passed
                                bFlag = true;

                                // break out of application row loop to go into next excel row loop
                                break;
                            }
                        }
                    }

                    // Increment the row_num variable so that screenshot cant be taken again
                    row_num++;

                    // Fail if the bFlag was never set to true
                    if (!bFlag) {
                        reporter.report("fail", row + " is not displayed in application", driver());
                    }

                }
            }
        } catch (Exception e) {

            // Fail if there is any run time exception
            reporter.report(e);

        }

        reporter.endNode();
    }

    /***
     * Converts map values into a LinkedList format
     *
     * @param map - input data where the keys have to be converted into list
     * @return List representation of given map values
     * @author vamsikrishna.kayyala
     */
    private List<String> convertToList(LinkedHashMap<String, String> map) {

        List<String> list = new LinkedList<>();

        for (String string : map.keySet()) {
            list.add(map.get(string));
        }

        int iSize = list.size();
        for (int i = iSize - 1; i == 0; i--) {
            if (list.get(i).equals(""))
                list.remove(i);
        }

        return list;
    }

    /***
     * this is directly linked to tableValidation function, used to compare the
     * expected list is equal to actual list
     *
     * @param expRowData - List of TableValidation sheet's cell values. user can
     *                   pass type of validation in excel cell by using ">>",
     *                   possible values are : contains/equals/equalsIgnoreCase
     * @param appRowData - List of actual application table's cell values
     * @return true/false based on the match
     * @author vamsikrishna.kayyala
     */
    private boolean compareRows(List<String> expRowData, List<String> appRowData) {
        int index = 0;
        if ((appRowData.size() == 0))
            return false;

        for (String actual : appRowData) {
            String expected = expRowData.get(index);
            if (!expected.contentEquals("")) {
                String[] split = expected.split(">>");
                switch (split[0].toLowerCase()) {
                    case "contains":
                        if (!split[0].contains(actual))
                            return false;
                        break;

                    case "equalsIgnoreCase":
                        if (!split[0].equalsIgnoreCase(actual))
                            return false;
                        break;

                    case "blank":
                        if (!actual.trim().contentEquals(""))
                            return false;
                        break;

                    default:
                        if (!split[0].contentEquals(actual))
                            return false;
                        break;
                }
            }
            index++;
        }

        return true;
    }

    /***
     * Wait until given object is invisible in application. Default time out is
     * object_sync_time from test.properties
     *
     * @param oEle - WebElement object
     * @author vamsikrishna.kayyala
     */
    public void waitUntilInvisible(WebElement oEle) {
        oWait.toBeInvisible(oEle);
    }

    /***
     * Wait until given object is invisible in application. time out is customizable
     *
     * @param oEle - WebElement object
     * @author vamsikrishna.kayyala
     * @return true/false
     */
    public boolean waitUntilInvisible(WebElement oEle, long timeout) {
        try {
            oWait.toBeInvisible(oEle, timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean waitTillVisible(WebElement oEle, long timeout) {
        try {
            oWait.toBeVisible(oEle, timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean waitTillVisible(WebElement oEle) {
        try {
            oWait.toBeVisible(oEle);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}


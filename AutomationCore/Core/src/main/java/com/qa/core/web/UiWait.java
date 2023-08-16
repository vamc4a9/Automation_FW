package com.qa.core.web;

import com.qa.core.context.RunConfiguration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
@Lazy
public class UiWait {

    private final WebDriverProvider webDriverProvider;
    private final RunConfiguration config;

    public UiWait(WebDriverProvider webDriverProvider, RunConfiguration config) {
        this.webDriverProvider = webDriverProvider;
        this.config = config;
    }

    private WebDriver driver() {
        try {
            return webDriverProvider.getActiveWebDriver();
        } catch (Exception e) {
            return null;
        }
    }

    public FluentWait<WebDriver> page_waiter(long iTimeout) {
        return new FluentWait<>(Objects.requireNonNull(driver()))
                .ignoring(NoSuchElementException.class, WebDriverException.class)
                .withTimeout(Duration.ofSeconds(iTimeout)).pollingEvery(Duration.ofSeconds(2));
    }

    public FluentWait<WebDriver> page_waiter() {
        long iTimeout = Long.parseLong(config.getProperty("page_sync_time"));
        return page_waiter(iTimeout);
    }

    public FluentWait<WebDriver> object_waiter() {
        long iTimeout = Long.parseLong(config.getProperty("object_sync_time"));
        return page_waiter(iTimeout);
    }

    public FluentWait<WebDriver> object_waiter(long iTimeout) {
        return page_waiter(iTimeout);
    }

    public void toBeVisible(WebElement oEle) {
        object_waiter().until(ExpectedConditions.visibilityOf(oEle));
    }

    public void toBeVisible(By locator) {
        object_waiter().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public void toBeVisible(WebElement oEle, long iTimeout) {
        object_waiter(iTimeout).until(ExpectedConditions.visibilityOf(oEle));
    }

    public void toBeInvisible(WebElement oEle) {
        object_waiter().until(ExpectedConditions.invisibilityOf(oEle));
    }

    public void toBeInvisible(WebElement oEle, long iTimeout) {
        object_waiter(iTimeout).until(ExpectedConditions.invisibilityOf(oEle));
    }

    public void toBeClickable(WebElement oEle) {
        object_waiter().until(ExpectedConditions.elementToBeClickable(oEle));
    }

    public void toBeClickable(WebElement oEle, long iTimeout) {
        object_waiter(iTimeout).until(ExpectedConditions.elementToBeClickable(oEle));
    }

    public void toBeSelected(WebElement oEle) {
        object_waiter().until(ExpectedConditions.elementToBeSelected(oEle));
    }

    public void toBeSelected(WebElement oEle, long iTimeout) {
        object_waiter(iTimeout).until(ExpectedConditions.elementToBeSelected(oEle));
    }

}

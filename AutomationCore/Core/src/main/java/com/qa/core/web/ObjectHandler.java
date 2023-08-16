package com.qa.core.web;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Lazy
public class ObjectHandler {

    private final WebDriverProvider webDriverProvider;

    public ObjectHandler(WebDriverProvider webDriverProvider) {
        this.webDriverProvider = webDriverProvider;
    }

    public WebElement getObject(String identifier, String identification) throws Exception {
        try {
            WebDriver driver = getActiveDriver();
            switch (identifier.toLowerCase()) {
                case "xpath":
                    return driver.findElement(By.xpath(identification));
                case "css":
                    return driver.findElement(By.cssSelector(identification));
                case "linktext":
                    return driver.findElement(By.linkText(identification));
                case "partiallinktext":
                    return driver.findElement(By.partialLinkText(identification));
                case "class":
                    return driver.findElement(By.className(identification));
                case "id":
                    return driver.findElement(By.id(identification));
                case "name":
                    return driver.findElement(By.name(identification));
                case "tag":
                    return driver.findElement(By.tagName(identification));
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    private WebDriver getActiveDriver() throws Exception {
        try {
            return webDriverProvider.getActiveWebDriver();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public List<WebElement> getObjects(String identifier, String identification) {
        try {
            WebDriver driver = getActiveDriver();
            switch (identifier.toLowerCase()) {
                case "xpath":
                    return driver.findElements(By.xpath(identification));
                case "css":
                    return driver.findElements(By.cssSelector(identification));
                case "linktext":
                    return driver.findElements(By.linkText(identification));
                case "partiallinktext":
                    return driver.findElements(By.partialLinkText(identification));
                case "class":
                    return driver.findElements(By.className(identification));
                case "id":
                    return driver.findElements(By.id(identification));
                case "name":
                    return driver.findElements(By.name(identification));
                case "tag":
                    return driver.findElements(By.tagName(identification));
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

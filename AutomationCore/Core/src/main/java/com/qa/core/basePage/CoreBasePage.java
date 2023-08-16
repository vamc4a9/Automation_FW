package com.qa.core.basePage;

import com.qa.core.web.WebDriverProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * The Class BasePage every Page should extend this class.
 */
@Component
@Lazy
@Scope("prototype")
public class CoreBasePage<T extends CoreBasePage<T>> {
    public final WebDriverProvider webDriverProvider;

    public WebDriver driver;

    public CoreBasePage(WebDriverProvider webDriverProvider) {
        try {
            this.webDriverProvider = webDriverProvider;
            this.driver = webDriverProvider.getActiveWebDriver();
            PageFactory.initElements(driver, this);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while initializing the page objects for "
                    + this.getClass().getName());
        }
    }
}



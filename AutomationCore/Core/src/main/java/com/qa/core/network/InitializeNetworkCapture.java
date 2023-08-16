package com.qa.core.network;

import com.qa.core.util.BeanUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v115.network.Network;
import org.openqa.selenium.devtools.v115.network.model.RequestWillBeSent;
import org.openqa.selenium.devtools.v115.network.model.ResponseReceived;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Lazy
public class InitializeNetworkCapture {

    public void startNetworkLogging(WebDriver driver) {
        if (!isNetworkInterceptionSupported(driver)) {
            throw new RuntimeException("Unsupported driver for network logging");
        }
        var devTools = getDevToolsForNetworkInterception(driver);
        devTools.addListener(Network.requestWillBeSent(), request -> handleRequestWillBeSent(driver, request));
        devTools.addListener(Network.responseReceived(), request -> handleResponseReceived(driver, request));
    }

    private DevTools getDevToolsForNetworkInterception(WebDriver driver) {
        DevTools devTools = getDevTools(driver);
        return createSession(devTools);
    }

    private DevTools getDevTools(WebDriver driver) {
        if (driver instanceof ChromeDriver)
            return getChromeDevTools(driver);
        else
            return getFirefoxDevTools(driver);
    }

    private DevTools getChromeDevTools(WebDriver driver) {
        return ((ChromeDriver) driver).getDevTools();
    }

    private DevTools getFirefoxDevTools(WebDriver driver) {
        return ((FirefoxDriver) driver).getDevTools();
    }

    private boolean isNetworkInterceptionSupported(WebDriver driver) {
        if (driver instanceof ChromeDriver || driver instanceof FirefoxDriver)
            return true;
        return false;
    }

    private DevTools createSession(DevTools devTools) {
        devTools.createSession();
        devTools.send(Network.enable(Optional.of(Integer.MAX_VALUE), Optional.of(Integer.MAX_VALUE),
                Optional.of(Integer.MAX_VALUE)));
        return devTools;
    }

    private void handleRequestWillBeSent(WebDriver driver, RequestWillBeSent request) {
        RequestLog log = BeanUtil.getBean(RequestLog.class);
        log.setRequestObj(request);
        log.setDriver(driver);
        BeanUtil.getBean(NetworkLog.class).setLog(driver.getWindowHandle(), driver.getTitle(), log);
    }

    private void handleResponseReceived(WebDriver driver, ResponseReceived response) {
        ResponseLog log = BeanUtil.getBean(ResponseLog.class);
        log.setResponseObj(response);
        log.setDriver(driver);
        log.setIsVerified(false);
        BeanUtil.getBean(NetworkLog.class).setLog(driver.getWindowHandle(), driver.getTitle(), log);
    }

}

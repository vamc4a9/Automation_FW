package com.qa.core.web;

import com.qa.core.context.CoreParameters;
import com.qa.core.context.RunConfiguration;
import com.qa.core.listeners.WebDriverListenerImpl;
import com.qa.core.network.InitializeNetworkCapture;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
//import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.assertj.core.util.Preconditions;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope("prototype")
public class WebDriverProvider {

    private static final String CHROME = "chrome";
    private static final String FIREFOX = "firefox";

    private static final ThreadLocal<Map<String, WebDriver>> activeWebDrivers
            = ThreadLocal.withInitial(ConcurrentHashMap::new);
    private static final ThreadLocal<WebDriver> activeWebDriver = new ThreadLocal<>();
    private static final ThreadLocal<String> activeWebDriverTag = new ThreadLocal<>();

    private final CoreParameters parameters;
    private final RunConfiguration config;
    private final InitializeNetworkCapture initializeNetworkCapture;

    private static final ThreadLocal<Map<String, WebDriver>> OriginalActiveWebDrivers
            = ThreadLocal.withInitial(ConcurrentHashMap::new);
    private static final ThreadLocal<WebDriver> OriginalActiveWebDriver = new ThreadLocal<>();
    private static final ThreadLocal<String> OriginalActiveWebDriverTag = new ThreadLocal<>();

    public WebDriverProvider(CoreParameters parameters, RunConfiguration config,
                             InitializeNetworkCapture initializeNetworkCapture) {
        this.parameters = parameters;
        this.config = config;
        this.initializeNetworkCapture = initializeNetworkCapture;
    }

    public void initializeDriver(String tag) throws Exception {
        isValidBrowser();
        areBrowserCapabilitiesThere();
        if (isThereATaggedWebDriver(tag)) setActiveWebDriver(tag);
        else createNewDriver(tag);
    }

    private void createNewDriver(String tag) throws Exception {
        Capabilities desiredCapabilities = getDesiredCapabilities(getExecutionBrowser(), getCapabilitiesPath());
        WebDriver driver = createDriver(tag, desiredCapabilities);
        setActiveWebDriver(tag);
        initializeNetworkCapture(driver);
        setActiveBrowserTag(tag);
    }

    private String getExecutionBrowser() {
        return config.getProperty("browser", "chrome");
    }

    private void areBrowserCapabilitiesThere() {
        Preconditions.checkArgument(isCapabilitiesExist(),
                "Unable to find capabilities file '%s'",
                getCapabilitiesPath());
    }

    private void isValidBrowser() {
        Preconditions.checkArgument(isChromeBrowser() || isFirefoxBrowser(),
                "Invalid browser name: %s",
                getExecutionBrowser());
    }

    private String getCapabilitiesPath() {
        return parameters.getTargetFolderPath() + config.getProperty("web_driver_capabilities_path");
    }

    private boolean isCapabilitiesExist() {
        return new File(getCapabilitiesPath()).exists();
    }

    public void initializeNetworkCapture(WebDriver driver) {
        if (isNetworkCaptureEnabled()) {
            initializeNetworkCapture.startNetworkLogging(driver);
        }
    }

    private boolean isNetworkCaptureEnabled() {
        return (getNetworkCaptureEnabledProp().equalsIgnoreCase("true") && isChromeBrowser());
    }

    private String getNetworkCaptureEnabledProp() {
        return config.getProperty("network_capture_enabled", "false");
    }

    private boolean isFirefoxBrowser() {
        return getExecutionBrowser().equalsIgnoreCase(FIREFOX);
    }

    private boolean isChromeBrowser() {
        return getExecutionBrowser().equalsIgnoreCase(CHROME);
    }

    private void setActiveWebDrivers(String sTag, WebDriver driver) {
        Map<String, WebDriver> map = activeWebDrivers.get();
        map.put(sTag, driver);
        activeWebDrivers.set(map);
    }

    private void setOriginalActiveWebDrivers(String sTag, WebDriver driver) {
        Map<String, WebDriver> map = OriginalActiveWebDrivers.get();
        map.put(sTag, driver);
        OriginalActiveWebDrivers.set(map);
    }

    private void setActiveBrowserTag(String sTag) {
        setActiveWebDriverTag(sTag);
        setOriginalActiveBrowserTag(sTag);
    }

    private void setOriginalActiveBrowserTag(String sTag) {
        setOriginalActiveWebDriverTag(sTag);
    }

    private void setActiveWebDriverTag(String sTag) {
        activeWebDriverTag.set(sTag);
    }

    private void setOriginalActiveWebDriverTag(String sTag) {
        OriginalActiveWebDriverTag.set(sTag);
    }

    /***
     * to get access to the active Decorated WebDriver
     * @return WebDriver
     * @throws Exception
     */
    public WebDriver getActiveWebDriver() throws Exception {
        try {
            return activeWebDriver.get();
        } catch (Exception e) {
            throw new Exception("There is no active Webdriver");
        }
    }

    private void setActiveWebDriver(String sTag) {
        Map<String, WebDriver> map = activeWebDrivers.get();
        activeWebDriver.set(map.get(sTag));
        setOriginalActiveWebDriver(sTag);
    }

    /***
     * to get access to the active undecorated webDriver
     * @return WebDriver
     * @throws Exception
     */
    public WebDriver getOriginalActiveWebDriver() throws Exception {
        try {
            return OriginalActiveWebDriver.get();
        } catch (Exception e) {
            throw new Exception("There is no active Webdriver");
        }
    }

    private void setOriginalActiveWebDriver(String sTag) {
        Map<String, WebDriver> map = OriginalActiveWebDrivers.get();
        OriginalActiveWebDriver.set(map.get(sTag));
    }

    /***
     * To get any tagged webDriver
     * @param sTag - tag name that was originally used to initializeDriver
     * @return
     * @throws Exception
     */
    public WebDriver getWebDriver(String sTag) throws Exception {
        if (isThereATaggedWebDriver(sTag)) {
            setActiveWebDriverTag(sTag);
            return getActiveWebDriver();
        } else {
            throw new Exception("There is no Webdriver tagged with " + sTag);
        }
    }

    private boolean isThereATaggedWebDriver(String sTag) {
        return getAllWebDrivers().containsKey(sTag);
    }

    /***
     * To get all the WebDrivers along with the tag names
     * @return Map of webDrivers
     */
    public Map<String, WebDriver> getAllWebDrivers() {
        return activeWebDrivers.get();
    }

    private void setAllWebDrivers(Map<String, WebDriver> drivers) {
        activeWebDrivers.set(drivers);
    }

    public Map<String, WebDriver> getAllOriginalWebDrivers() {
        return OriginalActiveWebDrivers.get();
    }

    /***
     * To remove the Active WebDriver from the memory
     * @throws Exception
     */
    public void removeActiveDriver() throws Exception {
        WebDriver driver = getActiveWebDriver();
        activeWebDriver.set(null);
        Map<String, WebDriver> allWebDrivers = getAllWebDrivers();
        for (String key : allWebDrivers.keySet()) {
            if (allWebDrivers.get(key) == driver) {
                allWebDrivers.remove(key);
                break;
            }
        }
        setAllWebDrivers(allWebDrivers);
    }

    private WebDriver createDriver(String tag, Capabilities capability) {
        var webDriver = createDriver(capability);
        var decoratedDriver = configureWebDriver(webDriver);
        saveWebDriver(tag, webDriver, decoratedDriver);
        return webDriver;
    }

    private WebDriver createDriver(Capabilities capability) {
        switch (getExecutionBrowser().toLowerCase()) {
            case CHROME:
                return createChromeDriver(capability);
            case FIREFOX:
                return createFirefoxDriver(capability);
            default:
                throw new RuntimeException("Unsupported browser, please use Chrome or Firefox");
        }
    }

    private WebDriver configureWebDriver(WebDriver webDriver) {
        maximizeBrowserWindow(webDriver);
        setupDefaultImplicitWait(webDriver);
        return createDecoratedDriver(webDriver);
    }

    private void saveWebDriver(String tag, WebDriver webDriver, WebDriver decoratedDriver) {
        setActiveWebDrivers(tag, decoratedDriver);
        setOriginalActiveWebDrivers(tag, webDriver);
    }

    private WebDriver createDecoratedDriver(WebDriver webDriver) {
        WebDriverListener listener = new WebDriverListenerImpl();
        return new EventFiringDecorator(listener).decorate(webDriver);
    }

    private void maximizeBrowserWindow(WebDriver webDriver) {
        webDriver.manage().window().maximize();
    }

    private void setupDefaultImplicitWait(WebDriver webDriver) {
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(getObjectSyncTime()));
    }

    private Long getObjectSyncTime() {
        return Long.parseLong(config.getProperty("object_sync_time", "0"));
    }

    private Long getPageSyncTime() {
        return Long.parseLong(config.getProperty("page_sync_time", "0"));
    }

    private WebDriver createFirefoxDriver(Capabilities capability) {
        setUpFirefoxDriver();
        FirefoxOptions ops = getFirefoxOptions(capability);
        setPageLoadTimeout(getPageSyncTime(), ops);
        if (isSeleniumGridEnabled()) {
            return createRemoteWebDriver(ops);
        } else {
            return new FirefoxDriver(ops);
        }
    }

    private void setPageLoadTimeout(long iPageTimeOut, FirefoxOptions ops) {
        ops.setPageLoadTimeout(Duration.ofSeconds(iPageTimeOut));
    }

    private WebDriver createRemoteWebDriver(MutableCapabilities caps) {
        return new RemoteWebDriver(createUrl(parameters.getGridUrl()), caps);
    }

    private WebDriver createChromeDriver(Capabilities capability) {
        setUpChromeDriver();
        ChromeOptions ops = getChromeOptions(capability);
        if (isSeleniumGridEnabled()) {
            return createRemoteWebDriver(ops);
        } else {
            return new ChromeDriver(ops);
        }
    }

    private boolean isSeleniumGridEnabled() {
        return parameters.getUseGrid();
    }

    private void setUpChromeDriver() {
        WebDriverManager.chromedriver().setup();
    }

    private void setUpFirefoxDriver() {
        WebDriverManager.firefoxdriver().driverVersion("85").setup();
    }

    private ChromeOptions getChromeOptions(Capabilities capabilities) {
        ChromeOptions ops = new ChromeOptions();
        prepareProxy(ops::setProxy);
        return transformCapabilitiesToOptions(capabilities, ops);
    }

    private FirefoxOptions getFirefoxOptions(Capabilities capabilities) {
        FirefoxOptions ops = new FirefoxOptions();
        prepareProxy(ops::setProxy);
        return transformCapabilitiesToOptions(capabilities, ops);
    }

    private <T extends MutableCapabilities> T transformCapabilitiesToOptions(Capabilities capabilities, T ops) {
        Map<String, Object> caps = capabilities.asMap();
        caps.forEach((name, value) -> {
            if (name.equals("args")) {
                if (ops instanceof ChromeOptions) {
                    ((ChromeOptions) ops).addArguments(safelyCastToList(value, String.class));
                } else if (ops instanceof FirefoxOptions) {
                    ((FirefoxOptions) ops).addArguments(safelyCastToList(value, String.class));
                } else {
                    throw new RuntimeException("Unsupported browser, only Firefox and Chrome are supported");
                }
            } else {
                ops.setCapability(name, value);
            }
        });
        return ops;
    }

    private <T> List<T> safelyCastToList(Object obj, Class<T> elementType) {
        if (obj instanceof List<?>) {
            List<?> rawList = (List<?>) obj;
            for (Object item : rawList) {
                if (!elementType.isInstance(item)) {
                    throw new IllegalArgumentException("Object is not a List of " + elementType.getSimpleName() + ".");
                }
            }
            @SuppressWarnings("unchecked")
            List<T> typedList = (List<T>) rawList;
            return typedList;
        } else {
            throw new IllegalArgumentException("Object is not a List.");
        }
    }

    private void prepareProxy(ProxySetter proxySetter) {
        if (isProxyEnabled()) {
            Proxy proxy = new Proxy();
            proxy.setAutodetect(false);
            if (isValueSet(parameters.getProxyHttp())) {
                proxy.setHttpProxy(parameters.getProxyHttp());
            }
            if (isValueSet(parameters.getProxySsl())) {
                proxy.setSslProxy(parameters.getProxySsl());
            }
            if (isValueSet(parameters.getProxySocket())) {
                proxy.setSocksProxy(parameters.getProxySocket());
            }
            proxySetter.set(proxy);
        }
    }

    private boolean isProxyEnabled() {
        return parameters.getUseProxy();
    }

    private interface ProxySetter {
        void set(Proxy proxy);
    }

    private boolean isValueSet(String value) {
        return !value.isEmpty() && !value.equals("-");
    }

    private URL createUrl(String url) {
        try {
            return new URL(url);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Provided value: %s is not valid URL", url), e);
        }
    }

    private Object parseJSON(String jsonLocation, String capabilityName) throws Exception {
        JSONParser jsonParser = new JSONParser();
        FileReader fileReader = new FileReader(jsonLocation);
        JSONObject parse2 = (JSONObject) jsonParser.parse(fileReader);
        return parse2.get(capabilityName);
    }

    private JSONObject getCapability(String capabilityName, String jsonLocation) throws Exception {
        return (JSONObject) parseJSON(jsonLocation, capabilityName);
    }

    private <T> T convertCapabilitiesToMap(String capabilityName, String jsonLocation, TypeReference<T> typeReference)
            throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(getCapability(capabilityName, jsonLocation).toString(), typeReference);
    }

    private Capabilities getDesiredCapabilities(String capabilityName, String jsonLocation)
            throws Exception {
        HashMap<String, Object> caps = convertCapabilitiesToMap(capabilityName, jsonLocation, getMapTypeReference());
        if (parameters.getUseGrid()) {
            caps.putAll(convertCapabilitiesToMap("grid", jsonLocation, getMapTypeReference()));
        }
        return new DesiredCapabilities(caps);
    }

    private TypeReference<HashMap<String, Object>> getMapTypeReference() {
        return new TypeReference<>() {};
    }
}

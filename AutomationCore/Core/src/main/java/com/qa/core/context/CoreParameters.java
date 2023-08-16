package com.qa.core.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CoreParameters {

    @Value("${app_module.build_folder}")
    String resourcesBuildFolder;

    @Value("${app_module.config_file}")
    String runConfigurationFile;

    @Value("${webdriver.useGrid}")
    private boolean useGrid;

    @Value("${webdriver.gridUrl}")
    private String gridUrl;

    @Value("${proxy.useProxy}")
    private boolean useProxy;

    @Value("${proxy.proxyHttp}")
    private String proxyHttp;

    @Value("${proxy.proxySsl}")
    private String proxySsl;

    @Value("${proxy.proxySocks}")
    private String proxySocket;

    private String WORKING_DIRECTORY;

    private String TARGET_FOLDER_PATH;

    private String PROPERTY_FILE_PATH;

    private static final ThreadLocal<Class<?>> currentPage = new ThreadLocal<>();

    public CoreParameters() {}

    @PostConstruct
    public void init() {
        WORKING_DIRECTORY = System.getProperty("user.dir");
        TARGET_FOLDER_PATH = WORKING_DIRECTORY + resourcesBuildFolder;
        PROPERTY_FILE_PATH = TARGET_FOLDER_PATH + runConfigurationFile;
    }

    public boolean getUseGrid() {
        return useGrid;
    }

    public String getGridUrl() {
        return gridUrl;
    }

    public boolean getUseProxy() {
        return useProxy;
    }

    public String getProxyHttp() {
        return proxyHttp;
    }

    public String getProxySsl() {
        return proxySsl;
    }

    public String getProxySocket() {
        return proxySocket;
    }

    public String getTargetFolderPath() {
        return TARGET_FOLDER_PATH;
    }

    public String getPropertyFilePath() {
        return PROPERTY_FILE_PATH;
    }

    public String getWorkingDirectory() {
        return WORKING_DIRECTORY;
    }

    public static Class<?> getPage() {
        return currentPage.get();
    }

    public static void setPage(Class<?> oCls) {
        currentPage.set(oCls);
    }

}
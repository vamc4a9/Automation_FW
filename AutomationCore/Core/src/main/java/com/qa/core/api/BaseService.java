package com.qa.core.api;

import com.qa.core.dataHandler.DataParserResolver;
import com.qa.core.util.BeanUtil;
import com.qa.core.context.RunConfiguration;
import com.qa.core.report.ReportManager;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Lazy
@Scope("prototype")
public class BaseService {

    private final RunConfiguration config;
    private final ApiProvider rest;
    private final DataParserResolver dataParserResolver;
    private final ReportManager reporter;

    private String api_name;
    public String response;
    public String callType;
    private boolean failFlag = false;
    private boolean report = true;
    public boolean executedService = false;
    private String responsePath = "";

    public BaseService(RunConfiguration config, ApiProvider rest,
                        DataParserResolver dataParserResolver, ReportManager reporter) {
        this.config = config;
        this.rest = rest;
        this.dataParserResolver = dataParserResolver;
        this.reporter = reporter;
    }

    private BaseService initialise(String name, boolean reporting) {
        this.api_name = name;

        rest.init(api_name);
        List<LinkedHashMap<String, String>> data = dataParserResolver
                .getInstance("api_config")
                .read("api_name", name);

        if (data.get(0).get("automatic_redirection").equalsIgnoreCase("false")) {
            rest.automaticRedirection(false);
        }

        if (!data.get(0).get("Headers").contentEquals("")) {
            rest.headers(getHeaders(data.get(0).get("Headers")));
        }

        if (!data.get(0).get("QueryParams").contentEquals("")) {
            rest.queryParams(getQueryParams(data.get(0).get("QueryParams")));
        }

        if (!data.get(0).get("FormData").contentEquals("")) {
            rest.addFormData(getQueryParams(data.get(0).get("FormData")));
        }

        if (!data.get(0).get("RequestBody").contentEquals("")) {
            rest.body(data.get(0).get("RequestBody"));
        }

        if (!data.get(0).get("BasicAuth_UserName").contentEquals("")) {
            String un = data.get(0).get("BasicAuth_UserName");
            String pw = data.get(0).get("BasicAuth_Password");
            String authKey = pw.contentEquals("") ? un + ":" : un +":" +pw;
            var encodedKey = Base64.encodeBase64(authKey.getBytes(StandardCharsets.UTF_8));
            rest.header("Authorization", "Basic " + new String(encodedKey));
        }

        report = reporting;
        callType = data.get(0).get("CallType");

        return this;
    }

    /***
     * To return the saved response file path, this method is created so that user can access response only when needed,
     * if user doesn't call this method, then response never gets saved to the ExtentReports folder
     *
     * @return path of the saved response file
     * @author vamsikrishna.kayyala
     */
    public String getResponsePath() {
        if (responsePath.contentEquals("")) {
            String[] responsePaths = rest.saveResponse(api_name);
            responsePath = responsePaths[1];
        }
        return responsePath;
    }

    public void saveResponse(String name) {
        String file_name = name.replace(" ", "_") + ".json";
        String absPath = ReportManager.screenshotsFolder + file_name;
        rest.createFile(absPath);
    }

    private BaseService newInstance() {
        return BeanUtil.getBean(BaseService.class);
    }

    /**
     * To initialize a new instance of BaseService to work with the provided api
     *
     * This method is immutable and hence we must store its return value in a variable
     *
     * @param name - service name from api configuration sheet
     *
     * @return returns a new instance of this class, caller must always store its return
     */
    public BaseService initNew(String name) {
        return initNew(name, config.getProperty("env"));
    }

    /**
     * To initialize a new instance of BaseService to work with the provided api
     *
     * This method is immutable and hence we must store its return value
     *
     * @param name - name of the service from api configuration sheet
     * @param env - environment name in which the service is supposed to be executed
     *
     * @return - returns a new instance of this class to work with provided api name
     */
    public BaseService initNew(String name, String env) {
        return initNew(name, env, true);
    }

    /**
     * To initialize a new instance of BaseService to work with the provided api
     *
     * This method is immutable and hence we must store its return value
     *
     * @param name - name of the service from api configuration sheet
     * @param reporting - whether to report the api execution in extent or not
     *
     * @return - returns a new instance of this class to work with provided api name
     */
    public BaseService initNew(String name, boolean reporting) {
        return initNew(name, config.getProperty("env"), reporting);
    }

    /**
     * To initialize a new instance of BaseService to work with the provided api
     *
     * This method is immutable and hence we must store its return value
     *
     * @param name - name of the service from api configuration sheet
     * @param env - environment name in which the service is supposed to be executed
     * @param reporting - whether to report the api execution in extent or not
     *
     * @return - returns a new instance of this class to work with provided api name
     */
    public BaseService initNew(String name, String env, boolean reporting) {
        return initNew(name, Map.of("env", env), reporting);
    }

    /**
     * To initialize a new instance of BaseService to work with the provided api
     *
     * This method is immutable and hence we must store its return value
     *
     * @param name - name of the service from api configuration sheet
     * @param parameters - key/value pairs to store update in run configuration
     *
     * @return - returns a new instance of this class to work with provided api name
     */
    public BaseService initNew(String name, Map<String, String> parameters) {
        return initNew(name, parameters, true);
    }

    /**
     * To initialize a new instance of BaseService to work with the provided api
     *
     * This method is immutable and hence we must store its return value
     *
     * @param name - name of the service from api configuration sheet
     * @param parameters - key/value pairs to store update in run configuration
     * @param reporting - whether to report the api execution in extent or not
     *
     * @return - returns a new instance of this class to work with provided api name
     */
    public BaseService initNew(String name, Map<String, String> parameters, boolean reporting) {
        var existingValues = config.getProperties(parameters.keySet());
        addParameters(parameters);
        BaseService baseService = newInstance();
        baseService.initialise(name, reporting);
        config.revertTheParameterValues(parameters, existingValues);
        return baseService;
    }

    private Map<String, String> getExistingParameters(Set<String> keys) {
        var parameters = new HashMap<String, String>();
        for (String key : keys) {
            if (config.checkProperty(key))
                parameters.put(key, config.getProperty(key));
        }
        return parameters;
    }

    private void addParameters(Map<String, String> parameters) {
        for (String key : parameters.keySet()) {
            if (parameters.get(key) == null)
                config.putProperty(key, "!IGNORE!");
            else if (parameters.get(key).contentEquals(""))
                config.putProperty(key, "!IGNORE!");
            else
                config.putProperty(key, parameters.get(key));
        }
    }

    public void deleteResponseFile() {
        if (!responsePath.contentEquals("")) {
            File file = new File(responsePath);
            file.delete();
            try {
                File allureFile = new File(System.getProperty("user.dir")
                        + "/allure-results/Screenshots/" + file.getName());
                allureFile.delete();
            } catch (Exception e) {
                //do nothing
            }
        }
    }

    public BaseService reRunIfStatusIsNot(int statusCode) {
        if (rest.getStatusCode() != statusCode) {
            send();
        }
        return this;
    }

    public BaseService send() {
        executeApi();
        executedService = true;
        return this;
    }

    private BaseService executeApi() {
        try {
            reporter.startNode("GET API Execution for '" + api_name + "'");
            if (callType.equalsIgnoreCase("get")) {
                rest.get();
            } else if (callType.equalsIgnoreCase("post")) {
                rest.post();
            } else if (callType.equalsIgnoreCase("delete")) {
                rest.delete();
            }
            response = rest.getApiResponseAsString();
            if (report) {
                String[] responsePaths = rest.saveResponse(api_name);
                responsePath = responsePaths[1];
                Map<String, String> report = new LinkedHashMap<String, String>();
                report.put("Call Type", callType);
                report.put("Status", api_name + " service is executed successfully");
                report = rest.get_request_details(report);
                report.put("<b><u>Response</b></u>", "<a href = \"" + responsePaths[0] + "\" target=\"_blank\"> response link </a>");
                reporter.report_extent("pass", "REQUEST details", report);
            }
        } catch (Exception e) {
            if (report) {
                reporter.report("Exception occurred while executing " + api_name + "  service", e);
                failFlag = true;
            }
        } finally {
            reporter.endNode();
        }
        return this;
    }

    public ApiProvider getRestUtil() {
        return rest;
    }

    public int getStatusCode() {
        return rest.getStatusCode();
    }

    public BaseService deleteResponseIfPassed() {
        if (getStatusCode() == 200)
            deleteResponseFile();
        return this;
    }

    public BaseService verifyStatusCode(int expected) {
        if (!failFlag) {
            reporter.assertEquals(getStatusCode(), expected, "Verifying api status code for " + api_name);
        }
        return this;
    }

    private Map<String, String> getHeaders(String headerName) {
        return dataParserResolver.getInstance("service_data").get_as_headers("name", headerName);
    }

    private Map<String, String> getQueryParams(String headerName) {
        return dataParserResolver.getInstance("service_data").get_as_query_params("name", headerName);
    }
}
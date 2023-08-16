package com.qa.core.api;


import com.qa.core.context.CoreParameters;
import com.qa.core.dataHandler.DataParserResolver;
import com.qa.core.context.RunConfiguration;
import com.qa.core.report.ReportManager;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Headers;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.minidev.json.JSONArray;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static io.restassured.RestAssured.given;

@Component
@Lazy
@Scope("prototype")
public class ApiProvider {

    private final DataParserResolver dataParserResolver;
    private final RunConfiguration config;
    private final CoreParameters coreParameters;

    private Response apiResponse;
    private RequestSpecBuilder requestSpecBuilder;
    private RequestSpecification requestSpecification;
    private Map<String, String> headers = new HashMap<String, String>();

    private Map<String, String> form_data;

    private Map<String, String> query_params;

    private String request_body;

    private String endpoint_url;
    private boolean redirectFlag = true;
    private String requestName;

    private String expectedResponseContentType;

    public ApiProvider(DataParserResolver dataParserResolver, RunConfiguration config, CoreParameters coreParameters) {
        this.dataParserResolver = dataParserResolver;
        this.config = config;
        this.coreParameters = coreParameters;
    }

    public void init(String api_name) {
        this.requestName = api_name;
        initializeRequestSpec(get_url(api_name));
    }

    public String get_url(String name) {
        try {
            var oExcel = dataParserResolver.getInstance("api_config");
            String sData = oExcel.read("api_name", name).get(0).get("endpoint-url");
            return config.resolveString(sData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Function returns the apiResponse as String
     *
     * @return apiResponse
     */

    public String getApiResponseAsString() {
        return apiResponse.asString();
    }

    private String getBeautifiedResponse() {
        return apiResponse.asPrettyString();
    }

    public int getStatusCode() {
        return apiResponse.getStatusCode();
    }

    /*********************************** User Functions ***********************************/

    public Headers responseHeaders() {
        return apiResponse.headers();
    }

    public String getHeaderValue(String header) {
        return apiResponse.headers().getValue(header);
    }

    public Map<String, String> responseCookies() {
        return apiResponse.cookies();
    }

    /**
     * Function initializes Request Specifications including the Base URI Path from test.properties
     *
     */

    private void initializeRequestSpec(String baseUri) {
        EncoderConfig encoderconfig = new EncoderConfig();
        requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.addFilters(
                Arrays.asList(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter(),
                        new RestRequestAllureFilter(),
                        new RestResponseAllureFilter()
                )
        );
        requestSpecBuilder.setBaseUri(baseUri);
        requestSpecBuilder.setConfig(RestAssured.config().encoderConfig(encoderconfig.appendDefaultContentCharsetToContentTypeIfUndefined(false)));
        this.endpoint_url = baseUri;
    }

    /**
     * Function defines API EndPoint Path to Request Specification
     *
     * @param path
     * @return this
     */

    private ApiProvider path(String path) {
        requestSpecBuilder.setBasePath(path);
        return this;
    }

    /**
     * Defines Path Parameters to Request Specification
     *
     * @param key
     * @param value
     * @return this
     */

    private ApiProvider pathParam(String key, String value) {
        requestSpecBuilder.addPathParam(key, value);
        return this;
    }

    public ApiProvider automaticRedirection(Boolean flag) {
        redirectFlag = flag;
        return this;
    }

    /**
     * Function defines Single Query Parameter to Request Specification
     *
     * @param key
     * @param value
     * @return this
     */

    private ApiProvider queryParam(String key, String value) {
        requestSpecBuilder.addQueryParam(key, value);
        return this;
    }

    /**
     * Function defines Multiple Query Parameters to Request Specification
     *
     * @return this
     */

    public ApiProvider queryParams(Map<String, String> queryParam) {
        requestSpecBuilder.addQueryParams(queryParam);
        this.query_params = queryParam;
        return this;
    }

    public ApiProvider addFormData(Map<String, String> queryParam) {
        requestSpecBuilder.addFormParams(queryParam);
        this.form_data = queryParam;
        return this;
    }

    /**
     * Function defines Content Type Header to Request Specification
     *
     * @param contentType
     * @return this
     */

    private ApiProvider contentType(ContentType contentType) {
        requestSpecBuilder.setContentType(contentType);
        return this;
    }

    /**
     * Function defines Headers to Request Specification
     *
     * @param headers
     * @return this
     */

    public ApiProvider headers(Map<String, String> headers) {
        requestSpecBuilder.addHeaders(headers);
        this.headers = headers;
        return this;
    }

    public ApiProvider header(String key, String value) {
        requestSpecBuilder.addHeader(key, value);
        this.headers.put(key, value);
        return this;
    }

    /**
     * Function defines Cookies to Request Specification
     *
     * @param cookies
     * @return this
     */

    public ApiProvider cookies(Map<String, String> cookies) {
        requestSpecBuilder.addCookies(cookies);
        return this;
    }

    /**
     * Function defines Cookies to Request Specification
     *
     * @param cookies
     * @return this
     */

    public ApiProvider cookies(Cookies cookies) {
        requestSpecBuilder.addCookies(cookies);
        return this;
    }

    /**
     * Function defines Cookie to Request Specification
     *
     * @param cookie
     * @return this
     */

    private ApiProvider cookie(Cookie cookie) {
        requestSpecBuilder.addCookie(cookie);
        return this;
    }

    /**
     * Function defines Body to Request Specification
     *
     * @param body
     * @return this
     */

    public ApiProvider body(Object body) {
        requestSpecBuilder.setBody(body);
        this.request_body = body.toString();
        return this;
    }

    /**
     * Function defines the Expected Response Content Type following successful API execution for validation
     *
     * @param contentType
     * @return this
     */

    private ApiProvider expectedResponseContentType(String contentType) {
        this.expectedResponseContentType = contentType;
        return this;
    }

    /**
     * Function defines the Expected Response Content Type following successful API execution for validation
     *
     * @param contentType
     * @return this
     */

    private ApiProvider expectedResponseContentType(ContentType contentType) {
        this.expectedResponseContentType = contentType.toString();
        return this;
    }

    /**
     * Function defines the GET Request
     * Takes the parameters as :
     * -  API EndPoint
     * -  Multiple Headers
     * -  Expected Status Code
     * -  Expected Content Type
     *
     * @return this
     */

    /**
     * Function hits the Pre-Defined Request Specification as GET Request
     * On successful response, method validates:
     * -   Status Code against the Status Code provided in Request Specification
     * -   Content Type against the Content Type provided in Request Specification
     *
     * @return this
     */

    public ApiProvider get() {
        requestSpecification = requestSpecBuilder.build();

        apiResponse = given().
                log().
                all().
                spec(requestSpecification).
                when().
                redirects().follow(redirectFlag).
                get();
        return this;
    }

    public Map<String, String> getCookies() {
        return apiResponse.cookies();
    }

    public String getCookies(String name) {
        return apiResponse.cookies().get(name);
    }

    private ApiProvider redirect_get() {
        requestSpecification = requestSpecBuilder.build();
        apiResponse = given().redirects().allowCircular(true).spec(requestSpecification).when().get();
        return this;
    }

    /**
     * Function hits the Pre-Defined Request Specification as POST Request
     * On successful response, method validates:
     * -   Status Code against the Status Code provided in Request Specification
     * -   Content Type against the Content Type provided in Request Specification
     *
     * @return this
     */

    public ApiProvider post() {
        requestSpecification = requestSpecBuilder.build();
        apiResponse =
                given()
                        .log().all()
                        .spec(requestSpecification)
                        .when()
                        .redirects().follow(redirectFlag)
                        .post();
        return this;
    }

    public ApiProvider delete() {
        requestSpecification = requestSpecBuilder.build();
        apiResponse =
                given()
                        .log().all()
                        .spec(requestSpecification)
                        .when()
                        .redirects().follow(redirectFlag)
                        .delete();
        return this;
    }

    /***
     * This is not implemented using RestAssured JsonPath, it is based on Jayway JsonPath
     *
     * @param jPath - Json Path expression, refer to "https://goessner.net/articles/JsonPath/index.html#e2" for more info
     * @return - return the size of the json array
     */
    public int readArraySize(String jPath) {
        JSONArray read = JsonPath.read(getApiResponseAsString(), jPath);
        return read.size();
    }

    /***
     * This is not implemented using RestAssured JsonPath, it is based on Jayway JsonPath
     *
     * @param jPath - Json Path expression, refer to "https://goessner.net/articles/JsonPath/index.html#e2" for more info
     * @return - return the size of the json array
     */
    public int readNodeSize(String jPath) {
        Object read = JsonPath.read(getApiResponseAsString(), jPath);
        if (read instanceof JSONArray) {
            return ((JSONArray) read).size();
        } else if (read instanceof LinkedHashMap) {
            return ((LinkedHashMap) read).size();
        } else {
            return 0;
        }
    }

    public LinkedHashMap readList(String jPath) {
        Object read = JsonPath.read(getApiResponseAsString(), jPath);
        if (read instanceof JSONArray) {
            return null;
        } else if (read instanceof LinkedHashMap) {
            return ((LinkedHashMap) read);
        } else {
            return null;
        }
    }

    /***
     *  This is not implemented using RestAssured JsonPath, it is based on Jayway JsonPath
     *
     * @param jPath - Json Path expression, refer to "https://goessner.net/articles/JsonPath/index.html#e2" for more info
     * @return - returns the first value identified in JsonArray
     */
    public String readNode(String jPath) {
        Object read = JsonPath.read(getApiResponseAsString(), jPath);
        if (read instanceof JSONArray) {
            return ((JSONArray) read).get(0).toString();
        }
        return read.toString();
    }

    public String readNode(String jPath, int index) {
        Object read = JsonPath.read(getApiResponseAsString(), jPath);
        if (read instanceof JSONArray) {
            return ((JSONArray) read).get(index).toString();
        }
        return read.toString();
    }

    public String readJsonString(String jPath) {
        Object read = JsonPath.read(getApiResponseAsString(), jPath);
        if (read instanceof JSONArray) {
            if (((JSONArray) read).get(0) instanceof LinkedHashMap) {
                Gson gson = new Gson();
                String json = gson.toJson(((JSONArray) read).get(0), LinkedHashMap.class);
                return json;
            }
            return ((JSONArray) read).get(0).toString();
        }
        return read.toString();
    }

    public JSONArray readArray(String jPath) {
        Object read = JsonPath.read(getApiResponseAsString(), jPath);
        return (JSONArray) read;
    }

    public boolean checkNode(String jpath) {
        try {
            Object read = JsonPath.read(getApiResponseAsString(), jpath);
            if (read instanceof JSONArray) {
                if (((JSONArray) read).size() > 0) {
                    return true;
                } else {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String[] saveResponse(String name) {
        String file_name = name.replace(" ", "_") + "_" + Calendar.getInstance().getTimeInMillis() + "_" + Thread.currentThread().getId() + ".json";
        String absPath = ReportManager.screenshotsFolder + file_name;
        String relativePath = absPath.replace(ReportManager.resultsFolder, "./");
        createFile(absPath, getBeautifiedResponse());
//        copyToAllureResults(absPath, file_name);
        return new String[]{relativePath, absPath};
    }

    public List<String> validateSchema(String filePath) {
        try {
            filePath = coreParameters.getTargetFolderPath() + filePath;
            apiResponse.then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(new File(filePath)));
            return List.of("pass");
        } catch (AssertionError e) {
            return List.of("fail", e.getMessage());
        }
    }

    private void createFile(String fileName, String content) {
        try {
            File oFile = new File(fileName);
            oFile.createNewFile();
            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(content);
            myWriter.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createFile(String fileName) {
        try {
            File oFile = new File(fileName);
            oFile.createNewFile();
            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(getApiResponseAsString());
            myWriter.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> get_request_details(Map<String, String> sReturn) {
        sReturn.put("End point url", endpoint_url);
        if (headers != null) {
            sReturn.put("<b><u>Request Headers</u></b>", "");
            sReturn.putAll(headers);
        }

        if (query_params != null) {
            sReturn.put("<b><u>Request Query Parameters</u></b>", "");
            sReturn.putAll(query_params);
        }

        if (form_data != null) {
            sReturn.put("<b><u>Request Form Data</u></b>", "");
            sReturn.putAll(form_data);
        }

        if (request_body != null) {
            sReturn.put("<b><u>Request Body</u></b>", "");
            sReturn.put("Body", request_body);
        }

        return sReturn;
    }

}

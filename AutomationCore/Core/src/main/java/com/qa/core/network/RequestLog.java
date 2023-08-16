package com.qa.core.network;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.v115.network.model.Headers;
import org.openqa.selenium.devtools.v115.network.model.Request;
import org.openqa.selenium.devtools.v115.network.model.RequestWillBeSent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Lazy
@Scope("prototype")
public class RequestLog {

    private RequestWillBeSent requestObj;

    private String requestId = "";

    private String method = "";

    private String url = "";

    private Map<String, Object> headers = null;

    private String requestBody = "";

    private WebDriver webDriver;

    public RequestLog() {}

    public void setRequestObj(RequestWillBeSent request) {
        this.requestObj = request;
    }

    public void setDriver(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public String getRequestId() {
        if (this.requestId.contentEquals("")) {
            try {
                this.requestId = requestObj.getRequestId().toString();
                return this.requestId;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.requestId;
        }
    }

    public String getMethod() {
        if (this.method.contentEquals("")) {
            try {
                Request oReq = requestObj.getRequest();
                this.method = oReq.getMethod();
                return this.method;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.method;
        }
    }

    public String getUrl() {
        if (this.url.contentEquals("")) {
            try {
                Request oReq = requestObj.getRequest();
                this.url = oReq.getUrl();
                return this.url;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.url;
        }
    }

    public String getRequestBody() {
        if (this.requestBody.contentEquals("")) {
            try {
                Request oReq = requestObj.getRequest();
                this.requestBody = oReq.getPostData().get();
                return this.requestBody;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.requestBody;
        }
    }

    public Map<String, Object> getHeaders() {
        if (this.headers == null) {
            try {
                Request oReq = requestObj.getRequest();
                Headers header = oReq.getHeaders();
                this.headers = header.toJson();
                return this.headers;
            } catch (Exception e) {
                return null;
            }
        } else {
            return this.headers;
        }
    }

    public Map<String, String> toMap() {
        Map<String, String> mReturn = new LinkedHashMap<String, String>();
        mReturn.put("Url", getUrl());
        mReturn.put("Method", getMethod());
        mReturn.put("Request Headers", getHeaders().toString());
        mReturn.put("Request Body", getRequestBody());
        return mReturn;
    }

}

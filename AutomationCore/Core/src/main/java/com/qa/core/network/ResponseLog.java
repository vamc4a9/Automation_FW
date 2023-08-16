package com.qa.core.network;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v115.network.Network;
import org.openqa.selenium.devtools.v115.network.model.Cookie;
import org.openqa.selenium.devtools.v115.network.model.Headers;
import org.openqa.selenium.devtools.v115.network.model.RequestId;
import org.openqa.selenium.devtools.v115.network.model.ResponseReceived;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Lazy
@Scope("prototype")
public class ResponseLog {
    private ResponseReceived responseObj;
    private Command<List<Cookie>> cookies;
    private String sUrl = "";
    private String sResponse = "";
    private String statusCode = "";
    private String requestId = "";
    private WebDriver webDriver;
    private boolean isVerified = false;

    public ResponseLog() {}

    public void setDriver(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public void setResponseObj(ResponseReceived response) {
        this.responseObj = response;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public boolean getIsVerified() {
        return this.isVerified;
    }

    public void setCookies(Command<List<Cookie>> cookies) {
        this.cookies = cookies;
    }

    public Command<List<Cookie>> getCookies() {
        return this.cookies;
    }

    public String getUrl() {
        if (this.sUrl.contentEquals("")) {
            try {
                this.sUrl = responseObj.getResponse().getUrl();
                return this.sUrl;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.sUrl;
        }
    }

    public String getResponse() {
        if (this.sResponse.contentEquals("")) {
            try {
                ChromeDriver driver = (ChromeDriver) webDriver;
                Optional<DevTools> maybeGetDevTools = driver.maybeGetDevTools();
                final DevTools devTools = maybeGetDevTools.get();
                RequestId requestId = responseObj.getRequestId();
                this.sResponse = devTools.send(Network.getResponseBody(requestId)).getBody();
                return this.sResponse;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.sResponse;
        }

    }

    public Headers getHeaders() {
        return this.responseObj.getResponse().getHeaders();
    }



    public String getStatusCode() {
        if (this.statusCode.contentEquals("")) {
            try {
                this.statusCode = String.valueOf(responseObj.getResponse().getStatus());
                return this.statusCode;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.statusCode;
        }
    }

    public String getRequestId() {
        if (this.requestId.contentEquals("")) {
            try {
                this.requestId = responseObj.getRequestId().toString();
                return this.requestId;
            } catch (Exception e) {
                return "";
            }
        } else {
            return this.requestId;
        }
    }

    public Map<String, String> toMap() {
        Map<String, String> mReturn = new LinkedHashMap<String, String>();
        mReturn.put("URL", getUrl());
        mReturn.put("Status Code", getStatusCode());
        mReturn.put("Response", getResponse());
        return mReturn;
    }
}

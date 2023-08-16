package com.qa.core.network;

import com.qa.core.api.BaseJsonParser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class NetworkLog {

    public NetworkLog() {}

    private static final Map<String, LinkedHashMap<String, LinkedList<RequestLog>>> requestLogs = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, LinkedList<ResponseLog>>> responseLogs = new ConcurrentHashMap<>();
    private static final Map<String, List<ResponseLog>> reportedResponses = new HashMap<>();


    public List<ResponseLog> getReportedResponses(String driver_handle) {
        if (reportedResponses.get(driver_handle) == null)
            return new ArrayList<>();
        else
            return reportedResponses.get(driver_handle);
    }

    public void setReportedResponses(ResponseLog log, String driver_handle) {
        List<ResponseLog> list = getReportedResponses(driver_handle);
        list.add(log);
        reportedResponses.put(driver_handle, list);
    }

    public LinkedHashMap<String, LinkedList<RequestLog>> getRequestLogs(String driver_handle) {
        if (requestLogs.get(driver_handle) == null)
            return new LinkedHashMap<>();
        else
            return requestLogs.get(driver_handle);
    }

    private void setRequestLogs(String driver_handle, LinkedHashMap<String, LinkedList<RequestLog>> logs) {
        requestLogs.put(driver_handle, logs);
    }

    public Map<String, LinkedList<ResponseLog>> getResponseLogs(String driver_handle) {
        if (responseLogs.get(driver_handle) == null)
            return new LinkedHashMap<>();
        else {
            return responseLogs.get(driver_handle);
        }
    }

    private void setResponseLogs(String driver_handle, Map<String, LinkedList<ResponseLog>> logs) {
        responseLogs.put(driver_handle, logs);
    }

    private Map<String, LinkedList<ResponseLog>> get_ResponseLogs(String driver_handle) {
        if (responseLogs.get(driver_handle) == null)
            return new LinkedHashMap<>();
        else {
            return responseLogs.get(driver_handle);
        }
    }

    public void setLog(String driver_handle, String sPage, RequestLog log) {
        LinkedHashMap<String, LinkedList<RequestLog>> logs = getRequestLogs(driver_handle);
        if (logs.containsKey(sPage)) {
            LinkedList<RequestLog> object = logs.get(sPage);
            object.add(log);
            logs.replace(sPage, object);
        } else {
            LinkedList<RequestLog> lData = new LinkedList<>();
            lData.add(log);
            logs.put(sPage, lData);
        }
        setRequestLogs(driver_handle, logs);
    }

    public void setLog(String driver_handle, String sPage, ResponseLog log) {
        Map<String, LinkedList<ResponseLog>> logs = getResponseLogs(driver_handle);
        if (logs.containsKey(sPage)) {
            LinkedList<ResponseLog> object = logs.get(sPage);
            object.add(log);
            logs.replace(sPage, object);
        } else {
            LinkedList<ResponseLog> lData = new LinkedList<>();
            lData.add(log);
            logs.put(sPage, lData);
        }
        setResponseLogs(driver_handle, logs);
    }

    public List<ResponseLog> getResponseLog(String driver_handle, String sPage) {
        return Collections.unmodifiableList(get_ResponseLogs(driver_handle).get(sPage));
    }

    public List<RequestLog> getRequestLog(String driver_handle, String sPage) {
        return Collections.unmodifiableList(getRequestLogs(driver_handle).get(sPage));
    }

    public RequestLog getRequestLog(String driver_handle, String sPage, String sRequestId) {
        LinkedList<RequestLog> linkedList = getRequestLogs(driver_handle).get(sPage);
        try {
            for (RequestLog request : linkedList) {
                if (request.getRequestId().equals(sRequestId))
                    return request;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public void clearLog(String sPage, String driver_handle) {
        var logs = get_ResponseLogs(driver_handle);
        logs.remove(sPage);
        setResponseLogs(driver_handle,logs);
        LinkedHashMap<String, LinkedList<RequestLog>> logs2 = getRequestLogs(driver_handle);
        logs2.remove(sPage);
        setRequestLogs(driver_handle, logs2);
    }

    public void clearLog(String driver_handle) {
        responseLogs.remove(driver_handle);
        requestLogs.remove(driver_handle);
    }

    public ResponseLog getResponse(String driver_handle, String page_title, String sUrl, String response) {
        Map<String, LinkedList<ResponseLog>> responseLogs = getResponseLogs(driver_handle);
        for (ResponseLog log : responseLogs.get(page_title)) {
            if (log.getUrl().contains(sUrl) && log.getResponse().contains(response)) {
                return log;
            }
        }
        return null;
    }

    public ResponseLog getResponseByUrl(String driver_handle, String page_title, String sUrl) {
        final long timeInMillis = Calendar.getInstance().getTimeInMillis();
        while (timeInMillis + 20000 > Calendar.getInstance().getTimeInMillis()) {
            try {
                List<ResponseLog> logs = getResponseLog(driver_handle, page_title);
                for (ResponseLog ignored : logs) {
                    for (int i = logs.size() - 1; i >= 0; i--) {
                        ResponseLog response = logs.get(i);
                        System.out.println(response.getUrl());
                        System.out.println(response.getResponse());
                        if (response.getUrl().endsWith(sUrl)) {
                            return response;
                        }
                    }
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
        return null;
    }

    public ResponseLog getResponseByUrlAndResponseContent(String driver_handle, String page_title, String sUrl) {
        final long timeInMillis = Calendar.getInstance().getTimeInMillis();
        while (timeInMillis + 20000 > Calendar.getInstance().getTimeInMillis()) {
            try {
                List<ResponseLog> logs = getResponseLog(driver_handle, page_title);
                for (ResponseLog ignored : logs) {
                    for (int i = logs.size() - 1; i >= 0; i--) {
                        ResponseLog response = logs.get(i);
                        if (response.getUrl().endsWith(sUrl) && (!response.getResponse().contentEquals(""))) {
                            return response;
                        }
                    }
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
        return null;
    }

    public ResponseLog getResponseById(String driver_handle, String page_title, String requestId) {
        var responseLogs = get_ResponseLogs(driver_handle);
        for (ResponseLog log : responseLogs.get(page_title)) {
            if (log.getRequestId().contains(requestId)) {
                return log;
            }
        }
        return null;
    }

    public RequestLog getRequest(String driver_handle, String page_title, String sUrl, String post_data) {
        LinkedHashMap<String, LinkedList<RequestLog>> requestLogs = getRequestLogs(driver_handle);
        for (RequestLog log : requestLogs.get(page_title)) {
            if (log.getUrl().contains(sUrl) && log.getRequestBody().contains(post_data)) {
                return log;
            }
        }
        return null;
    }

    public boolean waitForResponse(String driverHandle, String pageTitle, String sUrl, String[] posConditions, String[] negConditions,
                                   int timeOutSeconds) {
        final long startTime = Calendar.getInstance().getTimeInMillis();
        final long endTime = startTime + (timeOutSeconds * 1000L);
        while (Calendar.getInstance().getTimeInMillis() < endTime) {
            try {
                List<ResponseLog> log = getResponseLog(driverHandle, pageTitle);
                for (int i = log.size() - 1; i >= 0; i--) {
                    ResponseLog response = log.get(i);
                    if (isMatchingResponse(response, sUrl)) {
                        if (!response.getIsVerified() && checkConditions(posConditions, response.getResponse()))
                            return true;
                        else if (!response.getIsVerified() && checkConditions(negConditions, response.getResponse()))
                            return false;
                        response.setIsVerified(true);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isMatchingResponse(ResponseLog response, String sUrl) {
        return response.getUrl().contains(sUrl) && response.getStatusCode().startsWith("2");
    }

    private boolean checkConditions(String[] conditions, String json) {
        BaseJsonParser jp = new BaseJsonParser();
        for (String string : conditions) {
            String[] arCondition = string.split("==");
            try {
                if (!(jp.readNode(json, arCondition[0])).contentEquals(arCondition[1]))
                    return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

}

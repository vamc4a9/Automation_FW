package com.qa.core.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
@Lazy
public class TestUtil {

    private ITestResult result;

    @Autowired
    UtilityLib utilLib;

    private TestUtil() {}

    public void init(ITestResult result) {
        this.result = result;
    }

    /**
     * Gets the test case name
     * @return Test case name
     */
    private String getTestName() {
        Test testNgAnnotations = result.getMethod().
                getConstructorOrMethod().
                getMethod().getAnnotation(Test.class);

        return (testNgAnnotations.testName() == null ||
                testNgAnnotations.testName().contentEquals("")) ?
                result.getMethod().getConstructorOrMethod().getName() :
                testNgAnnotations.testName();
    }

    /**
     * Gets the test description.
     * @return the test description
     */
    public String getTestDescription() {
        String name = getUpdatedTestName();
        return result.getMethod().getDescription() != null ? result.getMethod().getDescription() : name;
    }

    public String getUpdatedTestName() {
        String name = getTestName();
        return getUpdatedTestName(name);
    }

    private String getUpdatedTestName(String name) {
        List<String> parameters = utilLib.getListUsingRegEx(name, "\\{.+?\\}");
        for (String parameter : parameters) {
            name = name.replace(parameter,
                    getParameterValue(parameter.replace("{", "")
                            .replace("}", "")));
        }
        return name;
    }

    public String getParameters() {
        Object[] data = result.getParameters();
        String parameters = "";
        for(Object o : data) {
            parameters = (parameters.contentEquals("")) ? o + "" : parameters + o;
        }
//        parameters = parameters.replace(",", ";");
        return parameters;
    }
    private String getParameterValue(String key) {
        Object[] parameters = result.getParameters();
        for(Object parameter : parameters) {
            if (parameter instanceof Map) {
                if (((Map<?, ?>) parameter).containsKey(key)) {
                    return ((Map<?, ?>) parameter).get(key).toString();
                }
            } else if (parameter instanceof List) {
                Object lObject = ((List<?>) parameter).get(0);
                if (lObject instanceof Map) {
                    if (((Map<?, ?>) lObject).containsKey(key)) {
                        return ((Map<?, ?>) lObject).get(key).toString();
                    }
                }
            }
        }
        return "null";
    }

}

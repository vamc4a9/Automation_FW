package com.qa.core.context;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope(value = "prototype")
public class RunConfiguration {
    private static final ThreadLocal<Boolean> isCreated = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Properties> tlProps = new ThreadLocal<>();
    private static final ThreadLocal<String> tlEnv = new ThreadLocal<>();
    private final CoreParameters parameters;

    public RunConfiguration(CoreParameters parameters) {
        this.parameters = parameters;
    }

    @PostConstruct
    public void init() {
        if (!isCreated.get()) {
            loadAllProperties();
            setEnv(getProperty("env"));
            isCreated.set(true);
        }
    }

    public HashMap<String, String> getProperties(Set<String> keys) {
        var parameters = new HashMap<String, String>();
        for (String key : keys) {
            if (checkProperty(key))
                parameters.put(key, getProperty(key));
        }
        if (keys.contains("env") && getProperty("env").equalsIgnoreCase("predev"))
            parameters.put("predev_id", getProperty("predev_id"));
        return parameters;
    }

    public HashMap<String, String> addParameters(Map<String, String> parameters) {
        var existingValues = getProperties(parameters.keySet());
        for (String key : parameters.keySet()) {
            if (parameters.get(key) == null)
                putProperty(key, "!IGNORE!");
            else if (parameters.get(key).contentEquals(""))
                putProperty(key, "!IGNORE!");
            else
                putProperty(key, parameters.get(key));
        }
        return existingValues;
    }

    public void revertTheParameterValues(Map<String, String> newValues, Map<String, String> oldValues) {
        for (String key : newValues.keySet()) {
            if (checkProperty(key)) {
                if (oldValues.containsKey(key)) {
                    putProperty(key, oldValues.get(key));
                } else {
                    removeProperty(key);
                }
            }
        }
    }

    public void restoreProperties(Properties properties) {
        storeProps(properties);
    }

    private static void setEnv(String env) {
        tlEnv.set(env);
    }

    private static void storeProps(Properties props) {
        tlProps.set(props);
    }

    private static Properties props() {
        return tlProps.get();
    }

    public Properties getProps() {
        return tlProps.get();
    }

    public String environment() {
        return tlEnv.get();
    }

    public String environmentId() {
        var key = tlEnv.get();
        if (key == null)
            return "";
        if (key.equalsIgnoreCase("predev"))
            key = props().getProperty("predev_id");
        return key;
    }

    private synchronized void loadAllProperties() {
        try {
            synchronized(this) {
                Properties props = new Properties();
                FileInputStream Locator = new FileInputStream(parameters.getPropertyFilePath());
                props.load(Locator);
                addAdditionalProperties(props);
                addBuildParameters(props);
                storeProps(props);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /***
     * Any property that is sent via CLI which is already part of RunConfiguration will be read in this function
     * and will override the existing value
     * @param props - properties
     * @return - returns updated properties
     */
    private Properties addBuildParameters(Properties props) {
        System.out.println("Environment Reader Listener");
        String path = parameters.getWorkingDirectory() + "/runTime.properties";
        Properties newProps = loadCLIProperties(path);
        Set<Object> data = newProps.keySet();
        int index = 0;
        for (Object prop : data) {
            String newProp = (String) prop;
            // Added this condition to make sure we only overwrite the properties
            // which are already present in RunConfiguration
            if (props.containsKey(newProp)) {
                props.put(newProp, newProps.get(prop));
                index++;
            }
        }
        System.out.println("Overwritten " + index + " properties from RunConfiguration");
        return props;
    }

    private Properties loadCLIProperties(String file) {
        Properties props = new Properties();
        try {
            FileInputStream Locator;
            Locator = new FileInputStream(file);
            props.load(Locator);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return props;
    }

    private synchronized Properties addAdditionalProperties(Properties props) {
        ConcurrentHashMap<String, String> propFiles = new ConcurrentHashMap<>();
        props.forEach((key, value) -> {
            if (key.toString().endsWith("_properties")) {
                propFiles.put(key.toString(), value.toString());
            }
        });
        for(String propFile: propFiles.values()) {
            String path = parameters.getTargetFolderPath() + propFile;
            Properties additionalProps = new Properties();
            try {
                FileInputStream Locator = new FileInputStream(path);
                additionalProps.load(Locator);
                for (Object newKey : additionalProps.keySet()) {
                    props.put(newKey, additionalProps.get(newKey));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return props;
    }

    public boolean checkProperty(String key) {
        if (props().containsKey(key + "_" + environment())) {
            return true;
        } else if (props().containsKey(key + "_" + environmentId())) {
            return true;
        } else return props().containsKey(key);
    }

    public String getProperty(String key) {
        String defaultValue = key;
        if (key.contains("~~")) {
            defaultValue = key.split("~~")[1];
            key = key.split("~~")[0];
        }
        return getProperty(key, defaultValue);
    }

    public String getProperty(String key, String defaultValue) {
        if (props().containsKey(key + "_" + environment())) {
            return resolveString(props().getProperty(key + "_" + environment()));
        } else if (props().containsKey(key + "_" + environmentId())) {
            return resolveString(props().getProperty(key + "_" + environmentId()));
        } else if (props().containsKey(key)) {
            return resolveString(props().getProperty(key));
        } else {
            System.out.println("There is no variable named " + key + " Run configurations");
            return defaultValue;
        }
    }

    public Object get(String key) {
        if (props().contains(key + "_" + environment())) {
            return props().get(key + "_" + environment());
        } else if (props().containsKey(key + "_" + environmentId())) {
            return props().get(key + "_" + environmentId());
        } else {
            return props().get(key);
        }
    }

    public void putProperty(String key, String value) {
        if (key.equalsIgnoreCase("env") && value.toLowerCase().startsWith("predev")) {
            if (value.equalsIgnoreCase("predev")) {
                _putProperty(key, value);
            } else if (value.startsWith("predev")) {
                _putProperty(key, "predev");
                _putProperty("predev_id", value);
            }
        } else {
            _putProperty(key, value);
        }
    }

    private void _putProperty(String key, String value) {
        Properties props = props();
        props.put(key, value);
        System.out.println("added " + key + " with value " + value);
        storeProps(props);
        if (key.equalsIgnoreCase("env"))
            setEnv(value);
    }

    public void removeProperty(String key) {
        Properties props = props();
        props.remove(key);
        System.out.println("deleted " + key );
        storeProps(props);
    }

    /***
     * Writes to the physical filesystem. should not be used within test classes
     * this method is created only to write the initial gradle.properties from BaseTest class
     *
     * @param key - key to write into the test.properties
     * @param value - value of the key
     * @author vamsikrishna.kayyala
     */
    public void writeProperty(String key, String value) {
        Properties props = props();
        props.setProperty(key, value);
        saveProperties(props);
        props.put(key, value);
        System.out.println("added " + key + " with value " + value);
        storeProps(props);
        if (key.equalsIgnoreCase("env"))
            setEnv(value);

    }

    private void saveProperties(Properties p) {
        try {
            FileOutputStream fr = new FileOutputStream(parameters.getPropertyFilePath());
            p.store(fr, "Properties");
            fr.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putProperty(String key, Object value) {
        Properties props = props();
        props.put(key, value);
        storeProps(props);
    }

    public String resolveString(String data) {
        List<String> lData = getListUsingRegEx(data, "<<(.+?)>>");
        if (lData.size() == 0)
            return data;
        for (String key : lData) {
            data = data.replace(key, getProperty(key.replace("<<", "").replace(">>", "")));
        }
        data = resolveString(data);
        return data;
    }

    private List<String> getListUsingRegEx(String data, String sRegEx) {
        var pattern = Pattern.compile(sRegEx);
        Matcher matcher = pattern.matcher(data);
        List<String> lReturn = new ArrayList<>();
        while (matcher.find()) {
            lReturn.add(matcher.group());
        }
        return lReturn;
    }
}

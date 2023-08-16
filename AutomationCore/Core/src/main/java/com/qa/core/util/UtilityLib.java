package com.qa.core.util;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UtilityLib {

    public UtilityLib() {}

    /***
     * make the script wait for given time, this is deprecated, please use following method
     * {@link #fn_wait(Duration)}
     * @param seconds - integer time out number
     * @author vamsikrishna.kayyala
     */
    @Deprecated
    public void fn_wait_seconds(int seconds) {
        fn_wait(Duration.ofSeconds(seconds));
    }

    /**
     * this is deprecated, please use following method
     * {@link #fn_wait(Duration)}
     * @param minutes
     */
    @Deprecated
    public void fn_wait_minutes(int minutes) {
        fn_wait(Duration.ofMinutes(minutes));
    }

    public void fn_wait(Duration duration) {
        long startTime = System.currentTimeMillis();
        while (startTime + (duration.toMillis()) >= System.currentTimeMillis()) {
            /* Do nothing */
        }
    }

    /**
     * this is deprecated, please use following method
     * {@link #fn_wait(Duration)}
     * @param milliSeconds
     */
    @Deprecated
    public void fn_wait_ms(long milliSeconds) {
        long startTime = System.currentTimeMillis();
        while (startTime + (milliSeconds) >= System.currentTimeMillis()) {
            /* Do nothing */
        }
    }

    public List<String> ListMapToList(List<LinkedHashMap<String, String>> data, String key) {
        List<String> list = new ArrayList<String>();
        for(Map<String, String> map : data) {
            list.add(map.get(key));
        }
        return list;
    }

    public String regEx(String data, String sRegEx) {
        Pattern pattern = Pattern.compile(sRegEx);
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    public List<String> getListUsingRegEx(String data, String sRegEx) {
        Pattern pattern = Pattern.compile(sRegEx);
        Matcher matcher = pattern.matcher(data);

        List<String> lReturn = new ArrayList<String>();
        while (matcher.find()) {
            lReturn.add(matcher.group());
        }
        return lReturn;
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public int getRandomNumber(int min, int max, List<Integer> except) {
        int number = getRandomNumber(min, max);
        while (except.contains(number))
            number = getRandomNumber(min, max);
        return number;
    }

    public long randomFun() {
        long no = Calendar.getInstance().getTimeInMillis();
        return no;
    }

    public synchronized String random_name(String name) {
        return name + Calendar.getInstance().getTimeInMillis() + "_" + Thread.currentThread().getId();

    }

    public void printOnConsole(String message) {
        System.out.println(message);
    }

    /***
     * Adds a group key to the List Map and assigns the group number based on the totalDivisions count
     *
     * @param meta - metadata which needs be divided adn tagged into groups
     * @param totalDivisions - total number of groups
     * @return returns the same metadata but with additional Group key
     * @author vamsikrishna.kayyala
     */
    public List<Map<String, String>> assignGroupsInListMap(List<Map<String, String>> meta, int totalDivisions) {
        int loopCount = meta.size()/totalDivisions;
        int initLoopCount = loopCount;
        int iRow = 0;
        int group = 1;

        for(int i = iRow; i<=loopCount; i++) {
            if (i >= meta.size())
                break;
            meta.get(i).put("Group", "Group" + group);
            if (i == loopCount) {
                i = loopCount;
                group++;
                if (group == totalDivisions)
                    loopCount = meta.size();
                else
                    loopCount = initLoopCount * group;
            }
            if (i == meta.size())
                break;
        }
        return meta;
    }

    /***
     * Capitalize EachWord FirstLetter Without Space
     * for example : customer_balances to CustomerBalances
     * @param name
     * @return
     */
    public static String capitalizeEachWordFirstLetterWithoutSpace(String name) {

        if (name != null && name.length() != 0) {
            String[] eachWord = name.replaceAll("[-+^_]"," ").split(" ");
            StringBuilder capitalizedString = new StringBuilder();
            for (String value:eachWord) {
                char[] chars = value.toCharArray();
                chars[0] = Character.toUpperCase(chars[0]);
                capitalizedString.append(chars);
            }
            return capitalizedString.toString();
        } else {
            return name;
        }
    }

}

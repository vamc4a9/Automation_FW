package com.qa.core.dataLib;

import com.github.javafaker.Faker;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Lazy
@Scope("prototype")
public class DataProcessor {

    private String data;
    private String dateFormat = "YYYY/M/d";
    private String timeZone = "UTC";

    private final Map<String, Function<String, String>> dateResolver;

    public DataProcessor() {
        dateResolver = createDefaultDateResolver();
    }

    private Map<String, Function<String, String>> createDefaultDateResolver() {
        Map<String, Function<String, String>> resolver = new HashMap<>();
        resolver.put("end_of_today", this::getEndOfDay);
        resolver.put("end_of_tomorrow", this::getEndOfTomorrow);
        resolver.put("end_of_yesterday", this::getEndOfYesterday);
        resolver.put("end_of_current_month", this::getEndOfMonth);
        resolver.put("end_of_next_month", this::getEndOfNextMonth);
        resolver.put("end_of_last_month", this::getEndOfLastMonth);
        resolver.put("end_of_current_year", this::getEndOfYear);
        resolver.put("end_of_next_year", this::getEndOfNextYear);
        resolver.put("end_of_last_year", this::getEndOfLastYear);
        resolver.put("start_of_today", this::getStartOfDay);
        resolver.put("start_of_tomorrow", this::getStartOfTomorrow);
        resolver.put("start_of_yesterday", this::getStartOfYesterday);
        resolver.put("start_of_current_month", this::getStartOfMonth);
        resolver.put("start_of_next_month", this::getStartOfNextMonth);
        resolver.put("start_of_last_month", this::getStartOfLastMonth);
        resolver.put("start_of_current_year", this::getStartOfYear);
        resolver.put("start_of_next_year", this::getStartOfNextYear);
        resolver.put("start_of_last_year", this::getStartOfLastYear);
        return resolver;
    }

    public void init(String data) {
        this.data = data;
    }

    public String parse() {
        List<String> lData = getListUsingRegEx(data, "\\$\\$(.+?)\\$\\$");
        if (lData.size() == 0)
            return data;

        for (String sData : lData) {
            data = data.replace(sData, process(sData.replace("$$", "").replace("$$", "")));
        }

        data = parse();
        return data;
    }

    private List<String> getListUsingRegEx(String data, String sRegEx) {
        Pattern pattern = Pattern.compile(sRegEx);
        Matcher matcher = pattern.matcher(data);

        List<String> lReturn = new ArrayList<>();
        while (matcher.find()) {
            lReturn.add(matcher.group());
        }
        return lReturn;
    }

    public String process(String data) {
        if (data.toLowerCase().startsWith("day")) {
            return parseDay(data);
        } else if (data.toLowerCase().startsWith("month")) {
            return parseMonth(data);
        } else if (data.toLowerCase().startsWith("year")) {
            return parseYear(data);
        } else if (data.toLowerCase().startsWith("randnum")) {
            return randomNumber(data);
        } else if (data.toLowerCase().startsWith("unixtime")) {
            return parseUnixTime(data);
        } else if (data.toLowerCase().startsWith("currenttimeinmillis")) {
            return currentTimeInMillis();
        } else if (data.toLowerCase().startsWith("unique_id")) {
            return currentTimeInMillis() + Thread.currentThread().getId();
        } else if (data.toLowerCase().startsWith("faker")) {
            return parseFaker(data);
        } else if (data.toLowerCase().startsWith("uuid")) {
            return UUID.randomUUID().toString();
        } else if (shouldProcessDate(data)) {
            var firstParam = data.split("~")[0];
            var resolvingFunction = dateResolver.get(firstParam);
            if (resolvingFunction != null) {
                return resolvingFunction.apply(data);
            }
            throw new RuntimeException(String.format("date resolver is not supported for: %s", firstParam));
        } else {
            return data;
        }
    }

    private boolean shouldProcessDate(String data) {
        var firstParam = data.split("~")[0];
        return dateResolver.containsKey(firstParam);
    }

    private String parseFaker(String data) {
        Faker faker = new Faker();
        switch (data.split("~")[1].toLowerCase()) {
            case "first_name":
                return faker.name().firstName();
            case "last_name":
                return faker.name().lastName();
            case "address":
                return faker.address().fullAddress();
            case "city":
                return faker.address().city();
            case "country":
                return faker.address().country();
            case "email":
                return UUID.randomUUID() + "@test.com";
            case "phonenumber":
                return faker.phoneNumber().phoneNumber();
            case "company":
                return faker.app().name();
        }
        return null;
    }

    private String parseUnixTime(String data) {
        fn_wait(Duration.ofMillis(100));
        if (data.contains("~")) {
            var value = data.split("~")[1];
            if (value.toLowerCase().startsWith("second")) {
                return parseUnixSeconds(value);
            } else if (value.toLowerCase().startsWith("minute")) {
                return parseUnixMinutes(value);
            } else if (value.toLowerCase().startsWith("hour")) {
                return parseUnixHours(value);
            } else if (value.toLowerCase().startsWith("day")) {
                return parseUnixDay(value);
            } else if (value.toLowerCase().startsWith("week")) {
                return parseUnixWeek(value);
            } else if (value.toLowerCase().startsWith("month")) {
                return parseUnixMonth(value);
            } else if (value.toLowerCase().startsWith("year")) {
                return parseUnixYear(value);
            } else {
                return getCurrentUnixTime();
            }
        } else {
            return getCurrentUnixTime();
        }
    }

    private String getCurrentUnixTime() {
        var clock = Clock.systemUTC();
        var current = LocalDateTime.now(clock);
        return String.valueOf(current.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseUnixMinutes(String value) {
        var clock = Clock.systemUTC();
        LocalDateTime ldt;
        if (value.contains("+")) {
            ldt = LocalDateTime.now(clock).plusMinutes(Integer.parseInt(value.split("/+")[1]));
        } else if (value.contains("-")) {
            ldt = LocalDateTime.now(clock).minusMinutes(Integer.parseInt(value.split("-")[1]));
        } else {
            ldt = LocalDateTime.now();
        }
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseUnixSeconds(String value) {
        var clock = Clock.systemUTC();
        LocalDateTime ldt;
        if (value.contains("+")) {
            ldt = LocalDateTime.now(clock).plusSeconds(Integer.parseInt(value.split("/+")[1])).plusSeconds(5);
        } else if (value.contains("-")) {
            ldt = LocalDateTime.now(clock).minusSeconds(Integer.parseInt(value.split("-")[1])).plusSeconds(5);
        } else {
            ldt = LocalDateTime.now();
        }
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseUnixHours(String value) {
        var clock = Clock.systemUTC();
        LocalDateTime ldt;
        if (value.contains("+")) {
            ldt = LocalDateTime.now(clock).plusHours(Integer.parseInt(value.split("/+")[1])).plusSeconds(5);
        } else if (value.contains("-")) {
            ldt = LocalDateTime.now(clock).minusHours(Integer.parseInt(value.split("-")[1])).plusSeconds(5);
        } else {
            ldt = LocalDateTime.now();
        }
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseUnixDay(String value) {
        var clock = Clock.systemUTC();
        LocalDateTime ldt;
        if (value.contains("+")) {
            ldt = LocalDateTime.now(clock).plusDays(Integer.parseInt(value.split("/+")[1])).plusSeconds(5);
        } else if (value.contains("-")) {
            ldt = LocalDateTime.now(clock).minusDays(Integer.parseInt(value.split("-")[1])).plusSeconds(5);
        } else {
            ldt = LocalDateTime.now();
        }
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseUnixWeek(String value) {
        var clock = Clock.systemUTC();
        LocalDateTime ldt;
        if (value.contains("+")) {
            ldt = LocalDateTime.now(clock).plusWeeks(Integer.parseInt(value.split("/+")[1])).plusSeconds(5);
        } else if (value.contains("-")) {
            ldt = LocalDateTime.now(clock).minusWeeks(Integer.parseInt(value.split("-")[1])).plusSeconds(5);
        } else {
            ldt = LocalDateTime.now();
        }
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseUnixMonth(String value) {
        var clock = Clock.systemUTC();
        LocalDateTime ldt;
        if (value.contains("+")) {
            ldt = LocalDateTime.now(clock).plusMonths(Integer.parseInt(value.split("/+")[1])).plusSeconds(5);
        } else if (value.contains("-")) {
            ldt = LocalDateTime.now(clock).minusMonths(Integer.parseInt(value.split("-")[1])).plusSeconds(5);
        } else {
            ldt = LocalDateTime.now();
        }
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseUnixYear(String value) {
        var clock = Clock.systemUTC();
        LocalDateTime ldt;
        if (value.contains("+")) {
            ldt = LocalDateTime.now(clock).plusYears(Integer.parseInt(value.split("/+")[1])).plusSeconds(5);
        } else if (value.contains("-")) {
            ldt = LocalDateTime.now(clock).minusYears(Integer.parseInt(value.split("-")[1])).plusSeconds(5);
        } else {
            ldt = LocalDateTime.now();
        }
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }

    public String parseDay(String value) {
        var values = parseDateParameter(value);
        value = values.get(2);
        ZonedDateTime ldt = getCurrentZonedDateTime(values.get(1));
        if (value.contains("+")) {
            ldt = ldt.plusDays(Integer.parseInt(value.split("\\+")[1]));
        } else if (value.contains("-")) {
            ldt = ldt.minusDays(Integer.parseInt(value.split("-")[1]));
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern(values.get(0), Locale.ENGLISH);
        return format.format(ldt);
    }


    public String getStartOfMonth(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),1,0,0,0);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getStartOfLastMonth(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var startOfMonth = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),1,0,0,0);
        var localDateTime = startOfMonth.minusMonths(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getStartOfNextMonth(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var startOfMonth = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),1,0,0,0);
        var localDateTime = startOfMonth.plusMonths(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getEndOfNextMonth(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var startOfMonth = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),1,0,0,0);
        var localDateTime = startOfMonth.plusMonths(2).minusSeconds(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getEndOfLastMonth(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var startOfMonth = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),1,0,0,0);
        var localDateTime = startOfMonth.minusSeconds(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getEndOfMonth(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var startOfMonth = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),1,0,0,0);
        var localDateTime = startOfMonth.plusMonths(1).minusSeconds(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getStartOfDay(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),ldt.getDayOfMonth(),0,0,0);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private void getTimeZoneAndDateFormat(String parameters) {
        var arParams = parameters.split("~");
        timeZone = arParams.length == 3 ? arParams[2] : "UTC";
        dateFormat = arParams.length >= 2 ? arParams[1] : "YYYY/M/d";
    }

    private String getEndOfDay(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = ldt.toLocalDate().atTime(23, 59, 59);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getStartOfTomorrow(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),ldt.getDayOfMonth(),0,0,0);
        localDateTime = localDateTime.plusDays(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getEndOfTomorrow(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),ldt.getDayOfMonth(),0,0,0);
        localDateTime = localDateTime.plusDays(2).minusSeconds(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getStartOfYesterday(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),ldt.getDayOfMonth(),0,0,0);
        localDateTime = localDateTime.minusDays(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    public String getEndOfYesterday(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                ldt.getMonth(),ldt.getDayOfMonth(),0,0,0);
        localDateTime = localDateTime.minusSeconds(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private String getStartOfYear(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                1,1,0,0,0);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private String getEndOfYear(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                12,31,23,59,59);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private String getStartOfNextYear(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                1,1,0,0,0);
        localDateTime = localDateTime.plusYears(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private String getEndOfNextYear(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                12,31,23,59,59);
        localDateTime = localDateTime.plusYears(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private String getStartOfLastYear(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                1,1,0,0,0);
        localDateTime = localDateTime.minusYears(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private String getEndOfLastYear(String parameters) {
        getTimeZoneAndDateFormat(parameters);
        ZonedDateTime ldt = getCurrentZonedDateTime(timeZone);
        var localDateTime = LocalDateTime.of(ldt.getYear(),
                12,31,23,59,59);
        localDateTime = localDateTime.minusYears(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
        return format.format(localDateTime);
    }

    private List<String> parseDateParameter(String value) {
        List<String> outputs = new ArrayList<>();
        if (value.contains("~")) {
            var values = value.split("~");
            dateFormat = values[1];
            if (values.length == 3) {
                timeZone = values[2];
                value = value.replace("~" + dateFormat + "~" + timeZone, "");
            } else {
                value = value.replace("~" + dateFormat, "");
            }
        }
        outputs.add(dateFormat);
        outputs.add(timeZone);
        outputs.add(value);
        return outputs;
    }

    private ZonedDateTime getCurrentZonedDateTime(String timeZone) {
        Instant instant = Clock.systemUTC().instant();
        return instant.atZone(TimeZone.getTimeZone(timeZone).toZoneId());
    }

    private String currentTimeInMillis() {
        return "" + Calendar.getInstance().getTimeInMillis();
    }

    private String parseMonth(String value) {
        var values = parseDateParameter(value);
        value = values.get(2);
        ZonedDateTime ldt = getCurrentZonedDateTime(values.get(0));
        if (value.contains("+")) {
            ldt = ldt.plusMonths(Integer.parseInt(value.split("\\+")[1]));
        } else if (value.contains("-")) {
            ldt = ldt.minusMonths(Integer.parseInt(value.split("-")[1]));
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern(values.get(0), Locale.ENGLISH);
        return format.format(ldt);
    }

    private String parseYear(String value) {
        var values = parseDateParameter(value);
        value = values.get(2);
        ZonedDateTime ldt = getCurrentZonedDateTime(values.get(2));
        if (value.contains("+")) {
            ldt = ldt.plusYears(Integer.parseInt(value.split("\\+")[1]));
        } else if (value.contains("-")) {
            ldt = ldt.minusYears(Integer.parseInt(value.split("-")[1]));
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern(values.get(0), Locale.ENGLISH);
        return format.format(ldt);
    }

    private String randomNumber(String value) {
        String range = value.split("~")[1];
        int min = Integer.parseInt(range.split(":")[0]);
        int max = Integer.parseInt(range.split(":")[1]);
        return String.valueOf(Math.round((Math.random() * (max - min)) + min));
    }

    private void fn_wait(Duration duration) {
        long startTime = System.currentTimeMillis();
        while (startTime + (duration.toMillis()) >= System.currentTimeMillis()) {
            /* Do nothing */
        }
    }

}

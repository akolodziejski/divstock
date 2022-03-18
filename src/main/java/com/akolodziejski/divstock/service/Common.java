package com.akolodziejski.divstock.service;

import lombok.SneakyThrows;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Common {
    public static SimpleDateFormat SIMPLE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @SneakyThrows
    public static Date getLastDayForYear(int year) {
        return SIMPLE_FORMAT.parse(String.valueOf(year) + "-12-31");
    }

    @SneakyThrows
    public static Date getFirstDayForYear(int year) {
        return SIMPLE_FORMAT.parse(year + "-01-01");
    }

    public static Date getPreviousWorkingDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int dayOfWeek;
        do {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        } while (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);

        return cal.getTime();
    }
}

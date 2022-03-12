package com.example.blind;

import java.util.Calendar;

public class TimeDate {
    private static String MYear;  //年
    private static String Month;  //月
    private static String MDay;   //日
    private static String MWay;   //星期
    private static String MHours; //小时
    private static String MMinute;//分钟
    String time;

    Calendar calendar = Calendar.getInstance();

    private MainActivity mainActivity;
//    TimeDate(MainActivity mainActivity) {
//
//    }
    public String getMYear(){
        MYear = String.valueOf(calendar.get(Calendar.YEAR));
        return MYear;
    }
    public String getMonth(){
        Month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        return Month;
    }
    public String getMDay () {
        MDay = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        return MDay;
    }
    public String getMWay () {
        MWay = String.valueOf(calendar.get(Calendar.DAY_OF_WEEK));
        if ("1".equals(MWay)) {
            MWay = "天";
        } else if ("2".equals(MWay)) {
            MWay = "一";
        } else if ("3".equals(MWay)) {
            MWay = "二";
        } else if ("4".equals(MWay)) {
            MWay = "三";
        } else if ("5".equals(MWay)) {
            MWay = "四";
        } else if ("6".equals(MWay)) {
            MWay = "五";
        } else if ("7".equals(MWay)) {
            MWay = "六";
        }
        return MWay;
    }
    public String getMHours () {
        //如果小时是个位数，就在其前面加一个“0”
        if (calendar.get(Calendar.HOUR) < 10) {
            MHours = "0" + calendar.get(Calendar.HOUR);
        } else {
            MHours = String.valueOf(calendar.get(Calendar.HOUR));
        }
        return MHours;
    }
    public String getMMinute () {
        //如果分钟是各位数，就在其前面加一个“0”
        if (calendar.get(Calendar.MINUTE) < 10) {
            MMinute = "0" + calendar.get((Calendar.MINUTE));
        } else {
            MMinute = String.valueOf(calendar.get(Calendar.MINUTE));
        }
        return MMinute;
    }
}
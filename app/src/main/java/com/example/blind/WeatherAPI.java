package com.example.blind;



import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;




public class WeatherAPI {
    private static String province;
    private static String city;
    //今日天气部分
    private static String weathertaday;
    private static String temperaturetaday;
    private static String winddirectiontaday;
    private static String windpowertaday;
    //明日天气部分
    private static String weathertomorrow;
    private static String temperaturetomorrow;
    private static String winddirectiontomorrow;
    private static String windpowertomorrow;
    String getProvince(){
        httpURLGETCase();
        return province;
    }
    String getCity(){
        httpURLGETCase();
        return city;
    }



    String gettadayWeather(){
        httpURLGETCase();
        return weathertaday;
    }
    String gettomorrowWeather(){
        httpURLGETCase();
        return weathertomorrow;
    }



    String gettadayTemperature(){
        httpURLGETCase();
        return temperaturetaday;
    }
    String gettomorrowTemperature(){
        httpURLGETCase();
        return temperaturetomorrow;
    }



    String gettadayWinddirection(){
        httpURLGETCase();
        return winddirectiontaday;
    }
    String gettomorrowWinddirection(){
        httpURLGETCase();
        return winddirectiontomorrow;
    }



    String gettadayWindpower(){
        httpURLGETCase();
        return windpowertaday;
    }
    String gettomorrowWindpower(){
        httpURLGETCase();
        return windpowertomorrow;
    }

    public static void httpURLGETCase() {

        String methodUrl = "http://restapi.amap.com/v3/weather/weatherInfo?key=ba3bcf7df9cfdb7bd0edf810ed98fb93&city=340321&extensions=all";
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String line = null;
        try {
//            Log.d("yunxxxxx", "省份" + "1111111111");
            URL url = new URL(methodUrl);
            // 根据URL生成HttpURLConnection
            connection = (HttpURLConnection) url.openConnection();
            // 默认GET请求
            connection.setRequestMethod("GET");
            connection.connect();// 建立TCP连接
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // 发送http请求
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder result = new StringBuilder();
                // 循环读取流
                while ((line = reader.readLine()) != null) {
                    result.append(line).append(System.getProperty("line.separator"));
                }

                JSONObject mapTypes = JSON.parseObject(result.toString());
                JSONObject forecasts = mapTypes.getJSONArray("forecasts").getJSONObject(0);
                //casts1代表今天的天气数据
                JSONObject casts1 = forecasts.getJSONArray("casts").getJSONObject(0);
                //casts2代表明天的天气数据
                JSONObject casts2 = forecasts.getJSONArray("casts").getJSONObject(1);


//                Log.d("yunxxxxx", "省份" + "1111111111");
                province = forecasts.getString("province");
                city = forecasts.getString("city");

                //今日天气
                weathertaday = casts1.getString("dayweather");
                temperaturetaday = casts1.getString("daytemp");
                winddirectiontaday = casts1.getString("daywind");
                windpowertaday = casts1.getString("daypower");

                //明日天气
                weathertomorrow = casts2.getString("dayweather");
                temperaturetomorrow = casts2.getString("daytemp");
                winddirectiontomorrow = casts2.getString("daywind");
                windpowertomorrow = casts2.getString("daypower");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}













package com.example.blind;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import org.json.JSONObject;

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

public class WeatherActivity extends AppCompatActivity {

    private String province;
    private String city;
    private String weather;
    public String getprovince() throws IOException {
        URL url = new URL("https://restapi.amap.com/v3/weather/weatherInfo?city=230123&key=4d1a68c3c6b8500db5b6ee66d8779c89");
        Scanner Scanner = new Scanner(url.openStream());
        if(Scanner.hasNext()){
            String string = Scanner.nextLine();
            string = string.replaceAll("\"","");
            Matcher matcher = Pattern.compile("\\[\\{(.*)\\}\\]").matcher(string);
            if(matcher.find()){
                string = matcher.group(1).replaceAll(",", "\r\n");
                Properties properties = new Properties();
                properties.load(new StringReader(string));
                province = properties.getProperty("province");
            }
        }
        return province;
    }
    public void getcity() throws IOException {
        URL url = new URL("https://restapi.amap.com/v3/weather/weatherInfo?city=230123&key=4d1a68c3c6b8500db5b6ee66d8779c89");
        Scanner Scanner = new Scanner(url.openStream());
        if(Scanner.hasNext()){
            String string = Scanner.nextLine();
            string = string.replaceAll("\"","");
            Matcher matcher = Pattern.compile("\\[\\{(.*)\\}\\]").matcher(string);
            if(matcher.find()){
                string = matcher.group(1).replaceAll(",", "\r\n");
                Properties properties = new Properties();
                properties.load(new StringReader(string));
                city = properties.getProperty("city");
            }
        }
    }
    public void getweather() throws IOException {
        URL url = new URL("https://restapi.amap.com/v3/weather/weatherInfo?city=230123&key=4d1a68c3c6b8500db5b6ee66d8779c89");
        Scanner Scanner = new Scanner(url.openStream());
        if(Scanner.hasNext()){
            String string = Scanner.nextLine();
            string = string.replaceAll("\"","");
            Matcher matcher = Pattern.compile("\\[\\{(.*)\\}\\]").matcher(string);
            if(matcher.find()){
                string = matcher.group(1).replaceAll(",", "\r\n");
                Properties properties = new Properties();
                properties.load(new StringReader(string));
                weather = properties.getProperty("weather");
            }
        }
    }

//    public static class Test {
//        private static void httpURLGETCase() {
//            String methodUrl = "http://restapi.amap.com/v3/weather/weatherInfo?key=ba3bcf7df9cfdb7bd0edf810ed98fb93&city=110101";
//            HttpURLConnection connection = null;
//            BufferedReader reader = null;
//            String line = null;
//            try {
//                URL url = new URL(methodUrl);
//                // 根据URL生成HttpURLConnection
//                connection = (HttpURLConnection) url.openConnection();
//                // 默认GET请求
//                connection.setRequestMethod("GET");
//                connection.connect();// 建立TCP连接
//                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    // 发送http请求
//                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
//                    StringBuilder result = new StringBuilder();
//                    // 循环读取流
//                    while ((line = reader.readLine()) != null) {
//                        result.append(line).append(System.getProperty("line.separator"));
//                    }
//                    System.out.println(result.toString());
//
//                    JSONObject mapTypes = JSON.parseObject(result.toString());
//                    for (Object obj : mapTypes.keySet()){
//                        System.out.println(obj+"的值为："+mapTypes.get(obj));
//                    }
//
//                    System.out.println("\n\n在lives中：");
//
//                    JSONObject lives = mapTypes.getJSONArray("lives").getJSONObject(0);
//                    for (Object obj : lives.keySet()){
//                        System.out.println(obj+"值为："+lives.get(obj));
//                    }
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }finally {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                connection.disconnect();
//            }
//        }
//
//        public static void main(String[] args){
//            httpURLGETCase();
//        }
//    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
    }

//    public static void main(String[] args) throws IOException {
//        URL url = new URL("https://restapi.amap.com/v3/weather/weatherInfo?city=230123&key=4d1a68c3c6b8500db5b6ee66d8779c89");
//        Scanner Scanner = new Scanner(url.openStream());
//        if(Scanner.hasNext()){
//            String string = Scanner.nextLine();
//            string = string.replaceAll("\"","");
//            Matcher matcher = Pattern.compile("\\[\\{(.*)\\}\\]").matcher(string);
//            if(matcher.find()){
//                string = matcher.group(1).replaceAll(",", "\r\n");
//                Properties properties = new Properties();
//                properties.load(new StringReader(string));
//                String province = properties.getProperty("province");
//                String city = properties.getProperty("city");
//                String weather = properties.getProperty("weather");
//
//            }
//        }
//    }

}
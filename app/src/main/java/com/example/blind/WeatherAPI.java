package com.example.blind;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

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
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    public AMapLocationClientOption mLocationOption = null;
    public AMapLocationListener mLocationListener;

    private MainActivity mainAct;

    public WeatherAPI(MainActivity mainAct) throws Exception {
        this.mainAct = mainAct;
        mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (amapLocation.getErrorCode() == 0) {
                    //定位成功回调信息，设置相关消息
                    amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    amapLocation.getLatitude();//获取纬度
                    amapLocation.getLongitude();//获取经度
                    amapLocation.getAccuracy();//获取精度信息
                    amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                    amapLocation.getCountry();//国家信息
                    amapLocation.getProvince();//省信息
                    amapLocation.getCity();//城市信息
                    amapLocation.getDistrict();//城区信息
                    amapLocation.getStreet();//街道信息
                    amapLocation.getStreetNum();//街道门牌号信息
                    amapLocation.getCityCode();//城市编码
                    amapLocation.getAdCode();//地区编码
                    amapLocation.getAoiName();//获取当前定位点的AOI信息
                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError", "location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
        };


        setmLocationOption();
        //声明定位回调监听器
        setmLocationClient();

        Log.d("gou", mLocationClient.getLastKnownLocation().getAdCode());
        httpURLGETCase();
    }

    private void setmLocationClient() throws Exception {
//初始化定位
//设置定位回调监听
        mLocationClient = new AMapLocationClient(mainAct.getApplicationContext());
        mLocationClient.setLocationListener(mLocationListener);
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    private void setmLocationOption() {
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
    }

    public String getProvince(){
        httpURLGETCase();
        return province;
    }
    public String getCity(){
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


    public void httpURLGETCase() {
        String adCode = mLocationClient.getLastKnownLocation().getAdCode();
        String methodUrl = "http://restapi.amap.com/v3/weather/weatherInfo?key=ba3bcf7df9cfdb7bd0edf810ed98fb93&city="+ adCode +"&extensions=all";
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String line = null;
        try {
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













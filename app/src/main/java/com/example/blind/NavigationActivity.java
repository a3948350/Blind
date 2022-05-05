package com.example.blind;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.tencentmap.mapsdk.maps.LocationSource;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.TextureMapView;
import com.tencent.tencentmap.mapsdk.maps.UiSettings;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptor;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.MyLocationStyle;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity implements
        TencentLocationListener, LocationSource, TencentMap.OnMapLongClickListener, RadioGroup.OnCheckedChangeListener {

    private TextureMapView mapView;
    protected TencentMap tencentMap;
    private TencentLocationManager mLocationManager;
    private TencentLocationRequest mLocationRequest;
    protected UiSettings mapUiSettings;
    private MyLocationStyle locationStyle;
    private LocationSource.OnLocationChangedListener locationChangedListener;

    private Button btn_Recognize;

    private String Location;
    private String City;
    private String District;
    private String Street;
    private String Town;
    private String Name;

    private String texts;

    // 语音合成对象
    private SpeechSynthesizer mTts;
    private static String TAG = NavigationActivity.class.getSimpleName();

    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private void checkPermissions() {

        List<String> permissions = new LinkedList<>();

        addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        addPermission(permissions, Manifest.permission.ACCESS_COARSE_LOCATION);
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE);

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

    }

    private void addPermission(List<String> permissionList, String permission) {

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(permission);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        checkPermissions();

        mapView = findViewById(R.id.mapview);
        mapView.setOpaque(false);
        //创建tencentMap地图对象，可以完成对地图的几乎所有操作
        tencentMap = mapView.getMap();

        mapUiSettings = tencentMap.getUiSettings();
        tencentMap.setOnMapLongClickListener(this);

        //建立定位
        initLocation();

        // 语音合成 1.创建SpeechSynthesizer对象, 第二个参数：本地合成时传InitListener
        mTts = SpeechSynthesizer.createSynthesizer(NavigationActivity.this,
                mTtsInitListener);
        mTts.startSpeaking("跳转至地图定位", mSynListener);

        //SDK版本4.3.5新增内置定位标点击回调监听
        tencentMap.setMyLocationClickListener(new TencentMap.OnMyLocationClickListener() {
            @Override
            public boolean onMyLocationClicked(LatLng latLng) {
                Toast.makeText(NavigationActivity.this, "内置定位标点击回调", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        GestureDetector gestureDetector = new GestureDetector(NavigationActivity.this,new GestureDetector.SimpleOnGestureListener(){
            /**
             * 发生确定的单击时执行
             * @param e
             * @return
             */
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {//单击事件
                Toast.makeText(NavigationActivity.this,"这是单击事件", Toast.LENGTH_SHORT).show();
                String address = City + " " + District + "," + Street + " " + Town + "," + Name;
                Log.d("Nav","address:" + address);
                texts = address;
                mTts.startSpeaking("当前位置是：" + texts, mSynListener);
                return super.onSingleTapConfirmed(e);
            }

            /**
             * 双击发生时的通知
             * @param e
             * @return
             */
            @Override
            public void onLongPress(MotionEvent e) {
                Toast.makeText(NavigationActivity.this,"这是双击事件",Toast.LENGTH_SHORT).show();
                texts = "已返回主界面";
                mTts.startSpeaking(texts,mSynListener);
                mapView.onDestroy();
                Intent intent = new Intent(NavigationActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        btn_Recognize = findViewById(R.id.btn_NavigationRecognize);
        btn_Recognize.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

    }

    /**
     * 定位的一些初始化设置
     */
    private void initLocation() {
        //用于访问腾讯定位服务的类, 周期性向客户端提供位置更新
        mLocationManager = TencentLocationManager.getInstance(this);
        //设置坐标系
        mLocationManager.setCoordinateType(TencentLocationManager.COORDINATE_TYPE_GCJ02);
        //创建定位请求
        mLocationRequest = TencentLocationRequest.create();
        //设置定位周期（位置监听器回调周期）为3s
        mLocationRequest.setInterval(500).setAllowGPS(true).setAllowDirection(true);

        //地图上设置定位数据源
        tencentMap.setLocationSource(this);
        //设置当前位置可见
        tencentMap.setMyLocationEnabled(true);
        //设置定位图标样式
        setLocMarkerStyle();
        tencentMap.setMyLocationStyle(locationStyle);
    }

    /**
     * 设置定位图标样式
     */
    private void setLocMarkerStyle() {
        locationStyle = new MyLocationStyle();
        //创建图标
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(getBitMap(R.drawable.location_icon));
        locationStyle.icon(bitmapDescriptor);
        //设置定位圆形区域的边框宽度
        locationStyle.strokeWidth(3);
        //设置圆区域的颜色
        locationStyle.fillColor(R.color.style);
    }

    private Bitmap getBitMap(int resourceId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceId);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = 55;
        int newHeight = 55;
        float widthScale = ((float) newWidth) / width;
        float heightScale = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(widthScale, heightScale);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return bitmap;
    }

    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
        if (i == TencentLocation.ERROR_OK && locationChangedListener != null) {
            Location location = new Location(tencentLocation.getProvider());
            //设置经纬度以及精度
            location.setLatitude(tencentLocation.getLatitude());
//            Log.d("Nav","经度：" + tencentLocation.getLatitude());
            location.setLongitude(tencentLocation.getLongitude());
//            Log.d("Nav","纬度：" + tencentLocation.getLongitude());
            Location = tencentLocation.toString();
            City = tencentLocation.getCity();
            District = tencentLocation.getDistrict();
            Street = tencentLocation.getStreet();
            Town = tencentLocation.getTown();
            Name = tencentLocation.getName();
            location.setAccuracy(tencentLocation.getAccuracy());
            locationChangedListener.onLocationChanged(location);

            //显示回调的实时位置信息
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //打印tencentLocation的json字符串
//                    Toast.makeText(getApplicationContext(), new Gson().toJson(location), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onStatusUpdate(String s, int status, String s1) {
        //GPS, WiFi, Radio 等状态发生变化
        Log.v("State changed", s + "===" + s1);
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
        locationChangedListener = onLocationChangedListener;

        int err = mLocationManager.requestLocationUpdates(mLocationRequest, this, Looper.myLooper());
        switch (err) {
            case 1:
                Toast.makeText(this, "设备缺少使用腾讯定位服务需要的基本条件", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(this, "manifest 中配置的 key 不正确", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(this, "自动加载libtencentloc.so失败", Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }

    @Override
    public void deactivate() {
        mLocationManager.removeUpdates(this);
        mLocationManager = null;
        mLocationRequest = null;
        locationChangedListener = null;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Location location = new Location("LongPressLocationProvider");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        location.setAccuracy(20);
        locationChangedListener.onLocationChanged(location);
        Log.i("long click", new Gson().toJson(latLng));
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch (i) {
            //连续定位，但不会移动到地图中心点，并且会跟随设备移动
            case R.id.btn_follow_no_center:

                initLocation();
                locationStyle = locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);
                tencentMap.setMyLocationStyle(locationStyle);
                break;
            //连续定位，且将视角移动到地图中心，定位点依照设备方向旋转，并且会跟随设备移动,默认是此种类型
            case R.id.btn_location_rotate:

                initLocation();
                locationStyle = locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
                tencentMap.setMyLocationStyle(locationStyle);
                break;
            //连续定位，但不会移动到地图中心点，定位点依照设备方向旋转，并且跟随设备移动
            case R.id.btn_location_rotate_no_center:

                initLocation();
                locationStyle = locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
                tencentMap.setMyLocationStyle(locationStyle);
                break;
            //连续定位，但不会移动到地图中心点，地图依照设备方向旋转，并且会跟随设备移动
            case R.id.btn_map_rotate_no_center:

                initLocation();
                locationStyle = locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE_NO_CENTER);
                tencentMap.setMyLocationStyle(locationStyle);
                break;
        }
    }

    /**
     * mapview的生命周期管理
     */
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
        mapView.onDestroy();
        super.onDestroy();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mapView.onRestart();
    }

    /**
     * 语音合成监听
     */
    private SynthesizerListener mSynListener = new SynthesizerListener() {
        // 会话结束回调接口，没有错误时，error为null
        public void onCompleted(SpeechError error) {
            if (error != null) {
                Log.d("mySynthesiezer complete code:", error.getErrorCode()
                        + "");
            } else {
                Log.d("mySynthesiezer complete code:", "0");
            }
        }

        // 缓冲进度回调
        // percent为缓冲进度0~100，beginPos为缓冲音频在文本中开始位置，endPos表示缓冲音频在文本中结束位置，info为附加信息。
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
        }

        // 开始播放
        public void onSpeakBegin() {
        }

        // 暂停播放
        public void onSpeakPaused() {
        }

        // 播放进度回调
        // percent为播放进度0~100,beginPos为播放音频在文本中开始位置，endPos表示播放音频在文本中结束位置.
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        // 恢复播放回调接口
        public void onSpeakResumed() {
        }

        // 会话事件回调接口
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }
    };

    /**
     * 初始化语音合成监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                // showTip("初始化失败,错误码：" + code);
                Toast.makeText(getApplicationContext(), "初始化失败,错误码：" + code,
                        Toast.LENGTH_SHORT).show();
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };
}
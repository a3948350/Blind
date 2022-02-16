package com.example.blind;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.TextureMapView;

public class NavigationActivity extends AppCompatActivity {

    private TextureMapView mapView;
    protected TencentMap tencentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        mapView = findViewById(R.id.mapview);
        mapView.setOpaque(false);
        //创建tencentMap地图对象，可以完成对地图的几乎所有操作
        tencentMap = mapView.getMap();

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
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mapView.onRestart();
    }
}
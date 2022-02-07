package com.example.blind;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.cloud.qcloudasrsdk.common.QCloudAudioFormat;
import com.tencent.cloud.qcloudasrsdk.models.QCloudOneSentenceRecognitionParams;
import com.tencent.cloud.qcloudasrsdk.recognizer.QCloudOneSentenceRecognizer;
import com.tencent.cloud.qcloudasrsdk.recognizer.QCloudOneSentenceRecognizerAudioPathListener;
import com.tencent.cloud.qcloudasrsdk.recognizer.QCloudOneSentenceRecognizerListener;

public class test extends AppCompatActivity implements QCloudOneSentenceRecognizerListener {

    private static final String TAG = test.class.getSimpleName();
    private boolean recording = false;

    private Button btn_Test;

    private int REQUEST_CODE = 1002;
    //磁盘读写权限 麦克风录音权限
    String[] permiss = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        if(!checkMyPermission(permiss)){
            ActivityCompat.requestPermissions(this,permiss, REQUEST_CODE);
        }

        btn_Test = findViewById(R.id.btn_Test);
        QCloudOneSentenceRecognizer recognizer = new QCloudOneSentenceRecognizer(this, String.valueOf(1307538305),"AKIDxjETR8ZibDYcPQAdplQtAD1ZruhEVWIo","HH2vb080bvykFTP6WeV5OQBfusZz91cw");

        recognizer.setCallback(this);

        btn_Test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (recording) {
                        recognizer.stopRecognizeWithRecorder();
                    }
                    else {

                        /**
                         * setDefaultParams 默认参数param
                         * @param filterDirty    0 ：默认状态 不过滤脏话 1：过滤脏话
                         * @param filterModal    0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
                         * @param filterPunc     0 ：默认状态 不过滤句末的句号 1：滤句末的句号
                         * @param convertNumMode 1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
                         * @param hotwordId  热词id，不使用则传null
                         */
                        recognizer.setDefaultParams(0, 0, 0, 1,null);
                        recognizer.recognizeWithRecorder();
                        recognizer.setQCloudOneSentenceRecognizerAudioPathListener(new QCloudOneSentenceRecognizerAudioPathListener() {
                            @Override
                            public void callBackAudioPath(String audioPath) {
                                Log.d(TAG, "callBackAudioPath: audioPath="+audioPath);
//                                Toast.makeText(OneSentenceRecognizeActivity.this, "文件路径："+audioPath, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("exception msg" + e.getMessage());
                } finally {
                }

            }
        });

    }

    private void updateUI() {
        Button recordButton = findViewById(R.id.btn_Test);
        if (recording) {
            recordButton.setText("stopRecord");
        }
        else {
            recordButton.setText("startRecord");
        }
    }

    private boolean checkMyPermission(String[] permiss){
        if(permiss !=null && permiss.length > 0 ){
            for(String per : permiss) {
                if (ContextCompat.checkSelfPermission(this, per) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == REQUEST_CODE) {
            boolean isAllGranted = true;
            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
            } else {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                openAppDetails();
            }
        }
    }

    /**
     * 打开 APP 的详情设置
     */
    private void openAppDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("录音需要访问 “外部存储器”，请到 “应用信息 -> 权限” 中授予！");
        builder.setPositiveButton("去手动授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    @Override
    public void didStartRecord() {
        recording = true;
        Toast.makeText(this, "开始录音!", Toast.LENGTH_SHORT).show();

        updateUI();
    }

    @Override
    public void didStopRecord() {
        recording = false;

        Toast.makeText(this, "停止录音!", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    public void recognizeResult(QCloudOneSentenceRecognizer recognizer, String result, Exception exception)  {

        TextView textView = findViewById(R.id.textViewTest);
        Log.e("recognizeResult","thread id:" + Thread.currentThread().getId() + " name:" + Thread.currentThread().getName());
        if (exception != null) {
            Log.e("recognizeResult","result: " + result + "exception msg" + exception + exception.getLocalizedMessage());
            textView.setText(exception.getLocalizedMessage());
        }
        else {
            Log.e("recognizeResult","result: " + result);
            textView.setText(result);
        }
    }

}
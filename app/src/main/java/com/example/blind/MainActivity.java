package com.example.blind;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.tencent.aai.AAIClient;
import com.tencent.aai.audio.data.AudioRecordDataSource;
import com.tencent.aai.audio.utils.WavCache;
import com.tencent.aai.auth.AbsCredentialProvider;
import com.tencent.aai.auth.LocalCredentialProvider;
import com.tencent.aai.config.ClientConfiguration;
import com.tencent.aai.exception.ClientException;
import com.tencent.aai.exception.ServerException;
import com.tencent.aai.listener.AudioRecognizeResultListener;
import com.tencent.aai.listener.AudioRecognizeStateListener;
import com.tencent.aai.listener.AudioRecognizeTimeoutListener;
import com.tencent.aai.log.AAILogger;
import com.tencent.aai.model.AudioRecognizeRequest;
import com.tencent.aai.model.AudioRecognizeResult;
import com.tencent.aai.model.type.AudioRecognizeConfiguration;
import com.tencent.aai.model.type.AudioRecognizeTemplate;
import com.tencent.aai.model.type.EngineModelType;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    Button btn_CallActivity;
    Button btn_SendActivity;
    Button btn_Recognize;
    Button btn_Navigation;
    Button btn_WeatherActivity;
    Button btn_Music;
    Button btn_TimeDate;
    TextView recognizeResult;
    TextView recognizeState;
    int currentRequestId = 0;
    Handler handler;

    String SHA1;



    private static String TAG = MainActivity.class.getSimpleName();
    //是否开始录音标记
    private boolean orderflag = true;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "xiaoyan";
    //  语音合成播报内容
    String texts = "";

    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    //  语音识别AAIClient构建
    AAIClient aaiClient;
    //  证书鉴权
    AbsCredentialProvider credentialProvider;
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);
    private boolean switchToDeviceAuth = false;
    boolean isSaveAudioRecordFiles=true;
    private final String PERFORMANCE_TAG = "PerformanceTag";

    YCM ycm = new YCM();

    private void checkPermissions() {

        List<String> permissions = new LinkedList<>();

        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        addPermission(permissions, Manifest.permission.RECORD_AUDIO);
        addPermission(permissions, Manifest.permission.INTERNET);
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE);
        addPermission(permissions, Manifest.permission.ACCESS_COARSE_LOCATION);
        addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        addPermission(permissions, Manifest.permission.ACCESS_NETWORK_STATE);
        addPermission(permissions, Manifest.permission.ACCESS_WIFI_STATE);
        addPermission(permissions, Manifest.permission.CHANGE_WIFI_STATE);

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

    LinkedHashMap<String, String> resMap = new LinkedHashMap<>();
    private String buildMessage(Map<String, String> msg) {

        StringBuffer stringBuffer = new StringBuffer();
        Iterator<Map.Entry<String, String>> iter = msg.entrySet().iterator();
        while (iter.hasNext()) {
            String value = iter.next().getValue();
            stringBuffer.append(value+"\r\n");
        }
        return stringBuffer.toString();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //隐私政策内容合规
        try{
            AMapLocationClient.updatePrivacyShow(this, true, true);
            AMapLocationClient.updatePrivacyAgree(this,true);
        }catch (Exception e){
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=8f27a48e");
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }



        recognizeResult = findViewById(R.id.textViewMainRecognizeResult);
        recognizeState = findViewById(R.id.textViewMainRecognizeState);
        handler = new Handler(getMainLooper());

        // 检查sdk运行的必要条件权限
        checkPermissions();

        SHA1 = sHA1(MainActivity.this);
        Log.d("SHA1","SHA1:" + SHA1);


        /************************语音识别部分************************/
        // 用户配置：需要在控制台申请相关的账号;
        final int appid = 1307538305;

        //设置ProjectId 不设置默认使用0，说明：项目功能用于按项目管理云资源，可以对云资源进行分项目管理，详情见 https://console.cloud.tencent.com/project
        final int projectId = 0;
        final String secretId = ycm.getSecretId();
        final String secretKey = ycm.getSecretKey();

        AAILogger.info(logger, "config : appid={}, projectId={}, secretId={}, secretKey={}", appid, projectId, secretId, secretKey);

        // 签名鉴权类，sdk中给出了一个本地的鉴权类，但由于需要用户提供secretKey，这可能会导致一些安全上的问题，
        // 因此，请用户自行实现CredentialProvider接口
        if (!switchToDeviceAuth) {
            credentialProvider = new LocalCredentialProvider(secretKey);
        } else {
//            credentialProvider = new LocalCredentialProvider(DemoConfig.secretKeyForDeviceAuth);
        }

        // 用户配置
        ClientConfiguration.setMaxAudioRecognizeConcurrentNumber(1); // 语音识别的请求的最大并发数
        ClientConfiguration.setMaxRecognizeSliceConcurrentNumber(1); // 单个请求的分片最大并发数

        // 识别结果回调监听器
        final AudioRecognizeResultListener audioRecognizeResultlistener = new AudioRecognizeResultListener() {

            boolean dontHaveResult = true;

            /**
             * 返回分片的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             * @param seq 该分片所在语音流的序号 (0, 1, 2...)
             */
            @Override
            public void onSliceSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {

                if (dontHaveResult && !TextUtils.isEmpty(result.getText())) {
                    dontHaveResult = false;
                    Date date=new Date();
                    DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                    String time=format.format(date);
                    String message = String.format("voice flow order = %d, receive first response in %s, result is = %s", seq, time, result.getText());
                    Log.i(PERFORMANCE_TAG, message);
                }

                AAILogger.info(logger, "分片on slice success..");
                AAILogger.info(logger, "分片slice seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
                resMap.put(String.valueOf(seq), result.getText());
                final String msg = buildMessage(resMap);
                AAILogger.info(logger, "分片slice msg="+msg);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeResult.setText(msg);
                    }
                });

            }

            /**
             * 返回语音流的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             * @param seq 该语音流的序号 (1, 2, 3...)
             */
            @Override
            public void onSegmentSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
                dontHaveResult = true;
                AAILogger.info(logger, "语音流on segment success");
                AAILogger.info(logger, "语音流segment seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
                resMap.put(String.valueOf(seq), result.getText());
                final String msg = buildMessage(resMap);
                AAILogger.info(logger, "语音流segment msg="+msg);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeResult.setText(msg);
                    }
                });
            }

            /**
             * 识别结束回调，返回所有的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             */
            @Override
            public void onSuccess(AudioRecognizeRequest request, String result) {
                AAILogger.info(logger, "识别结束, onSuccess..");
                AAILogger.info(logger, "识别结束, result = {}", result);
            }

            /**
             * 识别失败
             * @param request 相应的请求
             * @param clientException 客户端异常
             * @param serverException 服务端异常
             */
            @Override
            public void onFailure(AudioRecognizeRequest request, final ClientException clientException, final ServerException serverException) {
                if (clientException!=null) {
                    AAILogger.info(logger, "onFailure..:"+clientException.toString());
                }
                if (serverException!=null) {
                    AAILogger.info(logger, "onFailure..:"+serverException.toString());
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (clientException!=null) {
                            recognizeState.setText("识别状态：失败,  "+clientException.toString());
                            AAILogger.info(logger, "识别状态：失败,  "+clientException.toString());
                        } else if (serverException!=null) {
                            recognizeState.setText("识别状态：失败,  "+serverException.toString());
                        }
                    }
                });
            }
        };

        /**
         * 识别状态监听器
         */
        final AudioRecognizeStateListener audioRecognizeStateListener = new AudioRecognizeStateListener() {
            DataOutputStream dataOutputStream;
            String fileName = null;
            String filePath = null;
            ExecutorService mExecutorService;
            /**
             * 开始录音
             * @param request
             */
            @Override
            public void onStartRecord(AudioRecognizeRequest request) {
                currentRequestId = request.getRequestId();
                AAILogger.info(logger, "onStartRecord..");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_record));
                        recognizeResult.setText("");
                    }
                });
                //为本次录音创建缓存一个文件
                if(isSaveAudioRecordFiles) {
                    if(mExecutorService == null){
                        mExecutorService = Executors.newSingleThreadExecutor();
                    }
                    filePath = Environment.getExternalStorageDirectory().toString() + "/tencent_audio_sdk_cache";
                    fileName = System.currentTimeMillis() + ".pcm";
                    dataOutputStream = WavCache.creatPmcFileByPath(filePath, fileName);
                }
            }

            /**
             * 结束录音
             * @param request
             */
            @Override
            public void onStopRecord(AudioRecognizeRequest request) {
                AAILogger.info(logger, "onStopRecord..");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_record));
                        // start.setEnabled(true);
                    }
                });
                if(isSaveAudioRecordFiles) {
                    mExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            WavCache.closeDataOutputStream(dataOutputStream);
                            WavCache.makePCMFileToWAVFile(filePath, fileName);
                        }
                    });

                }
            }

            /**
             * 返回音频流，
             * 用于返回宿主层做录音缓存业务。
             * 由于方法跑在sdk线程上，这里多用于文件操作，宿主需要新开一条线程专门用于实现业务逻辑
             * @param audioDatas
             */
            @Override
            public void onNextAudioData(final short[] audioDatas, final int readBufferLength) {
                if(isSaveAudioRecordFiles) {
                    mExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            WavCache.savePcmData(dataOutputStream, audioDatas, readBufferLength);
                        }
                    });
                }
            }

            /**
             * 第seq个语音流开始识别
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowStartRecognize(AudioRecognizeRequest request, int seq) {
                AAILogger.info(logger, "onVoiceFlowStartRecognize.. seq = {}", seq);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_recognize));
                    }
                });
            }

            /**
             * 第seq个语音流结束识别
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowFinishRecognize(AudioRecognizeRequest request, int seq) {

                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, recognize finish in %s", seq, time);
                Log.i(PERFORMANCE_TAG, message);

                AAILogger.info(logger, "onVoiceFlowFinishRecognize.. seq = {}", seq);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_recognize));

                    }
                });
            }

            /**
             * 第seq个语音流开始
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowStart(AudioRecognizeRequest request, int seq) {

                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, start in %s", seq, time);
                Log.i(PERFORMANCE_TAG, message);

                AAILogger.info(logger, "onVoiceFlowStart.. seq = {}", seq);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_voice));
                    }
                });
            }

            /**
             * 第seq个语音流结束
             * @param request
             * @param seq
             */
            @Override
            public void onVoiceFlowFinish(AudioRecognizeRequest request, int seq) {

                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, stop in %s", seq, time);
                Log.i(PERFORMANCE_TAG, message);

                AAILogger.info(logger, "onVoiceFlowFinish.. seq = {}", seq);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_voice));
                    }
                });
            }

            /**
             * 语音音量回调
             * @param request
             * @param volume
             */
            @Override
            public void onVoiceVolume(AudioRecognizeRequest request, final int volume) {
                AAILogger.info(logger, "onVoiceVolume..");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
//                        MainActivity.this.volume.setText(getString(R.string.volume)+volume);
                    }
                });
            }

        };

        /**
         * 识别超时监听器
         */
        final AudioRecognizeTimeoutListener audioRecognizeTimeoutListener = new AudioRecognizeTimeoutListener() {

            /**
             * 检测第一个语音流超时
             * @param request
             */
            @Override
            public void onFirstVoiceFlowTimeout(AudioRecognizeRequest request) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.start_voice_timeout));
                    }
                });
            }

            /**
             * 检测下一个语音流超时
             * @param request
             */
            @Override
            public void onNextVoiceFlowTimeout(AudioRecognizeRequest request) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeState.setText(getString(R.string.end_voice_timeout));
                    }
                });
            }
        };

        if (aaiClient==null) {
            try {
//                        aaiClient = new AAIClient(MainActivity.this, appid, projectId, secretId, credentialProvider);
                //sdk crash 上传
                if (switchToDeviceAuth) {
//                    aaiClient = new AAIClient(MainActivity.this, Integer.valueOf(DemoConfig.appIdForDeviceAuth), projectId,
//                            DemoConfig.secretIdForDeviceAuth, DemoConfig.secretKeyForDeviceAuth,
//                            DemoConfig.serialNumForDeviceAuth, DemoConfig.deviceNumForDeviceAuth,
//                            credentialProvider, MainActivity.this);
                } else {
                    /**直接鉴权**/
                    aaiClient = new AAIClient(MainActivity.this, appid, projectId, secretId,secretKey ,credentialProvider);
                    /**使用临时密钥鉴权
                     * * 1.通过sts 获取到临时证书 （secretId secretKey  token） ,此步骤应在您的服务器端实现，见https://cloud.tencent.com/document/product/598/33416
                     *   2.通过临时密钥调用接口
                     * **/
                    // aaiClient = new AAIClient(MainActivity.this, appid, projectId,"临时secretId", "临时secretKey","对应的token" ,credentialProvider);
                }
            } catch (ClientException e) {
                e.printStackTrace();
                AAILogger.info(logger, e.toString());
            }
        }


        // 语音合成 1.创建SpeechSynthesizer对象, 第二个参数：本地合成时传InitListener
        mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this,
                mTtsInitListener);

        btn_CallActivity = findViewById(R.id.btn_CallActivity);
        btn_CallActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                texts = "跳转至语音拨打，请输入需要拨打的手机号码";
//                mTts.startSpeaking(texts, mSynListener);
                Intent intentActivityCall = new Intent(MainActivity.this, CallActivity.class);
                startActivity(intentActivityCall);
            }
        });

        btn_SendActivity = findViewById(R.id.btn_SendActivity);
        btn_SendActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                texts = "跳转至语音短信，请先输入电话号码";
//                mTts.startSpeaking(texts, mSynListener);
                Intent intentActivitySend = new Intent(MainActivity.this, SendActivity.class);
                startActivity(intentActivitySend);
            }
        });

        btn_TimeDate= findViewById(R.id.btn_TimeDate);
        btn_TimeDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TimeDate td = null;
                try {
                    td = new TimeDate();
                    String MYear = td.getMYear();
                    Log.d("Date","Myear:" + MYear);
                    String Month = td.getMonth();
                    Log.d("Date","Month:" + Month);
                    String MDay = td.getMDay();
                    Log.d("Date","MDay:" + MDay);
                    String MWay = td.getMWay();
                    Log.d("Date","MWay:" + MWay);
                    String MHoure = td.getMHours();
                    Log.d("Date","MHoure:" + MHoure);
                    String MMinute = td.getMMinute();
                    Log.d("Date","MMinute:" + MMinute);
                    mTts.startSpeaking(MYear + "年" + Month + "月" + MDay + "日" + "星期" + MWay  + MHoure + "点" + MMinute + "分", mSynListener);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        btn_WeatherActivity = findViewById(R.id.btn_WeatherActivity);
        btn_WeatherActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                WeatherAPI wt = null;
                try {
                    wt = new WeatherAPI(MainActivity.this);

                    String province = wt.getProvince();
                    String city = wt.getCity();
                    String weathertaday = wt.gettadayWeather();
                    String temperaturetaday= wt.gettadayTemperature();
                    String winddirectiontaday = wt.gettadayWinddirection();
                    String windpowertaday = wt.gettadayWindpower();

                    String weathertomorrow = wt.gettomorrowWeather();
                    String temperaturetomorrow = wt.gettomorrowTemperature();
                    String winddirectiontomorrow = wt.gettomorrowWinddirection();
                    String windpowertomorrow = wt.gettomorrowWindpower();

                    String taday = "今日：";
                    String tomorrow = "明日天气为：";
                    String wait = "。";
                    String textweather = "天气为:";
                    String temperature = "温度为：";
                    String degree = "摄氏度";
                    String winddirection = "风向为：";
                    String windpower = "风力为：";
                    String windpowerstrength = "级";


                    mTts.startSpeaking(province + city + taday+wait+textweather+weathertaday+wait+temperature+temperaturetaday+degree+wait+winddirection+winddirectiontaday+wait+windpower+windpowertaday+windpowerstrength
                            +wait+tomorrow+weathertomorrow+wait+temperature+temperaturetomorrow+degree+wait+winddirection+winddirectiontomorrow+wait+windpower+windpowertomorrow+windpowerstrength, mSynListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        btn_Navigation = findViewById(R.id.btn_Navigation);
        btn_Navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentActivityNavigation = new Intent(MainActivity.this, NavigationActivity.class);
                texts = "跳转至地图定位";
//                mTts.startSpeaking(texts, mSynListener);
                startActivity(intentActivityNavigation);
            }
        });

        btn_Music = findViewById(R.id.btn_Music);
        btn_Music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,MusicActivity.class);
                texts = "跳转至音乐播放";
//                mTts.startSpeaking(texts, mSynListener);
                startActivity(intent);
            }
        });

        btn_Recognize = findViewById(R.id.btn_Recognize);
        btn_Recognize.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    mTts.stopSpeaking();
                    buttonDown(audioRecognizeResultlistener,audioRecognizeStateListener,audioRecognizeTimeoutListener);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    buttonUp();
                    try {
                        Thread.currentThread().sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String msg = buildMessage(resMap);
                    Log.e("ycm", "结果" + msg);
                    orderRead(msg);
                }

                return true;
            }
        });

    }

    private void buttonDown(AudioRecognizeResultListener audioRecognizeResultlistener,AudioRecognizeStateListener audioRecognizeStateListener,
                            AudioRecognizeTimeoutListener audioRecognizeTimeoutListener) {
        if (aaiClient!=null) {
            boolean taskExist = aaiClient.cancelAudioRecognize(currentRequestId);
            AAILogger.info(logger, "taskExist=" + taskExist);
        }

        AAILogger.info(logger, "the start button has clicked..");
        resMap.clear();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //   start.setEnabled(false);
            }
        });
        AudioRecognizeRequest.Builder builder = new AudioRecognizeRequest.Builder();
        isSaveAudioRecordFiles=false;//默认是关的 false
        // 初始化识别请求
        final AudioRecognizeRequest audioRecognizeRequest = builder
//                        .pcmAudioDataSource(new AudioRecordDataSource()) // 设置数据源
                .pcmAudioDataSource(new AudioRecordDataSource(isSaveAudioRecordFiles)) // 设置数据源
                //.templateName(templateName) // 设置模板
                .template(new AudioRecognizeTemplate(EngineModelType.EngineModelType16K.getType(),0)) // 设置自定义模板
                .setFilterDirty(0)  // 0 ：默认状态 不过滤脏话 1：过滤脏话
                .setFilterModal(0) // 0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
                .setFilterPunc(1) // 0 ：默认状态 不过滤句末的句号 1：滤句末的句号
                .setConvert_num_mode(1) //1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
//                        .setVadSilenceTime(1000) // 语音断句检测阈值，静音时长超过该阈值会被认为断句（多用在智能客服场景，需配合 needvad = 1 使用） 默认不传递该参数
                .setNeedvad(0) //0：关闭 vad，1：默认状态 开启 vad。
//                        .setHotWordId("")//热词 id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词 id 设置，自动生效默认热词；如果进行了单独的热词 id 设置，那么将生效单独设置的热词 id。
                .build();
        // 自定义识别配置
        final AudioRecognizeConfiguration audioRecognizeConfiguration = new AudioRecognizeConfiguration.Builder()
                .setSilentDetectTimeOut(true)// 是否使能静音检测，true表示不检查静音部分
                .audioFlowSilenceTimeOut(1000) // 静音检测超时停止录音
                .minAudioFlowSilenceTime(1000) // 语音流识别时的间隔时间
                .minVolumeCallbackTime(80) // 音量回调时间
                .build();
        //currentRequestId = audioRecognizeRequest.getRequestId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (switchToDeviceAuth) {
                    aaiClient.startAudioRecognizeForDevice(audioRecognizeRequest,
                            audioRecognizeResultlistener,
                            audioRecognizeStateListener,
                            audioRecognizeTimeoutListener,
                            audioRecognizeConfiguration);
                } else {
                    aaiClient.startAudioRecognize(audioRecognizeRequest,
                            audioRecognizeResultlistener,
                            audioRecognizeStateListener,
                            audioRecognizeTimeoutListener,
                            audioRecognizeConfiguration);
                }
            }
        }).start();
    }

    private void buttonUp() {
        AAILogger.info(logger, "stop button is clicked..");
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean taskExist = false;
                if (aaiClient!=null) {
                    taskExist = aaiClient.stopAudioRecognize(currentRequestId);
                }
                if (!taskExist) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            recognizeState.setText(getString(R.string.cant_stop));
                        }
                    });
                }
            }
        }).start();
    }




    /**
     * 语音合成监听
     */
    private SynthesizerListener mSynListener = new SynthesizerListener() {
        // 会话结束回调接口，没有错误时，error为null
        @SuppressLint("LongLogTag")
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


    //屏幕触摸事件，当触摸屏幕的时候触发命令识别
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int Action = event.getAction();
        if (Action==1){
            step(); //命令识别函数
        }
        Log.e("demo",Action+"hhhhhh"+"MainActivity-----------onTouchEvent--------------" + event.toString());
        return super.onTouchEvent(event);
    }

    private void step(){
        if (orderflag){
            texts = "请按住屏幕中下方说出命令，松开屏幕以停止录音";
            //starSpeech();
            mTts.startSpeaking(texts, mSynListener);
        }
    }

    public static String sHA1(Context context){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length()-1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void orderRead(String order) {
        String orderCallActivity = "打电话";
        String orderCallActivity2 = "打个电话";
        String orderCallActivity3 = "拨打电话";
        String orderSendActivity = "发短信";
        String orderSendActivity2 = "发送短信";
        String orderWeatherActivity="天气";
        String orderWeatherActivity2="天气预报";
        String orderTimeDateActivity="时间";
        String orderTimeDateActivity2="日期";
        String orderTimeDateActivity3="时间和日期";
        String orderNavigationActivity1 = "地图定位";
        String orderNavigationActivity2 = "定位";
        String orderMusicActivity1 = "播放音乐";
        String orderMusicActivity2 = "音乐";
        String result = Util.str2HexStr(order);

        if(result == null || result.length() <= 4) {
            texts = "未听清楚命令，请重新输入";
            mTts.startSpeaking(texts, mSynListener);
        }

        else {
            if( (orderCallActivity.equals(Util.hexStr2Str(result.substring(0,result.length()-4))))
                    || (  orderCallActivity2.equals(Util.hexStr2Str(result.substring(0,result.length()-4))) )
                    || ( orderCallActivity3.equals(Util.hexStr2Str(result.substring(0,result.length()-4))) ) ) {
                Intent intentCallActivity = new Intent(MainActivity.this, CallActivity.class);
                texts = "跳转至语音拨打，请输入需要拨打的手机号码";
//                mTts.startSpeaking(texts, mSynListener);
                startActivity(intentCallActivity);
            }

            else if(  (orderSendActivity.equals(Util.hexStr2Str(result.substring(0, result.length()-4))))
            || (orderSendActivity2.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) ) {
                Intent intentSendActivity = new Intent(MainActivity.this, SendActivity.class);
                startActivity(intentSendActivity);
                texts = "跳转至语音短信，请先输入电话号码";
//                mTts.startSpeaking(texts, mSynListener);
            }

            else if(  (orderWeatherActivity.equals(Util.hexStr2Str(result.substring(0, result.length()-4))))
                    || (orderWeatherActivity2.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) ) {

                WeatherAPI wt = null;
                try {
                    wt = new WeatherAPI(MainActivity.this);

                    String province = wt.getProvince();
                    String city = wt.getCity();
                    String weathertaday = wt.gettadayWeather();
                    String temperaturetaday= wt.gettadayTemperature();
                    String winddirectiontaday = wt.gettadayWinddirection();
                    String windpowertaday = wt.gettadayWindpower();

                    String weathertomorrow = wt.gettomorrowWeather();
                    String temperaturetomorrow = wt.gettomorrowTemperature();
                    String winddirectiontomorrow = wt.gettomorrowWinddirection();
                    String windpowertomorrow = wt.gettomorrowWindpower();

                    String taday = "今日：";
                    String tomorrow = "明日天气为：";
                    String wait = "。";
                    String textweather = "天气为:";
                    String temperature = "温度为：";
                    String degree = "摄氏度";
                    String winddirection = "风向为：";
                    String windpower = "风力为：";
                    String windpowerstrength = "级";


                    mTts.startSpeaking(province + city + taday+wait+textweather+weathertaday+wait+temperature+temperaturetaday+degree+wait+winddirection+winddirectiontaday+wait+windpower+windpowertaday+windpowerstrength
                            +wait+tomorrow+weathertomorrow+wait+temperature+temperaturetomorrow+degree+wait+winddirection+winddirectiontomorrow+wait+windpower+windpowertomorrow+windpowerstrength, mSynListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if((orderTimeDateActivity.equals(Util.hexStr2Str(result.substring(0,result.length()-4))))
                    || (orderTimeDateActivity2.equals(Util.hexStr2Str(result.substring(0,result.length()-4))))
                    || (orderTimeDateActivity3.equals(Util.hexStr2Str(result.substring(0,result.length()-4))))){

                TimeDate td = null;

                try {
                    td = new TimeDate();
                    String MYear = td.getMYear();
                    Log.d("Date","Myear:" + MYear);
                    String Month = td.getMonth();
                    Log.d("Date","Month:" + Month);
                    String MDay = td.getMDay();
                    Log.d("Date","MDay:" + MDay);
                    String MWay = td.getMWay();
                    Log.d("Date","MWay:" + MWay);
                    String MHoure = td.getMHours();
                    Log.d("Date","MHoure:" + MHoure);
                    String MMinute = td.getMMinute();
                    Log.d("Date","MMinute:" + MMinute);
                    mTts.startSpeaking(MYear + "年" + Month + "月" + MDay + "日" + "星期" + MWay  + MHoure + "点" + MMinute + "分", mSynListener);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            else if(orderNavigationActivity1.equals(Util.hexStr2Str(result.substring(0, result.length()-4))) ||
                    orderNavigationActivity2.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                Intent intentNavigationActivity = new Intent(MainActivity.this, NavigationActivity.class);
                startActivity(intentNavigationActivity);
                texts = "跳转至地图定位";
//                mTts.startSpeaking(texts, mSynListener);
            }

            else if(orderMusicActivity1.equals(Util.hexStr2Str(result.substring(0, result.length()-4))) ||
                    orderMusicActivity2.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                Intent intentMusicActivity = new Intent(MainActivity.this, MusicActivity.class);
                startActivity(intentMusicActivity);
                texts = "跳转至音乐播放";
//                mTts.startSpeaking(texts, mSynListener);
            }

            else {
                texts = "未听清楚命令，请重新输入";
                mTts.startSpeaking(texts, mSynListener);
            }
        }

    }

    @Override
    protected void onDestroy() {
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
        if (aaiClient != null) {
            aaiClient.release();
        }
        super.onDestroy();
    }



}
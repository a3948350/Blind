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
    //????????????????????????
    private boolean orderflag = true;
    // ??????????????????
    private SpeechSynthesizer mTts;
    // ???????????????
    private String voicer = "xiaoyan";
    //  ????????????????????????
    String texts = "";

    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    //  ????????????AAIClient??????
    AAIClient aaiClient;
    //  ????????????
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
        //????????????????????????
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

        // ??????sdk???????????????????????????
        checkPermissions();

        SHA1 = sHA1(MainActivity.this);
        Log.d("SHA1","SHA1:" + SHA1);


        /************************??????????????????************************/
        // ??????????????????????????????????????????????????????;
        final int appid = 1307538305;

        //??????ProjectId ?????????????????????0???????????????????????????????????????????????????????????????????????????????????????????????????????????? https://console.cloud.tencent.com/project
        final int projectId = 0;
        final String secretId = ycm.getSecretId();
        final String secretKey = ycm.getSecretKey();

        AAILogger.info(logger, "config : appid={}, projectId={}, secretId={}, secretKey={}", appid, projectId, secretId, secretKey);

        // ??????????????????sdk??????????????????????????????????????????????????????????????????secretKey????????????????????????????????????????????????
        // ??????????????????????????????CredentialProvider??????
        if (!switchToDeviceAuth) {
            credentialProvider = new LocalCredentialProvider(secretKey);
        } else {
//            credentialProvider = new LocalCredentialProvider(DemoConfig.secretKeyForDeviceAuth);
        }

        // ????????????
        ClientConfiguration.setMaxAudioRecognizeConcurrentNumber(1); // ???????????????????????????????????????
        ClientConfiguration.setMaxRecognizeSliceConcurrentNumber(1); // ????????????????????????????????????

        // ???????????????????????????
        final AudioRecognizeResultListener audioRecognizeResultlistener = new AudioRecognizeResultListener() {

            boolean dontHaveResult = true;

            /**
             * ???????????????????????????
             * @param request ???????????????
             * @param result ????????????
             * @param seq ????????????????????????????????? (0, 1, 2...)
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

                AAILogger.info(logger, "??????on slice success..");
                AAILogger.info(logger, "??????slice seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
                resMap.put(String.valueOf(seq), result.getText());
                final String msg = buildMessage(resMap);
                AAILogger.info(logger, "??????slice msg="+msg);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeResult.setText(msg);
                    }
                });

            }

            /**
             * ??????????????????????????????
             * @param request ???????????????
             * @param result ????????????
             * @param seq ????????????????????? (1, 2, 3...)
             */
            @Override
            public void onSegmentSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
                dontHaveResult = true;
                AAILogger.info(logger, "?????????on segment success");
                AAILogger.info(logger, "?????????segment seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
                resMap.put(String.valueOf(seq), result.getText());
                final String msg = buildMessage(resMap);
                AAILogger.info(logger, "?????????segment msg="+msg);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizeResult.setText(msg);
                    }
                });
            }

            /**
             * ????????????????????????????????????????????????
             * @param request ???????????????
             * @param result ????????????
             */
            @Override
            public void onSuccess(AudioRecognizeRequest request, String result) {
                AAILogger.info(logger, "????????????, onSuccess..");
                AAILogger.info(logger, "????????????, result = {}", result);
            }

            /**
             * ????????????
             * @param request ???????????????
             * @param clientException ???????????????
             * @param serverException ???????????????
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
                            recognizeState.setText("?????????????????????,  "+clientException.toString());
                            AAILogger.info(logger, "?????????????????????,  "+clientException.toString());
                        } else if (serverException!=null) {
                            recognizeState.setText("?????????????????????,  "+serverException.toString());
                        }
                    }
                });
            }
        };

        /**
         * ?????????????????????
         */
        final AudioRecognizeStateListener audioRecognizeStateListener = new AudioRecognizeStateListener() {
            DataOutputStream dataOutputStream;
            String fileName = null;
            String filePath = null;
            ExecutorService mExecutorService;
            /**
             * ????????????
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
                //???????????????????????????????????????
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
             * ????????????
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
             * ??????????????????
             * ?????????????????????????????????????????????
             * ??????????????????sdk??????????????????????????????????????????????????????????????????????????????????????????????????????
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
             * ???seq????????????????????????
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
             * ???seq????????????????????????
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
             * ???seq??????????????????
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
             * ???seq??????????????????
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
             * ??????????????????
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
         * ?????????????????????
         */
        final AudioRecognizeTimeoutListener audioRecognizeTimeoutListener = new AudioRecognizeTimeoutListener() {

            /**
             * ??????????????????????????????
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
             * ??????????????????????????????
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
                //sdk crash ??????
                if (switchToDeviceAuth) {
//                    aaiClient = new AAIClient(MainActivity.this, Integer.valueOf(DemoConfig.appIdForDeviceAuth), projectId,
//                            DemoConfig.secretIdForDeviceAuth, DemoConfig.secretKeyForDeviceAuth,
//                            DemoConfig.serialNumForDeviceAuth, DemoConfig.deviceNumForDeviceAuth,
//                            credentialProvider, MainActivity.this);
                } else {
                    /**????????????**/
                    aaiClient = new AAIClient(MainActivity.this, appid, projectId, secretId,secretKey ,credentialProvider);
                    /**????????????????????????
                     * * 1.??????sts ????????????????????? ???secretId secretKey  token??? ,?????????????????????????????????????????????https://cloud.tencent.com/document/product/598/33416
                     *   2.??????????????????????????????
                     * **/
                    // aaiClient = new AAIClient(MainActivity.this, appid, projectId,"??????secretId", "??????secretKey","?????????token" ,credentialProvider);
                }
            } catch (ClientException e) {
                e.printStackTrace();
                AAILogger.info(logger, e.toString());
            }
        }


        // ???????????? 1.??????SpeechSynthesizer??????, ????????????????????????????????????InitListener
        mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this,
                mTtsInitListener);

        btn_CallActivity = findViewById(R.id.btn_CallActivity);
        btn_CallActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                texts = "????????????????????????????????????????????????????????????";
//                mTts.startSpeaking(texts, mSynListener);
                Intent intentActivityCall = new Intent(MainActivity.this, CallActivity.class);
                startActivity(intentActivityCall);
            }
        });

        btn_SendActivity = findViewById(R.id.btn_SendActivity);
        btn_SendActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                texts = "????????????????????????????????????????????????";
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
                    mTts.startSpeaking(MYear + "???" + Month + "???" + MDay + "???" + "??????" + MWay  + MHoure + "???" + MMinute + "???", mSynListener);
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

                    String taday = "?????????";
                    String tomorrow = "??????????????????";
                    String wait = "???";
                    String textweather = "?????????:";
                    String temperature = "????????????";
                    String degree = "?????????";
                    String winddirection = "????????????";
                    String windpower = "????????????";
                    String windpowerstrength = "???";


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
                texts = "?????????????????????";
//                mTts.startSpeaking(texts, mSynListener);
                startActivity(intentActivityNavigation);
            }
        });

        btn_Music = findViewById(R.id.btn_Music);
        btn_Music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,MusicActivity.class);
                texts = "?????????????????????";
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
                    Log.e("ycm", "??????" + msg);
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
        isSaveAudioRecordFiles=false;//??????????????? false
        // ?????????????????????
        final AudioRecognizeRequest audioRecognizeRequest = builder
//                        .pcmAudioDataSource(new AudioRecordDataSource()) // ???????????????
                .pcmAudioDataSource(new AudioRecordDataSource(isSaveAudioRecordFiles)) // ???????????????
                //.templateName(templateName) // ????????????
                .template(new AudioRecognizeTemplate(EngineModelType.EngineModelType16K.getType(),0)) // ?????????????????????
                .setFilterDirty(0)  // 0 ??????????????? ??????????????? 1???????????????
                .setFilterModal(0) // 0 ??????????????? ??????????????????  1???????????????????????? 2:????????????
                .setFilterPunc(1) // 0 ??????????????? ???????????????????????? 1?????????????????????
                .setConvert_num_mode(1) //1??????????????? ?????????????????????????????????????????????0??????????????????????????????
//                        .setVadSilenceTime(1000) // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????? needvad = 1 ????????? ????????????????????????
                .setNeedvad(0) //0????????? vad???1??????????????? ?????? vad???
//                        .setHotWordId("")//?????? id??????????????????????????????????????????????????????????????????????????????????????????????????? id ?????????????????????????????????????????????????????????????????? id ????????????????????????????????????????????? id???
                .build();
        // ?????????????????????
        final AudioRecognizeConfiguration audioRecognizeConfiguration = new AudioRecognizeConfiguration.Builder()
                .setSilentDetectTimeOut(true)// ???????????????????????????true???????????????????????????
                .audioFlowSilenceTimeOut(1000) // ??????????????????????????????
                .minAudioFlowSilenceTime(1000) // ?????????????????????????????????
                .minVolumeCallbackTime(80) // ??????????????????
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
     * ??????????????????
     */
    private SynthesizerListener mSynListener = new SynthesizerListener() {
        // ?????????????????????????????????????????????error???null
        @SuppressLint("LongLogTag")
        public void onCompleted(SpeechError error) {
            if (error != null) {
                Log.d("mySynthesiezer complete code:", error.getErrorCode()
                        + "");
            } else {
                Log.d("mySynthesiezer complete code:", "0");
            }
        }

        // ??????????????????
        // percent???????????????0~100???beginPos??????????????????????????????????????????endPos?????????????????????????????????????????????info??????????????????
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
        }

        // ????????????
        public void onSpeakBegin() {
        }

        // ????????????
        public void onSpeakPaused() {
        }

        // ??????????????????
        // percent???????????????0~100,beginPos??????????????????????????????????????????endPos??????????????????????????????????????????.
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        // ????????????????????????
        public void onSpeakResumed() {
        }

        // ????????????????????????
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }
    };

    /**
     * ??????????????????????????????
     */
    private InitListener mTtsInitListener = new InitListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                // showTip("???????????????,????????????" + code);
                Toast.makeText(getApplicationContext(), "???????????????,????????????" + code,
                        Toast.LENGTH_SHORT).show();
            } else {
                // ????????????????????????????????????startSpeaking??????
                // ????????????????????????onCreate???????????????????????????????????????????????????startSpeaking???????????????
                // ?????????????????????onCreate??????startSpeaking??????????????????
            }
        }
    };


    //???????????????????????????????????????????????????????????????
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int Action = event.getAction();
        if (Action==1){
            step(); //??????????????????
        }
        Log.e("demo",Action+"hhhhhh"+"MainActivity-----------onTouchEvent--------------" + event.toString());
        return super.onTouchEvent(event);
    }

    private void step(){
        if (orderflag){
            texts = "??????????????????????????????????????????????????????????????????";
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
        String orderCallActivity = "?????????";
        String orderCallActivity2 = "????????????";
        String orderCallActivity3 = "????????????";
        String orderSendActivity = "?????????";
        String orderSendActivity2 = "????????????";
        String orderWeatherActivity="??????";
        String orderWeatherActivity2="????????????";
        String orderTimeDateActivity="??????";
        String orderTimeDateActivity2="??????";
        String orderTimeDateActivity3="???????????????";
        String orderNavigationActivity1 = "????????????";
        String orderNavigationActivity2 = "??????";
        String orderMusicActivity1 = "????????????";
        String orderMusicActivity2 = "??????";
        String result = Util.str2HexStr(order);

        if(result == null || result.length() <= 4) {
            texts = "????????????????????????????????????";
            mTts.startSpeaking(texts, mSynListener);
        }

        else {
            if( (orderCallActivity.equals(Util.hexStr2Str(result.substring(0,result.length()-4))))
                    || (  orderCallActivity2.equals(Util.hexStr2Str(result.substring(0,result.length()-4))) )
                    || ( orderCallActivity3.equals(Util.hexStr2Str(result.substring(0,result.length()-4))) ) ) {
                Intent intentCallActivity = new Intent(MainActivity.this, CallActivity.class);
                texts = "????????????????????????????????????????????????????????????";
//                mTts.startSpeaking(texts, mSynListener);
                startActivity(intentCallActivity);
            }

            else if(  (orderSendActivity.equals(Util.hexStr2Str(result.substring(0, result.length()-4))))
            || (orderSendActivity2.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) ) {
                Intent intentSendActivity = new Intent(MainActivity.this, SendActivity.class);
                startActivity(intentSendActivity);
                texts = "????????????????????????????????????????????????";
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

                    String taday = "?????????";
                    String tomorrow = "??????????????????";
                    String wait = "???";
                    String textweather = "?????????:";
                    String temperature = "????????????";
                    String degree = "?????????";
                    String winddirection = "????????????";
                    String windpower = "????????????";
                    String windpowerstrength = "???";


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
                    mTts.startSpeaking(MYear + "???" + Month + "???" + MDay + "???" + "??????" + MWay  + MHoure + "???" + MMinute + "???", mSynListener);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            else if(orderNavigationActivity1.equals(Util.hexStr2Str(result.substring(0, result.length()-4))) ||
                    orderNavigationActivity2.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                Intent intentNavigationActivity = new Intent(MainActivity.this, NavigationActivity.class);
                startActivity(intentNavigationActivity);
                texts = "?????????????????????";
//                mTts.startSpeaking(texts, mSynListener);
            }

            else if(orderMusicActivity1.equals(Util.hexStr2Str(result.substring(0, result.length()-4))) ||
                    orderMusicActivity2.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                Intent intentMusicActivity = new Intent(MainActivity.this, MusicActivity.class);
                startActivity(intentMusicActivity);
                texts = "?????????????????????";
//                mTts.startSpeaking(texts, mSynListener);
            }

            else {
                texts = "????????????????????????????????????";
                mTts.startSpeaking(texts, mSynListener);
            }
        }

    }

    @Override
    protected void onDestroy() {
        if (null != mTts) {
            mTts.stopSpeaking();
            // ?????????????????????
            mTts.destroy();
        }
        if (aaiClient != null) {
            aaiClient.release();
        }
        super.onDestroy();
    }



}
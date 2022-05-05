package com.example.blind;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SendActivity extends AppCompatActivity {

    private EditText et_SendNumber;
    private EditText et_SendMessage;
    private Button btn_SendChoose;
    private Button btn_SendSend;
    private Button btn_SendRecognize;

    private int step = 1;

    private static final int PICK_CONTACT = 1;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 2;
    private Intent mIntent;

    int currentRequestId = 0;

    Handler handler;

    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    //  语音识别AAIClient构建
    AAIClient aaiClient;
    //  证书鉴权
    AbsCredentialProvider credentialProvider;
    private static final Logger logger = LoggerFactory.getLogger(SendActivity.class);
    private boolean switchToDeviceAuth = false;
    boolean isSaveAudioRecordFiles=true;
    private final String PERFORMANCE_TAG = "PerformanceTag";

    private static String TAG = SendActivity.class.getSimpleName();
    //是否开始录音标记
    private boolean orderflag = true;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "xiaoyan";
    //  语音合成播报内容
    String texts = "";

    YCM ycm = new YCM();

    private void checkPermissions() {

        List<String> permissions = new LinkedList<>();

        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        addPermission(permissions, Manifest.permission.RECORD_AUDIO);
        addPermission(permissions, Manifest.permission.INTERNET);
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE);
        addPermission(permissions, Manifest.permission.SEND_SMS);
        addPermission(permissions, Manifest.permission.READ_SMS);
        addPermission(permissions, Manifest.permission.READ_CONTACTS);

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
        super.onCreate(savedInstanceState);
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=8f27a48e");
        setContentView(R.layout.activity_send);

        handler = new Handler(getMainLooper());

        // 检查sdk运行的必要条件权限
        checkPermissions();

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
//                        et_SendMessage.setText(msg);
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
//                        et_SendMessage.setText(msg);
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
                            //recognizeState.setText("识别状态：失败,  "+clientException.toString());
                            AAILogger.info(logger, "识别状态：失败,  "+clientException.toString());
                        } else if (serverException!=null) {
                            //recognizeState.setText("识别状态：失败,  "+serverException.toString());
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
                        //recognizeState.setText(getString(R.string.start_record));
//                        et_SendMessage.setText("");
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
                        //recognizeState.setText(getString(R.string.end_record));
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
                        //recognizeState.setText(getString(R.string.start_recognize));
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
                        //recognizeState.setText(getString(R.string.end_recognize));

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
                        //recognizeState.setText(getString(R.string.start_voice));
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
                        //recognizeState.setText(getString(R.string.end_voice));
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
                        //recognizeState.setText(getString(R.string.start_voice_timeout));
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
                        //recognizeState.setText(getString(R.string.end_voice_timeout));
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
                    aaiClient = new AAIClient(SendActivity.this, appid, projectId, secretId,secretKey ,credentialProvider);
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
        mTts = SpeechSynthesizer.createSynthesizer(SendActivity.this,
                mTtsInitListener);
        mTts.startSpeaking("跳转至语音短信，请先输入电话号码", mSynListener);

        et_SendNumber = findViewById(R.id.editTextSendNumber);

        btn_SendChoose = findViewById(R.id.btn_SendChoose);
        btn_SendChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectConnection();
            }
        });

        et_SendMessage = findViewById(R.id.editTextSendMessage);
        btn_SendSend = findViewById(R.id.btn_SendSend);
        btn_SendSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*String message = et_SendMessage.getText().toString();//短信内容
                String phoneNo = et_SendNumber.getText().toString();//电话号码
                Log.i("Send SMS", "");
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.",
                            Toast.LENGTH_LONG).show();
                }*/
                if(ContextCompat.checkSelfPermission(SendActivity.this,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SendActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
                }

                else {
                    Send();
                }
            }
        });

        btn_SendRecognize = findViewById(R.id.btn_SendRecognize);
        btn_SendRecognize.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    mTts.stopSpeaking();
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

                else if(event.getAction() == MotionEvent.ACTION_UP) {
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
                                        //recognizeState.setText(getString(R.string.cant_stop));
                                    }
                                });
                            }
                        }
                    }).start();
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

    private void orderRead(String order) {
        String orderYes = "是";
        String orderNo = "否";
        String orderBack = "返回";
        String result = Util.str2HexStr(order);

        if(result == null || result.length() <= 4) {
            texts = "未听清楚命令，请重新输入";
            mTts.startSpeaking(texts, mSynListener);
        }

        else {
            if(step == 1) {
                texts = "发送短信号码是否为：";
                mTts.startSpeaking(texts+order+"或者返回主界面", mSynListener);
                Log.d("send","order:" + order);
                step = 2;
                et_SendNumber.setText(order);
            }

            else if(step == 2 && orderNo.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                texts = "请重新输入手机号码";
                mTts.startSpeaking(texts, mSynListener);
                step = 1;
                et_SendNumber.setText("");
            }

            else if(step == 2 && orderYes.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                texts = "请输入短信内容，或者返回主界面";
                mTts.startSpeaking(texts, mSynListener);
                step = 3;
            }

            else if (step == 2 && orderBack.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                Intent intentBack = new Intent(SendActivity.this, MainActivity.class);
                step = 1;
                texts = "已返回主界面";
                mTts.startSpeaking(texts, mSynListener);
                startActivity(intentBack);
            }

            else if(step == 3) {
//                texts = "短信内容是否为：";
//                mTts.startSpeaking(texts+et_SendMessage.getText().toString(), mSynListener);
//                step = 4;
                if (orderBack.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                    Intent intentBack = new Intent(SendActivity.this, MainActivity.class);
                    step = 1;
                    texts = "已返回主界面";
                    mTts.startSpeaking(texts, mSynListener);
                    startActivity(intentBack);
                }
                else {
                    texts = "短信内容是否为：";
                    mTts.startSpeaking(texts+order, mSynListener);
                    et_SendMessage.setText(order);
                    step = 4;
                }
            }

            else if(step == 4 && orderNo.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                texts = "请重新输入短信内容";
                mTts.startSpeaking(texts, mSynListener);
                step = 3;
            }

            else if(step == 4 && orderYes.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                if(ContextCompat.checkSelfPermission(SendActivity.this,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SendActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
                }
                else {
                    Send();
                    texts = "短信发送成功";
                    mTts.startSpeaking(texts, mSynListener);
                }
            }

            else if(step == 4 && orderBack.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
                Intent intentBack = new Intent(SendActivity.this, MainActivity.class);
                step = 1;
                texts = "已返回主界面";
                mTts.startSpeaking(texts, mSynListener);
                startActivity(intentBack);
            }

            else {
                texts = "未听清楚命令，请重新输入";
                mTts.startSpeaking(texts, mSynListener);
            }
        }

//        if(step == 1) {
//            texts = "发送短信号码是否为：";
//            mTts.startSpeaking(texts+et_SendMessage.getText().toString()+"或者返回主界面", mSynListener);
//            step = 2;
//        }
//
//        else if(step == 2 && orderNo.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
//            texts = "请重新输入手机号码";
//            mTts.startSpeaking(texts, mSynListener);
//            step = 1;
//        }
//
//        else if(step == 2 && orderYes.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
//            et_SendNumber.setText(et_SendMessage.getText());
//            texts = "请输入短信内容，或者返回主界面";
//            mTts.startSpeaking(texts, mSynListener);
//            step = 3;
//        }
//
//        else if (step == 2 && orderBack.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
//            Intent intentBack = new Intent(SendActivity.this, MainActivity.class);
//            step = 1;
//            texts = "已返回主界面";
//            mTts.startSpeaking(texts, mSynListener);
//            startActivity(intentBack);
//        }
//
//        else if(step == 3) {
//            texts = "短信内容是否为：";
//            mTts.startSpeaking(texts+et_SendMessage.getText().toString(), mSynListener);
//            step = 4;
//        }
//
//        else if(step == 4 && orderNo.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
//            texts = "请重新输入短信内容";
//            mTts.startSpeaking(texts, mSynListener);
//            step = 3;
//        }
//
//        else if(step == 4 && orderYes.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
//            if(ContextCompat.checkSelfPermission(SendActivity.this,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(SendActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
//            }
//
//            else {
//                Send();
//            }
//        }
//
//        else if(step == 4 && orderBack.equals(Util.hexStr2Str(result.substring(0, result.length()-4)))) {
//            Intent intentBack = new Intent(SendActivity.this, MainActivity.class);
//            step = 1;
//            texts = "已返回主界面";
//            mTts.startSpeaking(texts, mSynListener);
//            startActivity(intentBack);
//        }
//
//        else {
//            texts = "未听清楚命令，请重新输入";
//            mTts.startSpeaking(texts, mSynListener);
//        }

    }

    private void Send() {
        String message = et_SendMessage.getText().toString();//短信内容
        String phoneNo = et_SendNumber.getText().toString();//电话号码
        Log.i("Send SMS", "");
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "SMS faild, please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_CONTACT:
                mIntent = data;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    //申请授权，第一个参数为要申请用户授权的权限；第二个参数为requestCode 必须大于等于0，主要用于回调的时候检测，匹配特定的onRequestPermissionsResult。
                    //可以从方法名requestPermissions以及第二个参数看出，是支持一次性申请多个权限的，系统会通过对话框逐一询问用户是否授权。
                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);

                }else{
                    //如果该版本低于6.0，或者该权限已被授予，它则可以继续读取联系人。
                    getContacts(data);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户成功授予权限
                getContacts(mIntent);
            } else {
                Toast.makeText(this, "你拒绝了此应用对读取联系人权限的申请！", Toast.LENGTH_SHORT).show();
            }
        }

        if(requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Send();
            } else {
                Toast.makeText(this, "抱歉，没有该权限！", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void selectConnection() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    private void getContacts(Intent data) {
        if (data == null) {
            return;
        }

        Uri contactData = data.getData();
        if (contactData == null) {
            return;
        }
        String name = "";
        String phoneNumber = "";

        Uri contactUri = data.getData();
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            if (hasPhone.equalsIgnoreCase("1")) {
                hasPhone = "true";
            } else {
                hasPhone = "false";
            }
            if (Boolean.parseBoolean(hasPhone)) {
                Cursor phones = getContentResolver()
                        .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                        + " = " + id, null, null);
                while (phones.moveToNext()) {
                    phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
                phones.close();
            }
            cursor.close();

            et_SendNumber.setText(phoneNumber);
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
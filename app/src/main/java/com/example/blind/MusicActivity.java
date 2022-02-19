package com.example.blind;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MusicActivity extends AppCompatActivity {
    private String[] music_name;
    private String[] music_player;
    private ArrayList<Song> musics;
    private int num = 0;
    private int seq = 0;

    MediaPlayer m_music_player;
    Button btn_MusicRecognize = null;

    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private void checkPermissions() {

        List<String> permissions = new LinkedList<>();

        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        addPermission(permissions, Manifest.permission.READ_EXTERNAL_STORAGE);
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


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        checkPermissions();

        m_music_player = new MediaPlayer();

        initMusic();

        ListView listView = findViewById(R.id.music_list);
        ListBaseAdapter la = new ListBaseAdapter();
        listView.setAdapter(la);

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            Toast.makeText(MusicActivity.this,musics.get(i).getFileName(),Toast.LENGTH_SHORT).show();
            String path = musics.get(i).getFileUrl();
            try {
                if(m_music_player.isPlaying()){
                    m_music_player.stop();
                }
//                MyTextView name = findViewById(R.id.music_name);
//                name.setText(musics.get(i).getTitle());
//                TextView player = findViewById(R.id.music_player);
//                player.setText(musics.get(i).getSinger());
                m_music_player = new MediaPlayer();
                m_music_player.reset();
                m_music_player.setDataSource(path);
                m_music_player.prepare();
                m_music_player.start();

            }catch (Exception e){
                e.printStackTrace();
            }

        });

        String path = musics.get(seq).getFileUrl();
        m_music_player = new MediaPlayer();
        try {
            m_music_player.setDataSource(path);
            m_music_player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        GestureDetector gestureDetector = new GestureDetector(MusicActivity.this, new GestureDetector.SimpleOnGestureListener() {
            /**
             * 发生确定的单击时执行
             * @param e
             * @return
             */
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {//单击事件暂停开始
                Toast.makeText(MusicActivity.this,"这是单击事件", Toast.LENGTH_SHORT).show();
                try {
                    if(m_music_player.isPlaying()) {
                        m_music_player.pause();
                    }
                    else {
                        m_music_player.start();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                return super.onSingleTapConfirmed(e);
            }

            /**
             * 双击发生时的通知
             * @param e
             * @return
             */
            @Override
            public boolean onDoubleTap(MotionEvent e) {//双击事件下一首
                Toast.makeText(MusicActivity.this,"这是双击事件",Toast.LENGTH_SHORT).show();
                if(seq <0 || seq >= num-1) {
                    seq = 0;
                    m_music_player.stop();
                    m_music_player = new MediaPlayer();
                    m_music_player.reset();
                    String pa = musics.get(seq).getFileUrl();
                    try {
                        m_music_player.setDataSource(pa);
                        m_music_player.prepare();
                        m_music_player.start();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                else {
                    m_music_player.stop();
                    m_music_player = new MediaPlayer();
                    m_music_player.reset();
                    seq = seq + 1;
                    String pa = musics.get(seq).getFileUrl();
                    try {
                        m_music_player.setDataSource(pa);
                        m_music_player.prepare();
                        m_music_player.start();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                return super.onDoubleTap(e);
            }

            /**
             * 双击手势过程中发生的事件，包括按下、移动和抬起事件
             * @param e
             * @return
             */
            @Override
            public void onLongPress(MotionEvent e) {//长按返回
                m_music_player.stop();
                Intent intentBack = new Intent(MusicActivity.this, MainActivity.class);
                startActivity(intentBack);
            }
        });

        btn_MusicRecognize = findViewById(R.id.btn_MusicRecognize);
//        btn_MusicRecognize.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d("musicButton", "onClick: ");
//            }
//        });
        btn_MusicRecognize.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                return gestureDetector.onTouchEvent(event);

            }
        });



    }

    public void initMusic(){
        musics = new ArrayList<>();
        getAllSongs(MusicActivity.this);
        int count = musics.size();
        if(count > 0){
            music_name = new String[count];
            music_player = new String[count];
            for(int i = 0;i < count;i++) {
                music_name[i] = musics.get(i).getTitle();
                music_player[i] = musics.get(i).getSinger();
            }
        }
        else{
            music_name = new String[1];
            music_name[0] = "成都 - 赵雷";
            music_player= new String[1];
            music_player[0] = "赵雷";
        }
        num = musics.size();
        Log.d("music", "歌曲数量：" + num);
    }

    public void getAllSongs(Context context) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.IS_MUSIC,null, MediaStore.Audio.Media.IS_MUSIC);

        if (cursor != null) {
            Song song;
            while (cursor.moveToNext())
            {
                song = new Song();
                int i = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                song.setTitle(cursor.getString(i));
                i = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                song.setSinger(cursor.getString(i));
                i = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                song.setFileName(cursor.getString(i));
                i = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                song.setFileUrl(cursor.getString(i));
                Log.d(song.getFileName(),"2");
                Log.d(song.getFileName(), song.getFileUrl());
                musics.add(song);
            }
            cursor.close();
        }
    }

    class ListBaseAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return music_name.length;
        }

        @Override
        public Object getItem(int i) {
            return music_name[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View list_view = View.inflate(MusicActivity.this, R.layout.layout_list_view,null);
            TextView txt_music_name = list_view.findViewById(R.id.txt_music_name);
            TextView txt_music_player = list_view.findViewById(R.id.txt_music_player);
            txt_music_name.setText(music_name[i]);
            txt_music_player.setText(music_player[i]);
            return list_view;
        }
    }
}

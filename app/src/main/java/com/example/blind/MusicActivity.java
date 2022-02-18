package com.example.blind;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;

public class MusicActivity extends AppCompatActivity {
    private String[] music_name;
    private String[] music_player;
    private ArrayList<Song> musics;
    MediaPlayer m_music_player;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

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
                MyTextView name = findViewById(R.id.music_name);
                name.setText(musics.get(i).getTitle());
                TextView player = findViewById(R.id.music_player);
                player.setText(musics.get(i).getSinger());

                m_music_player = new MediaPlayer();
                m_music_player.setDataSource(path);
                m_music_player.prepare();
                m_music_player.start();
            }catch (Exception e){
                e.printStackTrace();
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
                song.setFileUrl(cursor.getString(1));
                Log.d(song.getFileName(),"2");
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

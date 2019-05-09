package com.example.dip.songm;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    //    ImageButton playBtn, prevBtn, nextBtn;
    private TextView mdTv1,mdTv2,mdTv3,elapsedTime,totalTime;
    private SeekBar seekBar;
    private ImageView songIcon;
    private  ImageButton playBtn,nextBtn, prevBtn;
    private String mCurrentArtUrl;
    MediaMetadataRetriever metaRetriver;
    byte[] art;
    private MediaPlayer mp;
    private Utilities utils;

    private double startTime = 0;
    private double finalTime = 0;
    private int forwardTime = 500;
    private int backwardTime =500;

    private int currentSongIndex = 0;

    private Handler mHandler = new Handler();

    private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        metaRetriver = new MediaMetadataRetriever();

        intiViews();

        seekBar.setOnSeekBarChangeListener(this); // Important
        mp.setOnCompletionListener(this); // Important

        songsList = getPlayList();
        playSong(0);

        nextBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int temp = (int) startTime;
                if ((temp + forwardTime)<= finalTime) {
                    startTime = startTime + forwardTime;
                    mp.seekTo((int) startTime);
                    Toast.makeText(getApplicationContext(), "You have Jumped forward 5 seconds", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show();

                }
                return false;
            }
        });
        prevBtn.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view) {
                int temp =(int)startTime;
                if((temp-backwardTime)>0){
                    startTime = startTime - backwardTime;
                    mp.seekTo((int) startTime);
                    Toast.makeText(getApplicationContext(),"You have Jumped backward 5 seconds",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Cannot jump backward 5 seconds",Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    private void intiViews() {
//        playBtn = (ImageButton) findViewById(R.id.btnPlay);
//        prevBtn = (ImageButton) findViewById(R.id.btnPrevious);
//        nextBtn = (ImageButton) findViewById(R.id.btnNext);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setClickable(false);

        mdTv1 = (TextView) findViewById(R.id.metadata_1);
        mdTv2 = (TextView) findViewById(R.id.metadata_2);
        mdTv3 = (TextView) findViewById(R.id.metadata_3);

        elapsedTime = (TextView) findViewById(R.id.elapsed_time);
        totalTime = (TextView) findViewById(R.id.total_time);


        playBtn = (ImageButton) findViewById(R.id.btnPlay);
        nextBtn = (ImageButton) findViewById(R.id.btnNext);
        prevBtn = (ImageButton) findViewById(R.id.btnPrevious);

        mp = new MediaPlayer();
        utils = new Utilities();

    }



    public void playPause(View view) {
        if(mp.isPlaying()){
            if(mp!=null){
                mp.pause();
                // Changing button image to play button
                playBtn.setImageResource(R.drawable.play_button);
            }
        }else{
            // Resume song
            if(mp!=null){
                mp.start();
                // Changing button image to pause button
                playBtn.setImageResource(R.drawable.pause);
            }
        }
    }

    public void playPrevious(View view) {
        if(currentSongIndex > 0){
            playSong(currentSongIndex - 1);
            currentSongIndex = currentSongIndex - 1;
        }else{
            // play last song
            playSong(songsList.size() - 1);
            currentSongIndex = songsList.size() - 1;
        }
    }

    public void playNext(View view) {

        if(currentSongIndex < (songsList.size() - 1)){
            playSong(currentSongIndex + 1);
            currentSongIndex = currentSongIndex + 1;
        }else{
            // play first song
            playSong(0);
            currentSongIndex = 0;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(currentSongIndex < (songsList.size() - 1)){
            playSong(currentSongIndex + 1);
            currentSongIndex = currentSongIndex + 1;
        }else{
            // play first song
            playSong(0);
            currentSongIndex = 0;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mp.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mp.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 100){
            currentSongIndex = data.getExtras().getInt("songIndex");
            // play selected song
            playSong(currentSongIndex);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mp.release();
    }

    public void  playSong(int songIndex){
        // Play song
        try {
            mp.reset();
            mp.setDataSource(songsList.get(songIndex).get("songPath"));
            mp.prepare();
            mp.start();
            // Displaying Song title
            String songTitle = songsList.get(songIndex).get("songTitle");
            String songArtist =songsList.get(songIndex).get("songArtist");
            String songAlbum =songsList.get(songIndex).get("songAlbum");

            mdTv1.setText(songTitle);
            mdTv2.setText(songArtist);
            mdTv3.setText(songAlbum);
           /* art = metaRetriver.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory .decodeByteArray(art, 0, art.length);
            songIcon.setImageBitmap(songImage);
*/


            // Changing Button Image to pause image
            playBtn.setImageResource(R.drawable.pause);

            // set Progress bar values
            seekBar.setProgress(0);
            seekBar.setMax(100);

            // Updating progress bar
            updateProgressBar();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
     /*   catch (Exception e) {
            songIcon.setBackgroundColor(Color.GRAY);
        }*/
    }



    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mp.getDuration();
            long currentDuration = mp.getCurrentPosition();

            // Displaying Total Duration time
            totalTime.setText(""+utils.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            elapsedTime.setText(""+utils.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            seekBar.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    public ArrayList<HashMap<String, String>> getPlayList(){

        final Cursor mCursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM}, null, null,
                "LOWER(" + MediaStore.Audio.Media.TITLE + ") ASC");

        int count = mCursor.getCount();

        if (mCursor.moveToFirst()) {
            do {
                HashMap<String, String> song = new HashMap<String, String>();
                song.put("songTitle", mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)));
                song.put("songArtist",mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                song.put("songAlbum",mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
                song.put("songPath", mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                songsList.add(song);
            } while (mCursor.moveToNext());
        }

        mCursor.close();

        return songsList;
    }

}

package com.project.hsteffano.ftune.Metronome;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.project.hsteffano.ftune.R;


public class Metro extends Fragment {

    private final short minBpm = 40;
    private final short maxBpm = 208;

    private short bpm = 100;
    private short noteValue = 4;
    private short beats = 4;
    private short volume;
    private short initialVolume;
    private double beatSound = 2440;
    private double sound = 6440;
    private AudioManager audio;
    private MetronomeAsyncTask metroTask;
    boolean isPressed=false;

    private Button plusButton;
    private Button minusButton;
    private Button startStop;
    private TextView currentBeat;

    private Handler mHandler;

    // have in mind that: http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
    // in this case we should be fine as no delayed messages are queued
    private Handler getHandler() {
        return new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = (String)msg.obj;
                if(message.equals("1"))
                    currentBeat.setTextColor(getResources().getColor(R.color.iconOrange));
                else
                    currentBeat.setTextColor(getResources().getColor(R.color.white));
                currentBeat.setText(message);
            }
        };
    }

    public static Metro newInstance(String text) {

        Metro f = new Metro();
        Bundle b = new Bundle();
        b.putString("msg", text);

        f.setArguments(b);

        return f;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_metronome, container, false);
        metroTask = new MetronomeAsyncTask();
        /* Set values and listeners to buttons and stuff */

        TextView bpmText = (TextView) v.findViewById(R.id.bpm);
        bpmText.setText(""+bpm);

        startStop = (Button) v.findViewById(R.id.startstop);

        plusButton = (Button) v.findViewById(R.id.plus);
        plusButton.setOnLongClickListener(plusListener);

        minusButton = (Button) v.findViewById(R.id.minus);
        minusButton.setOnLongClickListener(minusListener);

        currentBeat = (TextView) v.findViewById(R.id.currentBeat);
        currentBeat.setTextColor(getResources().getColor(R.color.iconOrange));

        audio = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        initialVolume = (short) audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume = initialVolume;

        SeekBar volumebar = (SeekBar) v.findViewById(R.id.volumebar);
        volumebar.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumebar.setProgress(volume);
        volumebar.setOnSeekBarChangeListener(volumeListener);

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlusClick(getView());
            }
        });
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMinusClick(getView());
            }
        });
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartStopClick(getView());
            }
        });

        return v;
    }

    public synchronized void onStartStopClick(View view) {
        if(isPressed==false) {
            isPressed=true;
            metroTask.execute();
        } else {
            isPressed=false;
            metroTask.stop();
            metroTask = new MetronomeAsyncTask();
        }
    }

    private void maxBpmGuard() {
        if(bpm >= maxBpm) {
            plusButton.setEnabled(false);
            plusButton.setPressed(false);
        } else if(!minusButton.isEnabled() && bpm>minBpm) {
            minusButton.setEnabled(true);
        }
    }

    public void onPlusClick(View view) {
        bpm++;
        TextView bpmText = (TextView) view.findViewById(R.id.bpm);
        bpmText.setText(""+bpm);
        metroTask.setBpm(bpm);
        maxBpmGuard();
    }

    private OnLongClickListener plusListener = new OnLongClickListener() {


        @Override
        public boolean onLongClick(View v) {
            // TODO Auto-generated method stub
            bpm+=20;
            if(bpm >= maxBpm)
                bpm = maxBpm;
            TextView bpmText = (TextView) v.findViewById(R.id.bpm);
            bpmText.setText(""+bpm);
            metroTask.setBpm(bpm);
            maxBpmGuard();
            return true;
        }

    };

    private void minBpmGuard() {
        if(bpm <= minBpm) {
            minusButton.setEnabled(false);
            minusButton.setPressed(false);
        } else if(!plusButton.isEnabled() && bpm<maxBpm) {
            plusButton.setEnabled(true);
        }
    }

    public void onMinusClick(View view) {
        bpm--;
        TextView bpmText = (TextView) view.findViewById(R.id.bpm);
        bpmText.setText(""+bpm);
        metroTask.setBpm(bpm);
        minBpmGuard();
    }

    private OnLongClickListener minusListener = new OnLongClickListener() {


        @Override
        public boolean onLongClick(View v) {
            // TODO Auto-generated method stub
            bpm-=20;
            if(bpm <= minBpm)
                bpm = minBpm;
            TextView bpmText = (TextView) v.findViewById(R.id.bpm);
            bpmText.setText(""+bpm);
            metroTask.setBpm(bpm);
            minBpmGuard();
            return true;
        }

    };

    private OnSeekBarChangeListener volumeListener = new OnSeekBarChangeListener() {


        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            // TODO Auto-generated method stub
            volume = (short) progress;
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }


        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }


        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }

    };



    public boolean onKeyUp(int keycode, KeyEvent e) {
        SeekBar volumebar = (SeekBar) getActivity().findViewById(R.id.volumebar);
        volume = (short) audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        switch(keycode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                volumebar.setProgress(volume);
                break;
        }


        return getActivity().onKeyUp(keycode, e);
    }

    private class MetronomeAsyncTask extends AsyncTask<Void,Void,String> {
        Metronome metronome;

        MetronomeAsyncTask() {
            mHandler = getHandler();
            metronome = new Metronome(mHandler);
        }


        protected String doInBackground(Void... params) {
            metronome.setBeat(beats);
            metronome.setNoteValue(noteValue);
            metronome.setBpm(bpm);
            metronome.setBeatSound(beatSound);
            metronome.setSound(sound);


            metronome.play();

            return null;
        }

        public void stop() {
            metronome.stop();
            metronome = null;
        }

        public void setBpm(short bpm) {
            metronome.setBpm(bpm);
            metronome.calcSilence();
        }

        public void setBeat(short beat) {
            if(metronome != null)
                metronome.setBeat(beat);
        }

    }


}
package com.project.hsteffano.ftune;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class Tuner extends Fragment {

    int audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
    int channel = AudioFormat.CHANNEL_IN_MONO;
    int encoding = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, encoding);
    AudioRecord micData;
    AudioManager am;
    int sample;
    short[] data;
    TextView note;
    TextView freq;
    Button help;
    Button rec;
    private MediaRecorder recorder;
    String file;
    EditText fileName;
    boolean isRunning=false;
    boolean isTRunning=true;

    public final static float LOG2 = 0.6931472f;

    public static Tuner newInstance(String text) {

        Tuner f = new Tuner();
        Bundle b = new Bundle();
        b.putString("msg", text);

        f.setArguments(b);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_tuner, container, false);

        //final Recorder recorder = new Recorder();

        fileName = (EditText) v.findViewById(R.id.fileName);

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

        rec = (Button) v.findViewById(R.id.record);
        note = (TextView) v.findViewById(R.id.note);
        freq = (TextView) v.findViewById(R.id.freq);
        help = (Button) v.findViewById(R.id.help);

        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it=new Intent(help.getContext(), Help.class);
                startActivity(it);
            }
        });

        create();

        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (isTRunning) {
                        Thread.sleep(750);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                acquire();
                                getPitchInSampleRange();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();

        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                file=Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+fileName.getText().toString()+".3gp";
                recorder.setOutputFile(file);
                if (isRunning==true){
                    isTRunning=true;
                    recStop(view);
                    create();
                }else{
                    t.interrupt();
                    isTRunning=false;
                    micData.stop();
                    micData.release();
                    recStart(view);
                }
            }
        });

        return v;
    }


    public void recStart(View view){
        try {
            recorder.prepare();
            recorder.start();
        }catch(IllegalStateException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        isRunning=true;
        Toast.makeText(getActivity().getApplicationContext(), "recording...", Toast.LENGTH_LONG).show();
    }
    public void recStop(View view) {
        recorder.stop();
        recorder.release();
        recorder=null;
        isRunning=false;
        Toast.makeText(getActivity().getApplicationContext(),"Saved",Toast.LENGTH_LONG).show();
    }
    private void create() {
        micData = new AudioRecord(audioSource, sampleRate, channel, encoding, bufferSize);
        data = new short[bufferSize];
        am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        am.setParameters("noise_suppression=auto");
        if (NoiseSuppressor.isAvailable() == true) {
            NoiseSuppressor.create(micData.getAudioSessionId());
        }
    }

    private void acquire() {
        micData.startRecording();
        sample = micData.read(data, 0, bufferSize);
    }

    public void getPitchInSampleRange() /*throws Exception*/ {
        double[] bufferData = new double[bufferSize];
        final int bytesPerSample = 2;
        final double amplification = 100.0;
        for (int i = 0, floatI = 0; i < bufferSize - bytesPerSample + 1; i += bytesPerSample, floatI++) {
            double sample = 0;
            for (int b = 0; b < bytesPerSample; b++) {
                int v = data[i + b];
                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
                    v &= 0xFF;
                }
                sample += v << (b * 8);
            }
            double sample32 = amplification * (sample / 32768.0);
            bufferData[floatI] = sample32;
        }

        int nLowPeriodInSamples = sampleRate / 4500;
        int nHiPeriodInSamples = sampleRate / 20;

        double[] samples = bufferData;

        double[] results = new double[nHiPeriodInSamples - nLowPeriodInSamples];

        for (int period = nLowPeriodInSamples; period < nHiPeriodInSamples; period++) {
            double sum = 0;

            for (int i = 0; i < samples.length - period; i++) {
                sum += samples[i] * samples[i + period];
            }

            double mean = sum / (double) samples.length;

            results[period - nLowPeriodInSamples] = mean;
        }

        double fBestValue = Double.MIN_VALUE;
        int nBestIndex = -1;

        for (int i = 0; i < results.length; i++) {
            if (results[i] > fBestValue) {
                nBestIndex = i;
                fBestValue = results[i];
            }
        }
        double res = sampleRate / (nBestIndex + nLowPeriodInSamples);

        double midi = Math.max(0f, (float)Math.log(res / 440.0f) / LOG2 * 12f + 69f);

        Integer midiInt = Integer.valueOf((int) Math.round(midi));

        if (midiInt==0||midiInt==12||midiInt==24||midiInt==36||midiInt==48||midiInt==60||midiInt==72||midiInt==84||midiInt==96||midiInt==108||midiInt==120){
            note.setText("C");
        }
        if (midiInt==1||midiInt==13||midiInt==25||midiInt==37||midiInt==49||midiInt==61||midiInt==73||midiInt==85||midiInt==97||midiInt==109||midiInt==121){
            note.setText("C#");
        }
        if (midiInt==02||midiInt==14||midiInt==26||midiInt==38||midiInt==50||midiInt==62||midiInt==74||midiInt==86||midiInt==98||midiInt==110||midiInt==122){
            note.setText("D");
        }
        if (midiInt==3||midiInt==15||midiInt==27||midiInt==39||midiInt==51||midiInt==63||midiInt==75||midiInt==87||midiInt==99||midiInt==111||midiInt==123){
            note.setText("D#");
        }
        if (midiInt==4||midiInt==16||midiInt==28||midiInt==40||midiInt==52||midiInt==64||midiInt==76||midiInt==88||midiInt==100||midiInt==112||midiInt==124){
            note.setText("E");
        }
        if (midiInt==5||midiInt==17||midiInt==29||midiInt==41||midiInt==53||midiInt==65||midiInt==77||midiInt==89||midiInt==101||midiInt==113||midiInt==125){
            note.setText("F");
        }
        if (midiInt==6||midiInt==18||midiInt==30||midiInt==42||midiInt==54||midiInt==66||midiInt==78||midiInt==90||midiInt==102||midiInt==114||midiInt==126){
            note.setText("F#");
        }
        if (midiInt==7||midiInt==19||midiInt==31||midiInt==43||midiInt==55||midiInt==67||midiInt==79||midiInt==91||midiInt==103||midiInt==115||midiInt==127){
            note.setText("G");
        }
        if (midiInt==8||midiInt==20||midiInt==32||midiInt==44||midiInt==56||midiInt==68||midiInt==80||midiInt==92||midiInt==104||midiInt==116){
            note.setText("G#");
        }
        if (midiInt==9||midiInt==21||midiInt==33||midiInt==45||midiInt==57||midiInt==69||midiInt==81||midiInt==93||midiInt==105||midiInt==117){
            note.setText("A");
        }
        if (midiInt==10||midiInt==22||midiInt==34||midiInt==46||midiInt==58||midiInt==70||midiInt==82||midiInt==94||midiInt==106||midiInt==118){
            note.setText("A#");
        }
        if (midiInt==11||midiInt==23||midiInt==35||midiInt==47||midiInt==59||midiInt==71||midiInt==83||midiInt==95||midiInt==107||midiInt==119){
            note.setText("B");
        }

        if (res > 4500) {
            freq.setText("Play a note");
        } else {
            freq.setText("" + res + "Hz");
        }
    }

}


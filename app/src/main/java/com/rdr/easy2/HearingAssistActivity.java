package com.rdr.easy2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class HearingAssistActivity extends AppCompatActivity {
    private static final String SOUND_AMPLIFIER_PACKAGE =
            "com.google.android.accessibility.soundamplifier";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 3003;
    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.VOICE_PERFORMANCE,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
    };
    private static final int[] SAMPLE_RATES = new int[]{16000, 24000, 32000, 44100};

    private VolumeOverlayController volumeOverlayController;
    private AudioManager audioManager;
    private AudioDeviceCallback audioDeviceCallback;

    private View rootView;
    private TextView titleView;
    private TextView subtitleView;
    private View statusCardView;
    private TextView statusTitleView;
    private TextView statusDetailView;
    private TextView noteView;
    private TextView primaryButtonView;
    private TextView soundButtonView;
    private TextView jackButtonView;
    private TextView amplifierButtonView;

    private volatile boolean monitoringActive;
    private boolean pendingStartAfterPermission;
    private Thread audioThread;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hearing_assist);
        volumeOverlayController = new VolumeOverlayController(this);
        audioManager = getSystemService(AudioManager.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        bindViews();
        bindActions();
        applyThemePalette();
        refreshHearingUi();
        enableFullscreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerAudioDeviceCallback();
        applyThemePalette();
        refreshHearingUi();
    }

    @Override
    protected void onPause() {
        stopMonitoring(false);
        unregisterAudioDeviceCallback();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopMonitoring(false);
        unregisterAudioDeviceCallback();
        if (volumeOverlayController != null) {
            volumeOverlayController.release();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableFullscreen();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (volumeOverlayController != null && volumeOverlayController.handleKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return;
        }

        boolean granted =
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            pendingStartAfterPermission = false;
            refreshHearingUi();
            Toast.makeText(this, R.string.hearing_permission_denied, Toast.LENGTH_SHORT).show();
            return;
        }

        refreshHearingUi();
        if (pendingStartAfterPermission) {
            startMonitoring();
        }
    }

    private void bindViews() {
        rootView = findViewById(R.id.hearing_root);
        titleView = findViewById(R.id.hearing_title_view);
        subtitleView = findViewById(R.id.hearing_subtitle_view);
        statusCardView = findViewById(R.id.hearing_status_card);
        statusTitleView = findViewById(R.id.hearing_status_title);
        statusDetailView = findViewById(R.id.hearing_status_detail);
        noteView = findViewById(R.id.hearing_note_view);
        primaryButtonView = findViewById(R.id.hearing_amplifier_button);
        soundButtonView = findViewById(R.id.hearing_sound_button);
        jackButtonView = findViewById(R.id.hearing_devices_button);
        amplifierButtonView = findViewById(R.id.hearing_accessibility_button);
    }

    private void bindActions() {
        findViewById(R.id.close_hearing_button).setOnClickListener(view -> finish());
        primaryButtonView.setOnClickListener(view -> handlePrimaryAction());
        soundButtonView.setOnClickListener(
                view -> openExternal(new Intent(Settings.ACTION_SOUND_SETTINGS))
        );
        jackButtonView.setOnClickListener(
                view -> openExternal(new Intent(Settings.ACTION_SOUND_SETTINGS))
        );
        amplifierButtonView.setOnClickListener(view -> openSoundAmplifier());
    }

    private void applyThemePalette() {
        LauncherThemePalette palette = LauncherThemePalette.fromPreferences(this);

        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (subtitleView != null) {
            subtitleView.setTextColor(palette.getBodyTextColor());
        }
        if (statusTitleView != null) {
            statusTitleView.setTextColor(palette.getHeadingColor());
        }
        if (statusDetailView != null) {
            statusDetailView.setTextColor(palette.getBodyTextColor());
        }
        if (noteView != null) {
            noteView.setTextColor(palette.getBodyTextColor());
        }
        if (statusCardView != null) {
            int strokeColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                    "hearing_status_card",
                    palette.getPrimaryColor(),
                    palette
            );
            int fillColor = blend(strokeColor, palette.getBackgroundColor(), 0.84f);
            statusCardView.setBackground(ColorblindStyleHelper.createRoundedBackground(
                    this,
                    fillColor,
                    strokeColor,
                    30,
                    ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 2
            ));
        }

        styleActionButton(
                primaryButtonView,
                "hearing_start",
                palette.getPrimaryColor(),
                palette
        );
        styleActionButton(
                soundButtonView,
                "hearing_sound",
                palette.getChipColor(),
                palette
        );
        styleActionButton(
                jackButtonView,
                "hearing_jack",
                palette.getCircleColor(),
                palette
        );
        styleActionButton(
                amplifierButtonView,
                "hearing_system_amplifier",
                blend(palette.getPrimaryColor(), palette.getCircleColor(), 0.42f),
                palette
        );

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void styleActionButton(
            TextView button,
            String stableKey,
            int fallbackColor,
            LauncherThemePalette palette
    ) {
        if (button == null) {
            return;
        }

        int fillColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                stableKey,
                fallbackColor,
                palette
        );
        button.setTextColor(ColorblindStyleHelper.resolveTextColorForBackground(fillColor));
        button.setBackground(ColorblindStyleHelper.createRoundedBackground(
                this,
                fillColor,
                fillColor,
                26,
                ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 0
        ));
    }

    private void handlePrimaryAction() {
        if (monitoringActive) {
            stopMonitoring(true);
            return;
        }

        if (!hasWiredHeadphones()) {
            Toast.makeText(this, R.string.hearing_jack_missing, Toast.LENGTH_SHORT).show();
            refreshHearingUi();
            return;
        }

        if (!hasAudioPermission()) {
            pendingStartAfterPermission = true;
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION
            );
            return;
        }

        startMonitoring();
    }

    private void startMonitoring() {
        pendingStartAfterPermission = false;

        if (monitoringActive) {
            return;
        }
        AudioDeviceInfo wiredDevice = findWiredHeadphonesDevice();
        if (wiredDevice == null) {
            Toast.makeText(this, R.string.hearing_jack_missing, Toast.LENGTH_SHORT).show();
            refreshHearingUi();
            return;
        }

        AudioSession session = createAudioSession(wiredDevice);
        if (session == null) {
            Toast.makeText(this, R.string.hearing_start_error, Toast.LENGTH_SHORT).show();
            refreshHearingUi();
            return;
        }

        audioRecord = session.audioRecord;
        audioTrack = session.audioTrack;
        prepareJackRouting(wiredDevice);

        try {
            audioTrack.play();
            audioRecord.startRecording();
        } catch (Exception exception) {
            releaseAudioSession(audioRecord, audioTrack);
            audioRecord = null;
            audioTrack = null;
            resetAudioRouting();
            Toast.makeText(this, R.string.hearing_start_error, Toast.LENGTH_SHORT).show();
            refreshHearingUi();
            return;
        }

        monitoringActive = true;
        AudioRecord runningRecord = audioRecord;
        AudioTrack runningTrack = audioTrack;
        int bufferSize = session.bufferSize;
        audioThread = new Thread(
                () -> runMonitoringLoop(runningRecord, runningTrack, bufferSize),
                "hearing-assist-audio"
        );
        audioThread.start();
        refreshHearingUi();
    }

    private void runMonitoringLoop(
            AudioRecord runningRecord,
            AudioTrack runningTrack,
            int bufferSize
    ) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        byte[] buffer = new byte[bufferSize];

        try {
            while (monitoringActive && !Thread.currentThread().isInterrupted()) {
                int readSize = runningRecord.read(buffer, 0, buffer.length);
                if (readSize <= 0) {
                    continue;
                }

                int written = 0;
                while (written < readSize && monitoringActive) {
                    int chunkSize = runningTrack.write(buffer, written, readSize - written);
                    if (chunkSize <= 0) {
                        break;
                    }
                    written += chunkSize;
                }
            }
        } catch (Exception exception) {
            if (monitoringActive) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.hearing_start_error, Toast.LENGTH_SHORT).show();
                    stopMonitoring(false);
                });
            }
        }
    }

    private void stopMonitoring(boolean userInitiated) {
        pendingStartAfterPermission = false;
        if (!monitoringActive && audioThread == null && audioRecord == null && audioTrack == null) {
            refreshHearingUi();
            return;
        }

        monitoringActive = false;
        Thread runningThread = audioThread;
        audioThread = null;

        if (runningThread != null && runningThread != Thread.currentThread()) {
            runningThread.interrupt();
            try {
                runningThread.join(250);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        releaseAudioSession(audioRecord, audioTrack);
        audioRecord = null;
        audioTrack = null;
        resetAudioRouting();
        refreshHearingUi();

        if (userInitiated) {
            Toast.makeText(this, R.string.hearing_stopped, Toast.LENGTH_SHORT).show();
        }
    }

    private AudioSession createAudioSession(AudioDeviceInfo wiredDevice) {
        for (int audioSource : AUDIO_SOURCES) {
            for (int sampleRate : SAMPLE_RATES) {
                int recordMinBuffer = AudioRecord.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                );
                int trackMinBuffer = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                );

                if (recordMinBuffer <= 0 || trackMinBuffer <= 0) {
                    continue;
                }

                int bufferSize = Math.max(recordMinBuffer, trackMinBuffer);
                bufferSize = Math.max(bufferSize * 2, sampleRate / 5);

                AudioFormat inputFormat = new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setSampleRate(sampleRate)
                        .build();
                AudioFormat outputFormat = new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setSampleRate(sampleRate)
                        .build();

                AudioRecord candidateRecord = buildAudioRecord(audioSource, inputFormat, bufferSize);
                AudioTrack candidateTrack = buildAudioTrack(outputFormat, bufferSize);

                if (candidateRecord == null || candidateTrack == null) {
                    releaseAudioSession(candidateRecord, candidateTrack);
                    continue;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    candidateRecord.setPreferredDevice(wiredDevice);
                    candidateTrack.setPreferredDevice(wiredDevice);
                }

                if (candidateRecord.getState() != AudioRecord.STATE_INITIALIZED
                        || candidateTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                    releaseAudioSession(candidateRecord, candidateTrack);
                    continue;
                }

                return new AudioSession(candidateRecord, candidateTrack, bufferSize);
            }
        }

        return null;
    }

    private AudioRecord buildAudioRecord(int audioSource, AudioFormat audioFormat, int bufferSize) {
        try {
            return new AudioRecord.Builder()
                    .setAudioSource(audioSource)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private AudioTrack buildAudioTrack(AudioFormat audioFormat, int bufferSize) {
        try {
            return new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void releaseAudioSession(AudioRecord record, AudioTrack track) {
        if (record != null) {
            try {
                record.stop();
            } catch (Exception ignored) {
            }
            record.release();
        }

        if (track != null) {
            try {
                track.pause();
                track.flush();
            } catch (Exception ignored) {
            }
            track.release();
        }
    }

    private void refreshHearingUi() {
        if (primaryButtonView == null) {
            return;
        }

        boolean hasJack = hasWiredHeadphones();
        boolean hasPermission = hasAudioPermission();

        if (monitoringActive) {
            statusTitleView.setText(R.string.hearing_status_title_live);
            statusDetailView.setText(R.string.hearing_status_detail_live);
            primaryButtonView.setText(R.string.hearing_stop_now);
            primaryButtonView.setEnabled(true);
            primaryButtonView.setAlpha(1f);
        } else if (!hasJack) {
            statusTitleView.setText(R.string.hearing_status_title_waiting_jack);
            statusDetailView.setText(R.string.hearing_status_detail_waiting_jack);
            primaryButtonView.setText(R.string.hearing_waiting_for_jack);
            primaryButtonView.setEnabled(false);
            primaryButtonView.setAlpha(0.55f);
        } else if (!hasPermission) {
            statusTitleView.setText(R.string.hearing_status_title_permission);
            statusDetailView.setText(R.string.hearing_status_detail_permission);
            primaryButtonView.setText(R.string.hearing_start_permission);
            primaryButtonView.setEnabled(true);
            primaryButtonView.setAlpha(1f);
        } else {
            statusTitleView.setText(R.string.hearing_status_title_ready);
            statusDetailView.setText(R.string.hearing_status_detail_ready);
            primaryButtonView.setText(R.string.hearing_start_now);
            primaryButtonView.setEnabled(true);
            primaryButtonView.setAlpha(1f);
        }
    }

    private void registerAudioDeviceCallback() {
        if (audioManager == null || audioDeviceCallback != null) {
            return;
        }

        audioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                runOnUiThread(HearingAssistActivity.this::handleAudioDevicesChanged);
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                runOnUiThread(HearingAssistActivity.this::handleAudioDevicesChanged);
            }
        };
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
    }

    private void unregisterAudioDeviceCallback() {
        if (audioManager == null || audioDeviceCallback == null) {
            return;
        }
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        audioDeviceCallback = null;
    }

    private void handleAudioDevicesChanged() {
        if (monitoringActive && !hasWiredHeadphones()) {
            stopMonitoring(false);
            Toast.makeText(this, R.string.hearing_jack_removed, Toast.LENGTH_SHORT).show();
            return;
        }
        refreshHearingUi();
    }

    private boolean hasAudioPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasWiredHeadphones() {
        return findWiredHeadphonesDevice() != null;
    }

    private AudioDeviceInfo findWiredHeadphonesDevice() {
        if (audioManager == null) {
            return null;
        }

        AudioDeviceInfo[] outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo outputDevice : outputDevices) {
            if (isJackOutputType(outputDevice.getType())) {
                return outputDevice;
            }
        }
        return null;
    }

    private boolean isJackOutputType(int deviceType) {
        return deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET
                || deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                || deviceType == AudioDeviceInfo.TYPE_LINE_ANALOG
                || deviceType == AudioDeviceInfo.TYPE_AUX_LINE;
    }

    private void prepareJackRouting(AudioDeviceInfo wiredDevice) {
        if (audioManager == null || wiredDevice == null) {
            return;
        }

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.setCommunicationDevice(wiredDevice);
        }
    }

    private void resetAudioRouting() {
        if (audioManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice();
        }
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    private void openSoundAmplifier() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(SOUND_AMPLIFIER_PACKAGE);
        if (launchIntent != null && launchExternal(launchIntent)) {
            return;
        }

        openExternal(new Intent(Settings.ACTION_SOUND_SETTINGS));
    }

    private void openExternal(Intent intent) {
        if (launchExternal(intent)) {
            return;
        }

        showOpenError();
    }

    private boolean launchExternal(Intent intent) {
        try {
            if (intent.resolveActivity(getPackageManager()) == null) {
                return false;
            }
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showOpenError() {
        Toast.makeText(this, R.string.hearing_open_error, Toast.LENGTH_SHORT).show();
    }

    private void enableFullscreen() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller == null) {
            return;
        }

        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private int blend(int baseColor, int targetColor, float ratio) {
        float clampedRatio = Math.max(0f, Math.min(1f, ratio));
        float inverseRatio = 1f - clampedRatio;
        return Color.argb(
                Math.round((Color.alpha(baseColor) * inverseRatio)
                        + (Color.alpha(targetColor) * clampedRatio)),
                Math.round((Color.red(baseColor) * inverseRatio)
                        + (Color.red(targetColor) * clampedRatio)),
                Math.round((Color.green(baseColor) * inverseRatio)
                        + (Color.green(targetColor) * clampedRatio)),
                Math.round((Color.blue(baseColor) * inverseRatio)
                        + (Color.blue(targetColor) * clampedRatio))
        );
    }

    private static final class AudioSession {
        private final AudioRecord audioRecord;
        private final AudioTrack audioTrack;
        private final int bufferSize;

        private AudioSession(AudioRecord audioRecord, AudioTrack audioTrack, int bufferSize) {
            this.audioRecord = audioRecord;
            this.audioTrack = audioTrack;
            this.bufferSize = bufferSize;
        }
    }
}

package com.rdr.easy2;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

import java.util.HashMap;
import java.util.Map;

public class Easy2InCallService extends InCallService {
    private static Easy2InCallService instance;
    private static CallAudioState currentAudioState;

    private final Map<Call, Call.Callback> callCallbacks = new HashMap<>();

    public static Easy2InCallService getInstance() {
        return instance;
    }

    public static CallAudioState getCurrentAudioState() {
        return currentAudioState;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        instance = null;
        currentAudioState = null;
        callCallbacks.clear();
        super.onDestroy();
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        Call.Callback callback = new Call.Callback() {
            @Override
            public void onStateChanged(Call changedCall, int state) {
                CallRepository.setCurrentCall(changedCall);
            }

            @Override
            public void onDetailsChanged(Call changedCall, Call.Details details) {
                CallRepository.setCurrentCall(changedCall);
            }
        };

        call.registerCallback(callback);
        callCallbacks.put(call, callback);
        CallRepository.setCurrentCall(call);
        openCallScreen();
    }

    @Override
    public void onCallRemoved(Call call) {
        Call.Callback callback = callCallbacks.remove(call);
        if (callback != null) {
            call.unregisterCallback(callback);
        }
        CallRepository.clearCurrentCall(call);
        super.onCallRemoved(call);
    }

    @Override
    public void onBringToForeground(boolean showDialpad) {
        super.onBringToForeground(showDialpad);
        openCallScreen();
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);
        currentAudioState = audioState;
        Call currentCall = CallRepository.getCurrentCall();
        if (currentCall != null) {
            CallRepository.setCurrentCall(currentCall);
        }
    }

    private void openCallScreen() {
        Intent intent = CallActivity.createIntent(this);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );
        startActivity(intent);
    }
}

package com.rdr.easy2;

import android.telecom.Call;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CallRepository {
    public interface Listener {
        void onCallUpdated(Call call);

        void onCallCleared();
    }

    private static final Set<Listener> LISTENERS = new LinkedHashSet<>();
    private static Call currentCall;

    private CallRepository() {
    }

    public static synchronized void setCurrentCall(Call call) {
        currentCall = call;
        notifyCallUpdated(call);
    }

    public static synchronized void clearCurrentCall(Call call) {
        if (currentCall != call) {
            return;
        }
        currentCall = null;
        notifyCallCleared();
    }

    public static synchronized Call getCurrentCall() {
        return currentCall;
    }

    public static synchronized void registerListener(Listener listener) {
        if (listener == null) {
            return;
        }
        LISTENERS.add(listener);
        if (currentCall != null) {
            listener.onCallUpdated(currentCall);
        } else {
            listener.onCallCleared();
        }
    }

    public static synchronized void unregisterListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    private static void notifyCallUpdated(Call call) {
        for (Listener listener : LISTENERS) {
            listener.onCallUpdated(call);
        }
    }

    private static void notifyCallCleared() {
        for (Listener listener : LISTENERS) {
            listener.onCallCleared();
        }
    }
}

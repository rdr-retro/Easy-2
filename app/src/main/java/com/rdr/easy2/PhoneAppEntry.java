package com.rdr.easy2;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public final class PhoneAppEntry {
    private final String componentKey;
    private final String label;
    private final Drawable icon;
    private final Intent launchIntent;

    public PhoneAppEntry(String componentKey, String label, Drawable icon, Intent launchIntent) {
        this.componentKey = componentKey;
        this.label = label;
        this.icon = icon;
        this.launchIntent = launchIntent;
    }

    public String getComponentKey() {
        return componentKey;
    }

    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Intent getLaunchIntent() {
        return launchIntent;
    }
}

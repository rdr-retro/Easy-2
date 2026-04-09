package com.rdr.easy2;

public class PinnedContact {
    private final String lookupKey;
    private final String displayName;

    public PinnedContact(String lookupKey, String displayName) {
        this.lookupKey = lookupKey;
        this.displayName = displayName;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getDisplayName() {
        return displayName;
    }
}

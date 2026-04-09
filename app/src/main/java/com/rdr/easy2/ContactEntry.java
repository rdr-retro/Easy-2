package com.rdr.easy2;

import android.net.Uri;

public class ContactEntry {
    private final long id;
    private final String lookupKey;
    private final String displayName;
    private final Uri contactPhotoUri;
    private final boolean pinned;
    private Uri customPhotoUri;

    public ContactEntry(
            long id,
            String lookupKey,
            String displayName,
            Uri contactPhotoUri,
            Uri customPhotoUri,
            boolean pinned
    ) {
        this.id = id;
        this.lookupKey = lookupKey;
        this.displayName = displayName;
        this.contactPhotoUri = contactPhotoUri;
        this.customPhotoUri = customPhotoUri;
        this.pinned = pinned;
    }

    public long getId() {
        return id;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Uri getContactPhotoUri() {
        return contactPhotoUri;
    }

    public Uri getCustomPhotoUri() {
        return customPhotoUri;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setCustomPhotoUri(Uri customPhotoUri) {
        this.customPhotoUri = customPhotoUri;
    }
}

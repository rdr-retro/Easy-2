package com.rdr.easy2;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ContactQueryHelper {
    private ContactQueryHelper() {
    }

    public static List<ContactEntry> queryContacts(Context context) {
        List<ContactEntry> contacts = new ArrayList<>();
        if (context == null) {
            return contacts;
        }

        ContentResolver contentResolver = context.getContentResolver();
        Set<String> pinnedLookupKeys = new HashSet<>(
                LauncherPreferences.getPinnedContactLookupKeys(context)
        );

        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        ContactsContract.Contacts.PHOTO_URI
                },
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " IS NOT NULL",
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " COLLATE NOCASE ASC"
        );

        if (cursor == null) {
            return contacts;
        }

        try {
            int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            int lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
            int nameIndex = cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            );
            int photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String lookupKey = cursor.getString(lookupIndex);
                String displayName = cursor.getString(nameIndex);
                String photoUri = cursor.getString(photoIndex);

                if (TextUtils.isEmpty(lookupKey) || TextUtils.isEmpty(displayName)) {
                    continue;
                }

                contacts.add(new ContactEntry(
                        id,
                        lookupKey,
                        displayName,
                        !TextUtils.isEmpty(photoUri) ? Uri.parse(photoUri) : null,
                        ContactPhotoStorage.getSavedContactImageUri(context, lookupKey),
                        pinnedLookupKeys.contains(lookupKey)
                ));
            }
        } finally {
            cursor.close();
        }

        return sortContactsWithPinnedFirst(context, contacts);
    }

    private static List<ContactEntry> sortContactsWithPinnedFirst(
            Context context,
            List<ContactEntry> contacts
    ) {
        List<String> pinnedLookupKeys = LauncherPreferences.getPinnedContactLookupKeys(context);
        if (pinnedLookupKeys.isEmpty() || contacts.isEmpty()) {
            return contacts;
        }

        List<ContactEntry> sortedContacts = new ArrayList<>(contacts.size());
        List<ContactEntry> remainingContacts = new ArrayList<>(contacts);

        for (String pinnedLookupKey : pinnedLookupKeys) {
            for (int i = 0; i < remainingContacts.size(); i++) {
                ContactEntry contactEntry = remainingContacts.get(i);
                if (!pinnedLookupKey.equals(contactEntry.getLookupKey())) {
                    continue;
                }

                sortedContacts.add(contactEntry);
                remainingContacts.remove(i);
                break;
            }
        }

        sortedContacts.addAll(remainingContacts);
        return sortedContacts;
    }
}

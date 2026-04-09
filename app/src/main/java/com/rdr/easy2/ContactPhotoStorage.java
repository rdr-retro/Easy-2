package com.rdr.easy2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;

public final class ContactPhotoStorage {
    private static final String KEY_CONTACT_IMAGE_PREFIX = "contact_image_";
    private static final String CONTACT_PHOTOS_DIRECTORY = "contact_photos";
    private static final int MAX_SAVED_IMAGE_SIZE_PX = 640;
    private static final int DEFAULT_DISPLAY_IMAGE_SIZE_PX = 256;
    private static final int DISPLAY_BITMAP_CACHE_BYTES = 4 * 1024 * 1024;
    private static final LruCache<String, Bitmap> DISPLAY_BITMAP_CACHE =
            new LruCache<String, Bitmap>(DISPLAY_BITMAP_CACHE_BYTES) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value == null ? 0 : value.getByteCount();
                }
            };

    private ContactPhotoStorage() {
    }

    public static Uri savePickedImage(Context context, Uri sourceUri, String lookupKey)
            throws Exception {
        Uri savedImageUri = copyContactImageToInternalStorage(context, sourceUri, lookupKey);
        clearCachedBitmap(savedImageUri);
        saveContactImageUri(context, lookupKey, savedImageUri);
        return savedImageUri;
    }

    public static Uri getSavedContactImageUri(Context context, String lookupKey) {
        if (context == null || TextUtils.isEmpty(lookupKey)) {
            return null;
        }

        SharedPreferences preferences = LauncherPreferences.getPreferences(context);
        String storedUri = preferences.getString(KEY_CONTACT_IMAGE_PREFIX + lookupKey, null);
        if (TextUtils.isEmpty(storedUri)) {
            return null;
        }

        Uri savedUri = Uri.parse(storedUri);
        String path = savedUri.getPath();
        if (TextUtils.isEmpty(path) || !new File(path).exists()) {
            preferences.edit().remove(KEY_CONTACT_IMAGE_PREFIX + lookupKey).apply();
            return null;
        }

        return savedUri;
    }

    public static Bitmap loadDisplayBitmap(Context context, Uri imageUri, int targetSizePx) {
        if (context == null || imageUri == null) {
            return null;
        }

        int safeTargetSizePx = targetSizePx > 0 ? targetSizePx : DEFAULT_DISPLAY_IMAGE_SIZE_PX;
        String cacheKey = imageUri.toString() + "#" + safeTargetSizePx;
        Bitmap cachedBitmap = DISPLAY_BITMAP_CACHE.get(cacheKey);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            return cachedBitmap;
        }

        try {
            Bitmap decodedBitmap = decodeBitmap(context, imageUri, safeTargetSizePx);
            if (decodedBitmap != null) {
                DISPLAY_BITMAP_CACHE.put(cacheKey, decodedBitmap);
            }
            return decodedBitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Uri copyContactImageToInternalStorage(
            Context context,
            Uri sourceUri,
            String lookupKey
    ) throws Exception {
        File directory = new File(context.getFilesDir(), CONTACT_PHOTOS_DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create contacts photo directory");
        }

        String safeName = lookupKey.replaceAll("[^a-zA-Z0-9._-]", "_");
        File destinationFile = new File(directory, safeName + ".jpg");
        Bitmap normalizedBitmap = decodeBitmap(context, sourceUri, MAX_SAVED_IMAGE_SIZE_PX);
        if (normalizedBitmap == null) {
            throw new IllegalStateException("Could not decode selected image");
        }

        try (FileOutputStream outputStream = new FileOutputStream(destinationFile, false)) {
            boolean compressed = normalizedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    84,
                    outputStream
            );
            if (!compressed) {
                throw new IllegalStateException("Could not store selected image");
            }
            outputStream.flush();
        }

        return Uri.fromFile(destinationFile);
    }

    private static Bitmap decodeBitmap(Context context, Uri imageUri, int maxEdgePx)
            throws Exception {
        ImageDecoder.Source source = ImageDecoder.createSource(
                context.getContentResolver(),
                imageUri
        );
        return ImageDecoder.decodeBitmap(source, (decoder, imageInfo, imageDecoderSource) -> {
            int sourceWidth = imageInfo.getSize().getWidth();
            int sourceHeight = imageInfo.getSize().getHeight();
            int largestEdge = Math.max(sourceWidth, sourceHeight);

            if (largestEdge > maxEdgePx) {
                float scale = (float) maxEdgePx / (float) largestEdge;
                decoder.setTargetSize(
                        Math.max(1, Math.round(sourceWidth * scale)),
                        Math.max(1, Math.round(sourceHeight * scale))
                );
            }

            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            decoder.setMemorySizePolicy(ImageDecoder.MEMORY_POLICY_LOW_RAM);
        });
    }

    private static void clearCachedBitmap(Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        String cachePrefix = imageUri.toString() + "#";
        synchronized (DISPLAY_BITMAP_CACHE) {
            String[] snapshotKeys = DISPLAY_BITMAP_CACHE.snapshot().keySet().toArray(new String[0]);
            for (String cacheKey : snapshotKeys) {
                if (cacheKey.startsWith(cachePrefix)) {
                    DISPLAY_BITMAP_CACHE.remove(cacheKey);
                }
            }
        }
    }

    private static void saveContactImageUri(Context context, String lookupKey, Uri imageUri) {
        LauncherPreferences.getPreferences(context)
                .edit()
                .putString(KEY_CONTACT_IMAGE_PREFIX + lookupKey, imageUri.toString())
                .apply();
    }
}

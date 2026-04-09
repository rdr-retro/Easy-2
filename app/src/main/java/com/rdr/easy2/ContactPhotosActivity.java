package com.rdr.easy2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactPhotosActivity extends AppCompatActivity {
    private static final int REQUEST_CONTACTS_PERMISSION = 3201;

    private View rootView;
    private TextView titleView;
    private TextView subtitleView;
    private TextView emptyView;
    private TextView closeButton;
    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private ExecutorService backgroundExecutor;
    private VolumeOverlayController volumeOverlayController;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String pendingContactLookupKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_photos);

        rootView = findViewById(R.id.contact_photos_root);
        titleView = findViewById(R.id.contact_photos_title_view);
        subtitleView = findViewById(R.id.contact_photos_subtitle_view);
        emptyView = findViewById(R.id.contact_photos_empty);
        closeButton = findViewById(R.id.close_contact_photos_button);
        recyclerView = findViewById(R.id.contact_photos_recycler);
        backgroundExecutor = Executors.newSingleThreadExecutor();
        volumeOverlayController = new VolumeOverlayController(this);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handlePickedContactImageResult
        );

        adapter = new ContactsAdapter(getLayoutInflater(), this::onContactClicked, null);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        if (closeButton != null) {
            closeButton.setOnClickListener(view -> finish());
        }
        if (emptyView != null) {
            emptyView.setOnClickListener(view -> {
                if (hasContactsPermission()) {
                    loadContacts();
                } else {
                    requestContactsPermission();
                }
            });
        }

        applyThemePalette();
        showLoadingState();
        enableFullscreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyThemePalette();
        refreshContactsIfPossible();
    }

    @Override
    protected void onDestroy() {
        if (volumeOverlayController != null) {
            volumeOverlayController.release();
        }
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
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

    private void refreshContactsIfPossible() {
        if (hasContactsPermission()) {
            loadContacts();
        } else {
            showPermissionState();
        }
    }

    private void loadContacts() {
        showLoadingState();
        if (backgroundExecutor == null) {
            return;
        }

        backgroundExecutor.submit(() -> {
            List<ContactEntry> contacts = ContactQueryHelper.queryContacts(this);
            runOnUiThread(() -> showContacts(contacts));
        });
    }

    private void showLoadingState() {
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.contacts_loading);
        }
    }

    private void showPermissionState() {
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(getString(
                    R.string.contact_photos_permission_message,
                    getString(R.string.contact_photos_permission_needed),
                    getString(R.string.contact_photos_permission_detail)
            ));
        }
    }

    private void showContacts(List<ContactEntry> contacts) {
        if (adapter != null) {
            adapter.submitList(contacts);
        }

        boolean hasContacts = contacts != null && !contacts.isEmpty();
        if (recyclerView != null) {
            recyclerView.setVisibility(hasContacts ? View.VISIBLE : View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(hasContacts ? View.GONE : View.VISIBLE);
            if (!hasContacts) {
                emptyView.setText(R.string.contacts_empty);
            }
        }
    }

    private void onContactClicked(ContactEntry contactEntry) {
        if (contactEntry == null || TextUtils.isEmpty(contactEntry.getLookupKey())) {
            return;
        }

        pendingContactLookupKey = contactEntry.getLookupKey();
        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");
        pickImageIntent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(Intent.createChooser(
                pickImageIntent,
                getString(R.string.contacts_photo_picker_title)
        ));
    }

    private void handlePickedContactImageResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            pendingContactLookupKey = null;
            return;
        }

        handlePickedContactImage(result.getData().getData());
    }

    private void handlePickedContactImage(Uri imageUri) {
        if (imageUri == null || TextUtils.isEmpty(pendingContactLookupKey)) {
            pendingContactLookupKey = null;
            return;
        }

        try {
            Uri savedImageUri = ContactPhotoStorage.savePickedImage(
                    this,
                    imageUri,
                    pendingContactLookupKey
            );
            if (adapter != null) {
                adapter.updateContactPhoto(pendingContactLookupKey, savedImageUri);
            }
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.contacts_pick_photo_error, Toast.LENGTH_SHORT).show();
        }

        pendingContactLookupKey = null;
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
        if (emptyView != null) {
            emptyView.setTextColor(palette.getHeadingColor());
        }
        if (recyclerView != null) {
            recyclerView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (closeButton != null) {
            closeButton.setBackgroundTintList(ColorStateList.valueOf(
                    palette.isDarkMode() ? palette.getChipColor() : palette.getPrimaryColor()
            ));
        }
        if (adapter != null) {
            adapter.setThemePalette(palette);
        }
        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private boolean hasContactsPermission() {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactsPermission() {
        requestPermissions(
                new String[]{Manifest.permission.READ_CONTACTS},
                REQUEST_CONTACTS_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CONTACTS_PERMISSION) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContacts();
        } else {
            showPermissionState();
        }
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
}

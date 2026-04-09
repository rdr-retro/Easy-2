package com.rdr.easy2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhoneAppsActivity extends AppCompatActivity {
    private View rootView;
    private TextView titleView;
    private TextView subtitleView;
    private TextView emptyView;
    private TextView closeButton;
    private RecyclerView recyclerView;
    private PhoneAppsAdapter adapter;
    private ExecutorService backgroundExecutor;
    private VolumeOverlayController volumeOverlayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_apps);

        rootView = findViewById(R.id.phone_apps_root);
        titleView = findViewById(R.id.phone_apps_title_view);
        subtitleView = findViewById(R.id.phone_apps_subtitle_view);
        emptyView = findViewById(R.id.phone_apps_empty);
        closeButton = findViewById(R.id.close_phone_apps_button);
        recyclerView = findViewById(R.id.phone_apps_recycler);
        backgroundExecutor = Executors.newSingleThreadExecutor();
        volumeOverlayController = new VolumeOverlayController(this);

        adapter = new PhoneAppsAdapter(getLayoutInflater(), this::launchPhoneApp, null);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        if (closeButton != null) {
            closeButton.setOnClickListener(view -> finish());
        }

        applyThemePalette();
        showLoadingState();
        enableFullscreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhoneApps();
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

    private void loadPhoneApps() {
        showLoadingState();
        if (backgroundExecutor == null) {
            return;
        }

        backgroundExecutor.submit(() -> {
            List<PhoneAppEntry> phoneApps = queryLaunchableApps();
            runOnUiThread(() -> showPhoneApps(phoneApps));
        });
    }

    private List<PhoneAppEntry> queryLaunchableApps() {
        PackageManager packageManager = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolveInfos = packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0)
            );
        } else {
            resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0);
        }

        List<PhoneAppEntry> phoneApps = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null) {
                continue;
            }

            String packageName = resolveInfo.activityInfo.packageName;
            if (getPackageName().equals(packageName)) {
                continue;
            }

            ComponentName componentName =
                    new ComponentName(packageName, resolveInfo.activityInfo.name);
            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setComponent(componentName);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            CharSequence label = resolveInfo.loadLabel(packageManager);
            phoneApps.add(new PhoneAppEntry(
                    componentName.flattenToString(),
                    label != null ? label.toString() : packageName,
                    resolveInfo.loadIcon(packageManager),
                    launchIntent
            ));
        }

        Collections.sort(
                phoneApps,
                Comparator.comparing(phoneApp ->
                        phoneApp.getLabel().toLowerCase(Locale.getDefault()))
        );
        return phoneApps;
    }

    private void launchPhoneApp(PhoneAppEntry phoneAppEntry) {
        try {
            startActivity(phoneAppEntry.getLaunchIntent());
        } catch (Exception exception) {
            Toast.makeText(this, R.string.shortcut_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadingState() {
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.phone_apps_loading);
        }
    }

    private void showPhoneApps(List<PhoneAppEntry> phoneApps) {
        if (adapter != null) {
            adapter.submitList(phoneApps);
        }

        boolean hasApps = phoneApps != null && !phoneApps.isEmpty();
        if (recyclerView != null) {
            recyclerView.setVisibility(hasApps ? View.VISIBLE : View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(hasApps ? View.GONE : View.VISIBLE);
            if (!hasApps) {
                emptyView.setText(R.string.phone_apps_empty);
            }
        }
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

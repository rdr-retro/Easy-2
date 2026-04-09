package com.rdr.easy2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.res.ColorStateList;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.app.KeyguardManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_WAKE_FROM_PICKUP = "extra_wake_from_pickup";

    private static final int REQUEST_LOCATION_PERMISSION = 2001;
    private static final int REQUEST_CONTACTS_PERMISSION = 2002;
    private static final String KEY_CONTACT_IMAGE_PREFIX = "contact_image_";
    private static final String KEY_LAST_WEATHER_SUMMARY = "last_weather_summary";

    private TextView batteryTextView;
    private TextView chargingTextView;
    private TextView weatherLineTextView;
    private TextView contactsEmptyView;
    private TextView nameView;
    private TextView ageView;
    private TextView utilitiesButton;
    private BatteryIconView batteryIconView;
    private View rootView;
    private View topPanel;
    private View bottomPanel;
    private HorizontalScrollView shortcutsScrollView;
    private LinearLayout shortcutsContainer;
    private View addAppButton;
    private View medicalInfoButton;
    private View settingsButton;
    private View sosButton;
    private View weatherSection;
    private RecyclerView contactsRecyclerView;
    private SharedPreferences preferences;
    private ExecutorService backgroundExecutor;
    private ContactsAdapter contactsAdapter;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private String pendingContactLookupKey;
    private boolean batteryReceiverRegistered;
    private LauncherThemePalette themePalette;
    private VolumeOverlayController volumeOverlayController;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            updateBatteryLevel(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!LauncherPreferences.isSetupComplete(this)) {
            openSetup(true);
            return;
        }

        setContentView(R.layout.activity_main);

        preferences = LauncherPreferences.getPreferences(this);
        themePalette = LauncherThemePalette.fromPreferences(this);
        backgroundExecutor = Executors.newSingleThreadExecutor();

        batteryTextView = findViewById(R.id.battery_text);
        chargingTextView = findViewById(R.id.charging_text);
        weatherLineTextView = findViewById(R.id.weather_line);
        contactsEmptyView = findViewById(R.id.contacts_empty_view);
        nameView = findViewById(R.id.name_view);
        ageView = findViewById(R.id.age_view);
        utilitiesButton = findViewById(R.id.utilities_button);
        batteryIconView = findViewById(R.id.battery_icon);
        rootView = findViewById(R.id.main_root);
        topPanel = findViewById(R.id.top_panel);
        bottomPanel = findViewById(R.id.bottom_panel);
        shortcutsScrollView = findViewById(R.id.shortcuts_scroll);
        shortcutsContainer = findViewById(R.id.shortcuts_container);
        addAppButton = findViewById(R.id.add_app_button);
        medicalInfoButton = findViewById(R.id.medical_info_button);
        settingsButton = findViewById(R.id.settings_button);
        sosButton = findViewById(R.id.sos_button);
        weatherSection = findViewById(R.id.weather_section);
        contactsRecyclerView = findViewById(R.id.contacts_recycler);
        volumeOverlayController = new VolumeOverlayController(this);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handlePickedContactImageResult
        );

        setupShortcutSection();
        setupWeatherSection();
        setupContactsSection();
        applyThemePalette();
        renderSavedShortcuts();
        refreshProfileHeader();
        applyLauncherInsets();
        configureWakePresentation(getIntent());
        enableFullscreen();
        ensurePickupServiceRunning();
        refreshWeatherIfPossible();
        refreshContactsIfPossible();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!LauncherPreferences.isSetupComplete(this)) {
            return;
        }
        registerBatteryReceiver();
        ensurePickupServiceRunning();
        applyThemePalette();
        refreshProfileHeader();
        refreshWeatherIfPossible();
        refreshContactsIfPossible();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        configureWakePresentation(intent);
        enableFullscreen();
    }

    @Override
    protected void onStop() {
        if (batteryReceiverRegistered) {
            unregisterReceiver(batteryReceiver);
            batteryReceiverRegistered = false;
        }
        super.onStop();
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
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event != null && event.getAction() == MotionEvent.ACTION_DOWN) {
            requestKeyguardDismissIfNeeded();
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (volumeOverlayController != null && volumeOverlayController.handleKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void enableFullscreen() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }

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

    private void ensurePickupServiceRunning() {
        PickupMonitorService.start(this);
    }

    private void configureWakePresentation(Intent intent) {
        setShowWhenLocked(true);
        setTurnScreenOn(true);

        if (intent == null || !intent.getBooleanExtra(EXTRA_WAKE_FROM_PICKUP, false)) {
            return;
        }

        requestKeyguardDismissIfNeeded();
        intent.removeExtra(EXTRA_WAKE_FROM_PICKUP);
    }

    private void requestKeyguardDismissIfNeeded() {
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            keyguardManager.requestDismissKeyguard(this, null);
        }
    }

    private void applyLauncherInsets() {
        View topPanel = findViewById(R.id.top_panel);
        View contentContainer = findViewById(R.id.top_content_container);
        if (topPanel == null || contentContainer == null) {
            return;
        }

        int baseLeft = contentContainer.getPaddingLeft();
        int baseTop = contentContainer.getPaddingTop();
        int baseRight = contentContainer.getPaddingRight();
        int baseBottom = contentContainer.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(topPanel, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());

            contentContainer.setPadding(
                    baseLeft + insets.left,
                    baseTop + insets.top,
                    baseRight + insets.right,
                    baseBottom
            );
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(topPanel);
    }

    private void registerBatteryReceiver() {
        if (batteryReceiverRegistered) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = registerReceiver(batteryReceiver, intentFilter);
        batteryReceiverRegistered = true;
        updateBatteryLevel(batteryIntent);
    }

    private void updateBatteryLevel(Intent batteryIntent) {
        if (batteryTextView == null || batteryIntent == null) {
            return;
        }

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            batteryTextView.setText(R.string.battery_unknown);
            if (batteryIconView != null) {
                batteryIconView.setBatteryLevel(0);
            }
            return;
        }

        int percentage = Math.round((level * 100f) / scale);
        batteryTextView.setText(getString(R.string.battery_percentage, percentage));
        if (batteryIconView != null) {
            batteryIconView.setBatteryLevel(percentage);
        }

        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL;

        if (chargingTextView != null) {
            chargingTextView.setVisibility(isCharging ? View.VISIBLE : View.GONE);
        }
    }

    private void setupShortcutSection() {
        if (addAppButton == null) {
            return;
        }

        addAppButton.setOnClickListener(view -> showAppPicker());
        if (medicalInfoButton != null) {
            medicalInfoButton.setOnClickListener(
                    view -> startActivity(new Intent(this, MedicalInfoActivity.class))
            );
        }
        if (settingsButton != null) {
            settingsButton.setOnClickListener(view -> openSetup(false));
        }
        if (sosButton != null) {
            sosButton.setOnClickListener(view -> openEmergencyDialer());
        }
    }

    private void setupWeatherSection() {
        if (weatherSection == null) {
            return;
        }

        weatherSection.setOnClickListener(view -> {
            if (hasLocationPermission()) {
                fetchWeatherForCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });
        showWeatherPermissionState();

        if (utilitiesButton != null) {
            utilitiesButton.setOnClickListener(view ->
                    startActivity(new Intent(this, UtilitiesActivity.class))
            );
        }
    }

    private void setupContactsSection() {
        if (contactsRecyclerView == null) {
            return;
        }

        contactsAdapter = new ContactsAdapter(
                getLayoutInflater(),
                this::onContactClicked,
                themePalette
        );
        contactsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        contactsRecyclerView.setAdapter(contactsAdapter);

        if (contactsEmptyView != null) {
            contactsEmptyView.setOnClickListener(view -> {
                if (hasContactsPermission()) {
                    loadContacts();
                } else {
                    requestContactsPermission();
                }
            });
            contactsEmptyView.setText(R.string.contacts_loading);
        }
    }

    private void showAppPicker() {
        List<String> savedComponents = loadSavedShortcutComponents();
        List<AppShortcut> availableApps = getLaunchableApps();
        if (availableApps.isEmpty()) {
            Toast.makeText(this, R.string.no_apps_found, Toast.LENGTH_SHORT).show();
            return;
        }

        List<AppShortcut> pickerApps = new ArrayList<>();
        Set<String> savedSet = new LinkedHashSet<>(savedComponents);
        for (AppShortcut appShortcut : availableApps) {
            if (!savedSet.contains(appShortcut.componentKey)) {
                pickerApps.add(appShortcut);
            }
        }

        if (pickerApps.isEmpty()) {
            Toast.makeText(this, R.string.shortcut_all_added, Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] labels = new CharSequence[pickerApps.size()];
        for (int i = 0; i < pickerApps.size(); i++) {
            labels[i] = pickerApps.get(i).label;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_app_title)
                .setItems(labels, (dialogInterface, which) -> addShortcut(pickerApps.get(which)))
                .show();
    }

    private void addShortcut(AppShortcut appShortcut) {
        List<String> savedComponents = loadSavedShortcutComponents();
        if (savedComponents.contains(appShortcut.componentKey)) {
            Toast.makeText(this, R.string.shortcut_already_added, Toast.LENGTH_SHORT).show();
            return;
        }

        savedComponents.add(appShortcut.componentKey);
        saveShortcutComponents(savedComponents);
        renderSavedShortcuts();
    }

    private void renderSavedShortcuts() {
        if (shortcutsContainer == null) {
            return;
        }

        shortcutsContainer.removeAllViews();

        List<String> savedComponents = loadSavedShortcutComponents();
        List<AppShortcut> availableApps = getLaunchableApps();
        List<String> validComponents = new ArrayList<>();

        for (String componentKey : savedComponents) {
            AppShortcut appShortcut = findShortcutByComponent(componentKey, availableApps);
            if (appShortcut == null) {
                continue;
            }

            validComponents.add(componentKey);
            shortcutsContainer.addView(createShortcutView(appShortcut));
        }

        if (!validComponents.equals(savedComponents)) {
            saveShortcutComponents(validComponents);
        }

        scrollShortcutsToEnd();
    }

    private View createShortcutView(AppShortcut appShortcut) {
        ImageView shortcutView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(dpToPx(64), dpToPx(64));
        layoutParams.setMarginStart(dpToPx(16));
        shortcutView.setLayoutParams(layoutParams);
        shortcutView.setBackgroundResource(R.drawable.bg_quick_action);
        shortcutView.setImageDrawable(appShortcut.icon);
        shortcutView.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        shortcutView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        shortcutView.setContentDescription(
                getString(R.string.shortcut_content_description, appShortcut.label)
        );
        shortcutView.setOnClickListener(view -> launchShortcut(appShortcut));
        return shortcutView;
    }

    private void launchShortcut(AppShortcut appShortcut) {
        try {
            startActivity(appShortcut.launchIntent);
        } catch (Exception exception) {
            Toast.makeText(this, R.string.shortcut_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSetup(boolean clearLauncher) {
        Intent setupIntent = new Intent(this, SetupActivity.class);
        if (clearLauncher) {
            startActivity(setupIntent);
            finish();
            return;
        }
        startActivity(setupIntent);
    }

    private void openEmergencyDialer() {
        try {
            startActivity(DialerActivity.createEmergencyIntent(this));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.sos_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshProfileHeader() {
        if (nameView != null) {
            String displayName = LauncherPreferences.getDisplayName(this);
            if (!TextUtils.isEmpty(displayName)) {
                nameView.setText(displayName);
            }
        }

        if (ageView != null) {
            String age = LauncherPreferences.getAge(this);
            if (!TextUtils.isEmpty(age)) {
                ageView.setText(getString(R.string.profile_age_format, age));
            }
        }
    }

    private void applyThemePalette() {
        themePalette = LauncherThemePalette.fromPreferences(this);

        if (rootView != null) {
            rootView.setBackgroundColor(themePalette.getBackgroundColor());
        }
        if (topPanel != null) {
            topPanel.setBackgroundTintList(ColorStateList.valueOf(themePalette.getPrimaryColor()));
            topPanel.setBackgroundColor(themePalette.getPrimaryColor());
        }
        if (bottomPanel != null) {
            bottomPanel.setBackgroundColor(themePalette.getBackgroundColor());
        }
        if (contactsRecyclerView != null) {
            contactsRecyclerView.setBackgroundColor(themePalette.getBackgroundColor());
        }

        if (contactsEmptyView != null) {
            contactsEmptyView.setTextColor(themePalette.getHeadingColor());
        }

        if (contactsAdapter != null) {
            contactsAdapter.setThemePalette(themePalette);
        }
        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(themePalette);
        }
    }

    private List<AppShortcut> getLaunchableApps() {
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

        List<AppShortcut> appShortcuts = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null) {
                continue;
            }

            String packageName = resolveInfo.activityInfo.packageName;
            if (getPackageName().equals(packageName)) {
                continue;
            }

            String activityName = resolveInfo.activityInfo.name;
            ComponentName componentName = new ComponentName(packageName, activityName);
            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setComponent(componentName);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            CharSequence label = resolveInfo.loadLabel(packageManager);
            Drawable icon = resolveInfo.loadIcon(packageManager);

            appShortcuts.add(new AppShortcut(
                    componentName.flattenToString(),
                    label != null ? label.toString() : packageName,
                    icon,
                    launchIntent
            ));
        }

        Collections.sort(
                appShortcuts,
                Comparator.comparing(appShortcut -> appShortcut.label.toLowerCase(Locale.getDefault()))
        );
        return appShortcuts;
    }

    private AppShortcut findShortcutByComponent(
            String componentKey,
            List<AppShortcut> availableApps
    ) {
        for (AppShortcut appShortcut : availableApps) {
            if (appShortcut.componentKey.equals(componentKey)) {
                return appShortcut;
            }
        }
        return null;
    }

    private List<String> loadSavedShortcutComponents() {
        return ShortcutStorage.load(this);
    }

    private void saveShortcutComponents(List<String> componentKeys) {
        ShortcutStorage.save(this, componentKeys);
    }

    private int dpToPx(int dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        );
    }

    private void scrollShortcutsToEnd() {
        if (shortcutsScrollView == null) {
            return;
        }

        shortcutsScrollView.post(() -> shortcutsScrollView.fullScroll(View.FOCUS_RIGHT));
    }

    private void refreshContactsIfPossible() {
        if (hasContactsPermission()) {
            loadContacts();
        } else {
            showContactsPermissionState();
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

    private void refreshWeatherIfPossible() {
        if (hasLocationPermission()) {
            fetchWeatherForCurrentLocation();
        } else {
            showWeatherPermissionState();
        }
    }

    private boolean hasLocationPermission() {
        return hasFineLocationPermission() || hasCoarseLocationPermission();
    }

    private void requestLocationPermission() {
        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSION
        );
    }

    private boolean hasFineLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCoarseLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (hasLocationPermission()) {
                fetchWeatherForCurrentLocation();
            } else {
                showWeatherPermissionState();
            }
            return;
        }

        if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                showContactsPermissionState();
            }
        }
    }

    private void showContactsPermissionState() {
        if (contactsRecyclerView != null) {
            contactsRecyclerView.setVisibility(View.GONE);
        }
        if (contactsEmptyView != null) {
            contactsEmptyView.setVisibility(View.VISIBLE);
            contactsEmptyView.setText(getString(
                    R.string.contacts_permission_message,
                    getString(R.string.contacts_permission_needed),
                    getString(R.string.contacts_permission_detail)
            ));
        }
    }

    private void loadContacts() {
        if (!hasContactsPermission()) {
            showContactsPermissionState();
            return;
        }

        if (contactsEmptyView != null) {
            contactsEmptyView.setVisibility(View.VISIBLE);
            contactsEmptyView.setText(R.string.contacts_loading);
        }

        if (backgroundExecutor == null) {
            return;
        }

        backgroundExecutor.submit(() -> {
            List<ContactEntry> contacts = queryContacts();
            runOnUiThread(() -> showContacts(contacts));
        });
    }

    private List<ContactEntry> queryContacts() {
        List<ContactEntry> contacts = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Set<String> pinnedLookupKeys = new HashSet<>(
                LauncherPreferences.getPinnedContactLookupKeys(this)
        );

        String[] projection = new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_URI
        };

        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
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
            int nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
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
                        getSavedContactImageUri(lookupKey),
                        pinnedLookupKeys.contains(lookupKey)
                ));
            }
        } finally {
            cursor.close();
        }

        return sortContactsWithPinnedFirst(contacts);
    }

    private List<ContactEntry> sortContactsWithPinnedFirst(List<ContactEntry> contacts) {
        List<String> pinnedLookupKeys = LauncherPreferences.getPinnedContactLookupKeys(this);
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

    private void showContacts(List<ContactEntry> contacts) {
        if (contactsAdapter != null) {
            contactsAdapter.submitList(contacts);
        }

        boolean hasContacts = !contacts.isEmpty();
        if (contactsRecyclerView != null) {
            contactsRecyclerView.setVisibility(hasContacts ? View.VISIBLE : View.GONE);
        }
        if (contactsEmptyView != null) {
            contactsEmptyView.setVisibility(hasContacts ? View.GONE : View.VISIBLE);
            if (!hasContacts) {
                contactsEmptyView.setText(R.string.contacts_empty);
            }
        }
    }

    private void onContactClicked(ContactEntry contactEntry) {
        List<ContactPhoneOption> phoneOptions = queryContactPhoneOptions(contactEntry);
        if (phoneOptions.isEmpty()) {
            Toast.makeText(this, R.string.contact_no_phone_number, Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneOptions.size() == 1) {
            startActivity(CallConfirmationActivity.createIntent(
                    this,
                    contactEntry.getDisplayName(),
                    phoneOptions.get(0).number
            ));
            return;
        }

        CharSequence[] labels = new CharSequence[phoneOptions.size()];
        for (int i = 0; i < phoneOptions.size(); i++) {
            ContactPhoneOption phoneOption = phoneOptions.get(i);
            labels[i] = getString(
                    R.string.contact_phone_option_format,
                    phoneOption.label,
                    phoneOption.number
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.contact_choose_number_title)
                .setItems(labels, (dialogInterface, which) ->
                        startActivity(CallConfirmationActivity.createIntent(
                                this,
                                contactEntry.getDisplayName(),
                                phoneOptions.get(which).number
                        ))
                )
                .show();
    }

    private List<ContactPhoneOption> queryContactPhoneOptions(ContactEntry contactEntry) {
        List<ContactPhoneOption> phoneOptions = new ArrayList<>();
        if (contactEntry == null) {
            return phoneOptions;
        }

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.LABEL
                },
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactEntry.getId())},
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY + " DESC, "
                        + ContactsContract.CommonDataKinds.Phone.IS_PRIMARY + " DESC, "
                        + ContactsContract.CommonDataKinds.Phone.NUMBER + " ASC"
        );

        if (cursor == null) {
            return phoneOptions;
        }

        Set<String> seenNumbers = new LinkedHashSet<>();
        try {
            int numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE);
            int labelIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL);

            while (cursor.moveToNext()) {
                String number = cursor.getString(numberIndex);
                if (TextUtils.isEmpty(number)) {
                    continue;
                }

                String normalizedNumber = number.replaceAll("\\s+", "");
                if (!seenNumbers.add(normalizedNumber)) {
                    continue;
                }

                int type = cursor.getInt(typeIndex);
                String customLabel = cursor.getString(labelIndex);
                CharSequence typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        getResources(),
                        type,
                        customLabel
                );
                String label = typeLabel != null ? typeLabel.toString() : getString(R.string.contact_phone_default_label);
                phoneOptions.add(new ContactPhoneOption(number, label));
            }
        } finally {
            cursor.close();
        }

        return phoneOptions;
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
            Uri savedImageUri = copyContactImageToInternalStorage(imageUri, pendingContactLookupKey);
            saveContactImageUri(pendingContactLookupKey, savedImageUri);
            if (contactsAdapter != null) {
                contactsAdapter.updateContactPhoto(pendingContactLookupKey, savedImageUri);
            }
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.contacts_pick_photo_error, Toast.LENGTH_SHORT).show();
        }

        pendingContactLookupKey = null;
    }

    private Uri copyContactImageToInternalStorage(Uri sourceUri, String lookupKey) throws Exception {
        File directory = new File(getFilesDir(), "contact_photos");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create contacts photo directory");
        }

        String safeName = lookupKey.replaceAll("[^a-zA-Z0-9._-]", "_");
        File destinationFile = new File(directory, safeName + ".jpg");

        try (
                InputStream inputStream = getContentResolver().openInputStream(sourceUri);
                FileOutputStream outputStream = new FileOutputStream(destinationFile, false)
        ) {
            if (inputStream == null) {
                throw new IllegalStateException("Could not read selected image");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        return Uri.fromFile(destinationFile);
    }

    private Uri getSavedContactImageUri(String lookupKey) {
        String storedUri = preferences.getString(KEY_CONTACT_IMAGE_PREFIX + lookupKey, null);
        if (TextUtils.isEmpty(storedUri)) {
            return null;
        }
        return Uri.parse(storedUri);
    }

    private void saveContactImageUri(String lookupKey, Uri imageUri) {
        preferences.edit()
                .putString(KEY_CONTACT_IMAGE_PREFIX + lookupKey, imageUri.toString())
                .apply();
    }

    private void showWeatherPermissionState() {
        updateWeatherUi(
                getString(
                        R.string.weather_single_line,
                        getString(R.string.weather_temperature_placeholder),
                        getString(R.string.weather_permission_needed),
                        getString(R.string.weather_permission_detail)
                )
        );
    }

    @SuppressLint("MissingPermission")
    private void fetchWeatherForCurrentLocation() {
        if (!hasLocationPermission()) {
            showWeatherPermissionState();
            return;
        }

        updateWeatherUi(
                getString(
                        R.string.weather_single_line,
                        getString(R.string.weather_temperature_placeholder),
                        getString(R.string.weather_loading),
                        getString(R.string.weather_location_fallback)
                )
        );

        LocationManager locationManager = getSystemService(LocationManager.class);
        if (locationManager == null) {
            showCachedWeatherOrError(getString(R.string.weather_location_disabled));
            return;
        }

        String provider = getBestLocationProvider(locationManager);
        Location lastKnownLocation = getBestLastKnownLocation(locationManager);
        if (provider == null && lastKnownLocation == null) {
            showCachedWeatherOrError(getString(R.string.weather_location_disabled));
            return;
        }

        if (provider == null) {
            loadWeatherForLocation(lastKnownLocation);
            return;
        }

        try {
            if (lastKnownLocation != null) {
                loadWeatherForLocation(lastKnownLocation);
            }

            final Location fallbackLocation = lastKnownLocation;
            locationManager.getCurrentLocation(provider, null, getMainExecutor(), location -> {
                if (location != null) {
                    loadWeatherForLocation(location);
                } else if (fallbackLocation == null) {
                    showCachedWeatherOrError(getString(R.string.weather_unavailable));
                }
            });
        } catch (SecurityException exception) {
            showWeatherPermissionState();
        } catch (Exception exception) {
            if (lastKnownLocation != null) {
                loadWeatherForLocation(lastKnownLocation);
            } else {
                showCachedWeatherOrError(getString(R.string.weather_unavailable));
            }
        }
    }

    private String getBestLocationProvider(LocationManager locationManager) {
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && (hasCoarseLocationPermission() || hasFineLocationPermission())) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && hasFineLocationPermission()) {
            return LocationManager.GPS_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
                && hasLocationPermission()) {
            return LocationManager.PASSIVE_PROVIDER;
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    private Location getBestLastKnownLocation(LocationManager locationManager) {
        List<String> providerCandidates = new ArrayList<>();
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && (hasCoarseLocationPermission() || hasFineLocationPermission())) {
            providerCandidates.add(LocationManager.NETWORK_PROVIDER);
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && hasFineLocationPermission()) {
            providerCandidates.add(LocationManager.GPS_PROVIDER);
        }
        if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
                && hasLocationPermission()) {
            providerCandidates.add(LocationManager.PASSIVE_PROVIDER);
        }

        Location bestLocation = null;
        for (String provider : providerCandidates) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate == null) {
                    continue;
                }
                if (bestLocation == null || candidate.getTime() > bestLocation.getTime()) {
                    bestLocation = candidate;
                }
            } catch (SecurityException ignored) {
                return bestLocation;
            }
        }
        return bestLocation;
    }

    private void loadWeatherForLocation(Location location) {
        if (backgroundExecutor == null) {
            return;
        }

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        backgroundExecutor.submit(() -> {
            try {
                String locationLabel = getLocationLabel(latitude, longitude);
                WeatherInfo weatherInfo = fetchWeather(latitude, longitude, locationLabel);

                runOnUiThread(() -> {
                    cacheWeatherSummary(weatherInfo.summary);
                    updateWeatherUi(weatherInfo.summary);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> showCachedWeatherOrError(getString(R.string.weather_unavailable)));
            }
        });
    }

    private WeatherInfo fetchWeather(double latitude, double longitude, String locationLabel)
            throws Exception {
        String apiUrl = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current=temperature_2m,weather_code,is_day,wind_speed_10m&timezone=auto&forecast_days=1",
                latitude,
                longitude
        );

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Weather request failed");
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            JSONObject root = new JSONObject(responseBuilder.toString());
            JSONObject current = root.getJSONObject("current");

            double temperature = current.getDouble("temperature_2m");
            int weatherCode = current.getInt("weather_code");
            int isDay = current.optInt("is_day", 1);
            double windSpeed = current.optDouble("wind_speed_10m", 0);

            String temperatureText = String.format(
                    Locale.getDefault(),
                    "%d°",
                    Math.round(temperature)
            );
            String description = getWeatherDescription(weatherCode, isDay == 1);
            String windText = getString(R.string.weather_wind, windSpeed);
                String meta = getString(
                        R.string.weather_location_and_wind,
                        locationLabel,
                        windText
                );

            return new WeatherInfo(
                    getString(R.string.weather_single_line, temperatureText, description, meta)
            );
        } finally {
            connection.disconnect();
        }
    }

    private String getLocationLabel(double latitude, double longitude) {
        String fallback = getString(R.string.weather_location_fallback);
        if (!Geocoder.isPresent()) {
            return fallback;
        }

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses == null || addresses.isEmpty()) {
                return fallback;
            }

            Address address = addresses.get(0);
            if (!TextUtils.isEmpty(address.getLocality())) {
                return address.getLocality();
            }
            if (!TextUtils.isEmpty(address.getSubAdminArea())) {
                return address.getSubAdminArea();
            }
            if (!TextUtils.isEmpty(address.getAdminArea())) {
                return address.getAdminArea();
            }
        } catch (Exception ignored) {
            return fallback;
        }

        return fallback;
    }

    private void updateWeatherUi(String summary) {
        if (weatherLineTextView != null) {
            weatherLineTextView.setText(summary);
        }
    }

    private void cacheWeatherSummary(String summary) {
        if (TextUtils.isEmpty(summary)) {
            return;
        }
        preferences.edit()
                .putString(KEY_LAST_WEATHER_SUMMARY, summary)
                .apply();
    }

    private void showCachedWeatherOrError(String description) {
        String cachedSummary = preferences.getString(KEY_LAST_WEATHER_SUMMARY, "");
        if (!TextUtils.isEmpty(cachedSummary)) {
            updateWeatherUi(cachedSummary);
            return;
        }
        showWeatherError(description);
    }

    private void showWeatherError(String description) {
        updateWeatherUi(
                getString(
                        R.string.weather_single_line,
                        getString(R.string.weather_temperature_placeholder),
                        description,
                        getString(R.string.weather_location_fallback)
                )
        );
    }

    private String getWeatherDescription(int weatherCode, boolean isDay) {
        switch (weatherCode) {
            case 0:
                return isDay ? "Despejado" : "Noche despejada";
            case 1:
            case 2:
                return "Poco nuboso";
            case 3:
                return "Cubierto";
            case 45:
            case 48:
                return "Niebla";
            case 51:
            case 53:
            case 55:
                return "Llovizna";
            case 56:
            case 57:
                return "Llovizna helada";
            case 61:
            case 63:
            case 65:
                return "Lluvia";
            case 66:
            case 67:
                return "Lluvia helada";
            case 71:
            case 73:
            case 75:
                return "Nieve";
            case 77:
                return "Granos de nieve";
            case 80:
            case 81:
            case 82:
                return "Chubascos";
            case 85:
            case 86:
                return "Chubascos de nieve";
            case 95:
                return "Tormenta";
            case 96:
            case 99:
                return "Tormenta con granizo";
            default:
                return "Clima variable";
        }
    }

    private static final class WeatherInfo {
        private final String summary;

        private WeatherInfo(String summary) {
            this.summary = summary;
        }
    }

    private static final class AppShortcut {
        private final String componentKey;
        private final String label;
        private final Drawable icon;
        private final Intent launchIntent;

        private AppShortcut(String componentKey, String label, Drawable icon, Intent launchIntent) {
            this.componentKey = componentKey;
            this.label = label;
            this.icon = icon;
            this.launchIntent = launchIntent;
        }
    }

    private static final class ContactPhoneOption {
        private final String number;
        private final String label;

        private ContactPhoneOption(String number, String label) {
            this.number = number;
            this.label = label;
        }
    }
}

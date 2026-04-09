package com.rdr.easy2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortcutOrganizerActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView titleView;
    private TextView emptyView;
    private TextView closeButton;
    private View rootView;
    private ShortcutOrganizerAdapter adapter;
    private VolumeOverlayController volumeOverlayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortcut_organizer);

        rootView = findViewById(R.id.shortcut_organizer_root);
        titleView = findViewById(R.id.shortcut_organizer_title);
        emptyView = findViewById(R.id.shortcut_organizer_empty);
        closeButton = findViewById(R.id.close_shortcut_organizer_button);
        recyclerView = findViewById(R.id.shortcut_organizer_recycler);
        volumeOverlayController = new VolumeOverlayController(this);

        adapter = new ShortcutOrganizerAdapter(
                getLayoutInflater(),
                this::moveShortcutUp,
                this::moveShortcutDown,
                this::removeShortcut
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (closeButton != null) {
            closeButton.setOnClickListener(view -> finish());
        }

        loadShortcuts();
        applyThemePalette();
        enableFullscreen();
    }

    @Override
    protected void onDestroy() {
        if (volumeOverlayController != null) {
            volumeOverlayController.release();
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

    private void loadShortcuts() {
        List<String> storedComponents = ShortcutStorage.load(this);
        Map<String, ShortcutOrganizerItem> availableItems = getAvailableShortcutMap();
        List<ShortcutOrganizerItem> visibleItems = new ArrayList<>();
        List<String> validComponents = new ArrayList<>();

        for (String componentKey : storedComponents) {
            ShortcutOrganizerItem item = availableItems.get(componentKey);
            if (item == null) {
                continue;
            }
            visibleItems.add(item);
            validComponents.add(componentKey);
        }

        if (!validComponents.equals(storedComponents)) {
            ShortcutStorage.save(this, validComponents);
        }

        adapter.submitList(visibleItems);
        updateEmptyState(visibleItems.isEmpty());
    }

    private Map<String, ShortcutOrganizerItem> getAvailableShortcutMap() {
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

        Map<String, ShortcutOrganizerItem> shortcutMap = new HashMap<>();
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
            String componentKey = componentName.flattenToString();
            CharSequence label = resolveInfo.loadLabel(packageManager);
            Drawable icon = resolveInfo.loadIcon(packageManager);

            shortcutMap.put(componentKey, new ShortcutOrganizerItem(
                    componentKey,
                    label != null ? label.toString() : packageName,
                    icon
            ));
        }
        return shortcutMap;
    }

    private void moveShortcutUp(ShortcutOrganizerItem item) {
        List<String> components = ShortcutStorage.load(this);
        int currentIndex = components.indexOf(item.componentKey);
        if (currentIndex <= 0) {
            return;
        }
        Collections.swap(components, currentIndex, currentIndex - 1);
        ShortcutStorage.save(this, components);
        loadShortcuts();
    }

    private void moveShortcutDown(ShortcutOrganizerItem item) {
        List<String> components = ShortcutStorage.load(this);
        int currentIndex = components.indexOf(item.componentKey);
        if (currentIndex < 0 || currentIndex >= components.size() - 1) {
            return;
        }
        Collections.swap(components, currentIndex, currentIndex + 1);
        ShortcutStorage.save(this, components);
        loadShortcuts();
    }

    private void removeShortcut(ShortcutOrganizerItem item) {
        List<String> components = ShortcutStorage.load(this);
        if (!components.remove(item.componentKey)) {
            return;
        }
        ShortcutStorage.save(this, components);
        loadShortcuts();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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

    private void applyThemePalette() {
        LauncherThemePalette palette = LauncherThemePalette.fromPreferences(this);

        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (emptyView != null) {
            emptyView.setTextColor(palette.getBodyTextColor());
        }
        if (closeButton != null) {
            closeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
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

    static class ShortcutOrganizerItem {
        private final String componentKey;
        private final String label;
        private final Drawable icon;

        private ShortcutOrganizerItem(String componentKey, String label, Drawable icon) {
            this.componentKey = componentKey;
            this.label = label;
            this.icon = icon;
        }
    }

    static class ShortcutOrganizerAdapter
            extends RecyclerView.Adapter<ShortcutOrganizerAdapter.ShortcutOrganizerViewHolder> {
        interface ShortcutActionListener {
            void onAction(ShortcutOrganizerItem item);
        }

        private final android.view.LayoutInflater layoutInflater;
        private final ShortcutActionListener moveUpListener;
        private final ShortcutActionListener moveDownListener;
        private final ShortcutActionListener removeListener;
        private final List<ShortcutOrganizerItem> items = new ArrayList<>();
        private LauncherThemePalette themePalette;

        ShortcutOrganizerAdapter(
                android.view.LayoutInflater layoutInflater,
                ShortcutActionListener moveUpListener,
                ShortcutActionListener moveDownListener,
                ShortcutActionListener removeListener
        ) {
            this.layoutInflater = layoutInflater;
            this.moveUpListener = moveUpListener;
            this.moveDownListener = moveDownListener;
            this.removeListener = removeListener;
        }

        void submitList(List<ShortcutOrganizerItem> updatedItems) {
            items.clear();
            items.addAll(updatedItems);
            notifyDataSetChanged();
        }

        void setThemePalette(LauncherThemePalette themePalette) {
            this.themePalette = themePalette;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ShortcutOrganizerViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = layoutInflater.inflate(R.layout.item_shortcut_organizer, parent, false);
            return new ShortcutOrganizerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull ShortcutOrganizerViewHolder holder,
                int position
        ) {
            holder.bind(
                    items.get(position),
                    moveUpListener,
                    moveDownListener,
                    removeListener,
                    themePalette != null ? themePalette : LauncherThemePalette.fromKey(null),
                    position,
                    getItemCount()
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ShortcutOrganizerViewHolder extends RecyclerView.ViewHolder {
            private final View cardView;
            private final android.widget.ImageView iconView;
            private final TextView labelView;
            private final TextView upButton;
            private final TextView downButton;
            private final TextView removeButton;

            ShortcutOrganizerViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.shortcut_item_card);
                iconView = itemView.findViewById(R.id.shortcut_item_icon);
                labelView = itemView.findViewById(R.id.shortcut_item_label);
                upButton = itemView.findViewById(R.id.shortcut_item_up_button);
                downButton = itemView.findViewById(R.id.shortcut_item_down_button);
                removeButton = itemView.findViewById(R.id.shortcut_item_remove_button);
            }

            void bind(
                    ShortcutOrganizerItem item,
                    ShortcutActionListener moveUpListener,
                    ShortcutActionListener moveDownListener,
                    ShortcutActionListener removeListener,
                    LauncherThemePalette palette,
                    int position,
                    int totalItems
            ) {
                iconView.setImageDrawable(item.icon);
                labelView.setText(item.label);
                labelView.setTextColor(palette.getHeadingColor());
                cardView.setBackground(createRoundedBackground(
                        palette.getSetupFieldFillColor(),
                        palette.getSetupFieldStrokeColor(),
                        24,
                        2
                ));

                styleActionButton(upButton, palette.getCircleColor());
                styleActionButton(downButton, palette.getCircleColor());
                styleActionButton(removeButton, 0xFFD84343);

                upButton.setEnabled(position > 0);
                upButton.setAlpha(position > 0 ? 1f : 0.35f);
                downButton.setEnabled(position < totalItems - 1);
                downButton.setAlpha(position < totalItems - 1 ? 1f : 0.35f);

                upButton.setOnClickListener(view -> moveUpListener.onAction(item));
                downButton.setOnClickListener(view -> moveDownListener.onAction(item));
                removeButton.setOnClickListener(view -> removeListener.onAction(item));
            }

            private void styleActionButton(TextView button, int fillColor) {
                button.setBackground(createRoundedBackground(fillColor, fillColor, 18, 0));
                button.setTextColor(Color.WHITE);
            }

            private GradientDrawable createRoundedBackground(
                    int fillColor,
                    int strokeColor,
                    int radiusDp,
                    int strokeWidthDp
            ) {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(dpToPx(radiusDp));
                drawable.setColor(fillColor);
                if (strokeWidthDp > 0) {
                    drawable.setStroke(Math.round(dpToPx(strokeWidthDp)), strokeColor);
                }
                return drawable;
            }

            private float dpToPx(int dpValue) {
                return TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dpValue,
                        itemView.getResources().getDisplayMetrics()
                );
            }
        }
    }
}

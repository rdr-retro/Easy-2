package com.rdr.easy2;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhoneAppsAdapter extends RecyclerView.Adapter<PhoneAppsAdapter.PhoneAppViewHolder> {
    public interface PhoneAppClickListener {
        void onPhoneAppClicked(PhoneAppEntry phoneAppEntry);
    }

    private final LayoutInflater layoutInflater;
    private final PhoneAppClickListener phoneAppClickListener;
    private final List<PhoneAppEntry> phoneApps = new ArrayList<>();
    private LauncherThemePalette themePalette;

    public PhoneAppsAdapter(
            LayoutInflater layoutInflater,
            PhoneAppClickListener phoneAppClickListener,
            LauncherThemePalette themePalette
    ) {
        this.layoutInflater = layoutInflater;
        this.phoneAppClickListener = phoneAppClickListener;
        this.themePalette = themePalette;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public PhoneAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_phone_app, parent, false);
        return new PhoneAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhoneAppViewHolder holder, int position) {
        holder.bind(phoneApps.get(position), phoneAppClickListener);
    }

    @Override
    public int getItemCount() {
        return phoneApps.size();
    }

    @Override
    public long getItemId(int position) {
        return phoneApps.get(position).getComponentKey().hashCode();
    }

    public void submitList(List<PhoneAppEntry> entries) {
        phoneApps.clear();
        phoneApps.addAll(entries);
        notifyDataSetChanged();
    }

    public void setThemePalette(LauncherThemePalette themePalette) {
        if (themePalette == null) {
            return;
        }
        if (this.themePalette != null
                && this.themePalette.getKey().equals(themePalette.getKey())) {
            return;
        }
        this.themePalette = themePalette;
        notifyDataSetChanged();
    }

    static class PhoneAppViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout iconContainer;
        private final ImageView iconView;
        private final TextView nameView;

        PhoneAppViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.phone_app_icon_container);
            iconView = itemView.findViewById(R.id.phone_app_icon);
            nameView = itemView.findViewById(R.id.phone_app_name);
        }

        void bind(PhoneAppEntry phoneAppEntry, PhoneAppClickListener clickListener) {
            LauncherThemePalette palette = themePaletteOrDefault();
            String appKey = phoneAppEntry.getComponentKey();
            int accentColor = ColorblindStyleHelper.resolvePhoneAppAccentColor(appKey, palette);
            boolean colorblindMode = ColorblindStyleHelper.isColorblindMode(palette);

            itemView.setBackground(ColorblindStyleHelper.createRoundedBackground(
                    itemView.getContext(),
                    ColorblindStyleHelper.resolvePhoneAppCardColor(appKey, palette),
                    ColorblindStyleHelper.resolvePhoneAppStrokeColor(appKey, palette),
                    28,
                    colorblindMode ? 3 : 2
            ));
            iconContainer.setBackground(ColorblindStyleHelper.createRoundedBackground(
                    itemView.getContext(),
                    ColorblindStyleHelper.resolvePhoneAppSurfaceColor(appKey, palette),
                    accentColor,
                    38,
                    colorblindMode ? 4 : 3
            ));
            nameView.setBackground(ColorblindStyleHelper.createRoundedBackground(
                    itemView.getContext(),
                    ColorblindStyleHelper.resolvePhoneAppLabelColor(appKey, palette),
                    ColorblindStyleHelper.resolvePhoneAppStrokeColor(appKey, palette),
                    18,
                    colorblindMode ? 0 : 1
            ));
            nameView.setTextColor(ColorblindStyleHelper.resolvePhoneAppTextColor(appKey, palette));
            nameView.setText(phoneAppEntry.getLabel());
            iconView.setImageDrawable(copyDrawable(phoneAppEntry.getIcon()));
            ColorblindStyleHelper.applyIconColor(iconView, appKey, palette);

            iconContainer.setOnClickListener(view -> clickListener.onPhoneAppClicked(phoneAppEntry));
            itemView.setOnClickListener(view -> clickListener.onPhoneAppClicked(phoneAppEntry));
        }

        private LauncherThemePalette themePaletteOrDefault() {
            PhoneAppsAdapter adapter = (PhoneAppsAdapter) getBindingAdapter();
            if (adapter == null || adapter.themePalette == null) {
                return LauncherThemePalette.fromKey(null);
            }
            return adapter.themePalette;
        }

        private Drawable copyDrawable(Drawable drawable) {
            if (drawable == null || drawable.getConstantState() == null) {
                return drawable;
            }
            return drawable.getConstantState().newDrawable().mutate();
        }
    }
}

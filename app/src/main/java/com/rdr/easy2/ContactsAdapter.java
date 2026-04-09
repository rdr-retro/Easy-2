package com.rdr.easy2;

import android.net.Uri;
import android.text.TextUtils;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    public interface ContactClickListener {
        void onContactClicked(ContactEntry contactEntry);
    }

    private final LayoutInflater layoutInflater;
    private final ContactClickListener contactClickListener;
    private final List<ContactEntry> contacts = new ArrayList<>();
    private LauncherThemePalette themePalette;

    public ContactsAdapter(
            LayoutInflater layoutInflater,
            ContactClickListener contactClickListener,
            LauncherThemePalette themePalette
    ) {
        this.layoutInflater = layoutInflater;
        this.contactClickListener = contactClickListener;
        this.themePalette = themePalette;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactEntry contactEntry = contacts.get(position);
        holder.bind(contactEntry, contactClickListener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public long getItemId(int position) {
        return contacts.get(position).getId();
    }

    public void submitList(List<ContactEntry> contactEntries) {
        contacts.clear();
        contacts.addAll(contactEntries);
        notifyDataSetChanged();
    }

    public void updateContactPhoto(String lookupKey, Uri imageUri) {
        for (int i = 0; i < contacts.size(); i++) {
            ContactEntry entry = contacts.get(i);
            if (entry.getLookupKey().equals(lookupKey)) {
                entry.setCustomPhotoUri(imageUri);
                notifyItemChanged(i);
                return;
            }
        }
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

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout imageContainer;
        private final ShapeableImageView imageView;
        private final TextView initialView;
        private final TextView nameView;
        private final ImageView pinBadgeView;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            imageContainer = itemView.findViewById(R.id.contact_image_container);
            imageView = itemView.findViewById(R.id.contact_image);
            initialView = itemView.findViewById(R.id.contact_initial);
            nameView = itemView.findViewById(R.id.contact_name);
            pinBadgeView = itemView.findViewById(R.id.contact_pin_badge);
        }

        void bind(ContactEntry contactEntry, ContactClickListener clickListener) {
            LauncherThemePalette palette = themePaletteOrDefault();
            nameView.setText(formatDisplayName(contactEntry.getDisplayName()));
            initialView.setText(getInitial(contactEntry.getDisplayName()));
            pinBadgeView.setVisibility(contactEntry.isPinned() ? View.VISIBLE : View.GONE);
            imageContainer.setBackground(createCircleBackground(palette.getCircleColor()));
            nameView.setBackground(createRoundedBackground(palette.getChipColor(), 18));

            Uri imageUri = contactEntry.getCustomPhotoUri() != null
                    ? contactEntry.getCustomPhotoUri()
                    : contactEntry.getContactPhotoUri();

            if (imageUri != null) {
                imageView.setImageURI(null);
                imageView.setImageURI(imageUri);
                imageView.setVisibility(View.VISIBLE);
                initialView.setVisibility(View.GONE);
            } else {
                imageView.setImageDrawable(null);
                imageView.setVisibility(View.INVISIBLE);
                initialView.setVisibility(View.VISIBLE);
            }

            imageContainer.setOnClickListener(view -> clickListener.onContactClicked(contactEntry));
            itemView.setOnClickListener(view -> clickListener.onContactClicked(contactEntry));
        }

        private LauncherThemePalette themePaletteOrDefault() {
            ContactsAdapter adapter = (ContactsAdapter) getBindingAdapter();
            if (adapter == null || adapter.themePalette == null) {
                return LauncherThemePalette.fromKey(null);
            }
            return adapter.themePalette;
        }

        private GradientDrawable createCircleBackground(int fillColor) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(fillColor);
            return drawable;
        }

        private GradientDrawable createRoundedBackground(int fillColor, int cornerRadiusDp) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(dpToPx(cornerRadiusDp));
            drawable.setColor(fillColor);
            return drawable;
        }

        private float dpToPx(int dpValue) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dpValue,
                    itemView.getResources().getDisplayMetrics()
            );
        }

        private static String getInitial(String displayName) {
            if (TextUtils.isEmpty(displayName)) {
                return "?";
            }
            return displayName.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        private static String formatDisplayName(String displayName) {
            if (TextUtils.isEmpty(displayName)) {
                return "";
            }
            return displayName.toUpperCase(Locale.getDefault());
        }
    }
}

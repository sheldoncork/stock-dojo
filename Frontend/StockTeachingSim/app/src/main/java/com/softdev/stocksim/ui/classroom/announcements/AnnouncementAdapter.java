package com.softdev.stocksim.ui.classroom.announcements;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.softdev.stocksim.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying announcement items in a list.
 * Handles both single-classroom and all-classroom announcement views.
 *
 * @author Blake Nelson
 */
public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
    private final List<AnnouncementItem> announcements;
    private final boolean showClassName;
    private final OnAnnouncementClickListener clickListener;

    /**
     * Callback interface for announcement item click events.
     */
    public interface OnAnnouncementClickListener {
        void onAnnouncementClick(AnnouncementItem announcement);
    }

    /**
     * ViewHolder class for caching announcement item views.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleView;
        final TextView contentPreview;
        final TextView dateTimeView;
        final TextView editedView;
        final TextView classNameView;
        final View notificationView;

        ViewHolder(View view) {
            super(view);
            titleView = view.findViewById(R.id.announcement_title);
            contentPreview = view.findViewById(R.id.announcement_content_preview);
            dateTimeView = view.findViewById(R.id.announcement_datetime);
            editedView = view.findViewById(R.id.announcement_edited);
            classNameView = view.findViewById(R.id.announcement_classname);
            notificationView = view.findViewById(R.id.new_announcement_indicator_announcement);
        }
    }

    /**
     * Constructs a new adapter instance.
     *
     * @param showClassName Whether to display classroom names (true for all-announcements view)
     * @param listener Callback for handling announcement click events
     */
    public AnnouncementAdapter(boolean showClassName, OnAnnouncementClickListener listener) {
        this.announcements = new ArrayList<>();
        this.showClassName = showClassName;
        this.clickListener = listener;
    }

    /**
     * Creates a new ViewHolder for an announcement item view.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnnouncementItem announcement = announcements.get(position);

        // Set basic announcement information
        holder.titleView.setText(announcement.getTitle());
        holder.contentPreview.setText(announcement.getPreviewContent());
        holder.dateTimeView.setText(announcement.getFormattedDateTime());

        // Handle edited status
        if (announcement.isEdited()) {
            holder.editedView.setVisibility(View.VISIBLE);
            String editedText = "(edited)";
            holder.editedView.setText(editedText);
        } else {
            holder.editedView.setVisibility(View.GONE);
        }

        // Show classroom name if in all-announcements view
        if (showClassName && announcement.getClassroomName() != null) {
            holder.classNameView.setVisibility(View.VISIBLE);
            holder.classNameView.setText(announcement.getClassroomName());
        } else {
            holder.classNameView.setVisibility(View.INVISIBLE);
        }

        if (announcement.isNewAnnouncement()) {
            holder.notificationView.setVisibility(View.VISIBLE);
        } else {
            holder.notificationView.setVisibility(View.INVISIBLE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> clickListener.onAnnouncementClick(announcement));
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    /**
     * Updates the adapter's data set with new announcements.
     * Triggers a full refresh of the RecyclerView.
     *
     * @param newAnnouncements Updated list of announcements to display
     */
    public void updateAnnouncements(List<AnnouncementItem> newAnnouncements) {
        this.announcements.clear();
        this.announcements.addAll(newAnnouncements);
        notifyDataSetChanged();
    }


}
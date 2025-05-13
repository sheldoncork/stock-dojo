package com.softdev.stocksim.ui.classroom.announcements;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Represents an announcement in the classroom system.
 * This class implements Parcelable to allow passing announcement data between Android components.
 * Each announcement contains information about its content, timing, and associated classroom.
 *
 * @author Blake Nelson
 */
public class AnnouncementItem implements Parcelable {

    // Core announcement data
    private final long id;
    private final String title;
    private final String content;
    private final String postDate;
    private final boolean edited;
    private final String editedDate;
    private final String classroomName;
    private final boolean notViewed;

    // Date formatting constants
    private static final SimpleDateFormat API_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

    /**
     * Constructs a new announcement item with all required fields.
     *
     * @param id The unique identifier for the announcement
     * @param title The announcement title
     * @param content The full content of the announcement
     * @param postDate The original posting date (ISO 8601 format)
     * @param edited Whether the announcement has been edited
     * @param editedDate The date of last edit (ISO 8601 format), if edited
     * @param classroomName The name of the associated classroom
     */
    public AnnouncementItem(long id, String title, String content, String postDate,
                            boolean edited, String editedDate, String classroomName, boolean notViewed) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.postDate = postDate;
        this.edited = edited;
        this.editedDate = editedDate;
        this.classroomName = classroomName;
        this.notViewed = notViewed;
    }

    // Getters

    /**
     * Returns the unique identifier for the announcement.
     * @return The ID of the announcement
     */
    public long getId() { return id; }

    /**
     * Returns the title of the announcement.
     * @return The title of the announcement
     */
    public String getTitle() { return title; }

    /**
     * Returns the full content of the announcement.
     * @return The content of the announcement
     */
    public String getContent() { return content; }

    /**
     * Checks if the announcement has been edited.
     * @return True if the announcement has been edited, false otherwise
     */
    public boolean isEdited() { return edited; }

    /**
     * Returns the name of the associated classroom.
     * @return The name of the classroom
     */
    public String getClassroomName() { return classroomName; }

    /**
     * Checks if the announcement is new.
     * @return True if the announcement is new, false otherwise
     */
    public boolean isNewAnnouncement() { return notViewed; }

    /**
     * Formats the post date into a human-readable string.
     * Converts from ISO 8601 format to "MMM d, yyyy at h:mm a" format.
     *
     * @return Formatted date string, or raw date string if parsing fails
     */
    public String getFormattedDateTime() {
        try {
            java.util.Date date = API_FORMAT.parse(postDate);
            if (date != null) {
                return String.format("%s at %s",
                        DATE_FORMAT.format(date),
                        TIME_FORMAT.format(date));
            }
        } catch (Exception e) {
            Log.e("AnnouncementItem", "Error parsing date: " + postDate, e);
        }
        return postDate;
    }

    /**
     * Formats the edited date into a human-readable string if the announcement was edited.
     * Converts from ISO 8601 format to "Edited MMM d, yyyy at h:mm a" format.
     *
     * @return Formatted date string with "Edited" prefix, or null if not edited
     */
    public String getFormattedEditedDateTime() {
        if (!edited || editedDate == null) {
            return null;
        }

        try {
            java.util.Date date = API_FORMAT.parse(editedDate);
            if (date != null) {
                return String.format("Edited %s at %s",
                        DATE_FORMAT.format(date),
                        TIME_FORMAT.format(date));
            }
        } catch (Exception e) {
            Log.e("AnnouncementItem", "Error parsing edited date: " + editedDate, e);
        }
        return "Edited " + editedDate;
    }

    /**
     * Creates a preview of the announcement content by truncating it.
     * Adds ellipsis if content exceeds preview length.
     *
     * @return Truncated content string with ellipsis if needed
     */
    public String getPreviewContent() {
        int previewLength = 100;
        if (content.length() > previewLength) {
            return content.substring(0, previewLength - 3) + "...";
        }
        return content;
    }

    // Parcelable implementation
    protected AnnouncementItem(Parcel in) {
        id = in.readLong();
        title = in.readString();
        content = in.readString();
        postDate = in.readString();
        edited = in.readByte() != 0;
        editedDate = in.readString();
        classroomName = in.readString();
        notViewed = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(postDate);
        dest.writeByte((byte) (edited ? 1 : 0));
        dest.writeString(editedDate);
        dest.writeString(classroomName);
        dest.writeByte((byte) (notViewed ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AnnouncementItem> CREATOR = new Creator<AnnouncementItem>() {
        @Override
        public AnnouncementItem createFromParcel(Parcel in) {
            return new AnnouncementItem(in);
        }

        @Override
        public AnnouncementItem[] newArray(int size) {
            return new AnnouncementItem[size];
        }
    };
}
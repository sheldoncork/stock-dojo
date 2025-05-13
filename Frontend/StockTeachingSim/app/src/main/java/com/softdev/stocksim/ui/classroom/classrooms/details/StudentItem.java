package com.softdev.stocksim.ui.classroom.classrooms.details;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Represents a student in a classroom with their portfolio status.
 * Implements Parcelable for safe data transfer between components.
 *
 * @author Blake Nelson
 */
public class StudentItem implements Parcelable {
    private final String studentName;
    private final String status;
    private final int portfolioId;

    public StudentItem(String studentName, String status, int portfolioId) {
        this.studentName = studentName;
        this.status = status;
        this.portfolioId = portfolioId;
    }

    public String getStudentName() {
        return studentName;
    }
    public String getStatus() {
        return status;
    }
    public int getPortfolioId() { return portfolioId;}

    // Parcelable implementation
    protected StudentItem(Parcel in) {
        studentName = in.readString();
        status = in.readString();
        portfolioId = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(studentName);
        dest.writeString(status);
        dest.writeInt(portfolioId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StudentItem> CREATOR = new Creator<StudentItem>() {
        @Override
        public StudentItem createFromParcel(Parcel in) {
            return new StudentItem(in);
        }

        @Override
        public StudentItem[] newArray(int size) {
            return new StudentItem[size];
        }
    };




}
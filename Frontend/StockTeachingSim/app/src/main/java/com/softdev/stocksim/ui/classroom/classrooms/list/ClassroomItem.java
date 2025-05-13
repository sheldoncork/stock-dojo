package com.softdev.stocksim.ui.classroom.classrooms.list;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Represents a classroom in the application with its associated data.
 * Implements Parcelable for data transfer between components.
 *
 * @author Blake Nelson
 */
public class ClassroomItem implements Parcelable {
    private final String classId;
    private final String className;
    private final String joinCode;
    private final String teacherNameOrNumStudents;  // Teacher name for students, student count for teachers

    public ClassroomItem(String classId, String className, String joinCode, String teacherNameOrNumStudents) {
        this.classId = classId;
        this.className = className;
        this.joinCode = joinCode;
        this.teacherNameOrNumStudents = teacherNameOrNumStudents;
    }

    // Getters
    public String getClassId() {
        return classId;
    }
    public String getClassName() {
        return className;
    }
    public String getJoinCode() {
        return joinCode;
    }
    public String getSubText() {
        return teacherNameOrNumStudents;
    }

    // Parcelable implementation
    protected ClassroomItem(Parcel in) {
        classId = in.readString();
        className = in.readString();
        joinCode = in.readString();
        teacherNameOrNumStudents = in.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(classId);
        dest.writeString(className);
        dest.writeString(joinCode);
        dest.writeString(teacherNameOrNumStudents);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ClassroomItem> CREATOR = new Creator<ClassroomItem>() {
        @Override
        public ClassroomItem createFromParcel(Parcel in) {
            return new ClassroomItem(in);
        }

        @Override
        public ClassroomItem[] newArray(int size) {
            return new ClassroomItem[size];
        }
    };

}

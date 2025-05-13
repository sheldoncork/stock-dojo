package com.softdev.stocksim.ui.classroom.classrooms.details;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.softdev.stocksim.R;

/**
 * RecyclerView adapter for displaying student list items.
 * Uses DiffUtil for efficient list updates.
 *
 * @author Blake Nelson
 */
public class StudentAdapter extends ListAdapter<StudentItem, StudentAdapter.ViewHolder> {
    private final OnStudentClickListener clickListener;

    /**
     * Interface for handling click events on student items.
     */
    public interface OnStudentClickListener {
        void onStudentClick(StudentItem student);
    }

    /**
     * Constructor for the adapter.
     *
     * @param listener The click listener for student items
     */
    public StudentAdapter(OnStudentClickListener listener) {
        super(new StudentDiffCallback());
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_classroom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentItem item = getItem(position);
        holder.bind(item, clickListener);
    }

    /**
     * ViewHolder for student items in the RecyclerView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView portfolioStatusView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.main_text);
            portfolioStatusView = itemView.findViewById(R.id.sub_text);
        }

        public void bind(StudentItem item, OnStudentClickListener listener) {
            nameView.setText(item.getStudentName());
            portfolioStatusView.setText(item.getStatus());

            // Get theme colors using TypedValue
            TypedValue typedValue = new TypedValue();
            Context context = itemView.getContext();
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            int gainColor = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true);
            int lossColor = typedValue.data;

            if (item.getStatus().contains("+")) {
                portfolioStatusView.setTextColor(gainColor);
            } else if (item.getStatus().contains("-")){
                portfolioStatusView.setTextColor(lossColor);
            }

            // Only set click listener if one was provided
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onStudentClick(item));
            } else {
                itemView.setOnClickListener(null); // Remove any existing click listener
                itemView.setClickable(false);      // Make item non-clickable
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    private static class StudentDiffCallback extends DiffUtil.ItemCallback<StudentItem> {
        @Override
        public boolean areItemsTheSame(@NonNull StudentItem oldItem, @NonNull StudentItem newItem) {
            return oldItem.getStudentName().equals(newItem.getStudentName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull StudentItem oldItem, @NonNull StudentItem newItem) {
            return oldItem.getStatus().equals(newItem.getStatus());
        }
    }
}
package com.softdev.stocksim.ui.classroom.classrooms.list;

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
 * RecyclerView adapter for displaying classroom list items.
 * Uses DiffUtil for efficient list updates.
 *
 * @author Blake Nelson
 */
public class ClassroomAdapter extends ListAdapter<ClassroomItem, ClassroomAdapter.ViewHolder> {
    private final OnClassroomClickListener clickListener;

    public interface OnClassroomClickListener {
        void onClassroomClick(ClassroomItem classroom);
    }

    public ClassroomAdapter(OnClassroomClickListener listener) {
        super(new ClassroomDiffCallback());
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
        ClassroomItem item = getItem(position);
        holder.bind(item, clickListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView detailsView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.main_text);
            detailsView = itemView.findViewById(R.id.sub_text);
        }

        public void bind(ClassroomItem item, OnClassroomClickListener listener) {
            nameView.setText(item.getClassName());
            detailsView.setText(item.getSubText());

            itemView.setOnClickListener(v -> listener.onClassroomClick(item));
        }
    }

    private static class ClassroomDiffCallback extends DiffUtil.ItemCallback<ClassroomItem> {
        @Override
        public boolean areItemsTheSame(@NonNull ClassroomItem oldItem, @NonNull ClassroomItem newItem) {
            return oldItem.getClassId().equals(newItem.getClassId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ClassroomItem oldItem, @NonNull ClassroomItem newItem) {
            return oldItem.getClassName().equals(newItem.getClassName()) &&
                    oldItem.getSubText().equals(newItem.getSubText());
        }
    }
}
package com.softdev.stocksim.ui.classroom.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.softdev.stocksim.R;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Adapter for displaying chat messages in a RecyclerView.
 *
 * @author Blake Nelson
 */
public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    protected ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return Objects.equals(oldItem.getMessage(), newItem.getMessage()) &&
                        Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp()) &&
                        Objects.equals(oldItem.getSender(), newItem.getSender());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return Objects.equals(oldItem.getMessage(), newItem.getMessage()) &&
                        Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp()) &&
                        Objects.equals(oldItem.getSender(), newItem.getSender());
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        return message.isSentByCurrentUser() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == VIEW_TYPE_SENT ?
                R.layout.item_chat_message_sent :
                R.layout.item_chat_message_received;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderName;
        private final TextView messageText;
        private final TextView messageTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.sender_name);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
        }

        public void bind(ChatMessage message) {

            // Set text content
            senderName.setText(message.isSentByCurrentUser() ? "You" : message.getSender());
            messageText.setText(message.getMessage());
            messageTime.setText(message.getTimestamp().format(TIME_FORMATTER));

        }
    }
}
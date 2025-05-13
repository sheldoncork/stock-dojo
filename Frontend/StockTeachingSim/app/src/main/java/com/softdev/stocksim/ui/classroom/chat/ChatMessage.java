package com.softdev.stocksim.ui.classroom.chat;

import java.time.LocalTime;

/**
 * Represents a chat message.
 *
 * @author Blake Nelson
 */
public class ChatMessage {
    private final String sender;
    private final String message;
    private final LocalTime timestamp;
    private final boolean isSentByCurrentUser;

    public ChatMessage(String sender, String message, LocalTime timestamp, boolean isSentByCurrentUser) {
    this.sender = sender;
    this.message = message;
    this.timestamp = timestamp;
    this.isSentByCurrentUser = isSentByCurrentUser;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    public boolean isSentByCurrentUser() {
        return isSentByCurrentUser;
    }
}

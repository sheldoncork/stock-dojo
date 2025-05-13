package com.softdev.stocksim.ui.classroom.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.api.WebSocketListener;
import com.softdev.stocksim.api.WebSocketManager;
import com.softdev.stocksim.ui.classroom.BaseClassroomFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.data.UserPreferences;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that handles real-time classroom chat functionality.
 * Manages WebSocket connections for bidirectional communication between users in a classroom.
 *
 * @author Blake Nelson
 */
public class ClassroomChatFragment extends BaseClassroomFragment implements WebSocketListener {
    private static final String TAG = "ClassroomChatFragment";

    // State
    private String classroomId;
    private String classroomName;
    private String username;
    private String password;
    private final List<ChatMessage> messages = new ArrayList<>();

    private TextInputLayout messageEditTextLayout;
    private TextInputEditText messageEditText;
    private ChatAdapter chatAdapter;
    private RecyclerView chatRecyclerView;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classroom_chat, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
        initializeUserData();
    }

    private void parseArguments() {
        ClassroomChatFragmentArgs args = ClassroomChatFragmentArgs.fromBundle(requireArguments());
        classroomId = args.getClassroomId();
        classroomName = args.getClassName();
    }

    private void initializeUserData() {
        UserPreferences userPreferences = UserPreferences.getInstance(requireContext());
        username = userPreferences.getUsername();
        password = userPreferences.getPassword();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();
        fetchChatHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupWebSocket();
    }

    @Override
    public void onPause() {
        super.onPause();
        WebSocketManager.getInstance().disconnectWebSocket();
        WebSocketManager.getInstance().removeWebSocketListener();
    }

    /**
     * Initializes views and sets up click listeners.
     *
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        setToolbarTitle(classroomName + " chat");
        inflateToolbarMenu(R.menu.top_menu);

        // UI Components
        messageEditTextLayout = view.findViewById(R.id.message_input_layout);
        messageEditText = view.findViewById(R.id.message_input);
        ImageButton sendButton = view.findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    /**
     * Sets up the RecyclerView for displaying chat messages.
     */
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        chatRecyclerView = requireView().findViewById(R.id.chat_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);  // Scroll from bottom
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    /**
     * Fetches chat history from the server.
     */
    private void fetchChatHistory() {
        showLoading();
        String url = AppConfig.BASE_URL + "/classroom/chatHistory?classId=" + classroomId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Response: " + response.toString());
                        messages.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject messageObj = response.getJSONObject(i);
                            String sender = messageObj.getString("username");
                            String content = messageObj.getString("content");
                            String timeStr = messageObj.getString("time");

                            // Parse ISO 8601 timestamp and convert to LocalTime
                            LocalTime messageTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME)
                                    .toLocalTime();

                            messages.add(new ChatMessage(
                                    sender,
                                    content,
                                    messageTime,
                                    sender.equals(username)
                            ));
                        }

                        chatAdapter.submitList(new ArrayList<>(messages));
                        chatRecyclerView.scrollToPosition(messages.size() - 1);
                        hideLoading();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing chat history", e);
                        showError("Failed to load chat history");
                        hideLoading();
                    }
                },
                error -> {
                    String errorMessage = "Failed to load chat history";
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        errorMessage = "Classroom not found";
                    }
                    showError(errorMessage);
                    hideLoading();
                });

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Sets up the WebSocket connection for real-time communication.
     */
    private void setupWebSocket() {
        WebSocketManager.getInstance().setWebSocketListener(this);
        String serverUrl = AppConfig.CLASSROOM_CHAT_WEBSOCKET_URL + "?classroomId=" + classroomId + "&username=" + username + "&password=" + password;
        WebSocketManager.getInstance().connectWebSocket(serverUrl);
    }

    /**
     * Sends a message to the classroom chat.
     */
    private void sendMessage() {
        if (messageEditText.getText() == null) {
            messageEditTextLayout.setError("Message cannot be empty");
            messageEditText.requestFocus();
            return;
        }

        String message = messageEditText.getText().toString().trim();

        if (message.isEmpty()) {
            messageEditTextLayout.setError("Message cannot be empty");
            messageEditText.requestFocus();
            return;
        } else {
            messageEditTextLayout.setError(null);
        }

        try {

            // Format the message content with newlines
            String formattedMessage = formatMessageContent(message);

            JSONObject messageObj = new JSONObject();
            messageObj.put("content", formattedMessage);
            messageObj.put("username", username);
            messageObj.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            WebSocketManager.getInstance().sendMessage(messageObj.toString());
            messageEditText.setText("");
            messageEditText.clearFocus();

            Log.d(TAG, "Message sent");
        } catch (Exception e) {
            showError("Failed to send message");
        }
    }

    private String formatMessageContent(String content) {
        StringBuilder formatted = new StringBuilder();
        int length = content.length();
        for (int i = 0; i < length; i++) {
            formatted.append(content.charAt(i));
            if ((i + 1) % 30 == 0 && i < length - 1) {
                formatted.append('\n');
            }
        }
        return formatted.toString();
    }

    @Override
    public void onWebSocketMessage(String message) {
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, ignoring message");
            return;
        }

        requireActivity().runOnUiThread(() -> {
            try {

                // Remove everything before and including the colon and space
                String jsonStr = message.substring(message.indexOf(": ") + 2);
                JSONObject messageObj = new JSONObject(jsonStr);

                String sender = messageObj.getString("username");
                String content = messageObj.getString("content");
                String timeStr = messageObj.getString("time");

                LocalTime messageTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME)
                        .toLocalTime();

                ChatMessage chatMessage = new ChatMessage(
                        sender,
                        content,
                        messageTime,
                        sender.equals(username)
                );

                messages.add(chatMessage);
                chatAdapter.submitList(new ArrayList<>(messages));
                chatRecyclerView.post(() ->
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1));
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing WebSocket message", e);
            }
        });
    }

    @Override
    public void onWebSocketOpen(ServerHandshake handShakeSata) {
        Log.d(TAG, "WebSocket connected");
    }

    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() ->
                showError("Connection closed: " + reason));
    }

    @Override
    public void onWebSocketError(Exception ex) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() ->
                showError("Connection error: " + ex.getMessage()));
    }

}



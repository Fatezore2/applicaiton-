package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatVH> {

    private List<ChatMessage> messages;
    private String currentUserId;

    private static final int TYPE_ME = 0;
    private static final int TYPE_OTHER = 1;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();


    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
            return TYPE_ME;
        } else {
            return TYPE_OTHER;
        }
    }

    @NonNull
    @Override
    public ChatVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_ME) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_left, parent, false);
        }
        return new ChatVH(view);
    }

    public void onBindViewHolder(@NonNull ChatVH holder, int position) {
        ChatMessage msg = messages.get(position);

        // Sender name
        holder.tvSenderName.setText(
                msg.getSenderName() != null && !msg.getSenderName().trim().isEmpty()
                        ? msg.getSenderName() + ":"
                        : "匿名:"
        );

        // Timestamp (null-safe)
        String timeText = "[時間未知]";
        if (msg.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeText = sdf.format(msg.getTimestamp().toDate());
        }
        holder.tvTimestamp.setText(timeText);

        // Content handling (now extensible)
        if (msg.getContent() == null) {
            holder.tvMessage.setText("[無內容]");
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.ivMessage.setVisibility(View.GONE);
            holder.btnPlay.setVisibility(View.GONE);
            return;
        }

        switch (msg.getContent().getType()) {
            case "text":
                holder.tvMessage.setVisibility(View.VISIBLE);
                holder.ivMessage.setVisibility(View.GONE);
                holder.btnPlay.setVisibility(View.GONE);
                holder.tvMessage.setText(((MCText) msg.getContent()).getText());
                break;

            case "image":
                holder.tvMessage.setVisibility(View.GONE);
                holder.ivMessage.setVisibility(View.VISIBLE);
                holder.btnPlay.setVisibility(View.GONE);
                // Assuming you have ivMessage in your item layouts
                ImageUrlLoader.loadImage(((MCImage) msg.getContent()).getImageUrl(), holder.ivMessage);
                break;

            case "voice":
                holder.tvMessage.setVisibility(View.GONE);
                holder.ivMessage.setVisibility(View.GONE);
                holder.btnPlay.setVisibility(View.VISIBLE);
                // Add play logic here later
                // e.g. holder.btnPlay.setOnClickListener(v -> playAudio(((MCVoice) msg.content).getAudioUrl()));
                break;

            default:
                holder.tvMessage.setText("[未知類型]");
                holder.tvMessage.setVisibility(View.VISIBLE);
                holder.ivMessage.setVisibility(View.GONE);
                holder.btnPlay.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatVH extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvMessage, tvTimestamp;
        ImageView ivMessage;
        Button btnPlay;

        ChatVH(View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivMessage = itemView.findViewById(R.id.ivMessage);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}

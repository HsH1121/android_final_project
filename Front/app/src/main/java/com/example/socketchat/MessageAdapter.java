package com.example.socketchat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_OTHER = 0;
    private static final int VIEW_TYPE_ME = 1;

    private final List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        // 보낸 사람이 나인지 여부에 따라 뷰 타입 구분
        if (messages.get(position).isMe()) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ME) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_me, parent, false);
            return new MeViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_other, parent, false);
            return new OtherViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);

        if (holder instanceof MeViewHolder) {
            ((MeViewHolder) holder).tvMessageMe.setText(msg.getContent());
        } else if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).tvMessageOther.setText(msg.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // 내 메시지용 ViewHolder
    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageMe;

        public MeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageMe = itemView.findViewById(R.id.tvMessageMe);
        }
    }

    // 상대 메시지용 ViewHolder
    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageOther;

        public OtherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
        }
    }
}

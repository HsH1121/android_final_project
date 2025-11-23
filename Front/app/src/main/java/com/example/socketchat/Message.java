package com.example.socketchat;

public class Message {
    private String content;
    private boolean isMe;

    public Message(String content, boolean isMe) {
        this.content = content;
        this.isMe = isMe;
    }

    public String getContent() {
        return content;
    }

    public boolean isMe() {
        return isMe;
    }
}

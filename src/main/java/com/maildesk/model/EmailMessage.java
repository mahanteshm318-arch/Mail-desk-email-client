package com.maildesk.model;

public class EmailMessage {
    private int id;
    private String uid;
    private String account;
    private String folder;
    private String subject;
    private String sender;
    private String recipients;
    private String body;
    private String receivedAt;
    private boolean isRead;

    public EmailMessage() {}

    public EmailMessage(String subject, String sender, String body) {
        this.subject = subject;
        this.sender = sender;
        this.body = body;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getReceivedAt() { return receivedAt; }
    public void setReceivedAt(String receivedAt) { this.receivedAt = receivedAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    @Override
    public String toString() {
        return (isRead ? "" : "[NEW] ") + (subject != null ? subject : "(no subject)") + "  —  " + (sender != null ? sender : "");
    }
}

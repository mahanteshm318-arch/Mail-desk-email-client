package com.maildesk.service;

import com.maildesk.model.EmailMessage;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailService {

    private final String email;
    private final String password;
    private final String imapHost;
    private final int imapPort;
    private final String smtpHost;
    private final int smtpPort;

    public MailService(String email, String password) {
        this.email = email;
        this.password = password;
        // Auto-detect host from email domain
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (domain.contains("gmail")) {
            this.imapHost = "imap.gmail.com";
            this.imapPort = 993;
            this.smtpHost = "smtp.gmail.com";
            this.smtpPort = 587;
        } else if (domain.contains("outlook") || domain.contains("hotmail") || domain.contains("live")) {
            this.imapHost = "imap-mail.outlook.com";
            this.imapPort = 993;
            this.smtpHost = "smtp-mail.outlook.com";
            this.smtpPort = 587;
        } else if (domain.contains("yahoo")) {
            this.imapHost = "imap.mail.yahoo.com";
            this.imapPort = 993;
            this.smtpHost = "smtp.mail.yahoo.com";
            this.smtpPort = 465;
        } else {
            this.imapHost = "imap." + domain;
            this.imapPort = 993;
            this.smtpHost = "smtp." + domain;
            this.smtpPort = 587;
        }
    }

    // ── IMAP ─────────────────────────────────────────────────────────────────

    public void testConnection() throws Exception {
        Store store = getStore();
        store.close();
    }

    private Store getStore() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(imapHost, email, password);
        return store;
    }

    public List<EmailMessage> fetchEmails(String folderName, int maxCount) {
        List<EmailMessage> result = new ArrayList<>();
        try {
            Store store = getStore();
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);

            Message[] messages = folder.getMessages();
            int start = Math.max(1, messages.length - maxCount + 1);

            for (int i = messages.length; i >= start; i--) {
                Message msg = messages[i - 1];
                try {
                    EmailMessage em = new EmailMessage();
                    em.setUid(String.valueOf(((UIDFolder) folder).getUID(msg)));
                    em.setAccount(email);
                    em.setFolder(folderName);
                    em.setSubject(msg.getSubject());
                    em.setSender(msg.getFrom() != null ? msg.getFrom()[0].toString() : "");
                    em.setReceivedAt(msg.getSentDate() != null ? msg.getSentDate().toString() : "");
                    em.setRead(msg.isSet(Flags.Flag.SEEN));
                    em.setBody(extractBody(msg));
                    result.add(em);
                } catch (Exception e) {
                    System.out.println("Skipping message: " + e.getMessage());
                }
            }
            folder.close(false);
            store.close();
        } catch (Exception e) {
            System.out.println("IMAP fetch error: " + e.getMessage());
        }
        return result;
    }

    private String extractBody(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String body = extractBody(mp.getBodyPart(i));
                if (body != null) return body;
            }
        }
        return "";
    }

    // ── SMTP ─────────────────────────────────────────────────────────────────

    public void sendEmail(String to, String subject, String body) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(email));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}

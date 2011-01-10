package platform.server;


import platform.base.ByteArray;
import platform.server.logics.EmailActionProperty;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.util.Map;
import java.util.Properties;

public class EmailSender {
    Session mailSession;
    MimeMessage message;
    Multipart mp = new MimeMultipart();
    String emails[];

    public static class AttachmentProperties {
        public String fileName;
        public String attachmentName;
        public EmailActionProperty.Format format;

        public AttachmentProperties(String fileName, String attachmentName, EmailActionProperty.Format format) {
            this.fileName = fileName;
            this.attachmentName = attachmentName;
            this.format = format;
        }
    }

    public EmailSender(String smtpHost, String fromAddress, String... targets) {
        emails = targets;

        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.host", smtpHost);
        mailProps.put("mail.from", fromAddress);
        mailSession = Session.getInstance(mailProps, null);
        
        message = new MimeMessage(mailSession);
    }

    public void setRecipients(String... targets) throws MessagingException {
        InternetAddress dests[] = new InternetAddress[targets.length];
        for (int i = 0; i < targets.length; i++) {
            dests[i] = new InternetAddress(targets[i].trim().toLowerCase());
        }
        message.setRecipients(MimeMessage.RecipientType.TO, dests);
    }

    public void setText(String text) throws MessagingException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setDataHandler((new DataHandler(new HTMLDataSource(text))));
        mp.addBodyPart(textPart);
    }

    static class HTMLDataSource implements DataSource {
        private String html;

        public HTMLDataSource(String htmlString) {
            html = htmlString;
        }

        public InputStream getInputStream() throws IOException {
            if (html == null) throw new IOException("null html");
            return new ByteArrayInputStream(html.getBytes());
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("HTMLDataHandler cannot write HTML");
        }

        public String getContentType() {
            return "text/html; charset=utf-8";
        }

        public String getName() {
            return "dataSource to send text/html";
        }
    }

    private String getMimeType(EmailActionProperty.Format format) {
        switch (format) {
            case PDF: return "application/pdf";
            case DOCX: return "application/msword";
            default: return "text/html";
        }
    }

    public void attachFile(AttachmentProperties props) throws MessagingException, IOException {
        FileDataSource fds = new FileDataSource(props.fileName);
        ByteArrayDataSource dataSource = new ByteArrayDataSource(fds.getInputStream(), getMimeType(props.format));
        attachFile(dataSource, props.attachmentName);
    }

    public void attachFile(byte[] buf, String attachmentName) throws MessagingException {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(buf, getMimeType(EmailActionProperty.Format.PDF));
        attachFile(dataSource, attachmentName);
    }

    private void attachFile(DataSource source, String attachmentName) throws MessagingException {
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.setDataHandler(new DataHandler(source));
        filePart.setFileName(attachmentName);
        mp.addBodyPart(filePart);
    }

    public void sendMail(String subject, Map<ByteArray, String> files, AttachmentProperties... forms) {
        sendMail(subject, null, files, forms);
    }

    public void sendMail(String subject, String htmlFilePath, Map<ByteArray, String> files, AttachmentProperties... forms) {
        try {
            message.setFrom();
            message.setSentDate(new java.util.Date());
            setRecipients(emails);
            message.setSubject(subject, "utf-8");

            String result = "";
            if (htmlFilePath != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(htmlFilePath), "utf-8"));
                while (in.ready()) {
                    String s = in.readLine();
                    result += s;
                }
            } else {
                result = "Вам пришли печатные формы";
            }

            setText(result);
            for (AttachmentProperties formProps : forms) {
                attachFile(formProps);
            }
            for (Map.Entry<ByteArray, String> entry : files.entrySet()) {
                attachFile(entry.getKey().array, entry.getValue());
            }
            message.setContent(mp);

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

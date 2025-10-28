package common;

import java.io.Serializable;
import java.util.Date;

public record Email(String from, String to, String subject, String content, Date sentDate)
        implements Serializable {
    public Email(String from, String to, String subject, String content) {
        this(from, to, subject, content, new Date());
    }

    @Override
    public String toString() {
        return String.format("From: %s\nTo: %s\nSubject: %s\nDate: %s\n\n%s",
                from, to, subject, sentDate, content);
    }
}
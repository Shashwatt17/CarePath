package com.carepath.care;
/** Optional delivery port. No adapter is configured and no email delivery is claimed in Phase 8. */
public interface EmailNotificationProvider {
 boolean available();
 void sendReminder(String recipient, String subject, String message);
}

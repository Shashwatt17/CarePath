package com.carepath.care;
import org.springframework.stereotype.Component;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.scheduling.annotation.Scheduled;import org.slf4j.LoggerFactory;
@Component @ConditionalOnProperty(name="carepath.reminders.enabled",havingValue="true",matchIfMissing=true)
public class ReminderScheduler {
 private final ReminderService reminders;public ReminderScheduler(ReminderService r){reminders=r;}
 @Scheduled(fixedDelayString="${carepath.reminders.poll-ms:30000}") public void poll(){try{for(var id:reminders.due())try{reminders.deliver(id);}catch(RuntimeException e){LoggerFactory.getLogger(getClass()).warn("Reminder delivery deferred");}}catch(RuntimeException e){LoggerFactory.getLogger(getClass()).warn("Reminder poll unavailable");}}
}

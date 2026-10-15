package com.hrgenius.service;

import com.hrgenius.dto.notification.NotificationResponse;
import com.hrgenius.entity.User;
import com.hrgenius.entity.notification.AppNotification;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.NotificationMapper;
import com.hrgenius.repository.NotificationRepository;
import com.hrgenius.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Transactional
    public void sendInAppNotification(User recipient, String title, String message, AppNotification.NotificationType type, String linkUrl) {
        if (recipient == null) return;
        AppNotification notification = new AppNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLinkUrl(linkUrl);
        notificationRepository.save(notification);
    }

    @Async
    public void sendEmailNotification(String toEmail, String subject, String text) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender != null && toEmail != null && !toEmail.isBlank()) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(text);
                message.setFrom("hrgenius-noreply@company.com");
                mailSender.send(message);
                log.info("Email sent successfully to {}", toEmail);
            } else {
                log.info("Mail sender not configured or empty email recipient: {}. Subject: {}", toEmail, subject);
            }
        } catch (Exception ex) {
            log.warn("Failed to send email to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void notifyUser(User recipient, String title, String message, AppNotification.NotificationType type, String linkUrl) {
        sendInAppNotification(recipient, title, message, type, linkUrl);
        if (recipient != null && recipient.getEmail() != null) {
            sendEmailNotification(recipient.getEmail(), title, message);
        }
    }

    public void sendNotification(Long userId, String title, String message, String type) {
        if (userId == null) return;
        userRepository.findById(userId).ifPresent(user -> {
            notifyUser(user, title, message, AppNotification.NotificationType.GENERAL, null);
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String username) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndReadFalse(username);
    }

    @Transactional
    public void markAsRead(Long id, String username) {
        AppNotification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        if (notif.getRecipient() != null && notif.getRecipient().getUsername().equals(username)) {
            notif.setRead(true);
            notif.setReadAt(LocalDateTime.now());
            notificationRepository.save(notif);
        }
    }

    @Transactional
    public void markAllAsRead(String username) {
        List<AppNotification> list = notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
        LocalDateTime now = LocalDateTime.now();
        list.stream().filter(n -> !n.isRead()).forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(list);
    }
}

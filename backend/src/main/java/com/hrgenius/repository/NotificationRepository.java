package com.hrgenius.repository;

import com.hrgenius.entity.notification.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<AppNotification> findByRecipientUsernameOrderByCreatedAtDesc(String username);
    long countByRecipientUsernameAndReadFalse(String username);
}

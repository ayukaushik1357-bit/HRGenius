package com.hrgenius.mapper;

import com.hrgenius.dto.notification.NotificationResponse;
import com.hrgenius.entity.notification.AppNotification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(AppNotification entity) {
        if (entity == null) return null;
        return NotificationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .read(entity.isRead())
                .readAt(entity.getReadAt())
                .linkUrl(entity.getLinkUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

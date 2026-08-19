package com.sportify.booking.dto;

import com.sportify.booking.entity.NotificationEntity;
import java.time.LocalDateTime;

public class NotificationDto {
    public Long id;
    public String title;
    public String message;
    public String type;
    public boolean isRead;
    public Long userId;
    public Long bookingId;
    public LocalDateTime createdAt;

    public NotificationDto() {}

    public NotificationDto(NotificationEntity entity) {
        this.id = entity.id;
        this.title = entity.title;
        this.message = entity.message;
        this.type = entity.type;
        this.isRead = entity.isRead;
        this.userId = entity.userId;
        this.bookingId = entity.bookingId;
        this.createdAt = entity.createdAt;
    }
}

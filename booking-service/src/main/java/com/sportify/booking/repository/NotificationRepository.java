package com.sportify.booking.repository;

import com.sportify.booking.entity.NotificationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<NotificationEntity> {
    
    public long countUnread(Long userId) {
        if (userId == null) {
            return count("isRead", false);
        }
        return count("userId = ?1 and isRead = false", userId);
    }
}

package com.sportify.booking.service;

import com.sportify.booking.dto.NotificationDto;
import com.sportify.booking.entity.NotificationEntity;
import com.sportify.booking.repository.NotificationRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {

    @Inject
    NotificationRepository notificationRepository;

    @Transactional
    public NotificationDto createNotification(String title, String message, String type, Long userId, Long bookingId) {
        NotificationEntity entity = new NotificationEntity();
        entity.title = title;
        entity.message = message;
        entity.type = type;
        entity.userId = userId;
        entity.bookingId = bookingId;
        entity.isRead = false;
        
        notificationRepository.persist(entity);
        return new NotificationDto(entity);
    }

    public Map<String, Object> getNotifications(Long userId, String search, int pageIndex, int pageSize) {
        PanacheQuery<NotificationEntity> query;
        if (userId != null) {
            if (search != null && !search.isBlank()) {
                query = notificationRepository.find("userId = ?1 and (lower(title) like ?2 or lower(message) like ?2)", 
                        Sort.by("createdAt", Sort.Direction.Descending), userId, "%" + search.toLowerCase() + "%");
            } else {
                query = notificationRepository.find("userId = ?1", Sort.by("createdAt", Sort.Direction.Descending), userId);
            }
        } else {
            // For admin broadcast (where userId is null)
            if (search != null && !search.isBlank()) {
                query = notificationRepository.find("lower(title) like ?1 or lower(message) like ?1", 
                        Sort.by("createdAt", Sort.Direction.Descending), "%" + search.toLowerCase() + "%");
            } else {
                query = notificationRepository.findAll(Sort.by("createdAt", Sort.Direction.Descending));
            }
        }

        query.page(Page.of(pageIndex, pageSize));

        List<NotificationDto> items = query.stream()
                .map(NotificationDto::new)
                .collect(Collectors.toList());

        return Map.of(
                "items", items,
                "totalItems", query.count(),
                "totalPages", query.pageCount(),
                "currentPage", pageIndex
        );
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    @Transactional
    public void markAsRead(Long id) {
        NotificationEntity entity = notificationRepository.findById(id);
        if (entity != null) {
            entity.isRead = true;
            notificationRepository.persist(entity);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        if (userId != null) {
            notificationRepository.update("isRead = true where userId = ?1", userId);
        } else {
            notificationRepository.update("isRead = true");
        }
    }
}

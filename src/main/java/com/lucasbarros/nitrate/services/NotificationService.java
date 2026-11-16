package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.NotificationDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private static final int LISTA_MAX = 30;

    @Autowired private NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationDTO> listar(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDescIdDesc(user, PageRequest.of(0, LISTA_MAX))
                .stream().map(NotificationDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public long naoLidas(User user) {
        return notificationRepository.countByUserAndReadAtIsNull(user);
    }

    @Transactional
    public void marcarTodasComoLidas(User user) {
        notificationRepository.marcarTodasComoLidas(user, Instant.now());
    }
}

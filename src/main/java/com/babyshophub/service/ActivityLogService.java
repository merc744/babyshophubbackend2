package com.babyshophub.service;

import com.babyshophub.dto.ActivityResponse;
import com.babyshophub.entity.ActivityLog;
import com.babyshophub.entity.User;
import com.babyshophub.repository.ActivityLogRepository;
import com.babyshophub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ActivityLogService {
    private final ActivityLogRepository activities;
    private final UserRepository users;

    public ActivityLogService(ActivityLogRepository activities, UserRepository users) {
        this.activities = activities;
        this.users = users;
    }

    @Transactional
    public void record(String email, String action, String details) {
        if (email == null)
            return;
        users.findByEmail(email).ifPresent(user -> record(user, action, details));
    }

    @Transactional
    public void record(User user, String action, String details) {
        if (user == null)
            return;
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setAction(action);
        log.setDetails(details);
        activities.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> all() {
        return activities.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> forUser(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return activities.findAllByUserEmailOrderByCreatedAtDesc(user.getEmail()).stream().map(this::response).toList();
    }

    private ActivityResponse response(ActivityLog log) {
        return new ActivityResponse(log.getActivityId(), log.getUser().getEmail(), log.getAction(),
                log.getDetails(), log.getCreatedAt());
    }
}
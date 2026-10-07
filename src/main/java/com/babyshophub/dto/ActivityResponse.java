package com.babyshophub.dto;

import java.time.LocalDateTime;

public record ActivityResponse(Long activityId, String email, String action, String details, LocalDateTime createdAt) {
}
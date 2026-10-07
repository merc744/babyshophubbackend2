package com.babyshophub.dto;

import java.time.LocalDate;

public record ProfileResponse(Long id, String name, String email, String phoneNumber, LocalDate dob, String profileImageUrl) {}

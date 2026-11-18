package com.tms.trainer.api;

/** Body of {@code PATCH /api/v1/trainers/{trainerId}/status}: {@code ACTIVE} or {@code INACTIVE}. */
public record UpdateTrainerStatusRequest(String status) {}

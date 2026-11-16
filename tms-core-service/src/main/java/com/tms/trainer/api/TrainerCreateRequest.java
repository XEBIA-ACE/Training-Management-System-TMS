package com.tms.trainer.api;

import com.tms.trainer.application.TrainerCreateCommand;
import java.util.List;
import java.util.UUID;

/**
 * TrainerCreateRequest body of {@code POST /api/v1/trainers} (US-003). Validated by
 * TrainerCreateValidator after the TRAINER.MANAGE check (FR-US-003-001/002/007).
 *
 * @param trainerType {@code INTERNAL} or {@code EXTERNAL}
 * @param employeeId required for INTERNAL trainers, unique among INTERNAL trainers
 * @param email unique (case-insensitive) among all trainers
 * @param location free-text location; the trainer's timeZone is derived from it
 * @param msaNdaStatus optional MSA/NDA status ({@code PENDING}, {@code SIGNED}, {@code EXPIRED})
 * @param msaNdaReference optional link or identifier of the MSA/NDA document (never its contents)
 */
public record TrainerCreateRequest(
    String trainerType,
    String employeeId,
    String name,
    String email,
    String location,
    String msaNdaStatus,
    String msaNdaReference,
    Boolean weekendAvailable,
    List<UUID> certificationIds) {

  TrainerCreateCommand toCommand() {
    return new TrainerCreateCommand(
        trainerType,
        employeeId,
        name,
        email,
        location,
        msaNdaStatus,
        msaNdaReference,
        weekendAvailable,
        certificationIds);
  }
}

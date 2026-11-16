/**
 * Module boundary (mod-trainer): Trainer profiles (e.g. Trainer, TrainerRepository).
 *
 * <p>US-002 adds the trainer directory ({@code GET /api/trainers}, {@code GET /api/trainers/{id}}):
 * {@code domain} (aggregate, repository, search criteria), {@code application} (TrainerAppService:
 * RBAC, audit, metrics) and {@code api} (REST controller, DTOs, error mapping).
 */
package com.tms.trainer;

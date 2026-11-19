import { useSyncExternalStore } from 'react';
import type { Trainer } from '../api/trainerApi';

/**
 * Client-side trainer cache (FR-US-003-009/010). Trainers created in-session are kept here, with
 * their server-derived `timeZone`, so trainer search/allocation UIs can read them without a reload.
 */
let trainers: readonly Trainer[] = [];
const listeners = new Set<() => void>();

export function addTrainer(trainer: Trainer): void {
  trainers = [trainer, ...trainers.filter((t) => t.id !== trainer.id)];
  listeners.forEach((listener) => listener());
}

export function getTrainers(): readonly Trainer[] {
  return trainers;
}

export function resetTrainerStore(): void {
  trainers = [];
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useTrainers(): readonly Trainer[] {
  return useSyncExternalStore(subscribe, getTrainers);
}

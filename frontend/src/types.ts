/**
 * Shared TypeScript types mirroring the amrutam-api DTOs.
 *
 * Envelope shape for every REST response:
 *   { success: boolean, data: T | null, error: { code, message } | null, traceId: string | null }
 */

export interface ApiErrorBody {
  code: string;
  message: string;
}

export interface Envelope<T> {
  success: boolean;
  data: T | null;
  error: ApiErrorBody | null;
  traceId: string | null;
}

/** Mirrors com.amrutam.api.dto.UserDto — `role` is the bare name (PATIENT / DOCTOR / ADMIN). */
export interface User {
  id: string;
  username: string;
  displayName: string;
  email: string;
  role: 'PATIENT' | 'DOCTOR' | 'ADMIN';
  phone: string | null;
  preferredLanguage: string | null;
}

export type Role = User['role'];

/** Mirrors com.amrutam.common.security.Roles — the ROLE_* authority strings. */
export const ROLE_PREFIX: Record<Role, string> = {
  PATIENT: 'ROLE_PATIENT',
  DOCTOR: 'ROLE_DOCTOR',
  ADMIN: 'ROLE_ADMIN'
};

export interface RegisterPatientInput {
  username: string;
  password: string;
  email: string;
  displayName: string;
  phone?: string;
}

/** Mirrors com.amrutam.api.dto.PatientDto (registration result). */
export interface PatientDto {
  id: string;
  username: string;
  displayName: string;
  email: string;
  phone: string | null;
}

export interface LoginInput {
  username: string;
  password: string;
  mfaCode?: string;
}

/** Mirrors com.amrutam.api.dto.MfaChallengeDto. */
export interface MfaChallenge {
  secret: string | null;
  provisioningUri: string | null;
}

/** Mirrors com.amrutam.api.dto.LoginResponse. */
export interface LoginResult {
  token: string | null;
  mfaChallenge: MfaChallenge | null;
  user: User;
}

export interface CreateDoctorInput {
  username: string;
  password: string;
  displayName: string;
  email?: string;
  specialization: string;
  consultationFee?: number;
  yearsExperience?: number;
  bio?: string;
}

/** Mirrors com.amrutam.api.dto.AvailabilitySlotDto. */
export interface Slot {
  id: string;
  doctorId: string;
  startAt: string;
  endAt: string;
  status: 'AVAILABLE' | 'LOCKED' | 'BOOKED' | 'CANCELLED';
}

/** Mirrors com.amrutam.api.dto.DoctorDto. */
export interface Doctor {
  id: string;
  userId: string;
  name: string;
  specialization: string;
  consultationFee: number | null;
  yearsExperience: number;
  bio: string | null;
  avatarUrl: string | null;
  ratings: number;
  totalRatings: number;
  slots?: Slot[];
}

export interface SlotCreateInput {
  startAt: string;
  endAt: string;
}

/** Mirrors com.amrutam.api.dto.ConsultationDto. */
export interface Consultation {
  id: string;
  patientId: string;
  doctorId: string;
  slotId: string;
  status: 'PENDING_PAYMENT' | 'PAID' | 'COMPLETED' | 'CANCELLED';
  scheduledAt: string;
  completedAt: string | null;
}

export interface BookingInput {
  doctorId: string;
  slotId: string;
  paymentMethod?: string;
  paymentToken?: string;
}

/** Mirrors com.amrutam.api.dto.BookConsultationResponse. */
export interface BookingResult {
  consultationId: string;
  slotId: string;
  paymentId: string;
  status: string;
  replayed: boolean;
}

/** Mirrors com.amrutam.api.dto.PaymentDto. */
export interface Payment {
  id: string;
  consultationId: string;
  amount: number;
  currency: string;
  status: 'PAYING' | 'CAPTURED' | 'FAILED' | 'REFUNDED';
  gatewayRef: string | null;
}

/** Mirrors com.amrutam.api.dto.PrescriptionDto. */
export interface Prescription {
  id: string;
  consultationId: string;
  patientId: string;
  doctorId: string;
  /** Free-text medication list (JSON text on the backend). */
  medications: string | null;
  dosageNotes: string | null;
  createdAt: string;
}

/** Spring Data Page<Content> as serialized by Jackson. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface DateRequest {
  date: string;
}

export interface DateRangeRequest {
  from: string;
  to: string;
}

/** Mirrors com.amrutam.api.dto.DailyConsultationsResponse. */
export interface DailyConsultations {
  date: string;
  total: number;
  perDoctor: Record<string, number>;
}

/** Mirrors com.amrutam.api.dto.RevenueSummaryResponse. */
export interface RevenueSummary {
  totalCaptured: number | null;
  capturedCount: number;
  currency: string;
}

/** Mirrors com.amrutam.api.dto.SystemHealthResponse. */
export interface SystemHealth {
  activeSessions: number;
  pendingAsyncJobs: number;
  deadLetters: number;
}

/** Mirrors com.amrutam.api.dto.ProfileUpdateRequest. */
export interface ProfileUpdate {
  phone?: string;
  dob?: string;
  gender?: string;
  preferredLanguage?: string;
  preferredTimezone?: string;
  bio?: string;
  notes?: string;
}

export interface MfaVerifyInput {
  username: string;
  code: string;
}

// Patient types
export interface Patient {
  patientId: number
  mrn: string
  issuerOfPatientId?: string
  patientName: string
  birthDate: string
  sex: string
  address?: string
  phoneNumbers?: string
  email?: string
  patientState: string
  allergies?: string
  createdAt?: string
  updatedAt?: string
}

// Order types
export interface ImagingServiceRequest {
  orderId: number
  patient: Patient
  placerOrderNumber: string
  fillerOrderNumber?: string
  orderPriority: string
  orderStatus: OrderStatus
  requestingPhysician?: Physician
  reasonForExam?: string
  clinicalInfo?: string
  requestedProcedures?: RequestedProcedure[]
  createdAt?: string
  updatedAt?: string
}

export enum OrderStatus {
  PENDING = 'PENDING',
  SCHEDULED = 'SCHEDULED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELED = 'CANCELED',
}

// Procedure types
export interface RequestedProcedure {
  requestedProcedureId: number
  procedureCode: ProcedureCode
  procedureDescription?: string
  reasonForRequestedProcedure?: string
  scheduledProcedureSteps?: ScheduledProcedureStep[]
}

export interface ScheduledProcedureStep {
  spsId: number
  scheduledStartDateTime: string
  scheduledModality: string
  scheduledStationAETitle?: string
  scheduledPerformingPhysician?: Physician
  spsStatus: SPSStatus
  scheduledDuration?: number
  requestedContrastAgent?: string
}

export enum SPSStatus {
  SCHEDULED = 'SCHEDULED',
  ARRIVED = 'ARRIVED',
  READY = 'READY',
  STARTED = 'STARTED',
  COMPLETED = 'COMPLETED',
  CANCELED = 'CANCELED',
  DISCONTINUED = 'DISCONTINUED',
}

// Report types
export interface Report {
  reportId: number
  scheduledProcedureStep: ScheduledProcedureStep
  reportContent?: string
  reportStatus: ReportStatus
  reportTemplate?: ReportTemplate
  authoringPhysician?: Physician
  verifyingPhysician?: Physician
  signedAt?: string
  verifiedAt?: string
  createdAt?: string
  updatedAt?: string
}

export enum ReportStatus {
  PENDING = 'PENDING',
  DRAFT = 'DRAFT',
  PRELIMINARY = 'PRELIMINARY',
  FINAL = 'FINAL',
  CORRECTED = 'CORRECTED',
  AMENDED = 'AMENDED',
  VERIFIED = 'VERIFIED',
}

// Physician types
export interface Physician {
  physicianId: number
  physicianName: string
  npi?: string
  specialty?: string
  department?: string
  email?: string
  phoneNumber?: string
}

// Procedure Code types
export interface ProcedureCode {
  procedureCodeId: number
  codeValue: string
  codingScheme: string
  codeMeaning: string
  modalityType?: string
  procedureDescription?: string
  estimatedDuration?: number
}

// Report Template types
export interface ReportTemplate {
  templateId: number
  templateName: string
  templateContent: string
  modalityType?: string
  procedureType?: string
}

// Authentication types
export interface User {
  userId: number
  username: string
  email: string
  fullName?: string
  role: UserRole
}

export enum UserRole {
  ADMIN = 'ADMIN',
  RADIOLOGIST = 'RADIOLOGIST',
  PHYSICIAN = 'PHYSICIAN',
  TECHNICIAN = 'TECHNICIAN',
  CLERK = 'CLERK',
}

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  token: string
  refreshToken: string
  user: User
}

// API Response types
export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  error?: ErrorDetails
  timestamp?: string
}

export interface ErrorDetails {
  code: string
  message: string
  details?: string
}

// Pagination types
export interface PageRequest {
  page?: number
  size?: number
  sort?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

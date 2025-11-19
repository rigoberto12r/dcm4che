import { ApiResponse, PageResponse, Patient } from '@/types'
import apiService from './api'

export const patientService = {
  // Get all patients with pagination
  getPatients: async (params?: {
    page?: number
    size?: number
    name?: string
    mrn?: string
    dateOfBirth?: string
    gender?: string
  }): Promise<ApiResponse<PageResponse<Patient>>> => {
    return apiService.get('/v1/patients', { params })
  },

  // Get patient by ID
  getPatientById: async (id: number): Promise<ApiResponse<Patient>> => {
    return apiService.get(`/v1/patients/${id}`)
  },

  // Get patient by MRN
  getPatientByMRN: async (mrn: string, issuer?: string): Promise<ApiResponse<Patient>> => {
    const params = issuer ? { issuer } : undefined
    return apiService.get(`/v1/patients/mrn/${mrn}`, { params })
  },

  // Create new patient
  createPatient: async (patient: Partial<Patient>): Promise<ApiResponse<Patient>> => {
    return apiService.post('/v1/patients', patient)
  },

  // Update patient
  updatePatient: async (id: number, patient: Partial<Patient>): Promise<ApiResponse<Patient>> => {
    return apiService.put(`/v1/patients/${id}`, patient)
  },

  // Delete patient
  deletePatient: async (id: number): Promise<ApiResponse<void>> => {
    return apiService.delete(`/v1/patients/${id}`)
  },

  // Check active visits
  hasActiveVisits: async (id: number): Promise<ApiResponse<boolean>> => {
    return apiService.get(`/v1/patients/${id}/active`)
  },

  // Check pending orders
  hasPendingOrders: async (id: number): Promise<ApiResponse<boolean>> => {
    return apiService.get(`/v1/patients/${id}/pending-orders`)
  },

  // Count total patients
  countPatients: async (): Promise<ApiResponse<number>> => {
    return apiService.get('/v1/patients/count')
  },
}

export default patientService

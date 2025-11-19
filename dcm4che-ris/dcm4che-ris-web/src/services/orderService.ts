import { ApiResponse, ImagingServiceRequest, OrderStatus, PageResponse } from '@/types'
import apiService from './api'

export const orderService = {
  // Get all orders
  getOrders: async (params?: {
    page?: number
    size?: number
    patientId?: number
    status?: OrderStatus
    priority?: string
  }): Promise<ApiResponse<PageResponse<ImagingServiceRequest>>> => {
    return apiService.get('/v1/orders', { params })
  },

  // Get order by ID
  getOrderById: async (id: number): Promise<ApiResponse<ImagingServiceRequest>> => {
    return apiService.get(`/v1/orders/${id}`)
  },

  // Get order by placer number
  getOrderByPlacerNumber: async (placerOrderNumber: string): Promise<ApiResponse<ImagingServiceRequest>> => {
    return apiService.get(`/v1/orders/placer/${placerOrderNumber}`)
  },

  // Create new order
  createOrder: async (order: Partial<ImagingServiceRequest>): Promise<ApiResponse<ImagingServiceRequest>> => {
    return apiService.post('/v1/orders', order)
  },

  // Update order status
  updateOrderStatus: async (id: number, status: OrderStatus): Promise<ApiResponse<ImagingServiceRequest>> => {
    return apiService.patch(`/v1/orders/${id}/status`, null, {
      params: { status },
    })
  },

  // Cancel order
  cancelOrder: async (id: number, reason?: string): Promise<ApiResponse<ImagingServiceRequest>> => {
    return apiService.post(`/v1/orders/${id}/cancel`, null, {
      params: { reason },
    })
  },

  // Get orders by patient
  getOrdersByPatient: async (patientId: number): Promise<ApiResponse<ImagingServiceRequest[]>> => {
    return apiService.get(`/v1/orders/patient/${patientId}`)
  },

  // Get pending orders
  getPendingOrders: async (): Promise<ApiResponse<ImagingServiceRequest[]>> => {
    return apiService.get('/v1/orders/pending')
  },

  // Get urgent orders
  getUrgentOrders: async (): Promise<ApiResponse<ImagingServiceRequest[]>> => {
    return apiService.get('/v1/orders/urgent')
  },
}

export default orderService

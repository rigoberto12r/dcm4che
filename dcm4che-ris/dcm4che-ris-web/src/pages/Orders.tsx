import { useState, useEffect } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  IconButton,
  Grid,
  MenuItem,
  Chip,
  Autocomplete,
} from '@mui/material'
import { DataGrid, GridColDef, GridPaginationModel } from '@mui/x-data-grid'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Refresh as RefreshIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material'
import { orderService } from '@/services/orderService'
import { patientService } from '@/services/patientService'
import { ImagingServiceRequest, OrderStatus, Patient } from '@/types'

const statusColors: Record<OrderStatus, 'default' | 'warning' | 'info' | 'success' | 'error'> = {
  PENDING: 'warning',
  SCHEDULED: 'info',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  CANCELED: 'error',
}

export default function Orders() {
  const [orders, setOrders] = useState<ImagingServiceRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [totalRows, setTotalRows] = useState(0)
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({
    page: 0,
    pageSize: 10,
  })

  // Dialog state
  const [openDialog, setOpenDialog] = useState(false)
  const [editingOrder, setEditingOrder] = useState<ImagingServiceRequest | null>(null)
  const [formData, setFormData] = useState({
    placerOrderNumber: '',
    patientId: null as number | null,
    requestedProcedureId: '',
    procedureCode: '',
    procedureDescription: '',
    modality: 'CR',
    scheduledDateTime: '',
    priority: 'ROUTINE',
    orderingPhysician: '',
    referringPhysician: '',
    reasonForStudy: '',
    accessionNumber: '',
  })

  // Patient search
  const [patientSearch, setPatientSearch] = useState('')
  const [patientOptions, setPatientOptions] = useState<Patient[]>([])
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null)

  useEffect(() => {
    loadOrders()
  }, [paginationModel])

  useEffect(() => {
    if (patientSearch.length > 2) {
      searchPatients()
    }
  }, [patientSearch])

  const loadOrders = async () => {
    try {
      setLoading(true)
      setError(null)

      const response = await orderService.getOrders({
        page: paginationModel.page,
        size: paginationModel.pageSize,
      })

      setOrders(response.data.content)
      setTotalRows(response.data.totalElements)
    } catch (err) {
      setError('Failed to load orders')
      console.error('Load orders error:', err)
    } finally {
      setLoading(false)
    }
  }

  const searchPatients = async () => {
    try {
      const response = await patientService.getPatients({
        name: patientSearch,
        page: 0,
        size: 10,
      })
      setPatientOptions(response.data.content)
    } catch (err) {
      console.error('Search patients error:', err)
    }
  }

  const handleOpenDialog = (order?: ImagingServiceRequest) => {
    if (order) {
      setEditingOrder(order)
      setSelectedPatient(order.patient)
      setFormData({
        placerOrderNumber: order.placerOrderNumber,
        patientId: order.patient.patientId,
        requestedProcedureId: order.requestedProcedureId,
        procedureCode: order.procedureCode || '',
        procedureDescription: order.procedureDescription || '',
        modality: order.modality,
        scheduledDateTime: order.scheduledDateTime || '',
        priority: order.priority || 'ROUTINE',
        orderingPhysician: order.orderingPhysician || '',
        referringPhysician: order.referringPhysician || '',
        reasonForStudy: order.reasonForStudy || '',
        accessionNumber: order.accessionNumber || '',
      })
    } else {
      setEditingOrder(null)
      setSelectedPatient(null)
      setFormData({
        placerOrderNumber: '',
        patientId: null,
        requestedProcedureId: '',
        procedureCode: '',
        procedureDescription: '',
        modality: 'CR',
        scheduledDateTime: '',
        priority: 'ROUTINE',
        orderingPhysician: '',
        referringPhysician: '',
        reasonForStudy: '',
        accessionNumber: '',
      })
    }
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingOrder(null)
  }

  const handleSubmit = async () => {
    try {
      setError(null)
      setSuccess(null)

      if (!formData.patientId) {
        setError('Please select a patient')
        return
      }

      if (editingOrder) {
        await orderService.updateOrder(editingOrder.orderId, formData)
        setSuccess('Order updated successfully')
      } else {
        await orderService.createOrder(formData)
        setSuccess('Order created successfully')
      }

      handleCloseDialog()
      loadOrders()
    } catch (err) {
      setError('Failed to save order')
      console.error('Save order error:', err)
    }
  }

  const handleCancelOrder = async (orderId: number) => {
    if (!confirm('Are you sure you want to cancel this order?')) {
      return
    }

    try {
      await orderService.cancelOrder(orderId, 'Canceled by user')
      setSuccess('Order canceled successfully')
      loadOrders()
    } catch (err) {
      setError('Failed to cancel order')
      console.error('Cancel order error:', err)
    }
  }

  const columns: GridColDef[] = [
    { field: 'placerOrderNumber', headerName: 'Order Number', width: 150 },
    { field: 'accessionNumber', headerName: 'Accession Number', width: 150 },
    {
      field: 'patientName',
      headerName: 'Patient',
      width: 180,
      valueGetter: (params) => params.row.patient?.patientName,
    },
    { field: 'procedureDescription', headerName: 'Procedure', width: 200 },
    { field: 'modality', headerName: 'Modality', width: 100 },
    {
      field: 'orderStatus',
      headerName: 'Status',
      width: 130,
      renderCell: (params) => (
        <Chip
          label={params.value}
          color={statusColors[params.value as OrderStatus]}
          size="small"
        />
      ),
    },
    { field: 'scheduledDateTime', headerName: 'Scheduled', width: 150 },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <Box>
          <IconButton
            size="small"
            onClick={() => handleOpenDialog(params.row as ImagingServiceRequest)}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          {params.row.orderStatus !== 'CANCELED' && params.row.orderStatus !== 'COMPLETED' && (
            <IconButton
              size="small"
              color="error"
              onClick={() => handleCancelOrder(params.row.orderId)}
            >
              <CancelIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
      ),
    },
  ]

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Orders</Typography>
        <Box>
          <IconButton onClick={loadOrders} sx={{ mr: 1 }}>
            <RefreshIcon />
          </IconButton>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            New Order
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      <Card>
        <DataGrid
          rows={orders}
          columns={columns}
          getRowId={(row) => row.orderId}
          rowCount={totalRows}
          loading={loading}
          pageSizeOptions={[5, 10, 25, 50]}
          paginationModel={paginationModel}
          paginationMode="server"
          onPaginationModelChange={setPaginationModel}
          autoHeight
          disableRowSelectionOnClick
        />
      </Card>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingOrder ? 'Edit Order' : 'New Order'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <Autocomplete
                options={patientOptions}
                getOptionLabel={(option) => `${option.patientName} (${option.mrn})`}
                value={selectedPatient}
                onChange={(_, newValue) => {
                  setSelectedPatient(newValue)
                  setFormData({ ...formData, patientId: newValue?.patientId || null })
                }}
                onInputChange={(_, newInputValue) => {
                  setPatientSearch(newInputValue)
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Patient"
                    required
                    placeholder="Search patient by name or MRN"
                  />
                )}
                disabled={!!editingOrder}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Placer Order Number"
                required
                value={formData.placerOrderNumber}
                onChange={(e) => setFormData({ ...formData, placerOrderNumber: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Accession Number"
                value={formData.accessionNumber}
                onChange={(e) => setFormData({ ...formData, accessionNumber: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Requested Procedure ID"
                required
                value={formData.requestedProcedureId}
                onChange={(e) => setFormData({ ...formData, requestedProcedureId: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Procedure Code"
                value={formData.procedureCode}
                onChange={(e) => setFormData({ ...formData, procedureCode: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Procedure Description"
                required
                value={formData.procedureDescription}
                onChange={(e) => setFormData({ ...formData, procedureDescription: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Modality"
                required
                value={formData.modality}
                onChange={(e) => setFormData({ ...formData, modality: e.target.value })}
              >
                <MenuItem value="CR">CR - Computed Radiography</MenuItem>
                <MenuItem value="CT">CT - Computed Tomography</MenuItem>
                <MenuItem value="MR">MR - Magnetic Resonance</MenuItem>
                <MenuItem value="US">US - Ultrasound</MenuItem>
                <MenuItem value="DX">DX - Digital Radiography</MenuItem>
                <MenuItem value="MG">MG - Mammography</MenuItem>
                <MenuItem value="NM">NM - Nuclear Medicine</MenuItem>
                <MenuItem value="PT">PT - Positron Emission Tomography</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Priority"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
              >
                <MenuItem value="STAT">STAT - Immediate</MenuItem>
                <MenuItem value="URGENT">URGENT</MenuItem>
                <MenuItem value="ROUTINE">ROUTINE</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Scheduled Date/Time"
                type="datetime-local"
                InputLabelProps={{ shrink: true }}
                value={formData.scheduledDateTime}
                onChange={(e) => setFormData({ ...formData, scheduledDateTime: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Ordering Physician"
                value={formData.orderingPhysician}
                onChange={(e) => setFormData({ ...formData, orderingPhysician: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Referring Physician"
                value={formData.referringPhysician}
                onChange={(e) => setFormData({ ...formData, referringPhysician: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Reason for Study"
                multiline
                rows={3}
                value={formData.reasonForStudy}
                onChange={(e) => setFormData({ ...formData, reasonForStudy: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {editingOrder ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

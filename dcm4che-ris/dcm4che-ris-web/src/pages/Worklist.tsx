import { useState, useEffect } from 'react'
import {
  Box,
  Card,
  Typography,
  Alert,
  IconButton,
  Chip,
  TextField,
  Grid,
  MenuItem,
} from '@mui/material'
import { DataGrid, GridColDef, GridPaginationModel } from '@mui/x-data-grid'
import { Refresh as RefreshIcon } from '@mui/icons-material'
import { orderService } from '@/services/orderService'
import { ImagingServiceRequest, OrderStatus } from '@/types'

const statusColors: Record<OrderStatus, 'default' | 'warning' | 'info' | 'success' | 'error'> = {
  PENDING: 'warning',
  SCHEDULED: 'info',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  CANCELED: 'error',
}

export default function Worklist() {
  const [procedures, setProcedures] = useState<ImagingServiceRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [totalRows, setTotalRows] = useState(0)
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({
    page: 0,
    pageSize: 10,
  })

  // Filter state
  const [filterModality, setFilterModality] = useState<string>('')
  const [filterDate, setFilterDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [filterStatus, setFilterStatus] = useState<string>('SCHEDULED')

  useEffect(() => {
    loadWorklist()
  }, [paginationModel, filterModality, filterDate, filterStatus])

  const loadWorklist = async () => {
    try {
      setLoading(true)
      setError(null)

      const response = await orderService.getOrders({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        modality: filterModality || undefined,
        status: filterStatus || undefined,
        scheduledDate: filterDate || undefined,
      })

      setProcedures(response.data.content)
      setTotalRows(response.data.totalElements)
    } catch (err) {
      setError('Failed to load worklist')
      console.error('Load worklist error:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleStatusChange = async (orderId: number, newStatus: OrderStatus) => {
    try {
      await orderService.updateOrder(orderId, { orderStatus: newStatus })
      loadWorklist()
    } catch (err) {
      setError('Failed to update status')
      console.error('Update status error:', err)
    }
  }

  const columns: GridColDef[] = [
    {
      field: 'scheduledDateTime',
      headerName: 'Scheduled Time',
      width: 150,
      valueFormatter: (params) => {
        if (!params.value) return '-'
        const date = new Date(params.value)
        return date.toLocaleTimeString('en-US', {
          hour: '2-digit',
          minute: '2-digit',
        })
      },
    },
    { field: 'accessionNumber', headerName: 'Accession #', width: 150 },
    {
      field: 'patientName',
      headerName: 'Patient Name',
      width: 180,
      valueGetter: (params) => params.row.patient?.patientName,
    },
    {
      field: 'patientMRN',
      headerName: 'MRN',
      width: 120,
      valueGetter: (params) => params.row.patient?.mrn,
    },
    {
      field: 'patientSex',
      headerName: 'Sex',
      width: 70,
      valueGetter: (params) => params.row.patient?.sex,
    },
    {
      field: 'patientBirthDate',
      headerName: 'Birth Date',
      width: 120,
      valueGetter: (params) => params.row.patient?.birthDate,
    },
    { field: 'modality', headerName: 'Modality', width: 100 },
    { field: 'procedureDescription', headerName: 'Procedure', width: 220 },
    {
      field: 'priority',
      headerName: 'Priority',
      width: 100,
      renderCell: (params) => {
        const color =
          params.value === 'STAT'
            ? 'error'
            : params.value === 'URGENT'
            ? 'warning'
            : 'default'
        return <Chip label={params.value} color={color} size="small" />
      },
    },
    {
      field: 'orderStatus',
      headerName: 'Status',
      width: 150,
      renderCell: (params) => (
        <TextField
          select
          size="small"
          value={params.value}
          onChange={(e) =>
            handleStatusChange(params.row.orderId, e.target.value as OrderStatus)
          }
          variant="outlined"
          sx={{ minWidth: 130 }}
        >
          <MenuItem value="SCHEDULED">Scheduled</MenuItem>
          <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
          <MenuItem value="COMPLETED">Completed</MenuItem>
        </TextField>
      ),
    },
    { field: 'referringPhysician', headerName: 'Referring MD', width: 150 },
  ]

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Worklist</Typography>
        <IconButton onClick={loadWorklist}>
          <RefreshIcon />
        </IconButton>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card sx={{ mb: 3, p: 2 }}>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              label="Date"
              type="date"
              InputLabelProps={{ shrink: true }}
              value={filterDate}
              onChange={(e) => setFilterDate(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              select
              label="Modality"
              value={filterModality}
              onChange={(e) => setFilterModality(e.target.value)}
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="CR">CR - Computed Radiography</MenuItem>
              <MenuItem value="CT">CT - Computed Tomography</MenuItem>
              <MenuItem value="MR">MR - Magnetic Resonance</MenuItem>
              <MenuItem value="US">US - Ultrasound</MenuItem>
              <MenuItem value="DX">DX - Digital Radiography</MenuItem>
              <MenuItem value="MG">MG - Mammography</MenuItem>
              <MenuItem value="NM">NM - Nuclear Medicine</MenuItem>
              <MenuItem value="PT">PT - PET Scan</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              select
              label="Status"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="PENDING">Pending</MenuItem>
              <MenuItem value="SCHEDULED">Scheduled</MenuItem>
              <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
              <MenuItem value="COMPLETED">Completed</MenuItem>
            </TextField>
          </Grid>
        </Grid>
      </Card>

      <Card>
        <DataGrid
          rows={procedures}
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
          getRowClassName={(params) => {
            if (params.row.priority === 'STAT') return 'priority-stat'
            if (params.row.priority === 'URGENT') return 'priority-urgent'
            return ''
          }}
          sx={{
            '& .priority-stat': {
              backgroundColor: 'error.lighter',
              '&:hover': {
                backgroundColor: 'error.light',
              },
            },
            '& .priority-urgent': {
              backgroundColor: 'warning.lighter',
              '&:hover': {
                backgroundColor: 'warning.light',
              },
            },
          }}
        />
      </Card>

      <Box sx={{ mt: 3 }}>
        <Typography variant="body2" color="text.secondary">
          Total scheduled procedures: {totalRows}
        </Typography>
      </Box>
    </Box>
  )
}

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
} from '@mui/material'
import { DataGrid, GridColDef, GridPaginationModel } from '@mui/x-data-grid'
import { Add as AddIcon, Edit as EditIcon, Refresh as RefreshIcon } from '@mui/icons-material'
import { patientService } from '@/services/patientService'
import { Patient } from '@/types'

export default function Patients() {
  const [patients, setPatients] = useState<Patient[]>([])
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
  const [editingPatient, setEditingPatient] = useState<Patient | null>(null)
  const [formData, setFormData] = useState({
    mrn: '',
    issuerOfPatientId: '',
    patientName: '',
    birthDate: '',
    sex: '',
    address: '',
    city: '',
    state: '',
    zipCode: '',
    country: '',
    phoneNumber: '',
    email: '',
  })

  // Search state
  const [searchName, setSearchName] = useState('')
  const [searchMrn, setSearchMrn] = useState('')

  useEffect(() => {
    loadPatients()
  }, [paginationModel, searchName, searchMrn])

  const loadPatients = async () => {
    try {
      setLoading(true)
      setError(null)

      const response = await patientService.getPatients({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        name: searchName || undefined,
        mrn: searchMrn || undefined,
      })

      setPatients(response.data.content)
      setTotalRows(response.data.totalElements)
    } catch (err) {
      setError('Failed to load patients')
      console.error('Load patients error:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleOpenDialog = (patient?: Patient) => {
    if (patient) {
      setEditingPatient(patient)
      setFormData({
        mrn: patient.mrn,
        issuerOfPatientId: patient.issuerOfPatientId || '',
        patientName: patient.patientName,
        birthDate: patient.birthDate,
        sex: patient.sex,
        address: patient.address || '',
        city: patient.city || '',
        state: patient.state || '',
        zipCode: patient.zipCode || '',
        country: patient.country || '',
        phoneNumber: patient.phoneNumber || '',
        email: patient.email || '',
      })
    } else {
      setEditingPatient(null)
      setFormData({
        mrn: '',
        issuerOfPatientId: '',
        patientName: '',
        birthDate: '',
        sex: '',
        address: '',
        city: '',
        state: '',
        zipCode: '',
        country: '',
        phoneNumber: '',
        email: '',
      })
    }
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingPatient(null)
  }

  const handleSubmit = async () => {
    try {
      setError(null)
      setSuccess(null)

      if (editingPatient) {
        await patientService.updatePatient(editingPatient.patientId, formData)
        setSuccess('Patient updated successfully')
      } else {
        await patientService.createPatient(formData)
        setSuccess('Patient created successfully')
      }

      handleCloseDialog()
      loadPatients()
    } catch (err) {
      setError('Failed to save patient')
      console.error('Save patient error:', err)
    }
  }

  const columns: GridColDef[] = [
    { field: 'mrn', headerName: 'MRN', width: 130 },
    { field: 'patientName', headerName: 'Patient Name', width: 200 },
    { field: 'birthDate', headerName: 'Birth Date', width: 130 },
    { field: 'sex', headerName: 'Sex', width: 80 },
    { field: 'phoneNumber', headerName: 'Phone', width: 150 },
    { field: 'email', headerName: 'Email', width: 200 },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 100,
      sortable: false,
      renderCell: (params) => (
        <IconButton
          size="small"
          onClick={() => handleOpenDialog(params.row as Patient)}
        >
          <EditIcon fontSize="small" />
        </IconButton>
      ),
    },
  ]

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Patients</Typography>
        <Box>
          <IconButton onClick={loadPatients} sx={{ mr: 1 }}>
            <RefreshIcon />
          </IconButton>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            New Patient
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

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Search by Name"
                value={searchName}
                onChange={(e) => setSearchName(e.target.value)}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Search by MRN"
                value={searchMrn}
                onChange={(e) => setSearchMrn(e.target.value)}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataGrid
          rows={patients}
          columns={columns}
          getRowId={(row) => row.patientId}
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
          {editingPatient ? 'Edit Patient' : 'New Patient'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="MRN"
                required
                value={formData.mrn}
                onChange={(e) => setFormData({ ...formData, mrn: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Issuer of Patient ID"
                value={formData.issuerOfPatientId}
                onChange={(e) => setFormData({ ...formData, issuerOfPatientId: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Patient Name"
                required
                value={formData.patientName}
                onChange={(e) => setFormData({ ...formData, patientName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Birth Date"
                type="date"
                required
                InputLabelProps={{ shrink: true }}
                value={formData.birthDate}
                onChange={(e) => setFormData({ ...formData, birthDate: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Sex"
                required
                value={formData.sex}
                onChange={(e) => setFormData({ ...formData, sex: e.target.value })}
              >
                <MenuItem value="M">Male</MenuItem>
                <MenuItem value="F">Female</MenuItem>
                <MenuItem value="O">Other</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Address"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="City"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="State"
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Zip Code"
                value={formData.zipCode}
                onChange={(e) => setFormData({ ...formData, zipCode: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Country"
                value={formData.country}
                onChange={(e) => setFormData({ ...formData, country: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Phone Number"
                value={formData.phoneNumber}
                onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {editingPatient ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

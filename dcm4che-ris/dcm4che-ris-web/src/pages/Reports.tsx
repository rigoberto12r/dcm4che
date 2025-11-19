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
} from '@mui/material'
import { DataGrid, GridColDef, GridPaginationModel } from '@mui/x-data-grid'
import {
  Visibility as ViewIcon,
  Edit as EditIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { apiService } from '@/services/api'

interface RadiologyReport {
  reportId: number
  orderId: number
  accessionNumber: string
  patientName: string
  procedureDescription: string
  reportStatus: 'PRELIMINARY' | 'FINAL' | 'CORRECTED'
  reportText: string
  findings: string
  impression: string
  radiologist: string
  reportDate: string
  verifiedDate?: string
}

const statusColors: Record<string, 'default' | 'warning' | 'success' | 'info'> = {
  PRELIMINARY: 'warning',
  FINAL: 'success',
  CORRECTED: 'info',
}

export default function Reports() {
  const [reports, setReports] = useState<RadiologyReport[]>([])
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
  const [viewMode, setViewMode] = useState(false)
  const [selectedReport, setSelectedReport] = useState<RadiologyReport | null>(null)
  const [formData, setFormData] = useState({
    findings: '',
    impression: '',
    reportText: '',
    reportStatus: 'PRELIMINARY' as 'PRELIMINARY' | 'FINAL' | 'CORRECTED',
    radiologist: '',
  })

  useEffect(() => {
    loadReports()
  }, [paginationModel])

  const loadReports = async () => {
    try {
      setLoading(true)
      setError(null)

      const response = await apiService.get<{
        content: RadiologyReport[]
        totalElements: number
      }>('/v1/reports', {
        params: {
          page: paginationModel.page,
          size: paginationModel.pageSize,
        },
      })

      setReports(response.data.content)
      setTotalRows(response.data.totalElements)
    } catch (err) {
      setError('Failed to load reports')
      console.error('Load reports error:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleOpenDialog = (report: RadiologyReport, isViewMode: boolean = false) => {
    setSelectedReport(report)
    setViewMode(isViewMode)
    setFormData({
      findings: report.findings || '',
      impression: report.impression || '',
      reportText: report.reportText || '',
      reportStatus: report.reportStatus,
      radiologist: report.radiologist || '',
    })
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setSelectedReport(null)
    setViewMode(false)
  }

  const handleSubmit = async () => {
    if (!selectedReport) return

    try {
      setError(null)
      setSuccess(null)

      await apiService.put(`/v1/reports/${selectedReport.reportId}`, formData)
      setSuccess('Report updated successfully')

      handleCloseDialog()
      loadReports()
    } catch (err) {
      setError('Failed to update report')
      console.error('Update report error:', err)
    }
  }

  const handleSignReport = async (reportId: number) => {
    try {
      await apiService.post(`/v1/reports/${reportId}/sign`)
      setSuccess('Report signed and finalized')
      loadReports()
    } catch (err) {
      setError('Failed to sign report')
      console.error('Sign report error:', err)
    }
  }

  const columns: GridColDef[] = [
    { field: 'accessionNumber', headerName: 'Accession #', width: 150 },
    { field: 'patientName', headerName: 'Patient', width: 180 },
    { field: 'procedureDescription', headerName: 'Procedure', width: 200 },
    {
      field: 'reportStatus',
      headerName: 'Status',
      width: 130,
      renderCell: (params) => (
        <Chip
          label={params.value}
          color={statusColors[params.value]}
          size="small"
        />
      ),
    },
    { field: 'radiologist', headerName: 'Radiologist', width: 150 },
    {
      field: 'reportDate',
      headerName: 'Report Date',
      width: 150,
      valueFormatter: (params) => {
        if (!params.value) return '-'
        return new Date(params.value).toLocaleDateString()
      },
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      renderCell: (params) => (
        <Box>
          <IconButton
            size="small"
            onClick={() => handleOpenDialog(params.row as RadiologyReport, true)}
          >
            <ViewIcon fontSize="small" />
          </IconButton>
          {params.row.reportStatus !== 'FINAL' && (
            <IconButton
              size="small"
              onClick={() => handleOpenDialog(params.row as RadiologyReport, false)}
            >
              <EditIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
      ),
    },
  ]

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Radiology Reports</Typography>
        <IconButton onClick={loadReports}>
          <RefreshIcon />
        </IconButton>
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
          rows={reports}
          columns={columns}
          getRowId={(row) => row.reportId}
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
          {viewMode ? 'View Report' : 'Edit Report'}
        </DialogTitle>
        <DialogContent>
          {selectedReport && (
            <Box sx={{ mt: 2 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Patient
                  </Typography>
                  <Typography variant="body1">{selectedReport.patientName}</Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Accession Number
                  </Typography>
                  <Typography variant="body1">{selectedReport.accessionNumber}</Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary">
                    Procedure
                  </Typography>
                  <Typography variant="body1">{selectedReport.procedureDescription}</Typography>
                </Grid>
              </Grid>

              <Box sx={{ mt: 3 }}>
                <TextField
                  fullWidth
                  label="Findings"
                  multiline
                  rows={6}
                  value={formData.findings}
                  onChange={(e) => setFormData({ ...formData, findings: e.target.value })}
                  disabled={viewMode}
                  sx={{ mb: 2 }}
                />

                <TextField
                  fullWidth
                  label="Impression"
                  multiline
                  rows={4}
                  value={formData.impression}
                  onChange={(e) => setFormData({ ...formData, impression: e.target.value })}
                  disabled={viewMode}
                  sx={{ mb: 2 }}
                />

                <TextField
                  fullWidth
                  label="Complete Report Text"
                  multiline
                  rows={4}
                  value={formData.reportText}
                  onChange={(e) => setFormData({ ...formData, reportText: e.target.value })}
                  disabled={viewMode}
                  sx={{ mb: 2 }}
                />

                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Radiologist"
                      value={formData.radiologist}
                      onChange={(e) => setFormData({ ...formData, radiologist: e.target.value })}
                      disabled={viewMode}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      select
                      label="Report Status"
                      value={formData.reportStatus}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          reportStatus: e.target.value as 'PRELIMINARY' | 'FINAL' | 'CORRECTED',
                        })
                      }
                      disabled={viewMode}
                    >
                      <MenuItem value="PRELIMINARY">Preliminary</MenuItem>
                      <MenuItem value="FINAL">Final</MenuItem>
                      <MenuItem value="CORRECTED">Corrected</MenuItem>
                    </TextField>
                  </Grid>
                </Grid>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Close</Button>
          {!viewMode && selectedReport && (
            <>
              <Button onClick={handleSubmit} variant="contained">
                Save
              </Button>
              {selectedReport.reportStatus !== 'FINAL' && (
                <Button
                  onClick={() => {
                    handleSignReport(selectedReport.reportId)
                    handleCloseDialog()
                  }}
                  variant="contained"
                  color="success"
                >
                  Sign & Finalize
                </Button>
              )}
            </>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  )
}

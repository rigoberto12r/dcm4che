import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Container,
  CircularProgress,
} from '@mui/material'
import { LocalHospital as HospitalIcon } from '@mui/icons-material'
import { useAuthStore } from '@/store/authStore'

export default function Login() {
  const navigate = useNavigate()
  const { login, isLoading, error, clearError } = useAuthStore()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    try {
      await login({ username, password })
      navigate('/')
    } catch (err) {
      // Error is handled by the store
    }
  }

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            mb: 3,
          }}
        >
          <HospitalIcon sx={{ fontSize: 48, mr: 2, color: 'primary.main' }} />
          <Typography component="h1" variant="h4">
            dcm4che RIS
          </Typography>
        </Box>

        <Card sx={{ width: '100%' }}>
          <CardContent>
            <Typography component="h2" variant="h5" align="center" gutterBottom>
              Sign In
            </Typography>

            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <Box component="form" onSubmit={handleSubmit} noValidate>
              <TextField
                margin="normal"
                required
                fullWidth
                id="username"
                label="Username"
                name="username"
                autoComplete="username"
                autoFocus
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isLoading}
              />
              <TextField
                margin="normal"
                required
                fullWidth
                name="password"
                label="Password"
                type="password"
                id="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoading}
              />
              <Button
                type="submit"
                fullWidth
                variant="contained"
                sx={{ mt: 3, mb: 2 }}
                disabled={isLoading || !username || !password}
              >
                {isLoading ? <CircularProgress size={24} /> : 'Sign In'}
              </Button>
            </Box>

            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="text.secondary" align="center">
                Radiology Information System
              </Typography>
              <Typography variant="caption" color="text.secondary" align="center" display="block">
                Version 1.0.0
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Container>
  )
}

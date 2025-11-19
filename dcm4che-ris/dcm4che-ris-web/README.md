# dcm4che RIS Web Interface

Modern web interface for the dcm4che Radiology Information System built with React, TypeScript, and Material-UI.

## Features

- **Authentication**: Secure JWT-based authentication with auto-refresh
- **Patient Management**: Full CRUD operations with advanced search
- **Order Management**: Create, update, and cancel imaging orders
- **Worklist**: Real-time modality worklist with filtering
- **Reports**: Radiology report viewing and editing
- **Responsive Design**: Mobile-friendly interface
- **Type Safety**: Full TypeScript coverage

## Technology Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **Material-UI (MUI) v5** - Component library
- **React Router v6** - Client-side routing
- **Zustand** - State management
- **React Query** - Server state management
- **Axios** - HTTP client

## Getting Started

### Prerequisites

- Node.js 18+ and npm

### Development

1. Install dependencies:
```bash
npm install
```

2. Create `.env.local` file:
```bash
cp .env.example .env.local
```

3. Update the API base URL in `.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080/api
```

4. Start development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### Production Build

```bash
npm run build
```

The build output will be in the `dist` directory.

### Preview Production Build

```bash
npm run preview
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t dcm4che-ris-web .
```

### Run Container

```bash
docker run -p 3000:80 dcm4che-ris-web
```

The application will be available at `http://localhost:3000`

### Docker Compose

The web interface is included in the main docker-compose.yml:

```bash
# From the root of the project
docker-compose up -d ris-web
```

## Project Structure

```
dcm4che-ris-web/
├── src/
│   ├── components/         # Reusable components
│   │   └── Layout.tsx     # Main layout with sidebar
│   ├── pages/             # Page components
│   │   ├── Login.tsx      # Authentication page
│   │   ├── Dashboard.tsx  # Dashboard with statistics
│   │   ├── Patients.tsx   # Patient management
│   │   ├── Orders.tsx     # Order management
│   │   ├── Worklist.tsx   # Modality worklist
│   │   └── Reports.tsx    # Radiology reports
│   ├── services/          # API services
│   │   ├── api.ts         # Axios instance with interceptors
│   │   ├── patientService.ts
│   │   └── orderService.ts
│   ├── store/             # State management
│   │   └── authStore.ts   # Authentication store
│   ├── types/             # TypeScript types
│   │   └── index.ts       # Type definitions
│   ├── App.tsx            # Root component with routing
│   ├── main.tsx           # Application entry point
│   └── theme.ts           # Material-UI theme
├── public/                # Static assets
├── Dockerfile             # Multi-stage Docker build
├── nginx.conf             # Nginx configuration
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## API Integration

The web interface communicates with the RIS backend through the following endpoints:

- **Authentication**: `/v1/auth/login`, `/v1/auth/refresh`
- **Patients**: `/v1/patients`
- **Orders**: `/v1/orders`
- **Reports**: `/v1/reports`
- **Dashboard**: `/v1/dashboard/stats`

All API requests include JWT authentication tokens in the Authorization header.

## Environment Variables

- `VITE_API_BASE_URL` - Backend API base URL (default: `/api` for production)
- `VITE_ENV` - Environment (development/production)

## Features Detail

### Dashboard
- Total patients count
- Pending orders
- Scheduled procedures for today
- Completed procedures
- Reports generated
- System status indicators

### Patient Management
- Search by name or MRN
- Create new patients
- Edit patient demographics
- Paginated table view
- Full demographic information

### Order Management
- Create imaging orders
- Patient autocomplete search
- Modality selection (CR, CT, MR, US, DX, MG, NM, PT)
- Priority levels (STAT, URGENT, ROUTINE)
- Schedule procedures
- Cancel orders
- Status tracking

### Worklist
- Filter by modality, date, and status
- Real-time status updates
- Priority highlighting (STAT = red, URGENT = yellow)
- Quick status changes
- Patient demographics
- Procedure details

### Reports
- View radiology reports
- Edit preliminary reports
- Sign and finalize reports
- Findings and impression sections
- Report status tracking

## Security

- JWT-based authentication
- Automatic token refresh
- Secure HTTP-only cookies (recommended)
- XSS protection headers
- CSRF protection
- Content Security Policy

## Performance

- Code splitting with React lazy loading
- Optimized bundle size
- Gzip compression
- Static asset caching
- Server-side pagination
- Virtualized lists for large datasets

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## License

This project is part of dcm4che RIS and follows the same license.

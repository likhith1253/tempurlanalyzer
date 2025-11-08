# SentinelAI Guard - Deployment Guide

This guide provides instructions for deploying SentinelAI Guard using Docker and Docker Compose.

## Prerequisites

- Docker (version 20.10.0 or higher)
- Docker Compose (version 1.29.0 or higher)
- (Optional) Vercel or Firebase account for frontend deployment

## Environment Setup

1. Copy the example environment file and update with your values:
   ```bash
   cp .env.example .env
   ```
2. Update the `.env` file with your actual configuration values.

## Building and Running with Docker Compose

1. Start all services:
   ```bash
   docker-compose up -d
   ```

2. View logs:
   ```bash
   docker-compose logs -f
   ```

3. Stop services:
   ```bash
   docker-compose down
   ```

## Services

- **Backend**: http://localhost:8080
- **Proxy**: http://localhost:8081
- **Database**: PostgreSQL on port 5432 (internal to Docker network)

## Frontend Deployment

### Option 1: Vercel

1. Push your code to a GitHub/GitLab repository
2. Import the project in Vercel
3. Set up environment variables in Vercel's project settings
4. Deploy

### Option 2: Firebase Hosting

1. Install Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```
2. Log in to Firebase:
   ```bash
   firebase login
   ```
3. Initialize Firebase in the frontend directory:
   ```bash
   cd sentinelai-dashboard
   firebase init
   ```
4. Deploy:
   ```bash
   firebase deploy
   ```

## Environment Variables

See `.env.example` for all available configuration options.

## Production Considerations

1. **Database Persistence**: The database is persisted in a Docker volume. For production, consider:
   - Using a managed database service
   - Setting up regular backups

2. **HTTPS**: Set up a reverse proxy like Nginx with Let's Encrypt for HTTPS

3. **Scaling**: Adjust resource limits in `docker-compose.yml` for production workloads

4. **Monitoring**: Consider adding monitoring tools like Prometheus and Grafana

## Troubleshooting

- Check container logs: `docker logs <container_name>`
- Access database: `docker exec -it sentinel-db psql -U postgres`
- Rebuild containers: `docker-compose up -d --build`

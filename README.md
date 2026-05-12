# Proyecto Primer Parcial de Software Seguro

## Descripción del Proyecto

Sistema de **Galería de Imágenes Segura** con gestión de usuarios, upload/escaneo de imágenes, panel supervisor y auditoría completa.

## 🛡️ **Características de Seguridad Implementadas**

| # | Característica | Implementación |
|---|---------------|----------------|
| 1 | **Autenticación JWT** | Tokens JWT con refresh, CustomUserDetailsService |
| 2 | **RBAC (Roles)** | USER (upload/view), SUPERVISOR (quarantine/audit) |
| 3 | **Rate Limiting** | 5 login attempts/IP (LoginAttemptService) |
| 4 | **XSS Prevention** | OWASP Java HTML Sanitizer + XssSanitizer util |
| 5 | **Malware Scan** | ClamAV Docker + EXIF metadata check (metadata-extractor) |
| 6 | **Auditoría** | AuditLog entity/repo/service para todas actions |
| 7 | **CSRF** | Custom CsrfCookieFilter con double-submit cookie |
| 8 | **File Storage** | UUID rename, safe/quarantine dirs, 50MB limit |
| 9 | **Input Validation** | @Valid DTOs, MIME types, size checks |

**Adicional:**
- BouncyCastle para crypto extras
- PostgreSQL con Hibernate DDL-auto=update

## 🏗️ **Arquitectura**

```
Proyecto (Backend Spring Boot)
├── Controllers: Auth, Images, Albums, Supervisor
├── Services: Audit, FileStorage, LoginAttempts
├── Entities: User, Image, Album, AuditLog
├── Security: JWT, CustomUserDetails, CSRF Filter
└── Storage: uploads/{safe|quarantine}/
    └── Frontend React/Vite
```

## 📁 **Estructura del Proyecto**

```
Proyecto/
├── README.md
├── pom.xml (Spring Boot 4.0.6)
├── src/main/java/... (Controllers, Services, Entities)
├── src/main/resources/application.yaml
├── frontend/ (React + Vite)
├── uploads/
│   ├── safe/ (imágenes aprobadas)
│   └── quarantine/ (SUSPECT_*.jpg)
└── Dockerfile
```

## 🌐 **Frontend (React + Vite)**

El frontend es una **SPA** (Single Page Application) que consume el backend mediante HTTP.

- **Framework**: React 18 + Vite
- **Router**: `react-router-dom`
- **Páginas**: Home, Album, Login/Register, UserPanel, SupervisorPanel, ProtectedImage
- **Componentes clave**: `ImageCard`, `ConfirmModal`, `LoadingSpinner`, `SkeletonLoader`, etc.
- **Comunicación con API**: `frontend/src/lib/api.js`
  - Usa el proxy de Vite para que las rutas `/api` apunten al backend (en dev: `http://localhost:8080`).

### Ejecutar frontend (dev)
```bash
cd frontend
npm install
npm run dev
```
- URL: **http://localhost:3001**

### Build frontend
```bash
cd frontend
npm run build
```
Se genera `dist/`.

## ⚙️ **Backend (Spring Boot)**

- **Framework**: Spring Boot 4.0.6
- **Puertos**:
  - **Dev**: `8080`
  - **Docker**: `8081` (según `Dockerfile`)
- **Key Modules**:
  | Module | Description |
  |--------|-------------|
  | Controllers | AuthController, ImageController, AlbumController, SupervisorController |
  | Services | AuditService, FileStorageService, LoginAttemptService |
  | Entities/DTOs | User (Role), Image (ImageStatus), Album, AuditLog, request/response DTOs |
  | Config | SecurityConfig, CsrfCookieFilter |
  | Utils | XssSanitizer |
- **Database**: PostgreSQL

## ⚙️ **Configuraciones Clave**


### application.yaml
> Ajusta los valores entre paréntesis antes de ejecutar.

```yaml
spring:
  application:
    name: Proyecto-Primer-Parcial-de-Software-Seguro

  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

  datasource:
    url: jdbc:postgresql://(host):(port)/(db_name)
    username: (user_database)
    password: (password_database)

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 8080
```


### ClamAV
```bash
docker run -d --name clamav -p 3310:3310 clamav/clamav:latest clamd
```

## 🛠️ **Instalación y Ejecución**

### Prerrequisitos
| Requisito | Versión | Instalación |
|-----------|---------|-------------|
| Java | 21 (Docker) / recomendado 21 | `winget install OpenJDK.21` (Win) / `apt install openjdk-21-jdk` (Linux) |
| Node.js | 20+ | `winget install NodeJS` / `apt install nodejs npm` |
| PostgreSQL | 14+ | Docker o local |
| Docker | - | Para ClamAV |
| Maven | - | mvnw incluido |

1. **DB Setup**:
   ```bash
   createdb (name)  # o CREATE DATABASE
   ```

2. **ClamAV**:
   ```bash
   docker run -d --name clamav -p 3310:3310 clamav/clamav:latest clamd
   ```

3. **Backend** (port 8080):
   ```bash
   ./mvnw clean spring-boot:run
   ```

4. **Frontend** (port 3001):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

**URLs**:
- Backend: http://localhost:8080
- Frontend: http://localhost:3001
- API proxy automático

## 🔐 **Usuarios de Prueba**
| Rol | Email | Password |
|-----|-------|----------|
| User | user@test.com | password123 |
| Supervisor | supervisor@test.com | Supervisor123_ |

## 🧪 **Panel Supervisor**
- Imágenes en cuarentena
- Auditoría logs
- Stats de seguridad
- IPs bloqueadas

## 🚀 **Producción**
```bash
# Backend Docker
docker build -t proyecto-seguro .
docker run -p 8081:8081 -e DB_PASSWORD=newpass proyecto-seguro

# Frontend build + serve static en backend
```

## 📄 **Documentación Adicional**
- [PLAN_ESTRATEGICO_SEGURIDAD.md](PLAN_ESTRATEGICO_SEGURIDAD.md)
- [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md)
- [HELP.md](HELP.md) (nota sobre el package name usado en el proyecto)


---
**ESPE - Software Seguro 2024**


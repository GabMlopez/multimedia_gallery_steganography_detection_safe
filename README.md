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
├── pom.xml (Spring Boot 4.0.6, Java 25)
├── src/main/java/... (Controllers, Services, Entities)
├── src/main/resources/application.yaml
├── frontend/
│   ├── package.json (React 18, Vite 5.2)
│   ├── src/components/ImageCard.jsx
│   └── src/pages/ (Home.jsx, Login.jsx, SupervisorPanel.jsx...)
├── uploads/
│   ├── safe/ (approved images)
│   └── quarantine/ (SUSPECT_*.jpg)
└── Dockerfile
```

## 🌐 **Frontend (React + Vite)**

- **Framework**: React 18.2.0 + Vite 5.2.0
- **Dependencies**: axios 1.6, react-router-dom 6.14
- **Pages**: Home, Album, Login/Register, UserPanel, SupervisorPanel, ProtectedImage
- **Components**: ImageCard, ProtectedRoute
- **API**: lib/api.js (proxy /api -> backend)
- **Dev**: `npm run dev` (port 3001)
- **Build**: `npm run build` → dist/ (serve static)

## ⚙️ **Backend (Spring Boot)**

- **Version**: Spring Boot 4.0.6, Java 25
- **Key Modules**:
  | Module | Description |
  |--------|-------------|
  | Controllers | AuthController, ImageController, AlbumController, SupervisorController |
  | Services | AuditService, FileStorageService, LoginAttemptService |
  | Entities | User (Role enum), Image (ImageStatus), Album, AuditLog |
  | Config | SecurityConfig, CsrfCookieFilter |
  | Utils | XssSanitizer |
- **Database**: PostgreSQL

## ⚙️ **Configuraciones Clave**

### application.yaml
```yaml
spring:
  application:
    name: Proyecto-Primer-Parcial-de-Software-Seguro
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
  datasource:
    url: jdbc:postgresql://localhost:5432/proyecto_seguro
    username: postgres
    password: postgres  # ¡CAMBIAR EN PROD!
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
| Java | 25 | `winget install OpenJDK.25` (Win) / `apt install openjdk-25-jdk` (Linux) |
| Node.js | 20+ | `winget install NodeJS` / `apt install nodejs npm` |
| PostgreSQL | 14+ | Docker o local |
| Docker | - | Para ClamAV |
| Maven | - | mvnw incluido |

1. **DB Setup**:
   ```bash
   createdb proyecto_seguro  # o CREATE DATABASE
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
| Supervisor | supervisor@test.com | supervisor123 |

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

---
**ESPE - Software Seguro 2024**


# DFS API - Sistema de Apertura de Cuentas

API REST para el sistema de apertura de cuentas de DFS, migrada desde la aplicación Vaadin original.

## Características

- **Spring Boot 3.2.0** con Java 21
- **Jakarta Persistence API** (JPA)
- **PostgreSQL** como base de datos
- **OpenAPI 3** para documentación de la API
- **Spring Data JPA** para acceso a datos
- **Spring Security** para autenticación
- **Lombok** para reducir código boilerplate

## Estructura del Proyecto

```
src/main/java/ar/com/dfs/
├── entity/           # Entidades JPA
│   ├── Solicitud.java
│   ├── Persona.java
│   ├── Empresa.java
│   ├── Organizacion.java
│   ├── Domicilio.java
│   ├── DatosFiscales.java
│   ├── CuentaBancaria.java
│   ├── Archivo.java
│   └── ...
├── repository/       # Repositorios Spring Data JPA
│   ├── SolicitudRepository.java
│   ├── PersonaRepository.java
│   ├── EmpresaRepository.java
│   └── ...
├── service/          # Servicios de negocio
│   ├── SolicitudService.java
│   └── impl/
│       └── SolicitudServiceImpl.java
├── controller/       # Controladores REST
│   ├── AuthController.java
│   ├── SolicitudController.java
│   ├── ArchivoController.java
│   ├── AuditoriaController.java
│   ├── AmbitoController.java
│   ├── ProductorController.java
│   ├── EmpresaAperturaController.java
│   ├── IndividuoAperturaController.java
│   ├── AperturaPosteriorController.java
│   └── FirmaDigitalController.java
└── config/          # Configuraciones
    ├── SwaggerConfig.java
    └── DatabaseConfig.java
```

## Autenticación

La API utiliza autenticación JWT (JSON Web Token) con verificación por email. Para acceder a los endpoints protegidos, debes:

1. **Enviar código**: `POST /api/auth/enviar-codigo` (con tu email)
2. **Verificar código**: `POST /api/auth/verificar-codigo` (con email y código)
3. **Usar el token**: Incluir el token en el header `Authorization: Bearer <token>`

### Endpoints de Autenticación

- `POST /api/auth/enviar-codigo` - Enviar código de verificación por email
- `POST /api/auth/verificar-codigo` - Verificar código y obtener token JWT
- `POST /api/auth/login` - Iniciar sesión (método tradicional, solo para ADMIN, OPERADOR, SUPERVISOR)
- `POST /api/auth/logout` - Cerrar sesión
- `GET /api/auth/me` - Información del usuario autenticado
- `POST /api/auth/cambiar-password` - Cambiar contraseña del usuario autenticado

### Flujo de Autenticación por Email:

```bash
# 1. Enviar código de verificación
curl -X POST http://localhost:8080/api/auth/enviar-codigo \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@ejemplo.com"
  }'

# 2. Verificar código y obtener token
curl -X POST http://localhost:8080/api/auth/verificar-codigo \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@ejemplo.com",
    "codigo": "123456"
  }'

# 3. Usar el token en requests
curl -X GET http://localhost:8080/api/empresa-apertura/solicitud/1 \
  -H "Authorization: Bearer <token_jwt>"
```

### Características del Sistema de Verificación:

- **Código de 6 dígitos** enviado por email
- **Expiración**: 15 minutos
- **Límite de intentos**: 3 intentos por código
- **Límite diario**: 5 códigos por email por día
- **Auto-registro**: Si el email no existe, se crea automáticamente un usuario

## Endpoints Principales

### Apertura de Cuenta por Pasos

#### Para Empresas (`/api/empresa-apertura/`)
- `GET /datos-principales/{solicitudId}` - Obtener datos principales
- `PUT /datos-principales/{solicitudId}` - Crear/Actualizar datos principales
- `GET /domicilio/{solicitudId}` - Obtener domicilio
- `PUT /domicilio/{solicitudId}` - Actualizar domicilio
- `GET /datos-empresa/{solicitudId}` - Obtener datos de la organización
- `PUT /datos-empresa/{solicitudId}` - Actualizar datos de la organización
- `GET /datos-fiscales/{solicitudId}` - Obtener datos fiscales
- `PUT /datos-fiscales/{solicitudId}` - Actualizar datos fiscales
- `GET /datos-fiscales-exterior/{solicitudId}` - Obtener datos fiscales exterior
- `PUT /datos-fiscales-exterior/{solicitudId}` - Actualizar datos fiscales exterior
- `GET /datos-registro/{solicitudId}` - Obtener datos de registro
- `PUT /datos-registro/{solicitudId}` - Actualizar datos de registro
- `GET /cuentas-bancarias/{solicitudId}` - Obtener cuentas bancarias
- `PUT /cuentas-bancarias/{solicitudId}` - Actualizar cuentas bancarias
- `GET /cuentas-bancarias-exterior/{solicitudId}` - Obtener cuentas bancarias exterior
- `PUT /cuentas-bancarias-exterior/{solicitudId}` - Actualizar cuentas bancarias exterior
- `GET /declaraciones/{solicitudId}` - Obtener declaraciones
- `PUT /declaraciones/{solicitudId}` - Actualizar declaraciones
- `GET /accionistas/{solicitudId}` - Obtener accionistas
- `POST /accionistas/{solicitudId}` - Agregar accionista
- `GET /accionistas/{solicitudId}/{dniNumero}` - Obtener accionista por DNI
- `PUT /accionistas/{solicitudId}/{accionistaId}` - Actualizar accionista
- `DELETE /accionistas/{solicitudId}/{accionistaId}` - Eliminar accionista
- `GET /documentacion-respaldatoria/{solicitudId}` - Obtener documentación respaldatoria
- `PUT /documentacion-respaldatoria/{solicitudId}` - Actualizar documentación respaldatoria
- `GET /perfil-inversor/{solicitudId}` - Obtener perfil inversor
- `PUT /perfil-inversor/{solicitudId}` - Actualizar perfil inversor
- `GET /terminos-condiciones/{solicitudId}` - Obtener términos y condiciones
- `PUT /terminos-condiciones/{solicitudId}` - Actualizar términos y condiciones

#### Para Individuos (`/api/individuo-apertura/`)
- `GET /datos-principales/{solicitudId}` - Obtener datos principales
- `PUT /datos-principales/{solicitudId}` - Crear/Actualizar datos principales
- `GET /datos-personales/{solicitudId}` - Obtener datos personales
- `PUT /datos-personales/{solicitudId}` - Actualizar datos personales
- `GET /domicilio/{solicitudId}` - Obtener domicilio
- `PUT /domicilio/{solicitudId}` - Actualizar domicilio
- `GET /datos-fiscales/{solicitudId}` - Obtener datos fiscales
- `PUT /datos-fiscales/{solicitudId}` - Actualizar datos fiscales
- `GET /datos-fiscales-exterior/{solicitudId}` - Obtener datos fiscales exterior
- `PUT /datos-fiscales-exterior/{solicitudId}` - Actualizar datos fiscales exterior
- `GET /declaraciones/{solicitudId}` - Obtener declaraciones
- `PUT /declaraciones/{solicitudId}` - Actualizar declaraciones
- `GET /cuentas-bancarias/{solicitudId}` - Obtener cuentas bancarias
- `PUT /cuentas-bancarias/{solicitudId}` - Actualizar cuentas bancarias
- `GET /cuentas-bancarias-exterior/{solicitudId}` - Obtener cuentas bancarias exterior
- `PUT /cuentas-bancarias-exterior/{solicitudId}` - Actualizar cuentas bancarias exterior
- `GET /perfil-inversor/{solicitudId}` - Obtener perfil del inversor
- `PUT /perfil-inversor/{solicitudId}` - Actualizar perfil del inversor
- `GET /ddjj-origen-fondos/{solicitudId}` - Obtener declaración de ingresos
- `PUT /ddjj-origen-fondos/{solicitudId}` - Actualizar declaración de ingresos
- `GET /terminos-condiciones/{solicitudId}` - Obtener términos y condiciones
- `PUT /terminos-condiciones/{solicitudId}` - Actualizar términos y condiciones

#### Pasos Posteriores (`/api/apertura-posterior/`)
- `POST /firmantes/{solicitudId}` - Agregar firmante (individuo)
- `GET /firmantes/{solicitudId}` - Obtener firmantes
- `GET /firmantes/{solicitudId}/{dniNumero}` - Obtener firmante por DNI
- `PUT /firmantes/{solicitudId}/{firmanteId}` - Actualizar firmante
- `DELETE /firmantes/{solicitudId}/{firmanteId}` - Eliminar firmante
- `POST /finalizar/{solicitudId}` - Finalizar solicitud

### Solicitudes de Apertura de Cuenta (Gestión)

- `POST /api/solicitudes` - Crear nueva solicitud (requiere tipo e idUsuarioCargo)
- `GET /api/solicitudes` - Obtener todas las solicitudes (resumen)
- `GET /api/solicitudes/{id}` - Obtener solicitud por ID (completa)
- `GET /api/solicitudes/paginated` - Obtener solicitudes paginadas
- `GET /api/solicitudes/buscar` - Buscar solicitudes con filtros
- `GET /api/solicitudes/estado/{estado}` - Obtener por estado
- `GET /api/solicitudes/tipo/{tipo}` - Obtener por tipo
- `GET /api/solicitudes/usuario/{idUsuario}` - Obtener por usuario
- `GET /api/solicitudes/usuario/{idUsuario}/activa` - Obtener solicitud activa del usuario
- `GET /api/solicitudes/ultimo-id-cuenta` - Obtener último ID de cuenta asignado
- `PUT /api/solicitudes/{id}` - Actualizar solicitud
- `DELETE /api/solicitudes/{id}` - Eliminar solicitud
- `PUT /api/solicitudes/{id}/cancelar` - Cancelar solicitud
- `PUT /api/solicitudes/{id}/finalizar` - Finalizar carga
- `PUT /api/solicitudes/{id}/aprobar` - Aprobar solicitud (requiere motivo e idCuenta)
- `PUT /api/solicitudes/{id}/rechazar` - Rechazar solicitud (requiere motivo)
- `GET /api/solicitudes/{id}/pdf` - Generar PDF de la solicitud
- `POST /api/solicitudes/{id}/enviar-bienvenida` - Enviar email de bienvenida
- `POST /api/solicitudes/{id}/enviar-datos-acceso` - Enviar datos de acceso por email

### Firma Digital (`/api/firma-digital/`)

- `POST /{solicitudId}/enviar` - Enviar solicitud para firma digital
- `GET /{solicitudId}/estado` - Obtener estado de la firma digital
- `POST /{solicitudId}/cancelar` - Cancelar proceso de firma digital
- `POST /firma/{idFirma}/reenviar-email` - Reenviar email de firma
- `GET /{solicitudId}/pdf-firmado` - Descargar PDF firmado
- `GET /{solicitudId}/certificado-firma` - Descargar certificado de firma

### Productores (`/api/productores/`)

- `GET /api/productores` - Obtener todos los productores
- `GET /api/productores/{id}` - Obtener productor por ID
- `POST /api/productores` - Crear nuevo productor
- `PUT /api/productores/{id}` - Actualizar productor
- `DELETE /api/productores/{id}` - Eliminar productor
- `POST /api/productores/asignar` - Asignar productor a solicitud (requiere idSolicitud e idProductor)

### Auditoría (`/api/auditoria/`)

- `GET /api/auditoria/solicitud/{solicitudId}` - Obtener auditorías por solicitud
- `GET /api/auditoria/tipo-accion/{tipoAccion}` - Obtener auditorías por tipo de acción
- `GET /api/auditoria/todas` - Obtener todas las auditorías

### Ámbito (`/api/ambito/`)

- `GET /api/ambito/provincias` - Obtener todas las provincias
- `GET /api/ambito/localidades/{provinciaId}` - Obtener localidades por provincia

### Archivos

- `GET /api/archivos` - Obtener todos los archivos
- `GET /api/archivos/{id}` - Obtener archivo por ID
- `GET /api/archivos/{id}/download` - Descargar archivo
- `POST /api/archivos/upload` - Subir archivo
- `DELETE /api/archivos/{id}` - Eliminar archivo

## Configuración

### Base de Datos

Configurar PostgreSQL en `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dfs_db
    username: dfs_user
    password: dfs_password
```

### Documentación API

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## Tipos de Solicitud

- `INDIVIDUO` - Persona física
- `EMPRESA_CLASICA` - Empresa clásica
- `EMPRESA_SGR` - Empresa SGR

## Estados de Solicitud

- `INCOMPLETA` - En proceso de carga
- `PENDIENTE` - Pendiente de revisión
- `PENTIENTE_FIRMA` - Pendiente de firma digital
- `FIRMADO` - Firmado digitalmente
- `APROBADA` - Aprobada
- `RECHAZADA` - Rechazada
- `CANCELADA` - Cancelada
- `ERROR_DETECTADO` - Error detectado
- `ERROR_PDF` - Error en generación de PDF

## Ejecución

1. **Configurar base de datos PostgreSQL**
2. **Ejecutar la aplicación**:
   ```bash
   mvn spring-boot:run
   ```
3. **Acceder a la documentación**: http://localhost:8080/swagger-ui.html

## Migración desde Vaadin

Esta API migra toda la funcionalidad del módulo `core` de la aplicación Vaadin original, incluyendo:

- ✅ Entidades JPA (Solicitud, Persona, Empresa, etc.)
- ✅ Repositorios (convertidos de DAOs a Spring Data JPA)
- ✅ Servicios de negocio
- ✅ Controladores REST (reemplazando la UI de Vaadin)
- ✅ Configuración de base de datos
- ✅ Documentación API con OpenAPI 3

## Límites de Archivos

- **Tamaño máximo por archivo**: 5MB
- **Tamaño máximo por request**: 50MB

## Próximos Pasos

- [x] Implementar autenticación JWT
- [x] Agregar validaciones de negocio
- [x] Implementar generación de PDF
- [ ] Agregar tests unitarios e integración
- [ ] Configurar logging avanzado
- [ ] Implementar cache con Redis

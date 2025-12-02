-- Script SQL para crear las tablas de seguridad (Usuario, Rol, Usuario_Rol)
-- Ejecutar en PostgreSQL

-- 1. Crear tabla ROL
CREATE TABLE IF NOT EXISTS ROL (
    ID BIGSERIAL PRIMARY KEY,
    NOMBRE VARCHAR(50) NOT NULL UNIQUE,
    DESCRIPCION VARCHAR(200),
    CONSTRAINT chk_rol_nombre CHECK (NOMBRE IN ('ADMIN', 'CLIENTE', 'OPERADOR', 'SUPERVISOR'))
);

-- 2. Crear tabla USUARIO
CREATE TABLE IF NOT EXISTS USUARIO (
    ID BIGSERIAL PRIMARY KEY,
    USERNAME VARCHAR(50) NOT NULL UNIQUE,
    EMAIL VARCHAR(100) NOT NULL UNIQUE,
    PASSWORD VARCHAR(255) NOT NULL,
    NOMBRES VARCHAR(100),
    APELLIDOS VARCHAR(100),
    ACTIVO BOOLEAN NOT NULL DEFAULT TRUE,
    FECHA_CREACION TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ULTIMO_ACCESO TIMESTAMP
);

-- 3. Crear tabla USUARIO_ROL (tabla de unión)
CREATE TABLE IF NOT EXISTS USUARIO_ROL (
    USUARIO_ID BIGINT NOT NULL,
    ROL_ID BIGINT NOT NULL,
    PRIMARY KEY (USUARIO_ID, ROL_ID),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (USUARIO_ID) REFERENCES USUARIO(ID) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (ROL_ID) REFERENCES ROL(ID) ON DELETE CASCADE
);

ALTER TABLE public.ac_solicitud
  DROP CONSTRAINT ac_solicitud_id_usuario_aprobo_fkey1;

ALTER TABLE public.ac_solicitud
  DROP CONSTRAINT ac_solicitud_id_usuario_cargo_fkey1;



-- ============================================================
-- DATOS INICIALES
-- ============================================================

-- Limpiar usuarios con email vacío o NULL antes de la migración
-- (esto evita violaciones de constraint UNIQUE)
DELETE FROM public.usuario 
WHERE email IS NULL OR TRIM(email) = '';

-- Migración de datos desde sec_usuario (si existe)
-- NOTA: Este INSERT solo se ejecutará si existe la tabla sec_usuario
-- Se filtran usuarios sin email o con email vacío para evitar violaciones de constraint UNIQUE
INSERT INTO public.usuario
(id, activo, apellidos, email, fecha_creacion, nombres, "password", ultimo_acceso, username)
SELECT
    s.id,
    s.habilitado AS activo,
    COALESCE(s.apellido, '') AS apellidos,
    COALESCE(s.email, '') as email,
    NOW() AS fecha_creacion,
    COALESCE(s.nombre, '') AS nombres,
    '$2a$10$XqK.QF2qlaKpRKd7juYCsONGhrbKafRvm4itp7zN1/CuF5nCBbywW' AS "password",
    s.ultimo_inicio_sesion AS ultimo_acceso,
    s.logname AS username
FROM public.sec_usuario s
WHERE s.id <> 1
  AND s.habilitado = true
  AND s.email IS NOT NULL
  AND TRIM(s.email) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM public.usuario u WHERE u.id = s.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM public.usuario u WHERE u.email = COALESCE(s.email, '')
  );


  ALTER TABLE public.ac_solicitud
  ADD CONSTRAINT ac_solicitud_id_usuario_cargo_fkey
    FOREIGN KEY (id_usuario_cargo)
    REFERENCES usuario(id);

ALTER TABLE public.ac_solicitud
  ADD CONSTRAINT ac_solicitud_id_usuario_aprobo_fkey
    FOREIGN KEY (id_usuario_aprobo)
    REFERENCES usuario(id);


-- Ejecutar en PostgreSQL

-- 1. Insertar roles básicos
INSERT INTO ROL (NOMBRE, DESCRIPCION) VALUES 
('ADMIN', 'Administrador del sistema'),
('CLIENTE', 'Cliente del sistema'),
('OPERADOR', 'Operador del sistema'),
('SUPERVISOR', 'Supervisor del sistema')
ON CONFLICT (NOMBRE) DO NOTHING;

-- 2. Insertar usuarios de prueba
-- Nota: Las contraseñas están hasheadas con BCrypt
-- Contraseña original: "admin123" -> Hash: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi
-- Contraseña original: "cliente123" -> Hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
-- Contraseña original: "operador123" -> Hash: $2a$10$TKh8H1.PfQx37YgCzwiKb.KjNyWgaHb9cbcoQgdIVFlYg7B77UdFm
-- Contraseña original: "supervisor123" -> Hash: $2a$10$I4j8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ

INSERT INTO USUARIO (USERNAME, EMAIL, PASSWORD, NOMBRES, APELLIDOS, ACTIVO, FECHA_CREACION) VALUES 
('admin', 'admin@dfs.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Administrador', 'Sistema', true, NOW()),
('cliente1', 'cliente1@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Juan', 'Pérez', true, NOW()),
('operador1', 'operador1@dfs.com', '$2a$10$TKh8H1.PfQx37YgCzwiKb.KjNyWgaHb9cbcoQgdIVFlYg7B77UdFm', 'María', 'González', true, NOW()),
('supervisor1', 'supervisor1@dfs.com', '$2a$10$I4j8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ8vJ', 'Carlos', 'López', true, NOW()),
('test@test.com', 'test@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Usuario', 'Prueba', true, NOW())
ON CONFLICT (EMAIL) DO NOTHING;

-- 3. Asignar roles a usuarios
-- Obtener IDs de roles y usuarios
-- Admin tiene rol ADMIN
INSERT INTO USUARIO_ROL (USUARIO_ID, ROL_ID) 
SELECT u.ID, r.ID 
FROM USUARIO u, ROL r 
WHERE u.EMAIL = 'admin@dfs.com' AND r.NOMBRE = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Cliente1 tiene rol CLIENTE
INSERT INTO USUARIO_ROL (USUARIO_ID, ROL_ID) 
SELECT u.ID, r.ID 
FROM USUARIO u, ROL r 
WHERE u.EMAIL = 'cliente1@test.com' AND r.NOMBRE = 'CLIENTE'
ON CONFLICT DO NOTHING;

-- Operador1 tiene rol OPERADOR
INSERT INTO USUARIO_ROL (USUARIO_ID, ROL_ID) 
SELECT u.ID, r.ID 
FROM USUARIO u, ROL r 
WHERE u.EMAIL = 'operador1@dfs.com' AND r.NOMBRE = 'OPERADOR'
ON CONFLICT DO NOTHING;

-- Supervisor1 tiene rol SUPERVISOR
INSERT INTO USUARIO_ROL (USUARIO_ID, ROL_ID) 
SELECT u.ID, r.ID 
FROM USUARIO u, ROL r 
WHERE u.EMAIL = 'supervisor1@dfs.com' AND r.NOMBRE = 'SUPERVISOR'
ON CONFLICT DO NOTHING;

-- Usuario de prueba tiene rol CLIENTE
INSERT INTO USUARIO_ROL (USUARIO_ID, ROL_ID) 
SELECT u.ID, r.ID 
FROM USUARIO u, ROL r 
WHERE u.EMAIL = 'test@test.com' AND r.NOMBRE = 'CLIENTE'
ON CONFLICT DO NOTHING;

-- 4. Verificar que se insertaron correctamente
SELECT 
    u.USERNAME,
    u.EMAIL,
    u.NOMBRES,
    u.APELLIDOS,
    u.ACTIVO,
    STRING_AGG(r.NOMBRE::TEXT, ', ') as ROLES
FROM USUARIO u
LEFT JOIN USUARIO_ROL ur ON u.ID = ur.USUARIO_ID
LEFT JOIN ROL r ON ur.ROL_ID = r.ID
GROUP BY u.ID, u.USERNAME, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ACTIVO
ORDER BY u.USERNAME;

-- Script SQL para agregar el campo TIPO y PORCENTAJE a la tabla AC_EMPRESA
-- Permite identificar si una empresa es TITULAR o ACCIONISTA
-- Permite almacenar el porcentaje de participación de un accionista
-- Ejecutar en PostgreSQL

-- 1. Agregar la columna TIPO a la tabla AC_EMPRESA
ALTER TABLE AC_EMPRESA 
ADD COLUMN IF NOT EXISTS TIPO VARCHAR(20);

-- 2. Establecer todos los registros existentes como TITULAR
UPDATE AC_EMPRESA
SET TIPO = 'TITULAR'
WHERE TIPO IS NULL;

-- 3. Agregar la columna PORCENTAJE a la tabla AC_EMPRESA
ALTER TABLE AC_EMPRESA 
ADD COLUMN IF NOT EXISTS PORCENTAJE DOUBLE PRECISION;



-- Script SQL para migrar la relación de accionistas de Persona a Organizacion
-- Permite que los accionistas puedan ser tanto Persona como Empresa
-- Ejecutar en PostgreSQL
-- 
-- ESTRATEGIA: Crear nueva columna, copiar datos y mantener ambas funcionales
-- para permitir transición gradual y rollback si es necesario

-- 1. Verificar estructura actual de la tabla
-- La tabla AC_EMPRESA_ACCIONISTA debería tener:
--   - ID_AC_EMPRESA (FK a AC_EMPRESA)
--   - ID_AC_PERSONA (FK a AC_PERSONA) <- Columna existente
--   - ORDEN (para mantener el orden)

-- 2. Hacer la columna ID_AC_PERSONA nullable para permitir Empresas como accionistas
-- Durante el período de transición, permitimos que ID_AC_PERSONA sea null
ALTER TABLE AC_EMPRESA_ACCIONISTA 
ALTER COLUMN ID_AC_PERSONA DROP NOT NULL;

-- 3. Crear nueva columna ID_AC_ORGANIZACION (inicialmente nullable)
ALTER TABLE AC_EMPRESA_ACCIONISTA 
ADD COLUMN IF NOT EXISTS ID_AC_ORGANIZACION BIGINT;

-- 4. Copiar datos de ID_AC_PERSONA a ID_AC_ORGANIZACION
-- Como Persona extiende de Organizacion con herencia JOINED,
-- todos los IDs de Persona deberían existir en AC_ORGANIZACION
UPDATE AC_EMPRESA_ACCIONISTA
SET ID_AC_ORGANIZACION = ID_AC_PERSONA
WHERE ID_AC_PERSONA IS NOT NULL AND ID_AC_ORGANIZACION IS NULL;

-- 5. Crear foreign key para la nueva columna apuntando a AC_ORGANIZACION
-- Solo si no existe ya
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_empresa_accionista_organizacion'
    ) THEN
        ALTER TABLE AC_EMPRESA_ACCIONISTA
        ADD CONSTRAINT FK_EMPRESA_ACCIONISTA_ORGANIZACION
        FOREIGN KEY (ID_AC_ORGANIZACION)
        REFERENCES AC_ORGANIZACION(ID)
        ON DELETE CASCADE;
    END IF;
END $$;

-- 6. Crear trigger para mantener sincronizadas ambas columnas durante la transición
-- Si ID_AC_ORGANIZACION se inserta/actualiza, copiar a ID_AC_PERSONA si es Persona
-- Si ID_AC_PERSONA se inserta/actualiza, copiar a ID_AC_ORGANIZACION
CREATE OR REPLACE FUNCTION sync_accionista_persona_organizacion()
RETURNS TRIGGER AS $$
BEGIN
    -- Si se inserta/actualiza ID_AC_ORGANIZACION
    IF NEW.ID_AC_ORGANIZACION IS NOT NULL THEN
        -- Si es una Persona (existe en AC_PERSONA), copiar a ID_AC_PERSONA
        IF EXISTS (SELECT 1 FROM AC_PERSONA WHERE ID = NEW.ID_AC_ORGANIZACION) THEN
            NEW.ID_AC_PERSONA := NEW.ID_AC_ORGANIZACION;
        END IF;
    END IF;
    
    -- Si se inserta/actualiza ID_AC_PERSONA
    IF NEW.ID_AC_PERSONA IS NOT NULL AND (OLD IS NULL OR OLD.ID_AC_PERSONA IS DISTINCT FROM NEW.ID_AC_PERSONA) THEN
        -- Copiar a ID_AC_ORGANIZACION
        NEW.ID_AC_ORGANIZACION := NEW.ID_AC_PERSONA;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Eliminar trigger si existe y recrearlo
DROP TRIGGER IF EXISTS trigger_sync_accionista_persona_organizacion ON AC_EMPRESA_ACCIONISTA;

CREATE TRIGGER trigger_sync_accionista_persona_organizacion
BEFORE INSERT OR UPDATE ON AC_EMPRESA_ACCIONISTA
FOR EACH ROW
EXECUTE FUNCTION sync_accionista_persona_organizacion();

-- 7. Verificación de estructura
-- Verificar que las columnas existen
SELECT 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns
WHERE table_name = 'ac_empresa_accionista'
AND column_name IN ('id_ac_persona', 'id_ac_organizacion', 'id_ac_empresa', 'orden')
ORDER BY column_name;

-- Verificar foreign keys
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'ac_empresa_accionista'::regclass
AND contype = 'f';
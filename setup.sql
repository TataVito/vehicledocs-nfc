-- Ejecutar en Supabase → SQL Editor

CREATE TABLE IF NOT EXISTS public.vehiculos (
  id              uuid    DEFAULT gen_random_uuid() PRIMARY KEY,
  patente         text    UNIQUE NOT NULL,
  marca           text    DEFAULT '',
  modelo          text    DEFAULT '',
  anio            text    DEFAULT '',
  color           text    DEFAULT '',
  permiso_municipio  text DEFAULT '',
  permiso_venc       text DEFAULT '',
  soap_compania   text    DEFAULT '',
  soap_venc       text    DEFAULT '',
  rt_planta       text    DEFAULT '',
  rt_venc         text    DEFAULT '',
  rg_planta       text    DEFAULT '',
  rg_venc         text    DEFAULT '',
  created_at      timestamptz DEFAULT now(),
  updated_at      timestamptz DEFAULT now()
);

ALTER TABLE public.vehiculos ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Lectura pública"   ON public.vehiculos FOR SELECT USING (true);
CREATE POLICY "Insertar anon"     ON public.vehiculos FOR INSERT WITH CHECK (true);
CREATE POLICY "Actualizar anon"   ON public.vehiculos FOR UPDATE USING (true);
CREATE POLICY "Eliminar anon"     ON public.vehiculos FOR DELETE USING (true);

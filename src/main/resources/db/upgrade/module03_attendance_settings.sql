-- Attendance register mode per school (Settings → Academic setup): DAILY = one register a day, AM_PM = morning and afternoon.
-- Schools without a row take one register a day. DPS keeps DAILY.
CREATE TABLE IF NOT EXISTS attendance_settings (
    tenant_id character varying(64) NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(255),
    updated_at timestamp(6) with time zone,
    updated_by character varying(255),
    version bigint,
    register_mode character varying(10) DEFAULT 'DAILY'::character varying NOT NULL,
    CONSTRAINT attendance_settings_pkey PRIMARY KEY (tenant_id),
    CONSTRAINT attendance_settings_register_mode_check CHECK (((register_mode)::text = ANY ((ARRAY['DAILY'::character varying, 'AM_PM'::character varying])::text[])))
);
INSERT INTO attendance_settings (tenant_id, register_mode, created_at, created_by, version)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DAILY', now(), 'upgrade:attendance-mode', 0)
ON CONFLICT (tenant_id) DO NOTHING;

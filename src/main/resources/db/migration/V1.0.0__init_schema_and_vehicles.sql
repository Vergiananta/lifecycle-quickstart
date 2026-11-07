-- V1.0.0__init_schema_and_vehicles.sql
-- Inisialisasi tabel Vehicle, Inspection, InspectionItem dan data 20 kendaraan

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INT NOT NULL
);

CREATE TABLE inspections (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
    client_id UUID UNIQUE,
    inspector_name VARCHAR(150) NOT NULL,
    has_key BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE inspection_items (
    id BIGSERIAL PRIMARY KEY,
    inspection_id BIGINT NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
    point_code VARCHAR(100) NOT NULL,
    condition VARCHAR(50) NOT NULL,
    note TEXT,
    photo_refs TEXT[]
);

-- Seed 20 kendaraan tipe CAR (id 1 sampai 20)
INSERT INTO vehicles (id, plate_number, type, brand, model, year) VALUES
(1, 'B 1001 ABC', 'CAR', 'Toyota', 'Avanza', 2021),
(2, 'B 1002 DEF', 'CAR', 'Honda', 'Brio', 2022),
(3, 'B 1003 GHI', 'CAR', 'Daihatsu', 'Xenia', 2020),
(4, 'B 1004 JKL', 'CAR', 'Mitsubishi', 'Xpander', 2023),
(5, 'B 1005 MNO', 'CAR', 'Suzuki', 'Ertiga', 2019),
(6, 'B 1006 PQR', 'CAR', 'Toyota', 'Innova', 2022),
(7, 'B 1007 STU', 'CAR', 'Honda', 'HR-V', 2023),
(8, 'B 1008 VWX', 'CAR', 'Nissan', 'Livina', 2020),
(9, 'B 1009 YZA', 'CAR', 'Hyundai', 'Creta', 2022),
(10, 'B 1010 BCD', 'CAR', 'Wuling', 'Almaz', 2021),
(11, 'B 1011 EFG', 'CAR', 'Toyota', 'Fortuner', 2023),
(12, 'B 1012 HIJ', 'CAR', 'Mitsubishi', 'Pajero Sport', 2022),
(13, 'B 1013 KLM', 'CAR', 'Honda', 'CR-V', 2021),
(14, 'B 1014 NOP', 'CAR', 'Mazda', 'CX-5', 2022),
(15, 'B 1015 QRS', 'CAR', 'Kia', 'Seltos', 2021),
(16, 'B 1016 TUV', 'CAR', 'Toyota', 'Yaris', 2020),
(17, 'B 1017 WXY', 'CAR', 'Honda', 'City Hatchback', 2022),
(18, 'B 1018 ZAB', 'CAR', 'Suzuki', 'Baleno', 2021),
(19, 'B 1019 CDE', 'CAR', 'Hyundai', 'Ioniq 5', 2023),
(20, 'B 1020 FGH', 'CAR', 'Toyota', 'Raize', 2022);

SELECT setval('vehicles_id_seq', 20, true);


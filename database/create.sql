CREATE TABLE electric_meter(
    id BIGINT NOT NULL,
    voltagell1 NUMERIC(7,15) NOT NULL,
    voltagell2 NUMERIC(7,15) NOT NULL,
    voltagell3 NUMERIC(7,15) NOT NULL,

    currentl1 NUMERIC(7,15) NOT NULL,
    currentl2 NUMERIC(7,15) NOT NULL,
    currentl3 NUMERIC(7,15) NOT NULL,

    activePowerL1 NUMERIC(7,15) NOT NULL,
    activePowerL2 NUMERIC(7,15) NOT NULL,
    activePowerL3 NUMERIC(7,15) NOT NULL,

    PFL1 NUMERIC(7,15) NOT NULL,
    PFL2 NUMERIC(7,15) NOT NULL,
    PFL3 NUMERIC(7,15) NOT NULL,

    PF NUMERIC(7,15) NOT NULL,
    TotalActiveEnergyImportTariff1 NUMERIC(7,15) NOT NULL
);
Alter TABLE
    electric_meter ADD PRIMARY KEY (id);
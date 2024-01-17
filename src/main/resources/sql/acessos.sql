
CREATE TABLE Estado (
                codEstado INTEGER NOT NULL,
                nome VARCHAR(50) NOT NULL,
                uf VARCHAR(2) NOT NULL,
                regiao VARCHAR(15) NOT NULL,
                CONSTRAINT estado_pk PRIMARY KEY (codEstado)
);


CREATE SEQUENCE tempo_codtempo_seq;

CREATE TABLE Tempo (
                codTempo INTEGER NOT NULL DEFAULT nextval('tempo_codtempo_seq'),
                ano INTEGER NOT NULL,
                CONSTRAINT tempo_pk PRIMARY KEY (codTempo)
);


ALTER SEQUENCE tempo_codtempo_seq OWNED BY Tempo.codTempo;

CREATE TABLE AcessoInternet (
                codTempo INTEGER NOT NULL,
                codEstado INTEGER NOT NULL,
                quantDomiciliosAcesso INTEGER NOT NULL,
                pib REAL NOT NULL,
                quantDomicilios INTEGER NOT NULL,
                quantEscolasAcesso INTEGER NOT NULL,
                quantEscolas INTEGER NOT NULL
);


CREATE TABLE Crescimento (
                taxa REAL NOT NULL,
                codEstado INTEGER NOT NULL,
                codTempo INTEGER NOT NULL
);


ALTER TABLE Crescimento ADD CONSTRAINT estado_crescimento_fk
FOREIGN KEY (codEstado)
REFERENCES Estado (codEstado)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;

ALTER TABLE AcessoInternet ADD CONSTRAINT estado_acessointernet_fk
FOREIGN KEY (codEstado)
REFERENCES Estado (codEstado)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;

ALTER TABLE Crescimento ADD CONSTRAINT tempo_crescimento_fk
FOREIGN KEY (codTempo)
REFERENCES Tempo (codTempo)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;

ALTER TABLE AcessoInternet ADD CONSTRAINT tempo_acessointernet_fk
FOREIGN KEY (codTempo)
REFERENCES Tempo (codTempo)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;

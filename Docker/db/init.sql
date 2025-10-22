
DROP DATABASE IF EXISTS gestor_db;
CREATE DATABASE gestor_db;
USE gestor_db;

CREATE TABLE IF NOT EXISTS cliente(
    id_cliente SMALLINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    nombre VARCHAR(30) NOT NULL,
    direccion VARCHAR(30),
    poblacion VARCHAR(20),
    telefono VARCHAR(12),
    observaciones VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS aviso(
    id_aviso MEDIUMINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    id_cliente SMALLINT NOT NULL,
    direccion VARCHAR(30),
    poblacion VARCHAR(30),
    aviso_raiz MEDIUMINT,
    descripcion TEXT NOT NULL,
    urgente BOOLEAN NOT NULL,
    fecha_aviso TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_ejecucion TIMESTAMP,
    firma BLOB,
    nombre_firma VARCHAR(30),
    dni_firma VARCHAR(10),
    obsInstalador VARCHAR(200),
    estado ENUM('PENDIENTE', 'PARCIAL', 'TERMINADO', 'CERRADO'),
    turno ENUM('MANANAS', 'TARDES', 'INDIFERENTE'),
    CONSTRAINT avi_cli_FK FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS instalador(
    id_instalador SMALLINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    nombre VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,  -- Hasheada con bcrypt
    refresh_token VARCHAR(255) DEFAULT NULL -- Guardará el token de refresco
);

CREATE TABLE IF NOT EXISTS asignacion(
    id_aviso MEDIUMINT NOT NULL,
    id_instalador SMALLINT NOT NULL,
    id_responsable SMALLINT NOT NULL,
    CONSTRAINT PRIMARY KEY (id_aviso, id_instalador),
    CONSTRAINT asi_avi_FK FOREIGN KEY (id_aviso) REFERENCES aviso(id_aviso)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT asi_ins_FK FOREIGN KEY (id_instalador) REFERENCES instalador(id_instalador)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS imagen_aviso(
    id_imagen INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    id_aviso MEDIUMINT NOT NULL,
    ruta_imagen VARCHAR(255) NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT img_avi_FK FOREIGN KEY (id_aviso) REFERENCES aviso(id_aviso)
        ON UPDATE CASCADE ON DELETE CASCADE
);
# Usa la imagen base de Python 3.9
FROM python:3.9-bullseye

# Establece el directorio de trabajo
WORKDIR /app

# Actualiza y instala las dependencias necesarias (sin el mysql-apt-config)
RUN apt-get update && apt-get install -y \
    default-libmysqlclient-dev \
    gcc \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Copia los archivos del proyecto al contenedor
COPY requirements.txt /app/

# Instala las dependencias de Python desde el archivo requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copia el resto de los archivos de la aplicación
COPY . /app/

# Expone el puerto 5000
EXPOSE 5000

# Comando para iniciar la aplicación
CMD ["flask", "run", "--host=0.0.0.0", "--port=5000"]
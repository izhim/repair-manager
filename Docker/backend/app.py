import base64
from flask import Flask, jsonify, request, url_for
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import Enum
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timedelta
from werkzeug.utils import secure_filename

import uuid
import enum
import os
import logging

UPLOAD_FOLDER = '/app/uploads'
app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# Configura el nivel de logging y el formato de los mensajes
logging.basicConfig(
    level=logging.DEBUG,  # Nivel de logging (DEBUG, INFO, WARNING, ERROR, CRITICAL)
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',  # Formato del mensaje
    handlers=[
        logging.FileHandler('server.log'),  # Guardar logs en un archivo
        logging.StreamHandler()  # Mostrar logs en la consola
    ]
)

logger = logging.getLogger(__name__)

# Configuración de la base de datos
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://root:password@db/gestor_db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

# Configuración de JWT
app.config["JWT_SECRET_KEY"] = os.getenv("JWT_SECRET_KEY", "valor_por_defecto")
app.config["JWT_ACCESS_TOKEN_EXPIRES"] = timedelta(hours=1)  # Token expira en 1 hora

# Configuración del host
app.config['PREFERRED_URL_SCHEME'] = 'http'
app.config['SERVER_NAME'] = '10.0.2.2:8080'

# Inicialización de extensiones
db = SQLAlchemy(app)
jwt = JWTManager(app)

# Enumeración para el estado del aviso
class EstadoAviso(enum.Enum):
    PENDIENTE = 'PENDIENTE'
    PARCIAL = 'PARCIAL'
    TERMINADO = 'TERMINADO'
    CERRADO = 'CERRADO'

# Enumeración para el estado del aviso
class TurnoAviso(enum.Enum):
    MANANAS = 'MANANAS'
    TARDES = 'TARDES'
    INDIFERENTE = 'INDIFERENTE'

# Modelos de la base de datos
class Cliente(db.Model):
    id_cliente = db.Column(db.SmallInteger, primary_key=True, autoincrement=True)
    nombre = db.Column(db.String(30), nullable=False)
    direccion = db.Column(db.String(30))
    poblacion = db.Column(db.String(20))
    telefono = db.Column(db.String(12))
    observaciones = db.Column(db.String(100))

class Aviso(db.Model):
    id_aviso = db.Column(db.Integer, primary_key=True, autoincrement=True)
    id_cliente = db.Column(db.SmallInteger, db.ForeignKey('cliente.id_cliente'), nullable=False)
    direccion = db.Column(db.String(30))
    poblacion = db.Column(db.String(30))
    descripcion = db.Column(db.Text, nullable=False)
    urgente = db.Column(db.Boolean, nullable=False)
    fecha_aviso = db.Column(db.DateTime, server_default=db.func.current_timestamp(), nullable=False)
    fecha_ejecucion = db.Column(db.DateTime, server_default=db.func.current_timestamp(), nullable=False)
    firma = db.Column(db.LargeBinary)
    nombre_firma = db.Column(db.String(30))
    dni_firma = db.Column(db.String(10))
    obsInstalador = db.Column(db.String(200))
    estado = db.Column(Enum(EstadoAviso))
    turno = db.Column(Enum(TurnoAviso))

class Instalador(db.Model):
    id_instalador = db.Column(db.SmallInteger, primary_key=True, autoincrement=True)
    nombre = db.Column(db.String(20), nullable=False)
    password = db.Column(db.String(200), nullable=False)  # Hash de contraseña
    refresh_token = db.Column(db.String(255), nullable=True)

class Asignacion(db.Model):
    id_aviso = db.Column(db.Integer, db.ForeignKey('aviso.id_aviso'), primary_key=True)
    id_instalador = db.Column(db.SmallInteger, db.ForeignKey('instalador.id_instalador'), primary_key=True)
    id_responsable = db.Column(db.SmallInteger)

class ImagenAviso(db.Model):
    id_imagen = db.Column(db.Integer, primary_key=True, autoincrement=True)
    id_aviso = db.Column(db.Integer, db.ForeignKey('aviso.id_aviso'), primary_key=True)
    ruta_imagen = db.Column(db.String(255), nullable=False)
    fecha_subida = db.Column(db.DateTime, server_default=db.func.current_timestamp(), nullable=False)

# Helper para generar enlaces HATEOAS
def make_public_resource(resource, endpoint, **kwargs):
    """Genera una representación pública de un recurso con enlaces HATEOAS."""
    public_resource = {}
    for column in resource.__table__.columns:
        public_resource[column.name] = getattr(resource, column.name)
    # Verifica que los valores necesarios estén presentes en kwargs
    if endpoint == 'get_aviso' and 'id_aviso' not in kwargs:
        raise ValueError("Se requiere 'id_aviso' para generar la URL del aviso.")
    public_resource['_links'] = {
        'self': {'href': url_for(endpoint, _external=True, **kwargs)},
        'collection': {'href': url_for(endpoint, _external=True)}
    }
    return public_resource

# Rutas de autenticación
@app.route('/register', methods=['POST'])
def register_instalador():
    data = request.get_json()
    hashed_password = generate_password_hash(data['password'], method='pbkdf2:sha256')
    nuevo_instalador = Instalador(nombre=data['nombre'], password=hashed_password)
    db.session.add(nuevo_instalador)
    db.session.commit()
    return jsonify({"message": "Instalador registrado"}), 201

@app.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    instalador = Instalador.query.filter_by(nombre=data['nombre']).first()
    if instalador and check_password_hash(instalador.password, data['password']):
        access_token = create_access_token(identity=str(instalador.id_instalador))
        return jsonify(access_token=access_token)
    return jsonify({"message": "Credenciales incorrectas"}), 401

# Rutas específicas para cada recurso
@app.route('/clientes', methods=['GET'])
@jwt_required()
def get_clientes():
    clientes = Cliente.query.all()
    return jsonify([make_public_resource(cliente, 'get_cliente', id_cliente=cliente.id_cliente) for cliente in clientes])

@app.route('/clientes/<int:id_cliente>', methods=['GET'])
@jwt_required()
def get_cliente(id_cliente):
    cliente = Cliente.query.get(id_cliente)
    if not cliente:
        return jsonify({"error": "Cliente no encontrado"}), 404
    return jsonify(make_public_resource(cliente, 'get_cliente', id_cliente=cliente.id_cliente))

@app.route('/clientes', methods=['POST'])
@jwt_required()
def create_cliente():
    data = request.get_json()
    nuevo_cliente = Cliente(**data)
    db.session.add(nuevo_cliente)
    db.session.commit()
    return jsonify(make_public_resource(nuevo_cliente, 'get_cliente', id_cliente=nuevo_cliente.id_cliente)), 201

@app.route('/clientes/<int:id_cliente>', methods=['PUT'])
@jwt_required()
def update_cliente(id_cliente):
    cliente = Cliente.query.get(id_cliente)
    if not cliente:
        return jsonify({"error": "Cliente no encontrado"}), 404
    data = request.get_json()
    for key, value in data.items():
        setattr(cliente, key, value)
    db.session.commit()
    return jsonify(make_public_resource(cliente, 'get_cliente', id_cliente=cliente.id_cliente))

@app.route('/clientes/<int:id_cliente>', methods=['DELETE'])
@jwt_required()
def delete_cliente(id_cliente):
    cliente = Cliente.query.get(id_cliente)
    if not cliente:
        return jsonify({"error": "Cliente no encontrado"}), 404
    db.session.delete(cliente)
    db.session.commit()
    return jsonify({"message": "Cliente eliminado"}), 204

@app.route('/avisos', methods=['GET'])
@jwt_required()
def get_avisos():
    try:
        # Filtrar directamente en la consulta
        avisos = Aviso.query.filter(Aviso.estado != 'CERRADO').all()
        result = []
        for aviso in avisos:
            aviso_data = {
                'id_aviso': aviso.id_aviso,
                'id_cliente': aviso.id_cliente,
                'direccion': aviso.direccion,
                'descripcion': aviso.descripcion,
                'urgente': aviso.urgente,
                'fecha_aviso': aviso.fecha_aviso.isoformat() if aviso.fecha_aviso else None,
                'fecha_ejecucion': aviso.fecha_ejecucion.isoformat() if aviso.fecha_ejecucion else None,
                'estado': aviso.estado.value if aviso.estado else None,
                'turno': aviso.turno.value if aviso.turno else None
            }
            result.append(aviso_data)
        return jsonify(result)
    except Exception as e:
        logger.error(f"Error en /avisos: {str(e)}", exc_info=True)
        return jsonify({"error": str(e)}), 500

@app.route('/avisos/<int:id_aviso>', methods=['GET'])
@jwt_required()
def get_aviso(id_aviso):
    aviso = Aviso.query.get(id_aviso)
    if not aviso:
        return jsonify({"error": "Aviso no encontrado"}), 404
    return jsonify(make_public_resource(aviso, 'get_aviso', id_aviso=aviso.id_aviso))

@app.route('/avisos', methods=['POST'])
@jwt_required()
def create_aviso():
    data = request.get_json()
    nuevo_aviso = Aviso(**data)
    db.session.add(nuevo_aviso)
    db.session.commit()
    return jsonify(make_public_resource(nuevo_aviso, 'get_aviso', id_aviso=nuevo_aviso.id_aviso)), 201



@app.route('/avisos/<int:id_aviso>', methods=['PUT'])
@jwt_required()
def update_aviso(id_aviso):
    aviso = Aviso.query.get(id_aviso)
    if not aviso:
        return jsonify({"error": "Aviso no encontrado"}), 404
    
    data = request.get_json()
    
    # Actualiza solo los campos permitidos
    campos_permitidos = ['estado', 'nombre_firma', 'dni_firma', 'obsInstalador']
    for campo in campos_permitidos:
        if campo in data:
            setattr(aviso, campo, data[campo])
    
    try:
        db.session.commit()
        # Devuelve una respuesta simple sin HATEOAS para evitar el error
        return jsonify({
            "id_aviso": aviso.id_aviso,
            "mensaje": "Aviso actualizado correctamente"
        }), 200
    except Exception as e:
        db.session.rollback()
        logger.error(f"Error al actualizar aviso: {str(e)}")
        return jsonify({"error": "Error al actualizar aviso"}), 500

@app.route('/avisos/<int:id_aviso>', methods=['DELETE'])
@jwt_required()
def delete_aviso(id_aviso):
    aviso = Aviso.query.get(id_aviso)
    if not aviso:
        return jsonify({"error": "Aviso no encontrado"}), 404
    db.session.delete(aviso)
    db.session.commit()
    return jsonify({"message": "Aviso eliminado"}), 204

# Rutas para Instaladores
@app.route('/instaladores', methods=['GET'])
@jwt_required()
def get_instaladores():
    instaladores = Instalador.query.all()
    return jsonify([make_public_resource(instalador, 'get_instalador', id_instalador=instalador.id_instalador) for instalador in instaladores])

@app.route('/instaladores/<int:id_instalador>', methods=['GET'])
@jwt_required()
def get_instalador(id_instalador):
    instalador = Instalador.query.get(id_instalador)
    if not instalador:
        return jsonify({"error": "Instalador no encontrado"}), 404
    return jsonify(make_public_resource(instalador, 'get_instalador', id_instalador=instalador.id_instalador))

# Rutas para Asignaciones
@app.route('/asignaciones', methods=['GET'])
@jwt_required()
def get_asignaciones():
    asignaciones = Asignacion.query.all()
    return jsonify([make_public_resource(asignacion, 'get_asignacion', id_aviso=asignacion.id_aviso, id_instalador=asignacion.id_instalador) for asignacion in asignaciones])


# método para la aplicación Android
@app.route('/avisos-instalador', methods=['GET'])
@jwt_required()
def get_mis_avisos():
    try:
        # Obtener ID del instalador desde el token JWT
        id_instalador = get_jwt_identity()
        
        # Verificar que el instalador existe
        if not Instalador.query.get(id_instalador):
            return jsonify({"error": "Instalador no válido"}), 401

        # Consulta optimizada con JOIN
        avisos = db.session.query(Aviso, Cliente)\
            .join(Cliente, Aviso.id_cliente == Cliente.id_cliente)\
            .join(Asignacion, Aviso.id_aviso == Asignacion.id_aviso)\
            .filter(
                Asignacion.id_responsable == id_instalador,
                Aviso.estado != EstadoAviso.CERRADO,
                Aviso.firma == None  # Filtro para firma nula
                
            )\
            .order_by(Aviso.fecha_aviso.desc())\
            .all()
        
        resultado = []
        for aviso, cliente in avisos:
            imagenes = {img.id_imagen: img.ruta_imagen 
                       for img in ImagenAviso.query.filter_by(id_aviso=aviso.id_aviso).all()}
            aviso_data = {
                "id_aviso": aviso.id_aviso,
                "id_cliente": aviso.id_cliente,
                "id_instalador": id_instalador,
                "direccion": aviso.direccion or cliente.direccion,
                "poblacion": aviso.poblacion or cliente.poblacion,
                "descripcion": aviso.descripcion,
                "urgente": aviso.urgente,
                "fecha_aviso": aviso.fecha_aviso.isoformat(),
                'fecha_ejecucion': aviso.fecha_ejecucion.isoformat(),
                "estado": aviso.estado.value,
                "turno": aviso.turno.value,
                "nombre": cliente.nombre,
                "telefono": cliente.telefono,
                "observaciones": cliente.observaciones,
                "imagenes": imagenes
            }
            resultado.append(aviso_data)
        
        return jsonify(resultado)
    
    except Exception as e:
        logger.error(f"Error en /avisos-instalador: {str(e)}")
        return jsonify({"error": "Error al obtener avisos"}), 500
    

# método para subir imágenes al servidor nginx    
@app.route('/upload', methods=['POST'])
def upload():
    print("Form Data:", request.form)
    print("Files:", request.files)
    
    # Obtener parámetros con valores por defecto seguros
    cliente_id = request.form.get('cliente_id', '').strip()
    aviso_id = request.form.get('aviso_id', '').strip()
    tipo = request.form.get('tipo', '').strip().lower()
    file = request.files.get('image')

    # Validación robusta
    if not all([cliente_id, aviso_id, tipo]) or tipo not in ['cliente', 'instalador']:
        logger.error(f"Datos faltantes o inválidos. cliente_id: {cliente_id}, aviso_id: {aviso_id}, tipo: {tipo}")
        return {'error': 'Se requieren cliente_id, aviso_id y tipo válido (cliente/instalador)'}, 400

    if not file or file.filename == '':
        return {'error': 'No se recibió un archivo válido'}, 400

    try:
        # Generar nombre de archivo seguro
        file_ext = os.path.splitext(file.filename)[1]
        filename = f"{uuid.uuid4().hex}{file_ext}"
        
        # Crear estructura de directorios
        upload_dir = os.path.join(app.config['UPLOAD_FOLDER'], aviso_id, tipo)
        os.makedirs(upload_dir, exist_ok=True)
        
        # Guardar archivo
        filepath = os.path.join(upload_dir, filename)
        file.save(filepath)
        
        # Retornar respuesta
        return {
            'url': f"/uploads/{aviso_id}/{tipo}/{filename}",
            'ruta_bd': f"/uploads/{aviso_id}/{tipo}/{filename}"
        }, 200
        
    except Exception as e:
        logger.error(f"Error al subir archivo: {str(e)}", exc_info=True)
        return {'error': 'Error interno al procesar el archivo'}, 500

# Endpoint para registrar imágenes en la base de datos
@app.route('/imagenes', methods=['POST'])
@jwt_required()
def registrar_imagen():
    try:
        data = request.get_json()
        nueva_imagen = ImagenAviso(
            id_aviso=data['id_aviso'],
            ruta_imagen=data['ruta_imagen']
        )
        db.session.add(nueva_imagen)
        db.session.commit()
        return jsonify({"message": "Imagen registrada"}), 201
    except Exception as e:
        db.session.rollback()
        logger.error(f"Error al registrar imagen: {str(e)}")
        return jsonify({"error": "Error al registrar imagen"}), 500

# Endpoint para obtener avisos modificados
@app.route('/avisos-instalador/modificados', methods=['GET'])
@jwt_required()
def get_avisos_modificados():
    try:
        id_instalador = get_jwt_identity()
        
        # Consulta para avisos con cambios del instalador
        avisos = db.session.query(Aviso, Cliente)\
            .join(Cliente, Aviso.id_cliente == Cliente.id_cliente)\
            .join(Asignacion, Aviso.id_aviso == Asignacion.id_aviso)\
            .filter(
                Asignacion.id_responsable == id_instalador,
                Aviso.estado != EstadoAviso.CERRADO,
                or_(
                    Aviso.firma != None,
                    Aviso.estado != EstadoAviso.PENDIENTE
                )
            )\
            .order_by(Aviso.fecha_aviso.desc())\
            .all()
        
        resultado = []
        for aviso, cliente in avisos:
            # Obtener imágenes del instalador
            imagenes = {img.id_imagen: img.ruta_imagen 
                       for img in ImagenAviso.query.filter_by(id_aviso=aviso.id_aviso).all()}
            aviso_data = {
                "id_aviso": aviso.id_aviso,
                "id_cliente": aviso.id_cliente,
                "estado": aviso.estado.value,
                "nombre_firma": aviso.nombre_firma,
                "dni_firma": aviso.dni_firma,
                "obsInstalador": aviso.obsInstalador,
                "firma": base64.b64encode(aviso.firma).decode('utf-8') if aviso.firma else None,
                #"firma_url": f"/uploads/{aviso.id_aviso}/firma/firma_{aviso.id_aviso}.png" if aviso.firma else None,
                "imagenes_instalador": imagenes
            }
            resultado.append(aviso_data)
        
        return jsonify(resultado)
    
    except Exception as e:
        logger.error(f"Error en /avisos-instalador/modificados: {str(e)}")
        return jsonify({"error": "Error al obtener avisos modificados"}), 500
    
@app.route('/subir-firma', methods=['POST'])
@jwt_required()
def subir_firma():
    try:
        # Verificar autenticación
        id_instalador = get_jwt_identity()
        
        # Obtener datos multipart
        id_aviso = request.form.get('avisoId')
        nombre_firma = request.form.get('nombreFirma', '')
        dni_firma = request.form.get('dniFirma', '')
        
        if not id_aviso:
            return jsonify({"error": "Se requiere ID de aviso"}), 400

        # Verificar permisos del instalador
        asignacion = Asignacion.query.filter_by(
            id_aviso=id_aviso,
            id_responsable=id_instalador
        ).first()
        
        if not asignacion:
            return jsonify({"error": "No autorizado para este aviso"}), 403

        # Procesar archivo de firma
        if 'firma' not in request.files:
            return jsonify({"error": "No se envió archivo de firma"}), 400
            
        file = request.files['firma']
        if file.filename == '':
            return jsonify({"error": "Nombre de archivo inválido"}), 400

        # Leer y guardar BLOB
        firma_blob = file.read()
        
        # Actualizar aviso
        aviso = Aviso.query.get(id_aviso)
        if not aviso:
            return jsonify({"error": "Aviso no encontrado"}), 404
            
        aviso.firma = firma_blob
        aviso.nombre_firma = nombre_firma
        aviso.dni_firma = dni_firma
        
        db.session.commit()

        return jsonify({
            "success": True,
            "message": "Firma guardada en base de datos",
            "id_aviso": id_aviso
        })

    except Exception as e:
        db.session.rollback()
        logger.error(f"Error al guardar firma: {str(e)}")
        return jsonify({"error": "Error interno al guardar firma"}), 500


# código para polling
@app.route('/avisos/check-updates', methods=['GET'])
@jwt_required()
def check_for_updates():
    try:
        last_sync = request.args.get('last_sync')
        
        if not last_sync:
            return jsonify({"error": "Se requiere parámetro last_sync"}), 400
            
        # Convertir de MILISEGUNDOS a segundos

        try:
            # Convierte string a float y pasa de milisegundos a segundos
            last_sync_seconds = float(last_sync) / 1000
        except ValueError:
            return jsonify({"error": "Parámetro last_sync inválido"}), 400

        # Ahora sí crea el datetime
        last_sync_dt = datetime.fromtimestamp(last_sync_seconds)
        
        # Resto de tu lógica...
        avisos = Aviso.query.filter(
            Aviso.fecha_aviso > last_sync_dt,
            Aviso.estado != EstadoAviso.CERRADO
        ).all()
        
        result = [{
            'id_aviso': aviso.id_aviso,
            'fecha_aviso': aviso.fecha_aviso.isoformat(),
            'descripcion': aviso.descripcion,
            'urgente': aviso.urgente
        } for aviso in avisos]
        
        return jsonify(result)
        
    except Exception as e:
        logger.error(f"Error en /avisos/check-updates: {str(e)}", exc_info=True)
        return jsonify({"error": "Error al verificar actualizaciones"}), 500
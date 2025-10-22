# Repair Manager

A full-stack repair management system featuring an Android application for technicians, a Dockerized backend (Python Flask API, MySQL, NGINX), and secure offline-capable data storage.

---

## 🚀 Project Overview

**Repair Manager** is a mobile-first solution designed to manage repair requests, client information, and technician workflows.  
It consists of three main components:

1. **Android Application:** Built in Java using Android Studio, it stores work logs locally using ROOM and handles authentication with JWT tokens. Retrofit is used to communicate securely with the backend API.  
2. **Backend & Server Simulation:** Docker containers run MySQL (database), NGINX (image storage), and a Python Flask API (data management, authentication, and RESTful communications).  
3. **Database:** Structured and secure MySQL schema designed to handle clients, repair requests, technicians, assignments, and images efficiently.

---

## 📱 Android Application

### Key Features

- **Technician Workflow Management:** Track repair requests, assignments, and daily work logs.  
- **Offline Capability:** Stores data locally in ROOM for offline usage.  
- **Secure Authentication:** JWT tokens manage user sessions without storing server-side sessions.  
- **RESTful API Integration:** Retrofit handles communication with the Flask backend.  
- **Image Upload:** Technicians can upload photos of incidents or client signatures, stored securely via NGINX.

### Technologies Used

- **Java** – Official Android language, stable and compatible with older Android versions.  
- **Android Studio** – Official IDE with visual editor, emulator, Gradle support, and advanced debugging tools.  
- **ROOM** – Simplifies database operations, reduces boilerplate, and ensures compile-time validation of queries.  
- **JWT** – Stateless authentication for scalable applications.  
- **Retrofit** – Simplifies HTTP requests and maps API responses directly to Java objects.

---

## 🗄️ Database Implementation

The project uses a MySQL database to store clients, repair requests, technicians, assignments, and image metadata.  
The database schema follows best practices in **normalization, security, and scalability**:

- Technician passwords are hashed using bcrypt.  
- JWT refresh tokens are securely stored for authentication.  
- Relationships between tables allow efficient querying and data integrity.  
- Designed to support offline-capable mobile operations and synchronized updates.

---

## 🖥️ Backend (Python + Flask)

### Functional Requirements

- Manage clients, repair requests, technicians, and assignments.  
- Assign repair requests to technicians.  
- Upload images and signatures.  
- Expose a RESTful API protected with JWT.  
- Log server activity and errors for maintenance.

### Technologies Used

- **Python + Flask:** Lightweight microframework for RESTful API and business logic.  
- **SQLAlchemy:** ORM for MySQL integration.  
- **Werkzeug & flask-jwt-extended:** Password hashing and JWT authentication.  
- **NGINX:** Serves uploaded images securely.  
- **Docker & Docker Compose:** Containerized environment for easy development and deployment.

### Security & Data Flow

- Technician logs in → receives JWT token.  
- App fetches assigned repair requests → stores in ROOM for offline use.  
- Offline edits are stored locally and synchronized when online.  
- Images uploaded via multipart/form-data → stored by NGINX, paths saved in MySQL.  
- API validates all requests and ensures only authorized users can access resources.

---

## ⚡ Notes

- Offline-first design ensures technicians can work without internet connection.  
- Docker simulates a cloud environment locally for backend, database, and image server.  
- Sensitive keys (API keys, certificates) are not included in the repository.  
- JSON is the standard format for data exchange; multipart/form-data is used for file uploads.


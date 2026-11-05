# Mail Sender Microservice 🩺📨



[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Port](https://img.shields.io/badge/Port-8095-blue.svg)](#)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](#)

---

## 📌 Overview

The **Mail Service** is an independent, decoupled notification microservice designed to handle mission-critical clinical communications. It primarily serves the **COSMOS Auth Service (Port 8081)** by delivering hospital-grade, authenticated notification emails to medical practitioners upon formal verification and approval by System Administrators.

### Key Capabilities:
- **Decoupled Architecture:** Runs as a dedicated service on port `8095`, isolated from core authentication and telemetry data pipelines.
- **Hospital-Grade Transactional Emails:** Dispatches responsive HTML emails featuring **COSMOS MEDICALS** and **GUPTA SOFTWORKS ORGANISATION** branding.
- **Issued Login ID Security:** Explicitly enforces practitioner sign-in via backend-issued Login IDs (e.g., `COSMOS-DOC-4091`) while restricting raw email logins.
- **Zero-Crash Local Simulation:** Automatically falls back to console simulation if SMTP credentials are missing or unconfigured during testing.
- **Structured Error Handling:** Intercepts all validation and runtime errors via `@RestControllerAdvice` returning standardized `ErrorResponseDto` or field validation mappings.

---

## 🏗️ Architecture & Interaction Flow

```
+------------------+         REST (HTTP POST)         +--------------------+         SMTP (Port 587)        +----------------------+
|   Admin Portal   |                                  |   COSMOS Mail      |                                |     Gmail / SMTP     |
|   (Front-End)    |                                  |   Microservice     |                                |        Server        |
+--------+---------+                                  |   (Port 8095)      |                                +----------+-----------+
         |                                            +---------+----------+                                           |
         | 1. Admin Approves Doctor                             |                                                      |
         v                                                      |                                                      |
+--------+---------+                                            |                                                      |
|   Auth Service   | -- 2. POST /mail/doctor-approved --------->|                                                      |
|   (Port 8081)    |    (Issued LoginID, License, Email)        | -- 3. Secure TLS Dispatch (HTML + PlainText) ------> |
+------------------+                                            |                                                      |
                                                                |                                                      v
                                                                |                                            +---------+----------+
                                                                |                                            |  Doctor's Inbox    |
                                                                |                                            |  (Live Email)      |
                                                                |                                            +--------------------+
                                                                v
                                                      +---------+----------+
                                                      |  200 OK Response   |
                                                      |  (MailResponseDto) |
                                                      +--------------------+
```

---

## 🚀 API Specification

Base URL: `http://localhost:8095` (or Render deployed URL)

### 1. Health Check
Monitors service availability and port status.

- **URL:** `GET /mail/health` or `GET /mail/` or `GET /`
- **Response (200 OK):**
```json
{
  "service": "MAIL SERVICE",
  "status": "UP",
  "port": 8095,
  "timestamp": "2026-11-05T18:30:00.123456"
}
```

---

### 2. Send Doctor Approval Email
Invoked by the Admin/Auth service when a medical practitioner's account is verified.

- **URL:** `POST /mail/doctor-approved`
- **Headers:** `Content-Type: application/json`
- **Request Body (`DoctorApprovalMailDto`):**
```json
{
  "recipientEmail": "doctor@hospital.org",
  "recipientName": "Dr. XYZ PQR",
  "licenseNumber": "MED-2026-9921",
  "loginID": "COSMOS-DOC-4091",
  "loginUrl": "url_login"
}
```

- **Field Validations:**
  - `recipientEmail`: Required, valid email format.
  - `recipientName`: Required, non-blank string.
  - `licenseNumber`: Required, non-blank string.
  - `loginID`: Optional (defaults to `PENDING-ISSUANCE` if blank).
  - `loginUrl`: Optional (defaults to `http://localhost:5173/login` if blank).

- **Success Response (200 OK - `MailResponseDto`):**
```json
{
  "status": "SENT",
  "message": "Doctor approval email successfully sent with Issued Login ID",
  "recipient": "doctor@hospital.org",
  "localDateTime": "2026-11-05T18:32:10.451"
}
```

- **Validation Failure Response (400 Bad Request):**
```json
{
  "recipientEmail": "Invalid email format",
  "licenseNumber": "License Number is required"
}
```

- **Server Error Response (500 / 503 - `ErrorResponseDto`):**
```json
{
  "apiPath": "uri=/mail/doctor-approved",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "errorMessage": "Unable to send email via SMTP server: Connection timed out",
  "errorTime": "2026-11-05T18:35:22.812"
}
```

---

## ⚙️ Configuration & Environment Variables

The microservice automatically reads `.env` on startup via a custom bootstrap loader in `MailsenderApplication.java`.

### Required Environment Variables:

| Variable | Description | Example                                        |
| :--- | :--- |:-----------------------------------------------|
| `PORT` | Service binding port (Default: `8095`) | `8095`                                         |
| `MAIL_USERNAME` | SMTP Authenticated Username / Email | `yourEmail@gmail.com`                          |
| `MAIL_PASSWORD` | 16-character Google App Password | `abcdefghijklmnop`                             |
| `MAIL_FROM_EMAIL` | Reply-To Address | `email-cosmos@org.in`                          |
| `MAIL_FROM_NAME` | Sender Display Name | `COSMOS Health (GUPTA-SOFTWORKS ORGANISATION)` |
| `mail.simulate.if-empty`| Enable console simulation if credentials empty | `false`                                        |

### Sample `.env` file (Root of project):
```env
PORT=8095
MAIL_FROM_EMAIL=email-cosmos@org.in
MAIL_FROM_NAME="COSMOS Health (GUPTA-SOFTWORKS ORGANISATION)"
MAIL_USERNAME=yourEmail@gmail.com
MAIL_PASSWORD=your16digitapppassword
```

> [!IMPORTANT]
> **Google App Password Requirement:**  
> Direct Google account passwords are blocked by Google security. You must generate a dedicated 16-character App Password via **Google Account $\rightarrow$ Security $\rightarrow$ 2-Step Verification $\rightarrow$ App Passwords**.

---

## 💻 Local Development & Execution

### Prerequisites:
- **Java Development Kit (JDK):** Version 21 LTS (`java -version`)
- **Maven:** Version 3.9+ (or use bundled `./mvnw`)

### 1. Build & Test:
```powershell
# Compile and run unit tests
.\mvnw.cmd clean test
```

### 2. Run the Service:
```powershell
# Run using Spring Boot Maven Plugin
.\mvnw.cmd spring-boot:run
```
Service will bind to **`http://localhost:8095`**.

### 3. Test Live Dispatch via PowerShell:
```powershell
Invoke-RestMethod -Uri "http://localhost:8095/mail/doctor-approved" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{
    "recipientEmail": "doctorEmail@gmail.com",
    "recipientName": "MNO Sharma",
    "licenseNumber": "MED-2026-9921",
    "loginID": "COSMOS-DOC-4091",
    "loginUrl": "http://localhost:5173/login"
  }'
```

---

## 🐳 Docker Deployment

The service includes a production-ready, multi-stage `Dockerfile`:

```powershell
# Build Docker Image
docker build -t cosmos-mail-service .

# Run Docker Container
docker run -p 8095:8095 --env-file .env cosmos-mail-service
```

---

## 📂 Project Directory Structure

```text
mailsender/
├── .env                                # Local secrets (GITIGNORED)
├── .gitignore                          # Git exclusions (.env, target, etc.)
├── Dockerfile                          # Multi-stage production Docker build
├── pom.xml                             # Spring Boot 3.4.2 & Java 21 dependencies
├── README.md                           # Documentation
└── src/
    ├── main/
    │   ├── java/com/service/mailsender/
    │   │   ├── MailsenderApplication.java        # Main class + .env auto-loader
    │   │   ├── controller/
    │   │   │   ├── HealthController.java         # GET /mail/health
    │   │   │   └── DoctorApprovalMailController.java # POST /mail/doctor-approved
    │   │   ├── dto/
    │   │   │   ├── DoctorApprovalMailDto.java    # Record for incoming approval data
    │   │   │   ├── ErrorResponseDto.java         # Record for error responses
    │   │   │   └── MailResponseDto.java          # Record for dispatch responses
    │   │   ├── exception/
    │   │   │   └── MailExceptionHandler.java     # @RestControllerAdvice
    │   │   └── service/
    │   │       ├── IDoctorApprovalMailService.java
    │   │       └── impl/DoctorApprovalMailServiceImpl.java # JavaMailSender + HTML Engine
    │   └── resources/
    │       └── application.properties            # Core configuration & fallbacks
    └── test/
        └── java/com/service/mailsender/
            └── MailsenderApplicationTests.java   # Spring Boot context tests
```

---

## 🛡️ License & Copyright

&copy; 2026 **GUPTA SOFTWORKS ORGANISATION**. All rights reserved.  


<img src="app/src/main/res/drawable/logo.jpg" alt="RoomSense Logo" width="70" align="left" style="margin-right: 20px;"/>

# RoomSense - Library Room Monitoring System

<br clear="left"/>

## Overview

**RoomSense** is an intelligent IoT-based mobile application designed to monitor and optimize library room conditions in real-time. Developed as an educational project for the **IoT course** at the **University of Modena and Reggio Emilia** (Master's Degree in Engineering), this system combines physical sensors, cloud backend infrastructure, and machine learning to provide comprehensive environmental monitoring.

The application empowers librarians and facility managers to:
- Monitor temperature, humidity, and occupancy levels in real-time
- Receive AI-powered recommendations
- Track historical data and analyze trends
  
## Key Features

### Mobile Application (Android)
- **Real-time Monitoring**: Live data visualization from physical sensors
- **Intuitive Dashboard**: Clean, Material Design interface for easy navigation
- **AI-Module**: Neural network recommends rooms based on user preferences and actions

### AI Module
- **Neural Network Model**: Machine Learning-based on the users feedback

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     High Level Architecture                 │
└─────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Physical   │         │              │         │    Mobile    │
│   Sensors    │───────▶│   Backend     │◀─────▶│  Application │
│  (ESP32/IoT) │  Serial │ (JS/Express) │   REST  │   (Kotlin)   │
│              │         │              │   API   │              │
└──────────────┘         │              │         └──────────────┘
                         │  + Docker    │
                         │  + Database  │
                         │  + ML Model  │
                         └──────────────┘
```

## Installation

### Prerequisites

- **Android Studio**
- **JDK 11+**
- **Docker** and **Docker Compose**
- **Git**
- Android device or emulator (API 21+)

### Step 1: Clone the Repository

```bash
# Clone the mobile frontend
git clone https://github.com/MDN-IA/frontend-mobile.git
cd frontend-mobile
```

### Step 2: Set Up the Backend

The application requires the backend services to be running. The backend is containerized using Docker.

```bash
# Clone the backend repository
git clone https://github.com/MDN-IA/backend.git
cd backend

# Start all backend services with Docker Compose
docker-compose up -d

# This will start:
# - API Server (JS)
# - PostgreSQL Database
# - ML Model
```

**Note**: Make sure Docker Desktop is running before executing `docker-compose up`. The backend must be running for the mobile app to function properly.

### Step 3: Configure the Mobile App

1. Open `frontend-mobile` in Android Studio
2. Update the API endpoint in your configuration file:

```kotlin
// app/src/main/java/com/example/iot_mobile/network/ApiClient.kt
const val BASE_URL = "http://10.0.2.2:4000/api" // Local IP
const val BASE_URL = "http://64.226.100.1:4000/api" // Public IP (cloud)

```

## Educational Context

This project was developed as part of the **Internet of Things (IoT) course** at the **University of Modena and Reggio Emilia**, Master's Degree in Engineering program.

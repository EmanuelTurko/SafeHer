# SafeHer

🚧 **Work in Progress** - This project is currently under active development

## Project Overview

SafeHer is an innovative personal safety solution designed to empower women with confidence through technology. The system combines a discreet wearable pendant with a comprehensive mobile application to provide instant emergency response capabilities.

**Core Concept**: An elegant pendant with SOS button and camera. One press sends an alert with your live location to trusted contacts. Safety within reach.

## System Architecture

### Hardware Component
- **Pendant Device**: Xiao Seeed ESP32S3 camera module
- **Form Factor**: Discreet necklace/locket/pendant design
- **Features**:
  - SOS button for emergency activation
  - Low-light camera for situation documentation
  - Secure data encryption

### Software Components

#### Backend (`/backend`)
- **Technology**: Node.js with TypeScript
- **Database**: MongoDB
- **Key Features**:
  - Real-time data processing
  - Emergency contact management
  - GPS coordinate handling
  - Twilio API integration for Whatsapp alerts

#### Frontend (`/frontend`)
- **Technology**: Kotlin for Android (Android Studio)
- **Key Features**:
  - Emergency contact configuration
  - Customizable alert messages
  - Real-time location tracking
  - Help tracking en route
  - User-friendly interface

## How It Works

1. **Emergency Activation**: User presses the discreet SOS button on the pendant
2. **Data Capture**: Device captures video with low-light camera and saves to the VideoLibrary
3. **Location Services**: GPS coordinates are automatically attached
4. **Emergency Alerts**: Twilio API sends SMS alerts with location to trusted contacts

## Key Features

- 📱 **Mobile Integration**: Seamless Android app for configuration and monitoring
- 📍 **Live Location**: Real-time GPS tracking with emergency alerts
- 👥 **Trusted Contacts**: Customizable emergency contact list
- 📷 **Situation Documentation**: Low-light camera for evidence capture
- 📞 **Whatsapp Integration**: Twilio API for reliable message delivery

## Technology Stack

### Backend
- Node.js
- TypeScript
- MongoDB
- Twilio API

### Frontend
- Kotlin
- Android Studio
- Android SDK

### Hardware
- Xiao Seeed ESP32S3 camera module
- Custom pendant housing
- GPS module

## Development Status

This project is currently under active development. Core functionalities are being implemented and tested. Contributions and feedback are welcome as we work towards creating a comprehensive safety solution.

## Project Structure

```
SafeHer/
├── backend/          # Node.js TypeScript backend
│   ├── src/
│   ├── package.json
│   └── ...
├── frontend/         # Kotlin Android application
│   ├── app/
│   ├── build.gradle
│   └── ...
└── README.md
```

## Contributing

This project represents a collaborative effort focused on women's safety technology. We welcome contributions that align with our mission of creating reliable, privacy-focused safety solutions.

## Mission

SafeHer aims to deliver reliable protection and privacy, empowering women with confidence through innovative technology that puts safety within reach.

---

*This project is developed with the vision of making personal safety accessible, discreet, and reliable for everyone.*

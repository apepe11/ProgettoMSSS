# HeartMusic 🎵💓

**HeartMusic** is an innovative Android application that merges the world of music with physical and emotional well-being. The project aims to monitor the user's emotional state through biometric parameters detected by a smartwatch and offer a personalized listening experience to positively influence mood.

## 🚀 The Idea
Music has a profound impact on our emotions. HeartMusic aims to create a technological "bridge" between the heartbeat and music playlists. By analyzing biometric data (BPM and EDA), the app can:
1.  **Analyze** the user's current emotional state.
2.  **Suggest** targeted playlists to improve mood or accompany a moment of relaxation.
3.  **Monitor** emotional trends over time through personalized charts and insights.

## 👥 The Team (MSSS Project)
This project is developed with passion by:
*   **Antonio**
*   **Alessandro**
*   **Luca**
*   **Zahra**

---

## 🛠️ Technical Architecture
The project follows a modern **Full-Stack** approach, clearly separating responsibilities between client and server to ensure scalability and robustness.

### 📱 Mobile (Android)
*   **UI**: Entirely developed with **Jetpack Compose** for a modern and reactive interface.
*   **Architecture**: Follows the **MVVM** (Model-View-ViewModel) pattern.
*   **Networking**: Uses **Retrofit** and **OkHttp** for secure communication with the backend.
*   **Data Persistence**: Implementation of **Jetpack DataStore** to keep the user session active even after closing the app.
*   **Language**: Kotlin.

### ⚙️ Backend (Python & Docker)
*   **Framework**: **Flask** for managing REST APIs.
*   **Database**: **PostgreSQL**, a robust relational database for managing users, sessions, and playlists.
*   **Containerization**: The entire backend is packaged via **Docker** and **Docker Compose**, allowing for quick and identical installation on any machine (Database + Server).
*   **Security**: Password encryption via `werkzeug.security` and session management.
*   **Automation**: Automatic email system for registration confirmation.

---

## 🛠️ How to Start the Project

### Prerequisites
*   Android Studio (latest version)
*   Docker Desktop installed and running

### 1. Start the Backend
Open the terminal in the `/backend` folder and type:
```bash
docker compose up --build
```
The server will be active at `http://localhost:5001`.

### 2. Start the Android App
*   Open the project's root folder with Android Studio.
*   Ensure the emulator is running.
*   Press **Run**. The app will automatically communicate with the server via the special address `10.0.2.2:5001`.

---

## 📈 Roadmap & Future Developments
- [x] Robust Login and Registration system.
- [x] User session persistence.
- [x] Docker + PostgreSQL integration.
- [ ] Integrated music player implementation.
- [ ] Emotion analysis algorithm based on smartwatch data.
- [ ] Personalized profile picture upload.

---
*Project created for the MSSS course - 2024/2025*

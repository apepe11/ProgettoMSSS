# HeartMusic 🎵💓

**HeartMusic** is an Android application that merges the world of music with physical and emotional well-being. The project aims to monitor the user's emotional state through biometric parameters detected by a smartwatch and EEG device. The goal is offer a personalized listening experience to positively influence mood.

## 🚀 The Idea
Music has a profound impact on our emotions. HeartMusic aims to create a "bridge" between the heartbeat and music playlists. By analyzing biometric data (BPM), the app can:
1.  **Analyze** the user's current emotional state.
2.  **Suggest** targeted playlists to improve mood or accompany a moment of relaxation.
3.  **Monitor** emotional trends over time through personalized charts and insights.

## 👥 The Team (MSSS Project)
This project is developed by:
*   **Antonio Pepe**
*   **Luca Rotelli**
*   **Alessandro Ascani**
*   **Zahra Omrani**

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
*   **Automation**: Setup for background tasks and potential future notifications.

### ♿ Accessibility & Best Practices
The application is designed to be inclusive and maintainable, following official Android guidelines:
*   **Internationalization (i18n)**: Zero hardcoded strings. All user-facing text is managed via `strings.xml` resources. The app currently supports **English** and **Italian**, automatically adapting to the system language.
*   **Adaptive Layouts**: Use of **Density-independent pixels (dp)** and **ConstraintLayout** to ensure a consistent experience across different screen sizes and densities.
*   **Semantic Navigation**: Clear back-stack management and semantic properties (`contentDescription`, `onClickLabel`, `stateDescription`) to improve the experience for users with assistive technologies like TalkBack.

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
The server will be active at `http://localhost:5005`.

### 2. Start the Android App
*   Open the project's root folder with Android Studio.
*   Ensure the emulator is running.
*   Press **Run**. The app will automatically communicate with the server via the special address `10.0.2.2:5005`.

For a real Android device, make sure USB debugging is enabled and run:

```bash
adb reverse tcp:5005 tcp:5005
```
```bash
while true; do
  adb reverse tcp:5005 tcp:5005
  sleep 2
done
```
The app will then talk to `http://127.0.0.1:5005` on the device.

---

## ✅ Useful Commands

### Android / Gradle

Build the debug APK without installing it:
```bash
./gradlew assembleDebug
```

Build and install the debug APK on a connected device:
```bash
./gradlew installDebug
```

Clean the project if Gradle gets stuck or artifacts become stale:
```bash
./gradlew clean
```

Check which devices ADB can see:
```bash
adb devices
```

Reset ADB if the device disappears or becomes unauthorized:
```bash
adb kill-server
adb start-server
adb devices
```

Forward the backend port to a real device:
```bash
adb reverse tcp:5005 tcp:5005
adb reverse --list
```

Read app logs for the network and sensor flow:
```bash
adb logcat | grep "ListeningVM\|EmotionAnalysisVM\|SensorManager\|SensorCollectionViewModel"
```

### Wear OS Companion App

HeartMusic includes a Wear OS companion module (`:wear`) used to collect heart rate samples from the smartwatch. The phone app sends `/start_sampling`; the watch app responds with `/sensor_series`.

Build the Wear OS APK:
```bash
./gradlew :wear:assembleDebug
```

The APK is generated at:
```bash
wear/build/outputs/apk/debug/wear-debug.apk
```

Connect the Pixel Watch with wireless debugging:
1. On the watch, enable **Developer options** and **Wireless debugging**.
2. Use **Pair new device** and run:
```bash
adb pair WATCH_REAL_IP:PAIRING_PORT
```
3. Enter the pairing code shown on the watch.
4. Go back to the main **Wireless debugging** screen and use the **IP address & Port** value:
```bash
adb connect WATCH_REAL_IP:DEBUG_PORT
```


Check that ADB sees both phone and watch:
```bash
adb devices
```

Install the Wear APK on the watch only:
```bash
adb -s WATCH_REAL_IP:DEBUG_PORT install wear/build/outputs/apk/debug/wear-debug.apk
```

After installation, open HeartMusic on the watch once and grant the `BODY_SENSORS` permission.

Useful Wear/sensor logs:
```bash
adb logcat | grep "WearableSamplingSender\|WearableMessageListener\|SamplingMessageService\|SamplingService\|WearSensorRepository\|WearDataSender"
```

If installation fails with:
```text
INSTALL_FAILED_MISSING_SHARED_LIBRARY: ... com.google.android.wearable
```
you are installing the Wear APK on the phone instead of the watch. Run `adb devices` and install again with `adb -s WATCH_DEVICE_ID ...`.

### Backend / Docker

Start backend and database:
```bash
cd backend
docker compose up -d
```

Start backend and print logs in the foreground:
```bash
cd backend
docker compose up
```

Stop backend and database:
```bash
cd backend
docker compose down
```

Check running containers:
```bash
cd backend
docker compose ps
```

Restart only the backend container:
```bash
cd backend
docker compose restart web
```

Test the API from the host machine:
```bash
curl -s http://localhost:5005/api/songs
curl -s http://localhost:5005/api/playlists
```

Test the API and count songs:
```bash
curl -s http://localhost:5005/api/songs | python3 -c 'import sys, json; print(len(json.load(sys.stdin)["songs"]))'
```

### Troubleshooting Checklist

If the app does not see the backend on a real device:
1. Run `adb devices` and confirm the device is listed as `device`.
2. Recreate port forwarding with `adb reverse tcp:5005 tcp:5005`.
3. Confirm the backend is running with `docker compose ps`.
4. Check logs with `adb logcat | grep "ListeningVM\|EmotionAnalysisVM"`.

If Emotion Analysis does not appear:
1. Check the app logs for `SensorManager` and `SensorCollectionViewModel`.
2. Confirm the wearable/EEG data source is actually connected.
3. Remember that the app can only react to real collected signals, not only to the phone's internal sensors.

If EEG samples work but HR stays at `0 samples`:
1. Confirm the Wear APK is installed on the Pixel Watch, not on the phone.
2. Open the Wear app once and grant `BODY_SENSORS`.
3. Check phone logs for `Sent sampling request`.
4. Check watch logs for `Start sampling received` and `Sending snapshot`.
5. Check phone logs for `Message received on path=/sensor_series`.

---
*Project created for the MSSS course - 2025/2026*

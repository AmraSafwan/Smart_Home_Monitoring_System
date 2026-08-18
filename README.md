# 🏠 SmartHome - Smart Home Monitoring System

A full-stack smart home management platform that lets you monitor, control, and automate your home devices in real time. It consists of three tightly integrated components: an **Android mobile app**, a **web-based hardware simulator**, and a **Firebase cloud backend**.

---

## 📐 System Architecture

```
┌─────────────────────┐       ┌──────────────────────┐
│   Android App        │       │  Home Simulator (Web) │
│  (Jetpack Compose)   │       │  (React + Vite)       │
└────────┬────────────┘       └──────────┬────────────┘
         │                               │
         │        Firestore (Real-time)  │
         └──────────────┬────────────────┘
                        │
          ┌─────────────▼──────────────┐
          │  Cloud Firestore Database   │
          │  (devices, rooms, floors,   │
          │   alerts, usageLogs)        │
          └─────────────┬──────────────┘
                        │
          ┌─────────────▼──────────────┐
          │  Cloud Functions (Node.js)  │
          │  - onDeviceUpdated          │
          │  - safetyCutoffWorker       │
          │  - lightScheduleWorker      │
          │  - Callable APIs            │
          └────────────────────────────┘
```

---

## 📁 Project Structure

```
Smart_Home_Monitoring_System/
├── frontend/                  # Android mobile app (Jetpack Compose + Kotlin)
│   └── app/src/main/java/
│       └── com/example/smarthome/
│           ├── data/
│           │   ├── model/     # Device, Floor, Room, UsageLog models
│           │   └── repository/
│           ├── ui/            # Compose screens (home, device, floor, camera, reports, auth)
│           ├── viewmodel/     # ViewModels per feature
│           └── MainActivity.kt
├── home-simulator/            # Web-based hardware simulator (React + Vite)
│   └── src/
│       ├── App.jsx            # Main simulator UI
│       └── firebase.js        # Firestore SDK config
├── functions/                 # Firebase Cloud Functions (Node.js v24)
│   └── index.js               # All cloud functions
├── firestore.rules            # Firestore security rules
├── firestore.indexes.json     # Composite indexes for queries
└── firebase.json              # Firebase project config
```

---

## ✨ Features

### 📱 Android App
- **Floor & Room Management** — Navigate your home by floor and room
- **Device Control** — Toggle lights, outlets, multi-switch gang boxes, security cameras, and safety appliances (iron)
- **Floor Plan View** — Visual grid-based map of device positions within a room
- **Camera Feed** — Live stream preview for CAMERA devices
- **Real-time Alerts** — View and acknowledge active alerts (device errors, disconnections, safety cutoffs)
- **Usage Reports** — Per-device usage logs and energy consumption tracking (Wh)
- **Automatic Scheduling** — Set ON/OFF times for any device
- **Firebase Auth** — User authentication (login/register)

### 🌐 Home Simulator (Web)
- Simulates physical IoT hardware by reading/writing directly to Firestore
- Supports all device types:
  - **Standard devices** — Toggle ON/OFF
  - **Multi-Switch Gang Box** — Independently toggle each sub-switch
  - **Security Camera** — Toggle power, simulate disconnection/reconnection, live stream preview
  - **Iron (Safety Appliance)** — Toggle power, simulate overheat/fault
- Periodic telemetry logging every 10 seconds for active devices

### ☁️ Cloud Functions

| Function | Trigger | Description |
|---|---|---|
| `onDeviceUpdated` | Firestore write | Logs usage on status change; raises alerts for ERROR/DISCONNECTED states |
| `safetyCutoffWorker` | Cron (every 1 min) | Auto-shuts off safety-critical devices that exceed their max ON duration |
| `lightScheduleWorker` | Cron (every 1 min) | Applies scheduled ON/OFF times to devices |
| `toggleDevice` | Callable | Mobile app turns a device ON or OFF |
| `toggleSwitch` | Callable | Mobile app toggles a specific sub-switch on a MULTI_SWITCH device |
| `acknowledgeAlert` | Callable | Marks an alert as acknowledged |
| `getDeviceUsage` | Callable | Returns last 100 usage log entries for a device |
| `getActiveAlerts` | Callable | Returns up to 50 unacknowledged alerts |

---

## 🗄️ Firestore Data Model

| Collection | Key Fields |
|---|---|
| `devices` | `name`, `type`, `status`, `floorId`, `roomId`, `safetyCritical`, `maxOnDuration`, `schedule`, `subSwitches`, `turnedOnAt`, `energyConsumptionWh` |
| `floors` | `name` |
| `rooms` | `name`, `floorId` |
| `alerts` | `deviceId`, `deviceName`, `type`, `severity`, `message`, `acknowledged`, `timestamp` |
| `usageLogs` | `deviceId`, `deviceName`, `deviceType`, `roomId`, `floorId`, `action`, `timestamp` |

### Device Types

| Type | Description |
|---|---|
| `LIGHT` | Standard binary light switch |
| `OUTLET` | Power outlet |
| `MULTI_SWITCH` | Gang box with multiple sub-switches |
| `CAMERA` | Security camera with stream URL |
| `IRON` | Safety-critical appliance with auto-cutoff |
| `SAFETY_DEVICE` | Other safety-critical appliance |

### Alert Types
- `DEVICE_ERROR` — Device reported an ERROR status (severity: MEDIUM)
- `DEVICE_DISCONNECTED` — Device went offline (severity: HIGH)
- `SAFETY_CUTOFF` — Device auto-shutdown due to max ON duration exceeded (severity: HIGH)

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Android App | Kotlin, Jetpack Compose, Material3, Firebase SDK, Navigation Compose, ViewModel, Coil |
| Web Simulator | React 19, Vite 8, Firebase JS SDK v12 |
| Backend | Firebase Cloud Functions v2 (Node.js 24) |
| Database | Cloud Firestore |
| Authentication | Firebase Auth |
| Region | `asia-south1` |
| Time Zone | `Asia/Colombo` |


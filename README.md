# Islamic Community Event & Prayer Scheduling App

An Android-based application designed to manage Islamic community events and prayer schedules efficiently. The system automates prayer time notifications, event scheduling, and administrative monitoring to reduce manual record-keeping and improve community coordination.

## 📱 Features

### User Features
- View daily prayer schedules
- Receive prayer time notifications and alarms
- Browse Islamic community events
- Join or unjoin events with remarks
- Access event and prayer information in real time

### Admin Features
- Manage prayer schedules
- Create and update community events
- View user participation
- Send SMS notifications
- Access event and prayer analytics dashboards

## 🛠️ Technologies Used

- **Platform:** Android
- **Language:** Java
- **IDE:** Android Studio
- **Database:** Firebase / Local Database (as configured)
- **Minimum SDK:** 24
- **Target SDK:** 35
- **Notifications:** AlarmManager & Foreground Services
- **Analytics:** WebView-based dashboards (HTML)

## 📂 Project Structure
islamic/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/islamic/
│   │   │   │   ├── admin/        # Admin-related activities and logic
│   │   │   │   ├── user/         # User-related activities and logic
│   │   │   │   ├── adapters/     # RecyclerView adapters
│   │   │   │   ├── models/       # Data models (Events, Prayer Times, Users)
│   │   │   │   ├── services/     # Background services (alarms, notifications)
│   │   │   │   └── utils/        # Helper and utility classes
│   │   │   ├── res/
│   │   │   │   ├── layout/       # XML layout files
│   │   │   │   ├── drawable/     # Images and icons
│   │   │   │   ├── values/       # Colors, strings, and styles
│   │   │   │   └── mipmap/       # App launcher icons
│   │   │   ├── assets/
│   │   │   │   └── dashboard/    # HTML files for analytics dashboards
│   │   │   └── AndroidManifest.xml
│   │   └── test/                # Unit tests
│   └── build.gradle
├── gradle/                       # Gradle wrapper files
├── build.gradle                  # Project-level Gradle configuration
├── settings.gradle
└── README.md


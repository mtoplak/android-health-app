# Health App

Android app for entering, storing, and syncing health measurements.

## What this project includes

- Jetpack Compose + Material 3 UI
- MVVM architecture with Repository layer
- Room local database (local-first behavior)
- Firebase Authentication (email/password)
- Cloud Firestore sync per user (`userId`)
- Input validation, edit/delete/list/details flows
- Sensor or mock API autofill support for selected fields
- Localization with Slovenian and English strings

## Tech stack

- Kotlin
- Jetpack Compose
- Room + KSP
- Navigation Compose
- ViewModel + StateFlow
- Kotlin Coroutines + Flow
- Firebase Auth + Firestore
- Retrofit + Gson

## SDK targets

- `minSdk = 28`
- `targetSdk = 36`
- `compileSdk = 36`

## Firebase and Gradle setup

This project uses a Version Catalog (`gradle/libs.versions.toml`) and plugin aliases.

### 1) Place Firebase config file

- Copy `google-services.json` to `app/google-services.json`.

### 2) Use required versions

In `gradle/libs.versions.toml`:

- `googleServices = "4.4.4"`
- `firebaseBom = "34.11.0"`

### 3) Root Gradle plugin registration

In `build.gradle.kts`:

```kotlin
plugins {
    // ...
    alias(libs.plugins.google.services) apply false
}
```

### 4) App module plugin and Firebase SDKs

In `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    // or alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
    // or alias(libs.plugins.google.services)
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))

    // add Firebase products you use
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
```

Equivalent alias-based setup already used in this project:

- `implementation(platform(libs.firebase.bom))`
- `implementation(libs.firebase.auth)`
- `implementation(libs.firebase.firestore)`

### 5) Sync project

```powershell
.\gradlew.bat --refresh-dependencies
```

## Firebase Console checklist

1. Create a Firebase project.
2. Add Android app with package `com.example.health_app`.
3. Enable `Authentication > Sign-in method > Email/Password`.
4. Create Firestore database (test mode for development).

## Firestore security rules (recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /meritve/{meritevId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null
                    && request.auth.uid == request.resource.data.userId;
    }
  }
}
```

## Run

```powershell
.\gradlew.bat :app:assembleDebug
```

Then run from Android Studio on an emulator/device (API 28+).

## Quick verification checklist

- Register user with valid email and password (6+ chars)
- Login and open measurement input screen
- Save measurement and confirm list/details update
- Trigger cloud sync and verify Firestore documents
- Logout and confirm app returns to auth screen
- Switch language and verify translated strings

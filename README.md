# Health App (Android)

A simple Android app for entering and storing personal health measurements.

The app is built with **Jetpack Compose** for UI, **Room** for local persistence, and follows **MVVM + Repository** architecture.

## What the app does

- Add a new health measurement (name, surname, date, heart rate, SpO2, temperature)
- Validate input values before saving
- Save measurements to a local Room database
- Show details of a selected measurement
- Show all saved measurements in a list
- Edit existing measurements
- Delete measurements (swipe-to-delete with confirmation)
- Show Snackbar feedback for insert/update/delete/error operations
- Support Slovenian and English UI strings

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- ViewModel + StateFlow
- Kotlin Coroutines + Flow
- Room (with KSP)

## Requirements

- Android Studio (recent stable version)
- Android SDK:
  - **minSdk = 28**
  - **targetSdk = 36**
  - **compileSdk = 36**
- JDK 11+

## Project structure

```text
app/src/main/java/com/example/health_app/
  data/
    Meritev.kt
    MeritevDao.kt
    MeritevDatabase.kt
    MeritevRepository.kt
  ui/
    navigation/
      NavGraph.kt
    screens/
      VnosMeritveScreen.kt
      SeznamMeritevScreen.kt
      PodrobnostiMeritveScreen.kt
    theme/
  viewmodel/
    MeritevViewModel.kt
  MainActivity.kt

app/src/main/res/
  values/strings.xml
  values-en/strings.xml
```

## Setup instructions

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Make sure an emulator/device with API 28+ is available.
4. Run the `app` configuration.

## Run from terminal

```powershell
Set-Location "D:\1\TZVA\android-health-app"
.\gradlew.bat assembleDebug
```

Optional Kotlin compile check:

```powershell
Set-Location "D:\1\TZVA\android-health-app"
.\gradlew.bat :app:compileDebugKotlin
```

## Navigation flow

- `VnosMeritveScreen` (input form)
  - Save -> persists measurement and opens details/list flow
  - "Seznam meritev" button -> opens list
- `SeznamMeritevScreen` (all measurements)
  - Tap card -> open details
  - Edit icon -> open input form pre-filled
  - Swipe left -> delete confirmation
- `PodrobnostiMeritveScreen` (single measurement details)
  - Back navigation
  - Edit action

## Validation rules

Before saving:

- `ime` and `priimek` must not be empty
- `srcniUtrip` must be between **30 and 250** bpm
- `SpO2` must be between **0 and 100**
- `temperatura` must be between **34.0 and 42.0** °C

Invalid fields display inline error text.

## Database details

- Entity: `Meritev`
- Table: `meritve`
- DAO operations:
  - `insert`
  - `update`
  - `delete`
  - `getAll(): Flow<List<Meritev>>`
  - `getById(id: Int): Flow<Meritev?>`

`MeritevDatabase` is implemented as a singleton Room database.

## Language support

All UI strings are externalized:

- Slovenian: `app/src/main/res/values/strings.xml`
- English: `app/src/main/res/values-en/strings.xml`

## Notes

- Date input uses Material 3 `DatePickerDialog`.
- The app uses reactive flows from Room so the list updates automatically when data changes.
- Current Room setup uses destructive migration fallback for schema version changes during development.

## Quick test checklist

- Insert valid measurement -> success snackbar appears
- Invalid inputs -> field-level error messages appear
- Open list -> new measurement is visible
- Open details -> all fields shown correctly
- Edit measurement -> changes are saved
- Delete measurement -> item is removed after confirmation
- Switch device language (SL/EN) -> strings change correctly


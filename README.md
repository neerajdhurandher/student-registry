# Student Registry (Android)

Kotlin + Jetpack Compose Android app for teachers to manage student records with:

- Google Sign-In
- Student CRUD (add, list, search/filter, edit, delete with confirmation)
- Local Room database
- Optional cloud backup sync to Firebase Firestore when internet is available
- Birthday reminder notifications
- CSV/PDF export from the currently filtered list
- Settings for theme, font scale, notification, and cloud sync toggle

## Build

```bash
./gradlew assembleDebug
```

## Notes

- Firebase sync is implemented and automatically used when Firebase is configured (for example with `google-services.json` in `app/` and a valid Firebase project).
- Without Firebase config, the app continues to work with local Room storage.
# Repository Guidelines

## Project Structure & Module Organization
- `app/src/main/java/com/example/real_estate_manager/`: Kotlin source code (UI in `ui/`, data layer in `data/`, DI in `di/`, navigation in `navigation/`).
- `app/src/main/res/`: Android resources (layouts, drawables, strings).
- `app/src/main/AndroidManifest.xml`: app manifest.
- `app/src/test/`: local JVM unit tests (JUnit4).
- `app/src/androidTest/`: instrumented tests (AndroidX + Espresso/Compose).

## Build, Test, and Development Commands
- `./gradlew :app:assembleDebug`: build a debug APK.
- `./gradlew :app:installDebug`: install the debug build on a connected device/emulator.
- `./gradlew :app:testDebugUnitTest`: run JVM unit tests.
- `./gradlew :app:connectedDebugAndroidTest`: run instrumented tests on a device/emulator.
- `./gradlew :app:lint`: run Android Lint.

## Coding Style & Naming Conventions
- Kotlin + Jetpack Compose; follow standard Kotlin formatting (4-space indentation).
- Compose UI functions are annotated with `@Composable` and use `PascalCase` names (e.g., `BillsListScreen`).
- Data models/entities are `PascalCase`; variables and functions are `camelCase`.
- Package names are lowercase (e.g., `com.example.real_estate_manager.ui`).
- No enforced formatter/linter is configured; keep changes consistent with nearby files.

## Testing Guidelines
- Unit tests live in `app/src/test/` and use JUnit4.
- Instrumented/UI tests live in `app/src/androidTest/` and use AndroidX + Espresso/Compose.
- Name tests with `*Test` suffix (e.g., `ExampleUnitTest`).

## Commit & Pull Request Guidelines
- Commit history mixes styles (e.g., `feat: ...`, `fix: ...`, or plain sentences). Keep messages short, imperative, and scoped when helpful (e.g., `feat(stats): add period filter`).
- PRs should include a clear description, the motivation, and screenshots/GIFs for UI changes.
- Link relevant issues/tasks when available.

## Configuration Tips
- Use Android Studio for local development; sync Gradle after changes to `build.gradle.kts`.
- Store local SDK paths in `local.properties` (do not commit secrets).

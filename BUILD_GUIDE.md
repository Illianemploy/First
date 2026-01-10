# Common Gradle Build Commands

## 🎯 Quick Build Commands

### For Development (Debug APK):
```bash
# Build debug APK
./gradlew assembleDebug

# Or on Windows:
gradlew.bat assembleDebug
```

### For Release (Production APK):
```bash
# Build release APK
./gradlew assembleRelease

# Or on Windows:
gradlew.bat assembleRelease
```

### Clean Build:
```bash
# Clean all build artifacts
./gradlew clean

# Clean and rebuild
./gradlew clean assembleDebug

# On Windows:
gradlew.bat clean assembleDebug
```

---

## 🔧 Android Studio Build Options

### Menu Options:
- **Build → Make Project** (Ctrl+F9 / Cmd+F9) - Compiles changed files
- **Build → Rebuild Project** - Clean build everything
- **Build → Build Bundle(s) / APK(s) → Build APK(s)** - Creates APK file
- **Run → Run 'app'** (Shift+F10) - Build and run on device/emulator

### Run Configurations:
If you see "testClasses" errors:
1. **Run → Edit Configurations**
2. Select your "app" configuration
3. Under "Before launch":
   - Remove any "Gradle-aware Make" tasks that mention "testClasses"
   - Keep only "Gradle-aware Make" with task "assembleDebug"

---

## ⚠️ Common Build Errors & Fixes

### Error: "Cannot locate tasks that match ':app:testClasses'"
**Fix:** Don't run test tasks, just build the app:
```bash
# Wrong:
./gradlew :app:testClasses

# Right:
./gradlew assembleDebug
```

### Error: "Unresolved reference" or "Redeclaration"
**Fix:** Pull latest code and clean:
```bash
git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML
./gradlew clean
```
Then in Android Studio: **File → Invalidate Caches → Restart**

### Error: Build cache issues
**Fix:** Nuclear clean:
```bash
./gradlew clean
rm -rf .gradle
rm -rf app/build
./gradlew assembleDebug
```

---

## 📱 Run on Device/Emulator

### From Command Line:
```bash
# Build and install debug APK
./gradlew installDebug

# Build, install, and launch
./gradlew installDebug
adb shell am start -n com.uktobacco/.MainActivity
```

### From Android Studio:
1. Select device/emulator from dropdown (top toolbar)
2. Click green "Run" button (▶️) or press Shift+F10
3. App will build, install, and launch automatically

---

## 🧪 Testing (Optional)

If you want to run tests (currently not configured):
```bash
# Run all tests (requires test configuration)
./gradlew test

# Run Android instrumented tests (requires test code)
./gradlew connectedAndroidTest
```

**Note:** Your project doesn't have test tasks configured yet. That's okay!
You don't need tests to build and run the app.

---

## 📦 Find Built APK

After building, find your APK at:
```
app/build/outputs/apk/debug/app-debug.apk
```

For release builds:
```
app/build/outputs/apk/release/app-release.apk
```

---

## ✅ Recommended Build Workflow

### Daily development:
1. Pull latest changes: `git pull origin <branch>`
2. Clean build: `./gradlew clean`
3. Build: `./gradlew assembleDebug`
4. Or use Android Studio: **Run → Run 'app'** (Shift+F10)

### After Git pull:
1. `./gradlew clean`
2. **File → Sync Project with Gradle Files** (in Android Studio)
3. **File → Invalidate Caches → Restart** (if needed)
4. `./gradlew assembleDebug`

---

## 🎯 Quick Reference

| Task | Command |
|------|---------|
| Build debug | `./gradlew assembleDebug` |
| Build release | `./gradlew assembleRelease` |
| Clean | `./gradlew clean` |
| Clean + build | `./gradlew clean assembleDebug` |
| Install on device | `./gradlew installDebug` |
| List all tasks | `./gradlew tasks --all` |

---

**Current Branch:** `claude/add-error-handling-mk6v6522i9elrgdi-dBwML`
**Last Updated:** 2026-01-09

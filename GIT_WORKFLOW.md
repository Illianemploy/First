# Git Branch Workflow for Android Studio

## 🎯 Current Branch Information

**Active Branch:** `claude/add-error-handling-mk6v6522i9elrgdi-dBwML`
**Main Branch:** `main` (or `master`)

---

## 📍 Check Where You Are

### In Terminal:
```bash
# See current branch
git branch

# See current branch with recent commits
git status

# See all branches (local and remote)
git branch -a
```

### In Android Studio:
- **Bottom-right corner** → Shows current branch name (e.g., "claude/add-error-handling...")
- **Git → Branches** → Shows all branches

---

## 🔄 Pull Latest Changes

### Before Building:
```bash
# Make sure you're on the right branch
git checkout claude/add-error-handling-mk6v6522i9elrgdi-dBwML

# Pull latest changes from remote
git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML
```

### In Android Studio:
1. **VCS → Git → Pull** (or Ctrl+T / Cmd+T)
2. Select the correct remote branch
3. Click "Pull"

---

## 🧹 Clean Build After Pulling

**IMPORTANT:** Always clean after pulling code changes!

### Option 1 - Terminal:
```bash
./gradlew clean
./gradlew assembleDebug
```

### Option 2 - Android Studio:
1. **Build → Clean Project**
2. Wait for it to finish
3. **Build → Rebuild Project**

### Option 3 - Nuclear Option (if still having issues):
1. **File → Invalidate Caches → Invalidate and Restart**
2. After restart: **Build → Clean Project**
3. Then: **Build → Rebuild Project**

---

## 🔍 Check for Uncommitted Changes

```bash
# See what files you've changed locally
git status

# See the actual changes
git diff

# Discard all local changes (BE CAREFUL!)
git reset --hard HEAD
```

---

## 📦 Typical Workflow

### When starting work:
```bash
# 1. Make sure you're on the right branch
git checkout claude/add-error-handling-mk6v6522i9elrgdi-dBwML

# 2. Pull latest changes
git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML

# 3. Clean build
./gradlew clean

# 4. Build
./gradlew assembleDebug
```

### When you see compilation errors:
```bash
# 1. Pull latest (you might be missing new files)
git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML

# 2. Clean and rebuild
./gradlew clean
./gradlew assembleDebug

# 3. If still failing, invalidate caches in Android Studio
# File → Invalidate Caches → Invalidate and Restart
```

---

## 🎨 Android Studio Git Panel

### View Git Tool Window:
- **View → Tool Windows → Git** (Alt+9 / Cmd+9)

This shows:
- **Log** tab: All commits and branches
- **Console** tab: Git command output
- **Branches** dropdown: Switch branches easily

### Common Actions:
- **Right-click branch** → Checkout: Switch to that branch
- **Right-click branch** → Update: Pull latest changes
- **Right-click branch** → Compare with Current: See differences

---

## ⚠️ Common Issues & Solutions

### Issue: "Unresolved reference" errors
**Solution:**
```bash
git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML
./gradlew clean
# Then in Android Studio: File → Invalidate Caches → Restart
```

### Issue: "Changes not staged for commit"
**Solution:**
```bash
# See what changed
git status

# If you want to keep changes
git add .
git commit -m "Your message"

# If you want to discard changes
git reset --hard HEAD
```

### Issue: "Your branch is behind 'origin/...'"
**Solution:**
```bash
git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML
```

### Issue: Build cache problems
**Solution:**
```bash
./gradlew clean
rm -rf .gradle/
rm -rf app/build/
./gradlew assembleDebug
```

---

## 🚀 Pro Tips

1. **Before every build session:** Pull first!
   ```bash
   git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML
   ```

2. **Keep Android Studio synced:**
   - After pulling, click "Sync Now" if the yellow banner appears

3. **Use Android Studio's Git integration:**
   - Bottom-right corner shows current branch
   - Click it to switch branches quickly

4. **Gradle sync:**
   - After pulling: **File → Sync Project with Gradle Files**

5. **Check commit history:**
   ```bash
   git log --oneline -10
   ```

---

## 📝 Branch Name Decoder

Current branch: `claude/add-error-handling-mk6v6522i9elrgdi-dBwML`

- `claude/` = Prefix (indicates Claude Code created it)
- `add-error-handling` = Feature description
- `mk6v6522i9elrgdi-dBwML` = Unique session ID

**Main branch:** Usually `main` or `master`
**Feature branches:** Like `claude/add-error-handling-...`

---

## 🎯 Quick Commands Card

Keep this handy:

```bash
# Where am I?
git branch

# Pull latest
git pull origin claude/add-error-handling-mk6v6522i9elrgdi-dBwML

# Clean build
./gradlew clean && ./gradlew assembleDebug

# What changed?
git status

# See commits
git log --oneline -5

# Reset everything (dangerous!)
git reset --hard HEAD
```

---

## ✅ Pre-Build Checklist

Before building in Android Studio:

- [ ] Check current branch (bottom-right corner)
- [ ] Pull latest changes (`git pull`)
- [ ] Clean project (**Build → Clean Project**)
- [ ] Sync Gradle files (**File → Sync Project with Gradle Files**)
- [ ] Build project (**Build → Rebuild Project**)

---

**Last Updated:** 2026-01-09
**Current Feature Branch:** `claude/add-error-handling-mk6v6522i9elrgdi-dBwML`

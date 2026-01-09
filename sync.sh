#!/bin/bash
# Quick sync script for Git + Android Studio workflow

echo "🔍 Checking Git status..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Show current branch
CURRENT_BRANCH=$(git branch --show-current)
echo "📍 Current branch: $CURRENT_BRANCH"
echo ""

# Show status
echo "📊 Git status:"
git status --short
echo ""

# Show recent commits
echo "📝 Recent commits:"
git log --oneline -3
echo ""

# Ask if user wants to pull
read -p "🔄 Pull latest changes? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "⬇️  Pulling from origin/$CURRENT_BRANCH..."
    git pull origin "$CURRENT_BRANCH"
    echo ""

    read -p "🧹 Clean build? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🧹 Cleaning..."
        ./gradlew clean
        echo ""
        echo "✅ Clean complete!"
        echo ""
        echo "🎯 Next steps in Android Studio:"
        echo "   1. File → Sync Project with Gradle Files"
        echo "   2. Build → Rebuild Project"
    fi
fi

echo ""
echo "✅ Sync check complete!"

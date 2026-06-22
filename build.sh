#!/bin/bash
# build.sh: Quick build script for gouji-card-counter
# Usage: ./build.sh

echo "🚀 Building Gouji Card Counter..."

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    echo "❌ Gradle wrapper not found. Generating..."
    gradle wrapper 2>/dev/null || echo "⚠️ Could not generate gradle wrapper"
fi

# Clean and build
echo "📦 Cleaning project..."
./gradlew clean 2>/dev/null

echo "🔨 Building debug APK..."
./gradlew assembleDebug 2>/dev/null

if [ $? -eq 0 ]; then
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "✅ Build successful!"
        echo "📍 APK location: $APK_PATH"
        ls -lh "$APK_PATH"
    else
        echo "❌ APK not found after build"
    fi
else
    echo "❌ Build failed"
    echo "💡 Try: ./gradlew assembleDebug --info"
fi

echo ""
echo "📋 Next steps:"
echo "1. Copy APK to your Android device"
echo "2. Install and grant all permissions"
echo "3. Enable floating window and screen capture"
echo "4. Start counting cards!"

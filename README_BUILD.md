# Build Instructions

## Prerequisites
- JDK 17 (Amazon Corretto or OpenJDK)
- Android SDK (API 34, build-tools 34.0.0)
- Gradle 8.5 (included via wrapper)

## Merge BubbleService.java

The `BubbleService.java` file is split into two parts due to file size limits:
- `app/src/main/java/com/hwcloud/chatfloat/BubbleService_part1.java`
- `app/src/main/java/com/hwcloud/chatfloat/BubbleService_part2.java`

To merge them:
```bash
cd app/src/main/java/com/hwcloud/chatfloat
cat BubbleService_part1.java BubbleService_part2.java > BubbleService.java
```

## Build APK

```bash
# Generate gradle wrapper if missing
git clone --depth 1 https://github.com/gradle/gradle.git /tmp/gradle
cp -r /tmp/gradle/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
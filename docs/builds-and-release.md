# Builds and releases

How the game is packaged, and the tooling traps that cost time. Exact versions live in the
Gradle files and the app config.

## Version bump locations

Three places move together:

- the root Gradle `version` (it drives the desktop jar name),
- the Android `versionCode` and `versionName`,
- the app version string that the window title shows.

## Desktop

- The desktop dist task builds a fat jar with the assets bundled, so it runs standalone from
  any folder.
- A small launcher exe can sit next to the jar. It finds any desktop jar in its own folder,
  starts it with the windowless java launcher, forwards command line arguments (so spectate
  and debug flags keep working), and shows a message box if the jar or Java is missing.
- The launcher is compiled with the C# compiler that ships with Windows, no extra tooling
  needed. Its source is not checked into the repo yet.
- The launcher icon is made from the Android launcher PNG converted to an ico.

## Android

- The Android debug assemble task builds the debug APK.
- Build requirements: a compatible JDK (the Gradle wrapper is old and rejects very new JDKs),
  a valid SDK path in `local.properties`, AndroidX enabled, and a compile SDK new enough for
  the gdx backend. The minimum SDK is high enough that multidex is native, so no support
  library is needed.
- The device-visible app name lives in the Android string resources. Keep it in sync with the
  in-game name, it went stale once.

## Desktop app images

- A bundled-JRE app image is a folder with an exe, the jar, a json config, a `jre` folder and
  optionally maps.
- The launcher exe does not forward the game's console output. To read logs or reproduce a
  crash, run the image's own bundled java directly on its jar with output redirected.
- The game runs fine on an old Java runtime, the desktop jar targets Java 8.

## Build output folder

Convention used on this machine: copy the versioned desktop jar, the debug APK and the
launcher exe into the builds folder, and refresh the jar inside any app image that is kept
there.

## Commands

Build both desktop and Android:

    .\gradlew.bat :desktop:dist :android:assembleDebug

If the build complains about class file versions, point `JAVA_HOME` at a compatible JDK for
that command. If the Android SDK is not found, check `local.properties`.

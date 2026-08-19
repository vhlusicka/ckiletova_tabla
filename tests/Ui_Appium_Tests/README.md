# Android Appium tests

This directory contains the Android regression suite derived from [`../CT Test Specification.md`](../CT%20Test%20Specification.md). It uses WebdriverIO, Mocha, Appium 2, and the UiAutomator2 driver. It does not use Cucumber.

## Suite structure

- `specs/setup.spec.js` — Feature 01, CT-01-001 through CT-01-007
- `specs/league.spec.js` — Feature 02, CT-02-001 through CT-02-009
- `specs/knockout.spec.js` — Feature 03, CT-03-001 through CT-03-008
- `helpers/app.js` — reusable Android interactions and tournament setup flows
- `wdio.conf.js` — physical-device capabilities and automatic Appium server configuration

Every documented test case has one normal Mocha `it()` block.

## Physical Android device preparation

1. Enable **Developer options** and **USB debugging** on the device.
   On OEM firmware that provides an additional **USB debugging (Security
   settings)**, **USB debugging input**, **Disable permission monitoring**, or
   similarly named option, enable it as well. Appium must be allowed to inject
   input events.
2. Connect the device by USB and accept its RSA authorization prompt.
3. Confirm that ADB can see it:

   ```sh
   adb devices -l
   ```

4. Build the signed APK if it is not already present:

   ```sh
   ../../gradlew -p ../.. assembleRelease
   ```

The default APK is `../../app/build/outputs/apk/release/Ckiletova-tabla-1.1.5.apk`.

## Install dependencies

From this directory:

```sh
npm install
npm run driver:install
```

The repository pins Appium 2.19.0, WebdriverIO 8.41.0, and UiAutomator2 4.2.9 for compatibility with Node.js 18.

## Run tests

If exactly one authorized Android device is connected:

```sh
npm test
```

If multiple devices are connected, specify the physical device serial shown by `adb devices`:

```sh
ANDROID_UDID=YOUR_DEVICE_SERIAL npm test
```

Run one feature only:

```sh
npm run test:setup
npm run test:league
npm run test:knockout
```

Use another APK when required:

```sh
APP_PATH=/absolute/path/to/application.apk npm test
```

The WebdriverIO Appium service starts and stops the local Appium server automatically. Tests run serially because they share one physical device. Each scenario clears the app's data in place to create clean state unless the scenario explicitly tests persistence. The run stops after its first failure.

Verify that every documented CT ID has exactly one `it()` block:

```sh
npm run verify
```

## Useful diagnostics

```sh
npm run driver:list
adb shell getprop ro.build.version.release
adb shell pm list packages | grep debelatabla
```

Confirm that the device permits automated input before starting the suite:

```sh
adb shell input tap 100 100
```

If this reports `INJECT_EVENTS permission`, the required OEM security option is
still disabled and Appium cannot automate that device.

Set `WDIO_LOG_LEVEL=debug` for detailed WebdriverIO logs. Appium service logs are written under `tests/Ui_Appium_Tests/logs/`.

const path = require('node:path');
const fs = require('node:fs');

const projectRoot = path.resolve(__dirname, '../..');
const localPropertiesPath = path.join(projectRoot, 'local.properties');

if (!process.env.ANDROID_HOME && !process.env.ANDROID_SDK_ROOT) {
  const localProperties = fs.existsSync(localPropertiesPath)
    ? fs.readFileSync(localPropertiesPath, 'utf8')
    : '';
  const sdkDirectory = localProperties
    .split(/\r?\n/)
    .find((line) => line.startsWith('sdk.dir='))
    ?.slice('sdk.dir='.length)
    .replace(/\\:/g, ':')
    .replace(/\\\\/g, '\\');

  if (sdkDirectory) {
    process.env.ANDROID_HOME = sdkDirectory;
    process.env.ANDROID_SDK_ROOT = sdkDirectory;
  }
}

process.env.APPIUM_HOME ||= path.join(__dirname, '.appium');
const defaultApk = path.join(
  projectRoot,
  'app/build/outputs/apk/release/Ckiletova-tabla-1.1.5.apk'
);

exports.config = {
  runner: 'local',
  specs: [path.join(__dirname, 'specs/**/*.spec.js')],
  maxInstances: 1,
  hostname: '127.0.0.1',
  port: 4723,
  path: '/',
  logLevel: process.env.WDIO_LOG_LEVEL || 'info',
  // Stop after the first failure so a device-level restriction cannot trigger
  // a clean reinstall for every remaining regression test.
  bail: 1,
  waitforTimeout: 10000,
  connectionRetryTimeout: 120000,
  connectionRetryCount: 2,
  framework: 'mocha',
  autoCompileOpts: {
    autoCompile: false
  },
  reporters: ['spec'],
  mochaOpts: {
    ui: 'bdd',
    timeout: 180000
  },
  services: [
    [
      'appium',
      {
        command: 'appium',
        args: {
          address: '127.0.0.1',
          port: 4723,
        relaxedSecurity: true
        },
        logPath: path.join(__dirname, 'logs')
      }
    ]
  ],
  capabilities: [
    {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': process.env.ANDROID_DEVICE_NAME || 'Android Device',
      ...(process.env.ANDROID_UDID ? { 'appium:udid': process.env.ANDROID_UDID } : {}),
      'appium:app': path.resolve(process.env.APP_PATH || defaultApk),
      'appium:appPackage': 'com.debelatabla.fifaleague',
      'appium:appActivity': '.MainActivity',
      'appium:autoGrantPermissions': false,
      'appium:noReset': true,
      'appium:newCommandTimeout': 240,
      'appium:disableWindowAnimation': true,
      'appium:ignoreHiddenApiPolicyError': true,
      'appium:skipDeviceInitialization': true
    }
  ]
};

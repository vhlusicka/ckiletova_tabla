const path = require('node:path');
const fs = require('node:fs');
const { startRun, recordTest, printSummary } = require('./helpers/test-report');

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
  'app/build/outputs/apk/release/Ckiletova-tabla-1.1.7.apk'
);

exports.config = {
  runner: 'local',
  specs: [
    path.join(__dirname, 'specs/CT-01_setup.spec.js'),
    path.join(__dirname, 'specs/CT-02_league.spec.js'),
    path.join(__dirname, 'specs/CT-03_knockout.spec.js'),
    path.join(__dirname, 'specs/CT-04_export.spec.js')
  ],
  maxInstances: 1,
  hostname: '127.0.0.1',
  port: 4723,
  path: '/',
  logLevel: process.env.WDIO_LOG_LEVEL || 'info',
  // Run the complete suite so the final report contains every test result.
  bail: 0,
  waitforTimeout: 10000,
  connectionRetryTimeout: 120000,
  connectionRetryCount: 2,
  framework: 'mocha',
  autoCompileOpts: {
    autoCompile: false
  },
  reporters: ['spec'],
  onPrepare: () => {
    if (!process.env.DEBELA_TEST_REPORT_FILE) startRun();
  },
  afterTest: async (test, context, { passed, error }) => {
    await recordTest(test.title, passed, browser, error);
  },
  afterHook: async (hook, context, { passed, error }) => {
    if (!passed && context?.currentTest?.title) {
      await recordTest(context.currentTest.title, false, browser, error);
    }
  },
  onComplete: () => {
    if (!process.env.DEBELA_TEST_WRAPPED) process.once('exit', printSummary);
  },
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

const {
  withAndroidManifest,
  withDangerousMod,
  withStringsXml,
  AndroidConfig,
} = require('@expo/config-plugins');
const fs = require('fs');
const path = require('path');

const PACKAGE_PATH = 'com/devclip/app';
const SRC_DIR = path.join(__dirname, 'android-src');

/**
 * Copies every .kt file (and the accessibility service config xml) from
 * plugins/android-src into the generated android project. This runs every
 * time `expo prebuild` runs — including automatically inside EAS Build — so
 * you never have to touch the generated /android folder by hand.
 */
function withDevClipNativeFiles(config) {
  return withDangerousMod(config, [
    'android',
    async (config) => {
      const projectRoot = config.modRequest.platformProjectRoot;
      const javaDir = path.join(
        projectRoot,
        'app/src/main/java',
        PACKAGE_PATH
      );
      const xmlDir = path.join(projectRoot, 'app/src/main/res/xml');
      fs.mkdirSync(javaDir, { recursive: true });
      fs.mkdirSync(xmlDir, { recursive: true });

      const ktFiles = [
        'ClipboardAccessibilityService.kt',
        'DevClipDatabaseHelper.kt',
        'OverlayService.kt',
        'OverlayModule.kt',
        'OverlayPackage.kt',
        'BootReceiver.kt',
      ];
      for (const file of ktFiles) {
        fs.copyFileSync(path.join(SRC_DIR, file), path.join(javaDir, file));
      }
      fs.copyFileSync(
        path.join(SRC_DIR, 'accessibility_service_config.xml'),
        path.join(xmlDir, 'accessibility_service_config.xml')
      );

      return config;
    },
  ]);
}

/** Adds the permissions and <service> declarations DevClip's native code needs. */
function withDevClipManifest(config) {
  return withAndroidManifest(config, (config) => {
    const androidManifest = config.modResults;
    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(androidManifest);

    // --- Permissions ---
    const permissions = [
      'android.permission.SYSTEM_ALERT_WINDOW',
      'android.permission.FOREGROUND_SERVICE',
      'android.permission.FOREGROUND_SERVICE_SPECIAL_USE',
      'android.permission.POST_NOTIFICATIONS',
      'android.permission.RECEIVE_BOOT_COMPLETED',
    ];
    androidManifest.manifest['uses-permission'] =
      androidManifest.manifest['uses-permission'] || [];
    for (const perm of permissions) {
      const exists = androidManifest.manifest['uses-permission'].some(
        (p) => p['$']['android:name'] === perm
      );
      if (!exists) {
        androidManifest.manifest['uses-permission'].push({
          $: { 'android:name': perm },
        });
      }
    }

    // --- Services ---
    mainApplication.service = mainApplication.service || [];

    const hasOverlayService = mainApplication.service.some(
      (s) => s['$']['android:name'] === '.OverlayService'
    );
    if (!hasOverlayService) {
      mainApplication.service.push({
        $: {
          'android:name': '.OverlayService',
          'android:exported': 'false',
          'android:foregroundServiceType': 'specialUse',
        },
      });
    }

    const hasAccessibilityService = mainApplication.service.some(
      (s) => s['$']['android:name'] === '.ClipboardAccessibilityService'
    );
    if (!hasAccessibilityService) {
      mainApplication.service.push({
        $: {
          'android:name': '.ClipboardAccessibilityService',
          'android:exported': 'true',
          'android:permission': 'android.permission.BIND_ACCESSIBILITY_SERVICE',
        },
        'intent-filter': [
          {
            action: [
              { $: { 'android:name': 'android.accessibilityservice.AccessibilityService' } },
            ],
          },
        ],
        'meta-data': [
          {
            $: {
              'android:name': 'android.accessibilityservice',
              'android:resource': '@xml/accessibility_service_config',
            },
          },
        ],
      });
    }

    // --- Receivers ---
    mainApplication.receiver = mainApplication.receiver || [];
    const hasBootReceiver = mainApplication.receiver.some(
      (r) => r['$']['android:name'] === '.BootReceiver'
    );
    if (!hasBootReceiver) {
      mainApplication.receiver.push({
        $: {
          'android:name': '.BootReceiver',
          'android:exported': 'true',
          'android:enabled': 'true',
        },
        'intent-filter': [
          {
            action: [{ $: { 'android:name': 'android.intent.action.BOOT_COMPLETED' } }],
          },
        ],
      });
    }

    return config;
  });
}

function withDevClipStrings(config) {
  return withStringsXml(config, (config) => {
    config.modResults = AndroidConfig.Strings.setStringItem(
      [
        {
          $: { name: 'devclip_accessibility_description' },
          _: 'Lets DevClip save text you copy anywhere on your phone, and paste a saved clip directly into the text field you were using.',
        },
      ],
      config.modResults
    );
    return config;
  });
}

/** Registers OverlayPackage inside MainApplication.kt's package list. */
function withDevClipMainApplication(config) {
  return withDangerousMod(config, [
    'android',
    async (config) => {
      const projectRoot = config.modRequest.platformProjectRoot;
      const mainAppPath = path.join(
        projectRoot,
        'app/src/main/java',
        PACKAGE_PATH,
        'MainApplication.kt'
      );
      if (!fs.existsSync(mainAppPath)) return config;

      let contents = fs.readFileSync(mainAppPath, 'utf8');
      if (!contents.includes('OverlayPackage()')) {
        contents = contents.replace(
          /(PackageList\(this\)\.packages)/,
          '$1.apply { add(OverlayPackage()) }'
        );
      }
      fs.writeFileSync(mainAppPath, contents);
      return config;
    },
  ]);
}

module.exports = function withDevClipNative(config) {
  config = withDevClipNativeFiles(config);
  config = withDevClipManifest(config);
  config = withDevClipStrings(config);
  config = withDevClipMainApplication(config);
  return config;
};

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
        'BootReceiver.kt',
        'Capture.kt',
        'ClipboardAccessibilityService.kt',
        'DevClipDatabaseHelper.kt',
        'DevClipEvents.kt',
        'DismissTargetView.kt',
        'ImeWatcher.kt',
        'OverlayModule.kt',
        'OverlayPackage.kt',
        'OverlayService.kt',
        'SelectionCapture.kt',
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

/**
 * Every string the native side shows the user.
 *
 * The JS side keeps its copy in src/strings.ts so it can be reviewed as copy
 * and localised through one seam. Native cannot read that file, so it gets the
 * same treatment through the mechanism Android provides: resources, not string
 * literals scattered through Kotlin.
 */
const NATIVE_STRINGS = [
  {
    name: 'devclip_accessibility_description',
    value:
      'Lets DevClip read the text you have highlighted, so tapping its bubble saves it, and paste a saved clip back into the field you were using.',
  },

  // Capture feedback. The preview is not decoration — reading a selection is
  // not perfect in every app, and seeing the right words back is how the user
  // knows the right thing was saved.
  { name: 'devclip_capture_saved', value: 'Saved \u201C%1$s\u201D' },
  {
    name: 'devclip_capture_saved_no_clipboard',
    value:
      'Saved \u201C%1$s\u201D \u2014 Android wouldn\u2019t put it on the clipboard, but DevClip has it.',
  },
  {
    name: 'devclip_capture_saved_large',
    value:
      'Saved %1$d characters. Android limits how much it hands between apps, so a selection this big may have arrived cut short \u2014 DevClip saved all of what it got.',
  },
  { name: 'devclip_capture_duplicate', value: 'That\u2019s already the clip at the top.' },
  {
    name: 'devclip_capture_password',
    value: 'Skipped that one \u2014 it looks like a password field.',
  },
  { name: 'devclip_capture_failed', value: 'DevClip couldn\u2019t save that. Try again.' },

  // The notification. It is the way back to a hidden bubble, so its copy has
  // to say what each button actually does. "Dismiss" is deliberately absent —
  // it implies a permanence hiding the bubble does not have.
  { name: 'devclip_notification_channel', value: 'DevClip bubble' },
  {
    name: 'devclip_notification_channel_description',
    value: 'A silent, ongoing notification while the DevClip bubble is running.',
  },
  { name: 'devclip_notification_title', value: 'DevClip is running' },
  { name: 'devclip_notification_text_awake', value: 'Tap the bubble to save what you\u2019ve highlighted.' },
  { name: 'devclip_notification_text_resting', value: 'Bubble hidden.' },
  { name: 'devclip_notification_hide', value: 'Hide bubble' },
  { name: 'devclip_notification_show', value: 'Show bubble' },
  { name: 'devclip_notification_turn_off', value: 'Turn off' },

  // Said with a Toast, because the windows have just been torn down and there
  // is nothing left to draw a message in.
  {
    name: 'devclip_bubble_hidden',
    value: 'Bubble hidden. Bring it back from the notification, or from inside DevClip.',
  },
  {
    name: 'devclip_bubble_hidden_no_notification',
    value: 'Bubble hidden. Bring it back from inside DevClip.',
  },

  // Failures that used to be silent. Every one of these was once a tap that
  // appeared to do nothing at all.
  {
    name: 'devclip_error_no_app',
    value: 'DevClip couldn\u2019t reach the app to draw its window.',
  },
  {
    name: 'devclip_error_start_app',
    value: 'DevClip couldn\u2019t start the app behind its window.',
  },
  { name: 'devclip_error_build_window', value: 'DevClip couldn\u2019t build its window.' },
  {
    name: 'devclip_error_place_window',
    value: 'DevClip couldn\u2019t place its window on screen.',
  },
  { name: 'devclip_error_open_window', value: 'DevClip couldn\u2019t open its window.' },
  { name: 'devclip_error_tap', value: 'DevClip couldn\u2019t handle that tap.' },
];

function withDevClipStrings(config) {
  return withStringsXml(config, (config) => {
    config.modResults = AndroidConfig.Strings.setStringItem(
      NATIVE_STRINGS.map((s) => ({ $: { name: s.name }, _: s.value })),
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

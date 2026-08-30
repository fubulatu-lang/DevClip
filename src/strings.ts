/**
 * Every user-visible string in DevClip.
 *
 * Kept in one place so copy can be reviewed as copy, and so the app has a
 * single seam to localise through later. Nothing here is formatted for a
 * specific screen — callers do their own layout.
 *
 * One UI writing: sentence case, verbs on buttons, errors that say what to
 * do next rather than what went wrong.
 */
export const strings = {
  app: {
    name: 'DevClip',
  },

  views: {
    small: 'Small',
    expanded: 'Expanded',
    full: 'Full app',
    a11ySuffix: 'view',
  },

  bubble: {
    on: 'Bubble on',
    off: 'Bubble off',
    turnOn: 'Turn on floating bubble',
    turnOff: 'Turn off floating bubble',
  },

  clips: {
    capture: 'Capture clipboard',
    captureA11y: 'Capture current clipboard',
    capturing: 'Loading…',
    emptyTitle: 'No clips yet',
    emptyBody: 'Copy something, then tap Capture below.',
    noMatches: (term: string) => `No clips match “${term}”`,
    when: {
      justNow: 'just now',
    },
    moreOptions: (title: string) => `More options for ${title}`,
    moreOptionsHint: 'Opens edit and delete.',
    fallbackTitle: 'this clip',
    pasteHint: 'Double tap to paste.',
    moveUp: 'Move clip up',
    moveDown: 'Move clip down',
  },

  paste: {
    confirmTitle: 'Paste this clip?',
    confirm: 'Paste',
    copiedBody: 'Could not paste automatically, so it’s on your clipboard — paste it manually.',
  },

  search: {
    placeholder: 'Search title or content',
    a11yLabel: 'Search clips',
  },

  sort: {
    dateDesc: 'Newest',
    dateAsc: 'Oldest',
    titleAsc: 'Title A–Z',
    titleDesc: 'Title Z–A',
    manual: 'Manual',
    a11yPrefix: 'Sort by',
  },

  edit: {
    title: 'Title',
    titlePlaceholder: 'Untitled',
    titleA11y: 'Clip title',
    content: 'Content',
    contentA11y: 'Clip content',
    save: 'Save',
    saveA11y: 'Save clip',
    delete: 'Delete',
    deleteA11y: 'Delete clip',
    close: 'Close edit sheet',
    deleteTitle: 'Delete this clip?',
    deleteBody: 'You can’t undo this.',
  },

  settings: {
    title: 'Settings',
    open: 'Settings',
    back: 'Back to clip list',

    permissions: 'Permissions',
    granted: 'Granted',
    notGranted: 'Off',

    appearance: 'Appearance',
    theme: 'Theme',
    themeLight: 'Light',
    themeDark: 'Dark',
    themeAuto: 'Auto',
    themeAutoA11y: 'Follow system',

    capture: 'Capture',
    devClientNote:
      'Background capture and the floating bubble need the custom dev client build — see SETUP_GUIDE.md.',
    backgroundCapture: 'Background capture',
    notifications: 'Notifications',
    floatingBubble: 'Floating bubble',
    enable: 'Enable',
    manage: 'Manage',
    start: 'Start',
    stop: 'Stop',

    bubbleSize: 'Bubble size',
    bubbleSmall: 'S',
    bubbleMedium: 'M',
    bubbleLarge: 'L',
    bubbleSmallA11y: 'Small',
    bubbleMediumA11y: 'Medium',
    bubbleLargeA11y: 'Large',

    autoStart: 'Auto-start after reboot',
    confirmPaste: 'Confirm before paste',
    on: 'On',
    off: 'Off',

    storage: 'Storage',
    keepAtMost: 'Keep at most',
    noLimit: 'No limit',
    keepAtMostA11y: (label: string) =>
      `Keep at most ${label === 'No limit' ? 'no limit' : `${label} clips`}`,

    exportBackup: 'Export backup',
    exporting: 'Exporting…',
    exportFailedBody: 'Could not create the backup file. Try again.',

    clearAll: 'Clear all clips',
    clearAllTitle: 'Clear all clips?',
    clearAllBody: 'This deletes everything in your history. You can’t undo this.',
  },

  onboarding: {
    heading: 'Set up DevClip',
    sub: 'Grant these permissions to finish setup. You can change them later in Settings.',
    notifications: 'Notifications',
    notificationsBody: 'Shows a quiet, permanent notification while capture is active.',
    backgroundCapture: 'Background capture',
    backgroundCaptureBody:
      'Lets DevClip save copies made in any app automatically, and paste a saved clip directly into whatever field you were using. Android will show a broader permission screen for this — that’s expected.',
    floatingBubble: 'Floating bubble',
    floatingBubbleBody: 'Lets DevClip draw the bubble on top of other apps.',
    enable: 'Enable',
    done: 'Done',
    pending: '…',
    enableA11y: (feature: string) => `Enable ${feature.toLowerCase()}`,
    enabledA11y: (feature: string) => `${feature} is enabled`,
    continue: 'Continue',
  },

  overlay: {
    expand: 'Expand',
    collapse: 'Shrink back to mini',
    openFullApp: 'Open the full app',
    close: 'Close DevClip',
  },

  common: {
    cancel: 'Cancel',
    dismiss: 'Dismiss',
    dismissError: 'Dismiss error',
  },
} as const;

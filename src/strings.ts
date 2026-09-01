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


  clips: {
    capture: 'Capture clipboard',
    captureA11y: 'Capture current clipboard',
    capturing: 'Capturing…',
    loading: 'Opening your clips…',
    emptyTitle: 'No clips yet',
    emptyBody: 'Highlight text anywhere, then tap the bubble.',
    noMatches: (term: string) => `No clips match “${term}”`,
    when: {
      justNow: 'just now',
    },
    moreOptions: (title: string) => `More options for ${title}`,
    moreOptionsHint: 'Opens edit and delete.',
    fallbackTitle: 'this clip',
    pasteHint: 'Double tap to paste.',
    pasteArmedHint: 'Double tap again to paste this clip.',
  },

  paste: {
    armed: 'Tap again to paste',
    copiedBody: 'Could not paste automatically, so it’s on your clipboard — paste it manually.',
  },

  search: {
    placeholder: 'Search title or content',
    a11yLabel: 'Search clips',
    clearA11yLabel: 'Clear search',
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
      'Text capture and the floating bubble need the custom dev client build — see SETUP_GUIDE.md.',
    textCapture: 'Text capture',
    notifications: 'Notifications',
    floatingBubble: 'Floating bubble',
    enable: 'Enable',
    manage: 'Manage',
    start: 'Start',
    stop: 'Stop',

    bubbleSize: 'Bubble size',
    bubbleSizeValue: (dp: number) => `${dp}dp`,
    bubbleSizeA11y: (dp: number) => `${dp} density pixels across`,

    bubbleVisibility: 'Show the bubble',
    hideBubble: 'Hide',
    showBubble: 'Show',
    bubbleHiddenNote:
      'The bubble is hidden. DevClip is still running — bring it back here or from the notification.',

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

    importBackup: 'Import backup',
    importing: 'Importing…',
    importFailedBody: 'Could not read that backup. Try again.',
    imported: (added: number, skipped: number) => {
      if (added === 0 && skipped === 0) return 'Nothing to import from that file.';
      if (added === 0) return `Already had all ${skipped} of those clips.`;
      if (skipped === 0) return `Added ${added} ${added === 1 ? 'clip' : 'clips'}.`;
      return `Added ${added} ${added === 1 ? 'clip' : 'clips'}, skipped ${skipped} you already had.`;
    },

    clearAll: 'Clear all clips',
    clearAllTitle: 'Clear all clips?',
    clearAllBody: 'This deletes everything in your history. You can’t undo this.',
  },

  setup: {
    heading: 'Set up DevClip',
    sub: 'DevClip needs all three to do its job. You can carry on without them and turn them on later in Settings.',
    expoGoSub:
      'This build can’t use the bubble or text capture — those need the custom dev client. Everything else works. See SETUP_GUIDE.md.',
    textCapture: 'Text capture',
    textCaptureBody:
      'Lets DevClip read the text you have highlighted, so tapping the bubble saves it, and paste a saved clip back into the field you were using. Android will show a broader permission screen for this — that’s expected.',
    floatingBubble: 'Floating bubble',
    floatingBubbleBody: 'Lets DevClip draw its bubble on top of other apps.',
    notifications: 'Notifications',
    notificationsBody:
      'Shows a silent, ongoing notification while the bubble is running. It’s also how you get a hidden bubble back.',
    notificationsDenied:
      'Android only offers this once. Turn notifications on for DevClip in Android Settings if you change your mind.',
    enable: 'Enable',
    done: 'Done',
    enableA11y: (feature: string) => `Enable ${feature.toLowerCase()}`,
    enabledA11y: (feature: string) => `${feature} is enabled`,
    continue: 'Continue',
    skip: 'Continue without these',
    skipHint: 'Opens DevClip with the features these permissions provide turned off.',

    /** Shown in the app itself while something is missing. */
    crippled: (missing: string) => `DevClip can’t ${missing} until you finish setup.`,
    crippledAction: 'Finish setup',
    missingCapture: 'save what you highlight',
    missingBubble: 'show its bubble',
    missingNotification: 'bring a hidden bubble back',
  },

  overlay: {
    openFullApp: 'Open the full app',
    close: 'Close DevClip',
  },

  common: {
    cancel: 'Cancel',
    dismiss: 'Dismiss',
    dismissError: 'Dismiss error',
  },
} as const;

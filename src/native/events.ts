import { DeviceEventEmitter, EmitterSubscription } from 'react-native';

/**
 * Events pushed from native. See plugins/android-src/DevClipEvents.kt.
 *
 * These are prompts to go and look, never the data itself. Native can emit
 * while no React instance exists — after a reboot, or once Android has trimmed
 * the app's process and left only the foreground service running — and those
 * emits are dropped on the floor by design. So a listener re-reads the
 * database rather than trusting the payload to be a complete picture, and the
 * app also refreshes when it comes back to the foreground, to catch whatever
 * it missed while it was not there to hear it.
 */

/** A clip was written to the database by native. */
export const CLIPS_CHANGED = 'DevClipClipsChanged';

/** The bubble was hidden or brought back. */
export const BUBBLE_STATE = 'DevClipBubbleState';

export interface ClipsChangedEvent {
  /** First few words of what was saved, for a confirmation message. */
  preview?: string;
}

export interface BubbleStateEvent {
  /** True when the service is running but the bubble is hidden. */
  resting: boolean;
}

export function onClipsChanged(
  handler: (event: ClipsChangedEvent) => void
): EmitterSubscription {
  return DeviceEventEmitter.addListener(CLIPS_CHANGED, (event) => handler(event ?? {}));
}

export function onBubbleState(
  handler: (event: BubbleStateEvent) => void
): EmitterSubscription {
  return DeviceEventEmitter.addListener(BUBBLE_STATE, (event) =>
    handler({ resting: !!event?.resting })
  );
}

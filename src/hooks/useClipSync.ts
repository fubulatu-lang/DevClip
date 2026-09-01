import { useEffect } from 'react';
import { AppState } from 'react-native';
import { useClipStore } from '../store/clipStore';
import { onClipsChanged } from '../native/events';

/**
 * Keeps a clip list showing what is actually in the database.
 *
 * Both surfaces used to run `init()` once on mount and then never look again.
 * Nothing refreshed on foreground, and nothing refreshed when native inserted
 * a row — so the bubble could capture perfectly and the list would keep
 * showing what it read when it was first opened. The feature would have
 * looked broken while working.
 *
 * Three triggers, because no one of them covers the others:
 *
 *  - mount, to open the database and read it;
 *  - the native event, for a capture that happens while a list is on screen
 *    (the floating list, mostly, since it is visible over other apps);
 *  - returning to the foreground, for everything captured while JS was not
 *    running to hear the event at all. Native emits regardless; if the
 *    process was trimmed there was nothing on the other end.
 */
export function useClipSync(): void {
  const init = useClipStore((s) => s.init);
  const refresh = useClipStore((s) => s.refresh);

  useEffect(() => {
    init();

    const clips = onClipsChanged(() => {
      refresh();
    });

    const appState = AppState.addEventListener('change', (state) => {
      if (state === 'active') refresh();
    });

    return () => {
      clips.remove();
      appState.remove();
    };
  }, [init, refresh]);
}

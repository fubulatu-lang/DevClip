import React, { useCallback, useEffect, useMemo } from 'react';
import { View, FlatList, StyleSheet, Text } from 'react-native';
import { ClipboardPaste, Inbox } from 'lucide-react-native';
import { useClipStore } from '../store/clipStore';
import { useClipSync } from '../hooks/useClipSync';
import { useSettingsStore } from '../store/settingsStore';
import SearchBar from '../components/SearchBar';
import ClipListItem from '../components/ClipListItem';
import Pressy from '../components/Pressy';
import Snackbar from '../components/Snackbar';
import { useEditStore } from '../store/editStore';
import { Clip } from '../types/clip';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import { strings } from '../strings';

export default function ClipListView() {
  const { colors, radii, spacing, text, icon } = useTheme();
  const { gutter, columns } = useAdaptiveLayout();
  // Zustand subscribes a component to whatever the hook returns. Calling it
  // with no selector returns the whole store, so every field change — each
  // keystroke in search included — re-rendered this tree. One selector per
  // value keeps each render to what actually changed.
  const clips = useClipStore((s) => s.clips);
  const initialised = useClipStore((s) => s.initialised);
  const search = useClipStore((s) => s.search);
  const capturing = useClipStore((s) => s.capturing);
  const error = useClipStore((s) => s.error);
  const setSearch = useClipStore((s) => s.setSearch);
  const capture = useClipStore((s) => s.capture);
  const trimToMax = useClipStore((s) => s.trimToMax);
  const dismissError = useClipStore((s) => s.dismissError);
  const maxClips = useSettingsStore((s) => s.maxClips);
  // The sheet itself is rendered a level up, over the app bar as well as the
  // list; the row only says which clip to open.
  const openEditor = useEditStore((s) => s.open);

  useClipSync();

  useEffect(() => {
    trimToMax(maxClips);
  }, [maxClips, clips.length]);

  const renderItem = useCallback(
    // Newest is 1. Positional, not an identity — the numbers renumber as
    // clips arrive and are deleted, which is what makes them useful.
    ({ item, index }: { item: Clip; index: number }) => (
      <ClipListItem clip={item} position={index + 1} onLongPress={openEditor} />
    ),
    [openEditor]
  );

  const styles = useMemo(
    () =>
      StyleSheet.create({
        container: { flex: 1 },
        /**
         * The One UI interaction area: actions sit at the bottom, within
         * thumb reach, not at the top where the eye lands first.
         */
        actionBar: {
          flexDirection: 'row',
          justifyContent: 'center',
          paddingHorizontal: gutter,
          paddingTop: spacing.md,
          paddingBottom: spacing.lg,
          borderTopWidth: 1,
          borderTopColor: colors.divider,
          backgroundColor: colors.bg,
        },
        /**
         * A secondary control now, not the primary one.
         *
         * Capture used to be the only way to save anything, so it was a full
         * width filled button. Tapping the bubble is how clips get saved now;
         * this button is the fallback for something copied with Android's own
         * Copy button, which is a real case but not the main one. Still a
         * 48dp target, just no longer the loudest thing on the screen.
         */
        captureBtn: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: spacing.sm,
          backgroundColor: colors.surfaceSunken,
          minHeight: 48,
          paddingHorizontal: spacing.xl,
          borderRadius: radii.pill,
        },
        captureBtnBusy: { opacity: 0.5 },
        captureText: { ...text.secondary, fontWeight: '500', color: colors.inkSoft },
        empty: {
          alignItems: 'center',
          justifyContent: 'center',
          marginTop: 60,
          gap: spacing.md,
          paddingHorizontal: gutter,
        },
        emptyText: { ...text.secondary, color: colors.inkFaint, textAlign: 'center', lineHeight: 22 },
        errorBanner: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginHorizontal: gutter,
          marginTop: spacing.sm,
          backgroundColor: colors.dangerSoft,
          paddingVertical: spacing.md,
          paddingHorizontal: spacing.lg,
          borderRadius: radii.md,
          gap: spacing.md,
        },
        errorText: { flex: 1, ...text.secondary, color: colors.danger },
        errorDismiss: { ...text.secondary, fontWeight: '500', color: colors.danger },
      }),
    [colors, radii, spacing, text, gutter]
  );

  return (
    <View style={styles.container} accessibilityLiveRegion="polite">
      {error && (
        <View style={styles.errorBanner}>
          <Text style={styles.errorText}>{error}</Text>
          <Pressy onPress={dismissError} accessibilityLabel={strings.common.dismissError} hitSlop={16}>
            <Text style={styles.errorDismiss}>{strings.common.dismiss}</Text>
          </Pressy>
        </View>
      )}

      <SearchBar value={search} onChange={setSearch} />

      <FlatList
        data={clips}
        keyExtractor={(item) => String(item.id)}
        // numColumns is fixed for the life of a FlatList, so the key changes
        // with it to force a remount when the window crosses a breakpoint.
        key={`cols-${columns}`}
        numColumns={columns}
        columnWrapperStyle={columns > 1 ? { gap: spacing.md } : undefined}
        contentContainerStyle={{
          paddingTop: spacing.xs,
          paddingBottom: spacing.lg,
          paddingHorizontal: gutter,
          flexGrow: 1,
        }}
        renderItem={renderItem}
        ListEmptyComponent={
          <View style={styles.empty}>
            {initialised && (
              <Inbox size={icon.lg} strokeWidth={icon.stroke} color={colors.inkDisabled} />
            )}
            <Text style={styles.emptyText}>
              {!initialised
                ? strings.clips.loading
                : search
                  ? strings.clips.noMatches(search)
                  : `${strings.clips.emptyTitle}\n${strings.clips.emptyBody}`}
            </Text>
          </View>
        }
      />

      <View style={styles.actionBar}>
        <Pressy
          onPress={capture}
          // An async action that stays tappable invites a double capture.
          disabled={capturing}
          style={[styles.captureBtn, capturing && styles.captureBtnBusy]}
          accessibilityLabel={strings.clips.captureA11y}
        >
          <ClipboardPaste size={icon.sm} strokeWidth={icon.stroke} color={colors.inkSoft} />
          <Text style={styles.captureText}>{capturing ? strings.clips.capturing : strings.clips.capture}</Text>
        </Pressy>
      </View>

      <Snackbar />
    </View>
  );
}

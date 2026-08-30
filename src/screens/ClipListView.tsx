import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { View, FlatList, StyleSheet, Text } from 'react-native';
import { ClipboardPaste, Inbox } from 'lucide-react-native';
import { useClipStore } from '../store/clipStore';
import { useSettingsStore } from '../store/settingsStore';
import SearchBar from '../components/SearchBar';
import SortMenu from '../components/SortMenu';
import ClipListItem from '../components/ClipListItem';
import EditClipModal from '../components/EditClipModal';
import Pressy from '../components/Pressy';
import Snackbar from '../components/Snackbar';
import { Clip } from '../types/clip';
import { readSystemClipboard } from '../utils/clipboardCapture';
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
  const search = useClipStore((s) => s.search);
  const sort = useClipStore((s) => s.sort);
  const loading = useClipStore((s) => s.loading);
  const error = useClipStore((s) => s.error);
  const init = useClipStore((s) => s.init);
  const setSearch = useClipStore((s) => s.setSearch);
  const setSort = useClipStore((s) => s.setSort);
  const addClip = useClipStore((s) => s.addClip);
  const updateClip = useClipStore((s) => s.updateClip);
  const deleteClip = useClipStore((s) => s.deleteClip);
  const moveUp = useClipStore((s) => s.moveUp);
  const moveDown = useClipStore((s) => s.moveDown);
  const trimToMax = useClipStore((s) => s.trimToMax);
  const dismissError = useClipStore((s) => s.dismissError);
  const maxClips = useSettingsStore((s) => s.maxClips);
  const [editing, setEditing] = useState<Clip | null>(null);

  useEffect(() => {
    init();
  }, []);

  useEffect(() => {
    trimToMax(maxClips);
  }, [maxClips, clips.length]);

  // Passing a fresh arrow per row would hand React.memo a new prop every
  // render and defeat it. One stable handler takes the index instead, and
  // the row binds its own.
  const handleMove = useCallback(
    (index: number, direction: -1 | 1) => (direction === -1 ? moveUp(index) : moveDown(index)),
    [moveUp, moveDown]
  );

  const renderItem = useCallback(
    ({ item, index }: { item: Clip; index: number }) => (
      <ClipListItem
        clip={item}
        isManualSort={sort === 'manual'}
        isFirst={index === 0}
        isLast={index === clips.length - 1}
        onLongPress={setEditing}
        index={index}
        onMove={handleMove}
      />
    ),
    [sort, clips.length, handleMove]
  );

  const handleCapture = async () => {
    const text = await readSystemClipboard();
    if (text) {
      await addClip(text);
    }
  };

  const styles = useMemo(
    () =>
      StyleSheet.create({
        container: { flex: 1 },
        /**
         * The One UI interaction area: the primary action sits at the bottom,
         * within thumb reach, not at the top where the eye lands first.
         */
        actionBar: {
          paddingHorizontal: gutter,
          paddingTop: spacing.md,
          paddingBottom: spacing.lg,
          borderTopWidth: 1,
          borderTopColor: colors.divider,
          backgroundColor: colors.bg,
        },
        captureBtn: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: spacing.sm,
          backgroundColor: colors.accent,
          minHeight: 48,
          paddingHorizontal: spacing.xl,
          borderRadius: radii.pill,
        },
        captureText: { ...text.button, color: colors.onAccent },
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
      <SortMenu value={sort} onChange={setSort} />

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
            <Inbox size={icon.lg} strokeWidth={icon.stroke} color={colors.inkDisabled} />
            <Text style={styles.emptyText}>
              {search
                ? strings.clips.noMatches(search)
                : `${strings.clips.emptyTitle}\n${strings.clips.emptyBody}`}
            </Text>
          </View>
        }
      />

      <View style={styles.actionBar}>
        <Pressy
          onPress={handleCapture}
          style={styles.captureBtn}
          accessibilityLabel={strings.clips.captureA11y}
        >
          <ClipboardPaste size={icon.sm} strokeWidth={icon.stroke} color={colors.onAccent} />
          <Text style={styles.captureText}>{loading ? strings.clips.capturing : strings.clips.capture}</Text>
        </Pressy>
      </View>

      <Snackbar />

      <EditClipModal
        clip={editing}
        onClose={() => setEditing(null)}
        onSave={async (id, content, title) => {
          await updateClip(id, content, title);
          setEditing(null);
        }}
        onDelete={async (id) => {
          await deleteClip(id);
          setEditing(null);
        }}
      />
    </View>
  );
}

import React, { useEffect, useMemo, useState } from 'react';
import { View, FlatList, StyleSheet, Text } from 'react-native';
import { ClipboardPaste, Inbox } from 'lucide-react-native';
import { useClipStore } from '../store/clipStore';
import { useSettingsStore } from '../store/settingsStore';
import SearchBar from '../components/SearchBar';
import SortMenu from '../components/SortMenu';
import ClipListItem from '../components/ClipListItem';
import EditClipModal from '../components/EditClipModal';
import Pressy from '../components/Pressy';
import { Clip } from '../types/clip';
import { readSystemClipboard } from '../utils/clipboardCapture';
import { useTheme } from '../theme/ThemeContext';

export default function ClipListView() {
  const { colors, radii, spacing, text, icon } = useTheme();
  const {
    clips,
    search,
    sort,
    loading,
    error,
    init,
    setSearch,
    setSort,
    addClip,
    updateClip,
    deleteClip,
    moveUp,
    moveDown,
    trimToMax,
    dismissError,
  } = useClipStore();
  const maxClips = useSettingsStore((s) => s.maxClips);
  const [editing, setEditing] = useState<Clip | null>(null);

  useEffect(() => {
    init();
  }, []);

  useEffect(() => {
    trimToMax(maxClips);
  }, [maxClips, clips.length]);

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
          paddingHorizontal: spacing.keyline,
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
          paddingHorizontal: spacing.keyline,
        },
        emptyText: { ...text.secondary, color: colors.inkFaint, textAlign: 'center', lineHeight: 22 },
        errorBanner: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginHorizontal: spacing.keyline,
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
    [colors, radii, spacing, text]
  );

  return (
    <View style={styles.container} accessibilityLiveRegion="polite">
      {error && (
        <View style={styles.errorBanner}>
          <Text style={styles.errorText}>{error}</Text>
          <Pressy onPress={dismissError} accessibilityLabel="Dismiss error" hitSlop={16}>
            <Text style={styles.errorDismiss}>Dismiss</Text>
          </Pressy>
        </View>
      )}

      <SearchBar value={search} onChange={setSearch} />
      <SortMenu value={sort} onChange={setSort} />

      <FlatList
        data={clips}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ paddingTop: spacing.xs, paddingBottom: spacing.lg, flexGrow: 1 }}
        renderItem={({ item, index }) => (
          <ClipListItem
            clip={item}
            isManualSort={sort === 'manual'}
            isFirst={index === 0}
            isLast={index === clips.length - 1}
            onLongPress={setEditing}
            onMoveUp={() => moveUp(index)}
            onMoveDown={() => moveDown(index)}
          />
        )}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Inbox size={icon.lg} strokeWidth={icon.stroke} color={colors.inkDisabled} />
            <Text style={styles.emptyText}>
              {search
                ? `No clips match “${search}”`
                : 'No clips yet\nCopy something, then tap Capture below.'}
            </Text>
          </View>
        }
      />

      <View style={styles.actionBar}>
        <Pressy
          onPress={handleCapture}
          style={styles.captureBtn}
          accessibilityLabel="Capture current clipboard"
        >
          <ClipboardPaste size={icon.sm} strokeWidth={icon.stroke} color={colors.onAccent} />
          <Text style={styles.captureText}>{loading ? 'Loading…' : 'Capture clipboard'}</Text>
        </Pressy>
      </View>

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

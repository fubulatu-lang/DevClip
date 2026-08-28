import React, { useEffect, useState } from 'react';
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
  const { colors, radii, spacing, type } = useTheme();
  const { clips, search, sort, init, setSearch, setSort, addClip, updateClip, deleteClip, moveUp, moveDown, trimToMax } =
    useClipStore();
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

  const styles = StyleSheet.create({
    container: { flex: 1 },
    captureBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8,
      marginHorizontal: spacing.md,
      marginTop: spacing.xs,
      backgroundColor: colors.accentSoft,
      paddingVertical: 11,
      borderRadius: radii.pill,
    },
    captureText: { fontFamily: type.semibold, fontSize: 12.5, color: colors.accent },
    empty: { alignItems: 'center', justifyContent: 'center', marginTop: 60, gap: spacing.sm, paddingHorizontal: spacing.xl },
    emptyText: { fontFamily: type.medium, fontSize: 12.5, color: colors.inkFaint, textAlign: 'center', lineHeight: 18 },
  });

  return (
    <View style={styles.container}>
      <Pressy onPress={handleCapture} style={styles.captureBtn}>
        <ClipboardPaste size={15} strokeWidth={1.75} color={colors.accent} />
        <Text style={styles.captureText}>Capture current clipboard</Text>
      </Pressy>

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
            <Inbox size={28} strokeWidth={1.25} color={colors.inkFaint} />
            <Text style={styles.emptyText}>No clips yet{'\n'}Copy something, then tap Capture above.</Text>
          </View>
        }
      />

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

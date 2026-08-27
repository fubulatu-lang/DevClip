import React, { useEffect, useState } from 'react';
import { View, FlatList, StyleSheet, Text, TouchableOpacity } from 'react-native';
import { useClipStore } from '../store/clipStore';
import SearchBar from '../components/SearchBar';
import SortMenu from '../components/SortMenu';
import ClipListItem from '../components/ClipListItem';
import EditClipModal from '../components/EditClipModal';
import { Clip } from '../types/clip';
import { readSystemClipboard } from '../utils/clipboardCapture';

export default function ClipListView() {
  const { clips, search, sort, init, setSearch, setSort, addClip, updateClip, deleteClip, moveUp, moveDown } =
    useClipStore();
  const [editing, setEditing] = useState<Clip | null>(null);

  useEffect(() => {
    init();
  }, []);

  const handleCapture = async () => {
    const text = await readSystemClipboard();
    if (text) {
      await addClip(text);
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.captureBtn} onPress={handleCapture}>
        <Text style={styles.captureText}>+ Capture current clipboard</Text>
      </TouchableOpacity>

      <SearchBar value={search} onChange={setSearch} />
      <SortMenu value={sort} onChange={setSort} />

      <FlatList
        data={clips}
        keyExtractor={(item) => String(item.id)}
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
          <Text style={styles.empty}>No clips yet. Copy something, then tap Capture above.</Text>
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

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  captureBtn: {
    margin: 10,
    backgroundColor: '#e8edff',
    paddingVertical: 10,
    borderRadius: 8,
    alignItems: 'center',
  },
  captureText: { color: '#4a6cf7', fontWeight: '600', fontSize: 13 },
  empty: { textAlign: 'center', color: '#999', marginTop: 40, paddingHorizontal: 20 },
});

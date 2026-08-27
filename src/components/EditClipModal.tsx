import React, { useEffect, useState } from 'react';
import { Modal, View, Text, TextInput, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { Clip } from '../types/clip';

interface Props {
  clip: Clip | null;
  onClose: () => void;
  onSave: (id: number, content: string, title: string | null) => void;
  onDelete: (id: number) => void;
}

export default function EditClipModal({ clip, onClose, onSave, onDelete }: Props) {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  useEffect(() => {
    if (clip) {
      setTitle(clip.title ?? '');
      setContent(clip.content);
    }
  }, [clip]);

  if (!clip) return null;

  const handleDelete = () => {
    Alert.alert('Delete this clip?', 'This cannot be undone.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => onDelete(clip.id) },
    ]);
  };

  return (
    <Modal visible={!!clip} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <Text style={styles.label}>Title (optional)</Text>
          <TextInput
            style={styles.titleInput}
            value={title}
            onChangeText={setTitle}
            placeholder="Add a title..."
          />

          <Text style={styles.label}>Content</Text>
          <TextInput
            style={styles.contentInput}
            value={content}
            onChangeText={setContent}
            multiline
          />

          <View style={styles.row}>
            <TouchableOpacity style={[styles.btn, styles.deleteBtn]} onPress={handleDelete}>
              <Text style={styles.deleteText}>Delete</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.btn} onPress={onClose}>
              <Text>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.btn, styles.saveBtn]}
              onPress={() => onSave(clip.id, content, title.trim() || null)}
            >
              <Text style={styles.saveText}>Save</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 16, borderTopRightRadius: 16, padding: 16 },
  label: { fontSize: 12, color: '#666', marginTop: 8, marginBottom: 4 },
  titleInput: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 8, fontSize: 14 },
  contentInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 8,
    fontSize: 14,
    minHeight: 100,
    textAlignVertical: 'top',
  },
  row: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 16, gap: 8 },
  btn: { paddingVertical: 8, paddingHorizontal: 14, borderRadius: 8 },
  deleteBtn: { marginRight: 'auto', backgroundColor: '#fdeaea' },
  deleteText: { color: '#c0392b', fontWeight: '600' },
  saveBtn: { backgroundColor: '#4a6cf7' },
  saveText: { color: '#fff', fontWeight: '600' },
});

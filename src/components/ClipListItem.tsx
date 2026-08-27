import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { Clip } from '../types/clip';
import { writeSystemClipboard } from '../utils/clipboardCapture';

interface Props {
  clip: Clip;
  isManualSort: boolean;
  isFirst: boolean;
  isLast: boolean;
  onLongPress: (clip: Clip) => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}

export default function ClipListItem({
  clip,
  isManualSort,
  isFirst,
  isLast,
  onLongPress,
  onMoveUp,
  onMoveDown,
}: Props) {
  const handleTap = () => {
    Alert.alert(
      'Copy to clipboard?',
      clip.content.length > 120 ? clip.content.slice(0, 120) + '…' : clip.content,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Copy',
          onPress: async () => {
            await writeSystemClipboard(clip.content);
            Alert.alert('Copied', 'Now switch apps and paste it.');
          },
        },
      ]
    );
  };

  return (
    <TouchableOpacity
      style={styles.row}
      onPress={handleTap}
      onLongPress={() => onLongPress(clip)}
      activeOpacity={0.7}
    >
      <View style={{ flex: 1 }}>
        {clip.title ? <Text style={styles.title}>{clip.title}</Text> : null}
        <Text style={styles.content} numberOfLines={2}>
          {clip.content}
        </Text>
        <Text style={styles.date}>{new Date(clip.createdAt).toLocaleString()}</Text>
      </View>

      {isManualSort && (
        <View style={styles.reorderCol}>
          <TouchableOpacity disabled={isFirst} onPress={onMoveUp} style={styles.reorderBtn}>
            <Text style={[styles.reorderText, isFirst && styles.disabled]}>▲</Text>
          </TouchableOpacity>
          <TouchableOpacity disabled={isLast} onPress={onMoveDown} style={styles.reorderBtn}>
            <Text style={[styles.reorderText, isLast && styles.disabled]}>▼</Text>
          </TouchableOpacity>
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#ddd',
    alignItems: 'center',
  },
  title: { fontWeight: '600', fontSize: 14, marginBottom: 2 },
  content: { fontSize: 13, color: '#333' },
  date: { fontSize: 10, color: '#999', marginTop: 4 },
  reorderCol: { marginLeft: 8, alignItems: 'center' },
  reorderBtn: { padding: 4 },
  reorderText: { fontSize: 14, color: '#4a6cf7' },
  disabled: { color: '#ccc' },
});

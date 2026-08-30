import React, { useEffect, useMemo, useState } from 'react';
import { Modal, View, Text, TextInput, StyleSheet, Alert, KeyboardAvoidingView, Platform, Pressable } from 'react-native';
import { Trash2 } from 'lucide-react-native';
import { Clip } from '../types/clip';
import { useTheme } from '../theme/ThemeContext';
import Pressy from './Pressy';

interface Props {
  clip: Clip | null;
  onClose: () => void;
  onSave: (id: number, content: string, title: string | null) => void;
  onDelete: (id: number) => void;
}

export default function EditClipModal({ clip, onClose, onSave, onDelete }: Props) {
  const { colors, radii, spacing, shadow, text, icon } = useTheme();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  useEffect(() => {
    if (clip) {
      setTitle(clip.title ?? '');
      setContent(clip.content);
    }
  }, [clip]);

  const styles = useMemo(() => StyleSheet.create({
    backdrop: { flex: 1, backgroundColor: colors.scrim, justifyContent: 'flex-end' },
    sheet: {
      backgroundColor: colors.surface,
      borderTopLeftRadius: radii.container,
      borderTopRightRadius: radii.container,
      padding: spacing.keyline,
      ...shadow.floating,
    },
    handle: {
      width: 36,
      height: 4,
      borderRadius: radii.pill,
      backgroundColor: colors.borderStrong,
      alignSelf: 'center',
      marginBottom: spacing.lg,
    },
    label: {
      ...text.secondary,
      color: colors.inkSoft,
      marginTop: spacing.lg,
      marginBottom: spacing.sm,
    },
    titleInput: {
      ...text.body,
      color: colors.ink,
      backgroundColor: colors.surfaceSunken,
      borderRadius: radii.md,
      paddingHorizontal: spacing.lg,
      paddingVertical: spacing.md,
      minHeight: 48,
    },
    contentInput: {
      ...text.body,
      color: colors.ink,
      backgroundColor: colors.surfaceSunken,
      borderRadius: radii.md,
      paddingHorizontal: spacing.lg,
      paddingVertical: spacing.md,
      minHeight: 120,
      lineHeight: 24,
    },
    actions: { flexDirection: 'row', alignItems: 'center', marginTop: spacing.xl, gap: spacing.md },
    deleteBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      gap: spacing.sm,
      paddingHorizontal: spacing.xl,
      borderRadius: radii.pill,
      backgroundColor: colors.dangerSoft,
      marginRight: 'auto',
      minHeight: 48,
    },
    deleteText: { ...text.button, color: colors.danger },
    saveBtn: {
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.accent,
      borderRadius: radii.pill,
      paddingHorizontal: spacing.xl,
      minHeight: 48,
    },
    saveText: { ...text.button, color: colors.onAccent },
  }), [colors, radii, spacing, shadow, text]);

  if (!clip) return null;

  const handleDelete = () => {
    Alert.alert('Delete this clip?', 'This cannot be undone.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => onDelete(clip.id) },
    ]);
  };

  return (
    <Modal visible={!!clip} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView
        style={styles.backdrop}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <Pressable onPress={onClose} style={StyleSheet.absoluteFill} accessibilityLabel="Close edit sheet" />

        <View style={styles.sheet}>
          <View style={styles.handle} importantForAccessibility="no" />

          <Text style={styles.label}>Title</Text>
          <TextInput
            style={styles.titleInput}
            value={title}
            onChangeText={setTitle}
            placeholder="Untitled"
            placeholderTextColor={colors.inkFaint}
            accessibilityLabel="Clip title"
          />

          <Text style={styles.label}>Content</Text>
          <TextInput
            style={styles.contentInput}
            value={content}
            onChangeText={setContent}
            multiline
            textAlignVertical="top"
            accessibilityLabel="Clip content"
          />

          <View style={styles.actions}>
            <Pressy onPress={handleDelete} style={styles.deleteBtn} accessibilityLabel="Delete clip">
              <Trash2 size={icon.sm} strokeWidth={icon.stroke} color={colors.danger} />
              <Text style={styles.deleteText}>Delete</Text>
            </Pressy>

            <Pressy
              onPress={() => onSave(clip.id, content, title.trim() || null)}
              style={styles.saveBtn}
              accessibilityLabel="Save clip"
            >
              <Text style={styles.saveText}>Save</Text>
            </Pressy>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

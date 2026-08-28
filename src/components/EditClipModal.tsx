import React, { useEffect, useState } from 'react';
import { Modal, View, Text, TextInput, StyleSheet, Alert, KeyboardAvoidingView, Platform, Pressable } from 'react-native';
import { Trash2, Check } from 'lucide-react-native';
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
  const { colors, radii, spacing, shadow, type } = useTheme();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  useEffect(() => {
    if (clip) {
      setTitle(clip.title ?? '');
      setContent(clip.content);
    }
  }, [clip]);

  const styles = StyleSheet.create({
    backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.45)', justifyContent: 'flex-end' },
    shell: {
      backgroundColor: 'rgba(0,0,0,0.03)',
      padding: 6,
      borderTopLeftRadius: radii.lg + 6,
      borderTopRightRadius: radii.lg + 6,
    },
    sheet: {
      backgroundColor: colors.surface,
      borderTopLeftRadius: radii.lg,
      borderTopRightRadius: radii.lg,
      borderRadius: radii.sm,
      padding: spacing.lg,
      ...shadow.floating,
    },
    handle: {
      width: 36,
      height: 4,
      borderRadius: radii.pill,
      backgroundColor: colors.borderStrong,
      alignSelf: 'center',
      marginBottom: spacing.md,
    },
    eyebrow: {
      fontFamily: type.bold,
      fontSize: 10,
      letterSpacing: 1.2,
      textTransform: 'uppercase',
      color: colors.inkFaint,
      marginTop: spacing.sm,
      marginBottom: spacing.xs,
    },
    titleInput: {
      fontFamily: type.semibold,
      fontSize: 15,
      color: colors.ink,
      backgroundColor: colors.surfaceSunken,
      borderRadius: radii.sm,
      paddingHorizontal: spacing.md,
      paddingVertical: 10,
    },
    contentInput: {
      fontFamily: type.regular,
      fontSize: 14,
      color: colors.ink,
      backgroundColor: colors.surfaceSunken,
      borderRadius: radii.sm,
      paddingHorizontal: spacing.md,
      paddingVertical: 10,
      minHeight: 120,
      lineHeight: 20,
    },
    actions: { flexDirection: 'row', alignItems: 'center', marginTop: spacing.lg, gap: spacing.sm },
    deleteBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
      paddingVertical: 10,
      paddingHorizontal: spacing.md,
      borderRadius: radii.pill,
      backgroundColor: colors.dangerSoft,
      marginRight: 'auto',
    },
    deleteText: { fontFamily: type.semibold, fontSize: 13, color: colors.danger },
    saveBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      backgroundColor: colors.ink,
      borderRadius: radii.pill,
      paddingLeft: spacing.lg,
      paddingRight: 6,
      paddingVertical: 6,
    },
    saveText: { fontFamily: type.semibold, fontSize: 13, color: colors.bg },
    saveIconWrap: {
      width: 26,
      height: 26,
      borderRadius: radii.pill,
      backgroundColor: 'rgba(128,128,128,0.25)',
      alignItems: 'center',
      justifyContent: 'center',
    },
  });

  if (!clip) return null;

  const handleDelete = () => {
    Alert.alert('Delete this clip?', 'This cannot be undone.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => onDelete(clip.id) },
    ]);
  };

  return (
    <Modal visible={!!clip} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView style={styles.backdrop} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <Pressable onPress={onClose} style={StyleSheet.absoluteFill} />

        <View style={styles.shell}>
          <View style={styles.sheet}>
            <View style={styles.handle} />

            <Text style={styles.eyebrow}>Title</Text>
            <TextInput
              style={styles.titleInput}
              value={title}
              onChangeText={setTitle}
              placeholder="Untitled"
              placeholderTextColor={colors.inkFaint}
            />

            <Text style={styles.eyebrow}>Content</Text>
            <TextInput
              style={styles.contentInput}
              value={content}
              onChangeText={setContent}
              multiline
              textAlignVertical="top"
            />

            <View style={styles.actions}>
              <Pressy onPress={handleDelete} style={styles.deleteBtn}>
                <Trash2 size={15} strokeWidth={1.75} color={colors.danger} />
                <Text style={styles.deleteText}>Delete</Text>
              </Pressy>

              <Pressy onPress={() => onSave(clip.id, content, title.trim() || null)} style={styles.saveBtn}>
                <Text style={styles.saveText}>Save</Text>
                <View style={styles.saveIconWrap}>
                  <Check size={13} strokeWidth={2} color={colors.bg} />
                </View>
              </Pressy>
            </View>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

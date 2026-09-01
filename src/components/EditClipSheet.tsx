import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Alert,
  Pressable,
  ScrollView,
  BackHandler,
  Animated,
  Easing,
} from 'react-native';
import { Trash2 } from 'lucide-react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTheme } from '../theme/ThemeContext';
import Pressy from './Pressy';
import { useReduceMotion } from '../theme/useReduceMotion';
import { useEditStore } from '../store/editStore';
import { strings } from '../strings';

interface Props {
  onSave: (id: number, content: string, title: string | null) => void;
  onDelete: (id: number) => void;
}

/**
 * The edit sheet, rendered inline in the app's own window.
 *
 * It used to be a `<Modal>`. A Modal renders into a *separate* Android
 * window, and a separate window does not inherit the activity's soft-input
 * mode — so `adjustResize` shrank the main window while the modal's window
 * stayed full height, leaving the sheet pinned to the bottom of the screen
 * underneath the keyboard. A `KeyboardAvoidingView` was tried and did nothing,
 * because there was never anything wrong with the measurement; the sheet was
 * simply in the wrong window.
 *
 * As an ordinary absolutely-positioned view in the main window, the window
 * itself shrinks to exclude the keyboard, `flex-end` puts the sheet directly
 * above it, and no keyboard handling is needed at all. The two things a Modal
 * was giving us for free — the back button and sitting above everything else
 * — are handled here instead: `BackHandler` below, and being rendered last in
 * the screen.
 */
export default function EditClipSheet({ onSave, onDelete }: Props) {
  const { colors, radii, spacing, shadow, text, icon, easing, duration } = useTheme();
  const insets = useSafeAreaInsets();
  const reduceMotion = useReduceMotion();
  const clip = useEditStore((s) => s.clip);
  const close = useEditStore((s) => s.close);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  const enter = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (clip) {
      setTitle(clip.title ?? '');
      setContent(clip.content);
    }
  }, [clip]);

  useEffect(() => {
    if (!clip) return;
    if (reduceMotion) {
      enter.setValue(1);
      return;
    }
    enter.setValue(0);
    Animated.timing(enter, {
      toValue: 1,
      duration: duration.medium,
      easing: Easing.bezier(...easing.emphasizedDecelerate),
      useNativeDriver: true,
    }).start();
  }, [clip, reduceMotion, enter, duration.medium, easing.emphasizedDecelerate]);

  useEffect(() => {
    if (!clip) return;
    // A Modal answered Back by itself. An ordinary view does not, and without
    // this the back gesture would leave the sheet open and exit the screen
    // behind it.
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      close();
      return true;
    });
    return () => sub.remove();
  }, [clip, close]);

  const styles = useMemo(
    () =>
      StyleSheet.create({
        backdrop: {
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          justifyContent: 'flex-end',
        },
        scrim: {
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: colors.scrim,
        },
        sheet: {
          backgroundColor: colors.surface,
          borderTopLeftRadius: radii.container,
          borderTopRightRadius: radii.container,
          // The sheet must never grow past the window it is in. With the
          // keyboard up that window is short, and the actions have to stay
          // reachable, so the fields scroll rather than the sheet overflowing.
          maxHeight: '100%',
          ...shadow.floating,
        },
        sheetScroll: { flexGrow: 0 },
        sheetContent: {
          paddingHorizontal: spacing.keyline,
          paddingTop: spacing.keyline,
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
        actions: {
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing.md,
          paddingHorizontal: spacing.keyline,
          paddingTop: spacing.xl,
          // The sheet sits on the gesture bar, so its own padding has to clear
          // it or the actions end up underneath.
          paddingBottom: spacing.keyline + insets.bottom,
        },
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
      }),
    [colors, radii, spacing, shadow, text, insets.bottom]
  );

  if (!clip) return null;

  const handleDelete = () => {
    Alert.alert(strings.edit.deleteTitle, strings.edit.deleteBody, [
      { text: strings.common.cancel, style: 'cancel' },
      { text: strings.edit.delete, style: 'destructive', onPress: () => onDelete(clip.id) },
    ]);
  };

  return (
    <View style={styles.backdrop}>
      <Animated.View style={[styles.scrim, { opacity: enter }]}>
        <Pressable
          onPress={close}
          style={StyleSheet.absoluteFill}
          accessibilityLabel={strings.edit.close}
        />
      </Animated.View>

      <Animated.View
        style={[
          styles.sheet,
          {
            opacity: enter,
            transform: [
              { translateY: enter.interpolate({ inputRange: [0, 1], outputRange: [24, 0] }) },
            ],
          },
        ]}
      >
        <ScrollView style={styles.sheetScroll} contentContainerStyle={styles.sheetContent}>
          <View style={styles.handle} importantForAccessibility="no" />

          <Text style={styles.label}>{strings.edit.title}</Text>
          <TextInput
            style={styles.titleInput}
            value={title}
            onChangeText={setTitle}
            placeholder={strings.edit.titlePlaceholder}
            placeholderTextColor={colors.inkFaint}
            accessibilityLabel={strings.edit.titleA11y}
          />

          <Text style={styles.label}>{strings.edit.content}</Text>
          <TextInput
            style={styles.contentInput}
            value={content}
            onChangeText={setContent}
            multiline
            textAlignVertical="top"
            accessibilityLabel={strings.edit.contentA11y}
          />
        </ScrollView>

        <View style={styles.actions}>
          <Pressy
            onPress={handleDelete}
            style={styles.deleteBtn}
            accessibilityLabel={strings.edit.deleteA11y}
          >
            <Trash2 size={icon.sm} strokeWidth={icon.stroke} color={colors.danger} />
            <Text style={styles.deleteText}>{strings.edit.delete}</Text>
          </Pressy>

          <Pressy
            onPress={() => onSave(clip.id, content, title.trim() || null)}
            style={styles.saveBtn}
            accessibilityLabel={strings.edit.saveA11y}
          >
            <Text style={styles.saveText}>{strings.edit.save}</Text>
          </Pressy>
        </View>
      </Animated.View>
    </View>
  );
}

import React, { useMemo } from 'react';
import { View, Text, StyleSheet, Alert, Animated } from 'react-native';
import { ChevronUp, ChevronDown, Copy, MoreVertical } from 'lucide-react-native';
import { Clip } from '../types/clip';
import { pasteClip } from '../utils/clipboardCapture';
import { useTheme } from '../theme/ThemeContext';
import { useSettingsStore } from '../store/settingsStore';
import Pressy from './Pressy';

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
  const { colors, radii, spacing, shadow, text, icon } = useTheme();
  const confirmBeforePaste = useSettingsStore((s) => s.confirmBeforePaste);

  const styles = useMemo(
    () =>
      StyleSheet.create({
        card: {
          backgroundColor: colors.surface,
          borderRadius: radii.md,
          marginHorizontal: spacing.keyline,
          marginBottom: spacing.sm,
          padding: spacing.lg,
          ...shadow.card,
        },
        row: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.md },
        iconWrap: {
          width: 32,
          height: 32,
          borderRadius: radii.sm,
          backgroundColor: colors.surfaceSunken,
          alignItems: 'center',
          justifyContent: 'center',
          marginTop: 2,
        },
        title: { ...text.body, fontWeight: '500', color: colors.ink, marginBottom: 2 },
        content: { ...text.secondary, color: colors.inkSoft, lineHeight: 22 },
        date: { ...text.caption, color: colors.inkFaint, marginTop: spacing.sm },
        reorderCol: { alignItems: 'center', gap: spacing.sm },
        circleBtn: {
          width: 32,
          height: 32,
          borderRadius: radii.pill,
          backgroundColor: colors.surfaceSunken,
          alignItems: 'center',
          justifyContent: 'center',
        },
        moreBtn: {
          width: 32,
          height: 32,
          borderRadius: radii.pill,
          alignItems: 'center',
          justifyContent: 'center',
        },
      }),
    [colors, radii, spacing, shadow, text]
  );

  const doPaste = async () => {
    const result = await pasteClip(clip.content);
    if (result === 'copiedOnly') {
      Alert.alert('Copied', 'Could not paste automatically, so it’s on your clipboard — paste it manually.');
    }
  };

  const handleTap = () => {
    if (!confirmBeforePaste) {
      doPaste();
      return;
    }
    Alert.alert(
      'Paste this clip?',
      clip.content.length > 120 ? clip.content.slice(0, 120) + '…' : clip.content,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Paste', onPress: doPaste },
      ]
    );
  };

  return (
    <Pressy
      onPress={handleTap}
      onLongPress={() => onLongPress(clip)}
      style={styles.card}
      accessibilityLabel={`${clip.title ? clip.title + ': ' : ''}${clip.content}`}
      accessibilityHint="Double tap to paste."
    >
      <View style={styles.row}>
        <View style={styles.iconWrap} importantForAccessibility="no">
          <Copy size={icon.sm} strokeWidth={icon.stroke} color={colors.inkSoft} />
        </View>

        <View style={{ flex: 1 }}>
          {clip.title ? <Text style={styles.title}>{clip.title}</Text> : null}
          <Text style={styles.content} numberOfLines={2}>
            {clip.content}
          </Text>
          <Text style={styles.date}>{formatWhen(clip.createdAt)}</Text>
        </View>

        {isManualSort && (
          <View style={styles.reorderCol}>
            <CircleButton
              style={styles.circleBtn}
              disabled={isFirst}
              onPress={onMoveUp}
              accessibilityLabel="Move clip up"
            >
              <ChevronUp size={icon.sm} strokeWidth={icon.stroke} color={isFirst ? colors.inkDisabled : colors.ink} />
            </CircleButton>
            <CircleButton
              style={styles.circleBtn}
              disabled={isLast}
              onPress={onMoveDown}
              accessibilityLabel="Move clip down"
            >
              <ChevronDown size={icon.sm} strokeWidth={icon.stroke} color={isLast ? colors.inkDisabled : colors.ink} />
            </CircleButton>
          </View>
        )}

        {/*
          Long press is a shortcut, not the only route: switch control and
          many motor-impairment setups cannot perform one, so edit and delete
          need a control that can simply be activated.
        */}
        <Pressy
          onPress={() => onLongPress(clip)}
          style={styles.moreBtn}
          accessibilityLabel={`More options for ${clip.title || 'this clip'}`}
          accessibilityHint="Opens edit and delete."
          hitSlop={8}
        >
          <MoreVertical size={icon.sm} strokeWidth={icon.stroke} color={colors.inkFaint} />
        </Pressy>
      </View>
    </Pressy>
  );
}

function CircleButton({
  children,
  onPress,
  disabled,
  style,
  accessibilityLabel,
}: {
  children: React.ReactNode;
  onPress: () => void;
  disabled?: boolean;
  style: any;
  accessibilityLabel: string;
}) {
  return (
    <Pressy
      onPress={disabled ? undefined : onPress}
      style={style}
      disabled={disabled}
      accessibilityLabel={accessibilityLabel}
      hitSlop={8}
    >
      <Animated.View style={{ opacity: disabled ? 0.4 : 1 }}>{children}</Animated.View>
    </Pressy>
  );
}

function formatWhen(ts: number): string {
  const diffMs = Date.now() - ts;
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return new Date(ts).toLocaleDateString();
}

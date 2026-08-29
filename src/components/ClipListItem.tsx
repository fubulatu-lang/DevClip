import React, { useMemo } from 'react';
import { View, Text, StyleSheet, Alert, Animated } from 'react-native';
import { ChevronUp, ChevronDown, Copy } from 'lucide-react-native';
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
  const { colors, radii, spacing, shadow, type } = useTheme();
  const confirmBeforePaste = useSettingsStore((s) => s.confirmBeforePaste);

  const styles = useMemo(
    () =>
      StyleSheet.create({
        card: {
          backgroundColor: colors.surface,
          borderRadius: radii.md,
          marginHorizontal: spacing.md,
          marginBottom: spacing.sm,
          padding: spacing.md,
          ...shadow.card,
        },
        row: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm },
        iconWrap: {
          width: 28,
          height: 28,
          borderRadius: radii.sm,
          backgroundColor: colors.accentSoft,
          alignItems: 'center',
          justifyContent: 'center',
          marginTop: 2,
        },
        title: { fontFamily: type.semibold, fontSize: 14, color: colors.ink, marginBottom: 2 },
        content: { fontFamily: type.regular, fontSize: 13, color: colors.inkSoft, lineHeight: 18 },
        date: { fontFamily: type.medium, fontSize: 10, color: colors.inkFaint, marginTop: 6 },
        reorderCol: { alignItems: 'center', gap: 8 },
        circleBtn: {
          width: 28,
          height: 28,
          borderRadius: radii.pill,
          backgroundColor: colors.surfaceSunken,
          alignItems: 'center',
          justifyContent: 'center',
        },
      }),
    [colors, radii, spacing, shadow, type]
  );

  const doPaste = async () => {
    const result = await pasteClip(clip.content);
    if (result === 'copiedOnly') {
      Alert.alert('Copied', 'Could not paste automatically, so it\u2019s on your clipboard \u2014 paste it manually.');
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
      accessibilityHint="Double tap to paste. Long press to edit or delete."
    >
      <View style={styles.row}>
        <View style={styles.iconWrap}>
          <Copy size={14} strokeWidth={1.5} color={colors.accent} />
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
              <ChevronUp size={14} strokeWidth={1.75} color={isFirst ? colors.inkFaint : colors.ink} />
            </CircleButton>
            <CircleButton
              style={styles.circleBtn}
              disabled={isLast}
              onPress={onMoveDown}
              accessibilityLabel="Move clip down"
            >
              <ChevronDown size={14} strokeWidth={1.75} color={isLast ? colors.inkFaint : colors.ink} />
            </CircleButton>
          </View>
        )}
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
      hitSlop={10}
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

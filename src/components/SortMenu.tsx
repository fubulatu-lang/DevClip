import React, { useMemo } from 'react';
import { StyleSheet, ScrollView, Text } from 'react-native';
import { SortMode } from '../types/clip';
import { useTheme } from '../theme/ThemeContext';
import Pressy from './Pressy';
import { strings } from '../strings';

const OPTIONS: { value: SortMode; label: string }[] = [
  { value: 'date-desc', label: strings.sort.dateDesc },
  { value: 'date-asc', label: strings.sort.dateAsc },
  { value: 'title-asc', label: strings.sort.titleAsc },
  { value: 'title-desc', label: strings.sort.titleDesc },
  { value: 'manual', label: strings.sort.manual },
];

interface Props {
  value: SortMode;
  onChange: (s: SortMode) => void;
}

export default function SortMenu({ value, onChange }: Props) {
  const { colors, radii, spacing, text } = useTheme();
  const styles = useMemo(
    () =>
      StyleSheet.create({
        wrap: {
          paddingHorizontal: spacing.keyline,
          paddingVertical: spacing.sm,
          gap: spacing.sm,
          // A horizontal ScrollView stretches its children on the cross axis
          // by default, which would pull each pill to the full row height.
          alignItems: 'center',
        },
        chip: {
          paddingHorizontal: spacing.lg,
          justifyContent: 'center',
          borderRadius: radii.pill,
          backgroundColor: colors.surfaceSunken,
          marginRight: spacing.sm,
          // 40dp visual + 4dp hitSlop top and bottom = a 48dp touch target.
          minHeight: 40,
          paddingVertical: spacing.sm,
        },
        chipActive: { backgroundColor: colors.accent },
        chipText: { ...text.secondary, color: colors.inkSoft },
        chipTextActive: { color: colors.onAccent },
      }),
    [colors, radii, spacing, text]
  );

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.wrap}
      accessibilityRole="tablist"
    >
      {OPTIONS.map((opt) => {
        const active = value === opt.value;
        return (
          <Pressy
            key={opt.value}
            onPress={() => onChange(opt.value)}
            style={[styles.chip, active && styles.chipActive]}
            accessibilityLabel={`${strings.sort.a11yPrefix} ${opt.label}`}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            hitSlop={{ top: 4, bottom: 4, left: 0, right: 0 }}
          >
            <Text style={[styles.chipText, active && styles.chipTextActive]}>{opt.label}</Text>
          </Pressy>
        );
      })}
    </ScrollView>
  );
}

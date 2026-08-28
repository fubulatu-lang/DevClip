import React from 'react';
import { StyleSheet, ScrollView, Text } from 'react-native';
import { SortMode } from '../types/clip';
import { useTheme } from '../theme/ThemeContext';
import Pressy from './Pressy';

const OPTIONS: { value: SortMode; label: string }[] = [
  { value: 'date-desc', label: 'Newest' },
  { value: 'date-asc', label: 'Oldest' },
  { value: 'title-asc', label: 'Title A–Z' },
  { value: 'title-desc', label: 'Title Z–A' },
  { value: 'manual', label: 'Manual' },
];

interface Props {
  value: SortMode;
  onChange: (s: SortMode) => void;
}

export default function SortMenu({ value, onChange }: Props) {
  const { colors, radii, spacing, type } = useTheme();
  const styles = StyleSheet.create({
    wrap: { paddingHorizontal: spacing.md, paddingVertical: spacing.sm, gap: spacing.xs },
    chip: {
      paddingHorizontal: spacing.md,
      paddingVertical: 7,
      borderRadius: radii.pill,
      backgroundColor: colors.surfaceSunken,
      marginRight: spacing.xs,
    },
    chipActive: { backgroundColor: colors.ink },
    chipText: { fontSize: 12, fontFamily: type.semibold, color: colors.inkSoft },
    chipTextActive: { color: colors.bg },
  });

  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.wrap}>
      {OPTIONS.map((opt) => {
        const active = value === opt.value;
        return (
          <Pressy key={opt.value} onPress={() => onChange(opt.value)} style={[styles.chip, active && styles.chipActive]}>
            <Text style={[styles.chipText, active && styles.chipTextActive]}>{opt.label}</Text>
          </Pressy>
        );
      })}
    </ScrollView>
  );
}

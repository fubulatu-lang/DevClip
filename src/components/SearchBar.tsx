import React, { useMemo } from 'react';
import { View, TextInput, StyleSheet } from 'react-native';
import { Search } from 'lucide-react-native';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import { strings } from '../strings';

interface Props {
  value: string;
  onChange: (text: string) => void;
}

export default function SearchBar({ value, onChange }: Props) {
  const { colors, radii, spacing, text, icon } = useTheme();
  const { gutter } = useAdaptiveLayout();
  const styles = useMemo(
    () =>
      StyleSheet.create({
        wrap: {
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing.md,
          backgroundColor: colors.surfaceSunken,
          borderRadius: radii.pill,
          paddingHorizontal: spacing.lg,
          // minHeight, never height: the field has to grow with the text at
          // large font scales rather than clip it.
          minHeight: 48,
          paddingVertical: spacing.sm,
          marginHorizontal: gutter,
          marginTop: spacing.sm,
        },
        input: { flex: 1, ...text.body, color: colors.ink, padding: 0 },
      }),
    [colors, radii, spacing, text, gutter]
  );

  return (
    <View style={styles.wrap}>
      <Search size={icon.md} strokeWidth={icon.stroke} color={colors.inkFaint} />
      <TextInput
        style={styles.input}
        placeholder={strings.search.placeholder}
        placeholderTextColor={colors.inkFaint}
        value={value}
        onChangeText={onChange}
        autoCorrect={false}
        accessibilityLabel={strings.search.a11yLabel}
      />
    </View>
  );
}

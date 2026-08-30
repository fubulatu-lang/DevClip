import React, { useMemo } from 'react';
import { View, TextInput, StyleSheet } from 'react-native';
import { Search } from 'lucide-react-native';
import { useTheme } from '../theme/ThemeContext';

interface Props {
  value: string;
  onChange: (text: string) => void;
}

export default function SearchBar({ value, onChange }: Props) {
  const { colors, radii, spacing, text, icon } = useTheme();
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
          marginHorizontal: spacing.keyline,
          marginTop: spacing.sm,
        },
        input: { flex: 1, ...text.body, color: colors.ink, padding: 0 },
      }),
    [colors, radii, spacing, text]
  );

  return (
    <View style={styles.wrap}>
      <Search size={icon.md} strokeWidth={icon.stroke} color={colors.inkFaint} />
      <TextInput
        style={styles.input}
        placeholder="Search title or content"
        placeholderTextColor={colors.inkFaint}
        value={value}
        onChangeText={onChange}
        autoCorrect={false}
        accessibilityLabel="Search clips"
      />
    </View>
  );
}

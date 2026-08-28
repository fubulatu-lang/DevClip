import React from 'react';
import { View, TextInput, StyleSheet } from 'react-native';
import { Search } from 'lucide-react-native';
import { useTheme } from '../theme/ThemeContext';

interface Props {
  value: string;
  onChange: (text: string) => void;
}

export default function SearchBar({ value, onChange }: Props) {
  const { colors, radii, spacing, type } = useTheme();
  const styles = StyleSheet.create({
    wrap: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      backgroundColor: colors.surfaceSunken,
      borderRadius: radii.pill,
      paddingHorizontal: spacing.md,
      height: 40,
      marginHorizontal: spacing.md,
      marginTop: spacing.sm,
    },
    input: { flex: 1, fontSize: 14, fontFamily: type.medium, color: colors.ink, padding: 0 },
  });

  return (
    <View style={styles.wrap}>
      <Search size={16} strokeWidth={1.5} color={colors.inkFaint} />
      <TextInput
        style={styles.input}
        placeholder="Search title or content"
        placeholderTextColor={colors.inkFaint}
        value={value}
        onChangeText={onChange}
        autoCorrect={false}
      />
    </View>
  );
}

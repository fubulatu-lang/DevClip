import React, { useMemo } from 'react';
import { View, TextInput, StyleSheet } from 'react-native';
import { Search, X } from 'lucide-react-native';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import Pressy from './Pressy';
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
        // inkDisabled reads as decoration, not a control: it gives the glyph
        // 2.57:1 in light and 2.32:1 in dark, under the 3:1 a non-text UI
        // component needs. inkFaint carries the same weight as the leading
        // search icon and clears it — 5.30:1 and 5.08:1.
        clear: {
          width: 24,
          height: 24,
          borderRadius: radii.pill,
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: colors.inkFaint,
        },
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
        returnKeyType="search"
      />
      {/*
        Clearing a search should not mean holding backspace. Rendered only
        when there is something to clear, so the field is not permanently
        carrying a control that does nothing.
      */}
      {value.length > 0 && (
        <Pressy
          onPress={() => onChange('')}
          style={styles.clear}
          accessibilityLabel={strings.search.clearA11yLabel}
          hitSlop={12}
        >
          <X size={14} strokeWidth={2} color={colors.surface} />
        </Pressy>
      )}
    </View>
  );
}

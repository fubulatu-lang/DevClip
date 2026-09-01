import React, { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import { useReduceMotion } from '../theme/useReduceMotion';
import { useSnackbarStore } from '../store/snackbarStore';
import Pressy from './Pressy';
import { strings } from '../strings';

/**
 * Transient feedback that does not interrupt.
 *
 * Confirming a paste, or reporting that an export failed, is news — not a
 * decision. Delivering it through a dialog stops the task to demand a
 * dismissal at the exact moment the user was mid-flow. Dialogs are kept for
 * choices that must interrupt and are hard to undo: delete, clear all.
 *
 * Announced politely so a screen reader reads it without stealing focus.
 */
export default function Snackbar() {
  const { colors, radii, spacing, text } = useTheme();
  const { gutter } = useAdaptiveLayout();
  const insets = useSafeAreaInsets();
  const reduceMotion = useReduceMotion();
  const message = useSnackbarStore((s) => s.message);
  const dismiss = useSnackbarStore((s) => s.dismiss);

  const enter = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!message) return;
    if (reduceMotion) {
      enter.setValue(1);
    } else {
      Animated.timing(enter, {
        toValue: 1,
        duration: 200,
        easing: Easing.bezier(0.22, 0.25, 0, 1),
        useNativeDriver: true,
      }).start();
    }
    const timer = setTimeout(dismiss, 4000);
    return () => {
      clearTimeout(timer);
      enter.setValue(0);
    };
  }, [message, reduceMotion, dismiss, enter]);

  if (!message) return null;

  const styles = StyleSheet.create({
    wrap: {
      position: 'absolute',
      left: gutter,
      right: gutter,
      bottom: insets.bottom + spacing.lg,
    },
    bar: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.md,
      backgroundColor: colors.ink,
      borderRadius: radii.md,
      paddingVertical: spacing.md,
      paddingHorizontal: spacing.lg,
      minHeight: 48,
    },
    message: { flex: 1, ...text.secondary, color: colors.bg },
    action: { ...text.secondary, fontWeight: '500', color: colors.bg },
  });

  return (
    <Animated.View
      style={[
        styles.wrap,
        {
          opacity: enter,
          transform: [{ translateY: enter.interpolate({ inputRange: [0, 1], outputRange: [12, 0] }) }],
        },
      ]}
      pointerEvents="box-none"
      accessibilityLiveRegion="polite"
    >
      <View style={styles.bar}>
        <Text style={styles.message}>{message}</Text>
        <Pressy onPress={dismiss} accessibilityLabel={strings.common.dismiss} hitSlop={16}>
          <Text style={styles.action}>{strings.common.dismiss}</Text>
        </Pressy>
      </View>
    </Animated.View>
  );
}

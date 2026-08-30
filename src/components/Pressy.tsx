import React, { useEffect, useRef, useState } from 'react';
import {
  AccessibilityInfo,
  Animated,
  Easing,
  Pressable,
  ViewStyle,
  StyleProp,
  AccessibilityRole,
  AccessibilityState,
  Insets,
} from 'react-native';
import { useTheme } from '../theme/ThemeContext';

interface Props {
  onPress?: () => void;
  onLongPress?: () => void;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  children: React.ReactNode;
  /** Required for any icon-only control — TalkBack has nothing else to read. */
  accessibilityLabel?: string;
  accessibilityRole?: AccessibilityRole;
  accessibilityState?: AccessibilityState;
  accessibilityHint?: string;
  /** Expands the touchable area without changing the visual size. */
  hitSlop?: Insets | number;
}

/**
 * A button/card that compresses slightly on press — a small but deliberate
 * "physical" touch that reads as considered rather than templated. The
 * compression uses the One UI standard curve at the instant duration rather
 * than spring physics, so press feedback is choreographed with the rest of
 * the system instead of bouncing on its own terms.
 *
 * Honours the platform reduce-motion setting: when it is on, the scale
 * transform is skipped entirely and press feedback falls back to the
 * ripple, which is not motion the setting is meant to suppress.
 */
export default function Pressy({
  onPress,
  onLongPress,
  disabled,
  style,
  children,
  accessibilityLabel,
  accessibilityRole = 'button',
  accessibilityState,
  accessibilityHint,
  hitSlop,
}: Props) {
  const { colors, easing, duration } = useTheme();
  const scale = useRef(new Animated.Value(1)).current;
  const [reduceMotion, setReduceMotion] = useState(false);

  useEffect(() => {
    let cancelled = false;
    AccessibilityInfo.isReduceMotionEnabled().then((enabled) => {
      if (!cancelled) setReduceMotion(enabled);
    });
    const sub = AccessibilityInfo.addEventListener('reduceMotionChanged', setReduceMotion);
    return () => {
      cancelled = true;
      sub.remove();
    };
  }, []);

  const animateTo = (value: number) => {
    if (reduceMotion) return;
    Animated.timing(scale, {
      toValue: value,
      useNativeDriver: true,
      duration: duration.instant,
      easing: Easing.bezier(...easing.standard),
    }).start();
  };

  return (
    <Pressable
      disabled={disabled}
      onPress={onPress}
      onLongPress={onLongPress}
      onPressIn={() => animateTo(0.97)}
      onPressOut={() => animateTo(1)}
      accessibilityLabel={accessibilityLabel}
      accessibilityRole={accessibilityRole}
      accessibilityState={{ disabled, ...accessibilityState }}
      accessibilityHint={accessibilityHint}
      hitSlop={hitSlop}
      android_ripple={{ color: colors.border, borderless: false }}
    >
      <Animated.View style={[style, { transform: [{ scale }] }]}>{children}</Animated.View>
    </Pressable>
  );
}

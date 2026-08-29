import React, { useRef } from 'react';
import {
  Animated,
  Pressable,
  ViewStyle,
  StyleProp,
  AccessibilityRole,
  AccessibilityState,
  Insets,
} from 'react-native';

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
 * A button/card that compresses slightly on press with spring physics
 * instead of the default instant opacity flash — a small but deliberate
 * "physical" touch that reads as considered rather than templated. Also
 * carries a subtle Android ripple so touch feedback still reads as native,
 * and forwards accessibility props since every icon-only usage needs them.
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
  const scale = useRef(new Animated.Value(1)).current;

  const animateTo = (value: number) => {
    Animated.spring(scale, {
      toValue: value,
      useNativeDriver: true,
      speed: 40,
      bounciness: 6,
    }).start();
  };

  return (
    <Pressable
      disabled={disabled}
      onPress={onPress}
      onLongPress={onLongPress}
      onPressIn={() => animateTo(0.96)}
      onPressOut={() => animateTo(1)}
      accessibilityLabel={accessibilityLabel}
      accessibilityRole={accessibilityRole}
      accessibilityState={{ disabled, ...accessibilityState }}
      accessibilityHint={accessibilityHint}
      hitSlop={hitSlop}
      android_ripple={{ color: 'rgba(128,128,128,0.15)', borderless: false }}
    >
      <Animated.View style={[style, { transform: [{ scale }] }]}>{children}</Animated.View>
    </Pressable>
  );
}

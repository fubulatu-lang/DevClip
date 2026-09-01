import React, { useMemo, useRef, useState } from 'react';
import { View, StyleSheet, PanResponder, AccessibilityActionEvent } from 'react-native';
import { useTheme } from '../theme/ThemeContext';

/** Knob diameter, in dp. */
const KNOB = 24;

interface Props {
  value: number;
  min: number;
  max: number;
  /** Rounding applied to every reported value. Defaults to whole units. */
  step?: number;
  /** Fires continuously while dragging — the point of a slider is live feedback. */
  onChange: (value: number) => void;
  accessibilityLabel: string;
  /** Spoken instead of the bare number, e.g. "56 density pixels". */
  formatValue?: (value: number) => string;
}

/**
 * A One UI slider, built here rather than pulled in.
 *
 * There is exactly one slider in DevClip, and a dependency for it would have
 * to be a native module — a new library in the prebuild, a new lockfile entry,
 * and a new thing to keep matched to the Expo version — for a track, a fill
 * and a knob.
 *
 * Accessible without the gesture: TalkBack gets increment and decrement
 * actions, so the value can be changed by a control that is simply activated
 * rather than dragged. A slider that can only be dragged is unusable with
 * switch control.
 */
export default function Slider({
  value,
  min,
  max,
  step = 1,
  onChange,
  accessibilityLabel,
  formatValue,
}: Props) {
  const { colors, radii } = useTheme();
  const [width, setWidth] = useState(0);

  // PanResponder is created once and closes over its handlers, so the values
  // it needs at gesture time have to be read from refs rather than captured.
  const viewRef = useRef<View>(null);
  const widthRef = useRef(0);
  /** Left edge of the track in window coordinates. */
  const originRef = useRef(0);
  const boundsRef = useRef({ min, max, step });
  const onChangeRef = useRef(onChange);
  boundsRef.current = { min, max, step };
  onChangeRef.current = onChange;

  const clampToStep = (raw: number) => {
    const { min: lo, max: hi, step: s } = boundsRef.current;
    const clamped = Math.min(hi, Math.max(lo, raw));
    return Math.round(clamped / s) * s;
  };

  /**
   * [pageX] is a window coordinate, not a coordinate within this component.
   *
   * `nativeEvent.locationX` would be the obvious thing to use and is wrong
   * here: it is relative to whichever view actually received the touch, so a
   * finger landing on the knob reports a position within the knob and the
   * value jumps.
   */
  const report = (pageX: number) => {
    const w = widthRef.current;
    if (w <= 0) return;
    const { min: lo, max: hi } = boundsRef.current;
    const ratio = Math.min(1, Math.max(0, (pageX - originRef.current) / w));
    onChangeRef.current(clampToStep(lo + ratio * (hi - lo)));
  };

  const responder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: () => true,
        // A tap anywhere on the track jumps to that value, which is what
        // every other slider on the phone does.
        onPanResponderGrant: (_event, gesture) => report(gesture.x0),
        onPanResponderMove: (_event, gesture) => report(gesture.moveX),
      }),
    []
  );

  const onLayout = () => {
    // measureInWindow, not the layout event: the gesture reports window
    // coordinates, so the track's origin has to be in the same space.
    viewRef.current?.measureInWindow((x, _y, w) => {
      originRef.current = x;
      widthRef.current = w;
      setWidth(w);
    });
  };

  const ratio = max > min ? (Math.min(max, Math.max(min, value)) - min) / (max - min) : 0;

  const styles = useMemo(
    () =>
      StyleSheet.create({
        // The touch target is the whole row, not the 4dp track: a hairline is
        // impossible to hit and the track is decoration, not the control.
        wrap: { height: 48, justifyContent: 'center' },
        track: {
          height: 4,
          borderRadius: radii.pill,
          backgroundColor: colors.surfaceSunken,
          overflow: 'hidden',
        },
        fill: { height: 4, backgroundColor: colors.accent },
        knob: {
          position: 'absolute',
          width: KNOB,
          height: KNOB,
          borderRadius: radii.pill,
          backgroundColor: colors.accent,
          borderWidth: 2,
          borderColor: colors.surface,
        },
      }),
    [colors, radii]
  );

  const onAccessibilityAction = (event: AccessibilityActionEvent) => {
    const { step: s } = boundsRef.current;
    if (event.nativeEvent.actionName === 'increment') onChange(clampToStep(value + s));
    if (event.nativeEvent.actionName === 'decrement') onChange(clampToStep(value - s));
  };

  return (
    <View
      ref={viewRef}
      style={styles.wrap}
      onLayout={onLayout}
      accessibilityRole="adjustable"
      accessibilityLabel={accessibilityLabel}
      accessibilityValue={{
        min,
        max,
        now: value,
        text: formatValue ? formatValue(value) : undefined,
      }}
      accessibilityActions={[{ name: 'increment' }, { name: 'decrement' }]}
      onAccessibilityAction={onAccessibilityAction}
      {...responder.panHandlers}
    >
      <View style={styles.track}>
        <View style={[styles.fill, { width: `${ratio * 100}%` }]} />
      </View>
      {/* Kept fully inside the track's width at both ends, rather than
          hanging half off at 0 and at maximum. */}
      <View
        style={[styles.knob, { left: Math.min(Math.max(width - KNOB, 0), Math.max(0, ratio * width - KNOB / 2)) }]}
        importantForAccessibility="no"
      />
    </View>
  );
}

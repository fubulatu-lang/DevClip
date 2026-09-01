import React, { useMemo, useState } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ShieldCheck, CircleDot, Bell, ArrowRight, Check } from 'lucide-react-native';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import {
  isNativeOverlayAvailable,
  requestNotificationPermission,
  requestOverlayPermission,
  requestAccessibilityPermission,
} from '../native/OverlayModule';
import { PermissionState } from '../hooks/usePermissions';
import Pressy from '../components/Pressy';
import { strings } from '../strings';

interface Props {
  permissions: PermissionState;
  refresh: () => void;
  /** Carry on into the app with whatever is granted. */
  onSkip: () => void;
}

/**
 * The setup wall: mandatory, and a wall you can walk past.
 *
 * Two of the three permissions cannot be a runtime prompt. Android requires
 * the user to go into system Settings and flip a switch, and it shows a broad
 * warning screen before the accessibility one. Blocking the app until they do
 * would mean holding somebody hostage to a screen DevClip does not control
 * and cannot detect the outcome of.
 *
 * So: everything is asked for plainly, and "Continue without" is always
 * available. The app behind it says clearly what it cannot do, rather than
 * silently doing nothing — which is how each of these failures presented
 * before there was a wall at all.
 *
 * If a permission is revoked later, this comes back. Android revokes them by
 * itself for apps left unopened for a few months, and announces nothing.
 */
export default function SetupScreen({ permissions, refresh, onSkip }: Props) {
  const { colors, radii, spacing, text, icon } = useTheme();
  const { gutter } = useAdaptiveLayout();
  const [notifAsked, setNotifAsked] = useState(false);

  const granted = permissions.overlay && permissions.accessibility && permissions.notifications;

  const styles = useMemo(
    () =>
      StyleSheet.create({
        safe: { flex: 1, backgroundColor: colors.bg },
        scroll: { flex: 1 },
        // The heading, the subtitle and three permission cards fit at the
        // default text size in portrait, and clip at a large font scale or in
        // landscape — on the one screen a first-run user cannot skip past. It
        // scrolls.
        scrollContent: {
          paddingHorizontal: gutter,
          paddingTop: spacing.xl,
          paddingBottom: spacing.xl,
        },
        heading: { ...text.display, color: colors.ink, marginBottom: spacing.sm },
        sub: { ...text.secondary, color: colors.inkSoft, marginBottom: spacing.xl, lineHeight: 22 },
        card: {
          backgroundColor: colors.surface,
          borderRadius: radii.md,
          padding: spacing.lg,
          marginBottom: spacing.md,
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing.md,
        },
        iconWrap: {
          width: 40,
          height: 40,
          borderRadius: radii.sm,
          backgroundColor: colors.surfaceSunken,
          alignItems: 'center',
          justifyContent: 'center',
        },
        iconWrapDone: { backgroundColor: colors.accentSoft },
        cardTitle: { ...text.body, fontWeight: '500', color: colors.ink, marginBottom: 2 },
        cardBody: { ...text.secondary, color: colors.inkSoft, lineHeight: 22 },
        actionBtn: {
          backgroundColor: colors.surfaceSunken,
          paddingHorizontal: spacing.lg,
          minHeight: 48,
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: radii.pill,
        },
        actionBtnDone: { backgroundColor: colors.accentSoft },
        actionText: { ...text.secondary, fontWeight: '500', color: colors.inkSoft },
        actionTextDone: { color: colors.accent },
        footer: { paddingHorizontal: gutter, paddingVertical: spacing.keyline, gap: spacing.md },
        continueBtn: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: spacing.sm,
          backgroundColor: colors.accent,
          borderRadius: radii.pill,
          minHeight: 48,
        },
        continueText: { ...text.button, color: colors.onAccent },
        skipBtn: {
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: 48,
          borderRadius: radii.pill,
        },
        skipText: { ...text.secondary, fontWeight: '500', color: colors.inkSoft },
      }),
    [colors, radii, spacing, text, gutter]
  );

  const cards = [
    {
      key: 'accessibility',
      icon: <ShieldCheck size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />,
      title: strings.setup.textCapture,
      body: strings.setup.textCaptureBody,
      done: permissions.accessibility,
      onPress: requestAccessibilityPermission,
    },
    {
      key: 'overlay',
      icon: <CircleDot size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />,
      title: strings.setup.floatingBubble,
      body: strings.setup.floatingBubbleBody,
      done: permissions.overlay,
      onPress: requestOverlayPermission,
    },
    {
      key: 'notifications',
      icon: <Bell size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />,
      title: strings.setup.notifications,
      body: strings.setup.notificationsBody,
      done: permissions.notifications,
      onPress: async () => {
        // The one permission Android lets an app ask for directly. Fired on a
        // press rather than automatically on mount: a dialog that appears
        // before the user has read what it is for gets dismissed reflexively,
        // and Android only offers it once.
        await requestNotificationPermission();
        setNotifAsked(true);
        refresh();
      },
    },
  ];

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}>
        <Text style={styles.heading}>{strings.setup.heading}</Text>
        <Text style={styles.sub}>
          {isNativeOverlayAvailable() ? strings.setup.sub : strings.setup.expoGoSub}
        </Text>

        {isNativeOverlayAvailable() &&
          cards.map((card) => (
            <View key={card.key} style={styles.card}>
              <View
                style={[styles.iconWrap, card.done && styles.iconWrapDone]}
                importantForAccessibility="no"
              >
                {card.done ? (
                  <Check size={icon.md} strokeWidth={icon.stroke} color={colors.accent} />
                ) : (
                  card.icon
                )}
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.cardTitle}>{card.title}</Text>
                <Text style={styles.cardBody}>{card.body}</Text>
              </View>
              <Pressy
                onPress={card.onPress}
                style={[styles.actionBtn, card.done && styles.actionBtnDone]}
                accessibilityLabel={
                  card.done
                    ? strings.setup.enabledA11y(card.title)
                    : strings.setup.enableA11y(card.title)
                }
              >
                <Text style={[styles.actionText, card.done && styles.actionTextDone]}>
                  {card.done ? strings.setup.done : strings.setup.enable}
                </Text>
              </Pressy>
            </View>
          ))}
      </ScrollView>

      <View style={styles.footer}>
        {granted || !isNativeOverlayAvailable() ? (
          <Pressy
            onPress={onSkip}
            style={styles.continueBtn}
            accessibilityLabel={strings.setup.continue}
          >
            <Text style={styles.continueText}>{strings.setup.continue}</Text>
            <ArrowRight size={icon.sm} strokeWidth={icon.stroke} color={colors.onAccent} />
          </Pressy>
        ) : (
          // Always available, and deliberately the quieter of the two: the
          // permissions are what make DevClip work, but refusing to let
          // somebody into their own app until they have visited two system
          // Settings screens is not a thing an app gets to do.
          <Pressy
            onPress={onSkip}
            style={styles.skipBtn}
            accessibilityLabel={strings.setup.skip}
            accessibilityHint={strings.setup.skipHint}
          >
            <Text style={styles.skipText}>{strings.setup.skip}</Text>
          </Pressy>
        )}
        {notifAsked && !permissions.notifications ? (
          <Text style={styles.cardBody}>{strings.setup.notificationsDenied}</Text>
        ) : null}
      </View>
    </SafeAreaView>
  );
}

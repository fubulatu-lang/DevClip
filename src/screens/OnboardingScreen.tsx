import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, AppState } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ShieldCheck, CircleDot, Bell, ArrowRight } from 'lucide-react-native';
import { useTheme } from '../theme/ThemeContext';
import { useSettingsStore } from '../store/settingsStore';
import {
  isNativeOverlayAvailable,
  requestNotificationPermission,
  requestOverlayPermission,
  requestAccessibilityPermission,
  isAccessibilityServiceEnabled,
  isOverlayPermissionGranted,
} from '../native/OverlayModule';
import Pressy from '../components/Pressy';
import { strings } from '../strings';

export default function OnboardingScreen({ onDone }: { onDone: () => void }) {
  const { colors, radii, spacing, text, icon } = useTheme();
  const [notifAsked, setNotifAsked] = useState(false);
  const [overlayGranted, setOverlayGranted] = useState(false);
  const [accessibilityGranted, setAccessibilityGranted] = useState(false);

  const refresh = async () => {
    setOverlayGranted(await isOverlayPermissionGranted());
    setAccessibilityGranted(await isAccessibilityServiceEnabled());
  };

  useEffect(() => {
    // Fires the real system permission dialog automatically, on first launch —
    // this is the one permission Android lets an app ask for directly.
    requestNotificationPermission().finally(() => setNotifAsked(true));
    refresh();

    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') refresh();
    });
    return () => sub.remove();
  }, []);

  const styles = useMemo(() => StyleSheet.create({
    safe: { flex: 1, backgroundColor: colors.bg },
    scroll: { flex: 1, paddingHorizontal: spacing.keyline, paddingTop: spacing.xl },
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
    footer: { padding: spacing.keyline },
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
  }), [colors, radii, spacing, text]);

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.scroll}>
        <Text style={styles.heading}>{strings.onboarding.heading}</Text>
        <Text style={styles.sub}>{strings.onboarding.sub}</Text>

        <View style={styles.card}>
          <View style={styles.iconWrap} importantForAccessibility="no">
            <Bell size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardTitle}>{strings.onboarding.notifications}</Text>
            <Text style={styles.cardBody}>{strings.onboarding.notificationsBody}</Text>
          </View>
          <View style={[styles.actionBtn, notifAsked && styles.actionBtnDone]}>
            <Text style={[styles.actionText, notifAsked && styles.actionTextDone]}>
              {notifAsked ? strings.onboarding.done : strings.onboarding.pending}
            </Text>
          </View>
        </View>

        {isNativeOverlayAvailable() && (
          <>
            <View style={styles.card}>
              <View style={styles.iconWrap} importantForAccessibility="no">
                <ShieldCheck size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.cardTitle}>{strings.onboarding.backgroundCapture}</Text>
                <Text style={styles.cardBody}>{strings.onboarding.backgroundCaptureBody}</Text>
              </View>
              <Pressy
                onPress={requestAccessibilityPermission}
                style={[styles.actionBtn, accessibilityGranted && styles.actionBtnDone]}
                accessibilityLabel={
                  accessibilityGranted
                    ? strings.onboarding.enabledA11y(strings.onboarding.backgroundCapture)
                    : strings.onboarding.enableA11y(strings.onboarding.backgroundCapture)
                }
              >
                <Text style={[styles.actionText, accessibilityGranted && styles.actionTextDone]}>
                  {accessibilityGranted ? strings.onboarding.done : strings.onboarding.enable}
                </Text>
              </Pressy>
            </View>

            <View style={styles.card}>
              <View style={styles.iconWrap} importantForAccessibility="no">
                <CircleDot size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.cardTitle}>{strings.onboarding.floatingBubble}</Text>
                <Text style={styles.cardBody}>{strings.onboarding.floatingBubbleBody}</Text>
              </View>
              <Pressy
                onPress={requestOverlayPermission}
                style={[styles.actionBtn, overlayGranted && styles.actionBtnDone]}
                accessibilityLabel={
                  overlayGranted
                    ? strings.onboarding.enabledA11y(strings.onboarding.floatingBubble)
                    : strings.onboarding.enableA11y(strings.onboarding.floatingBubble)
                }
              >
                <Text style={[styles.actionText, overlayGranted && styles.actionTextDone]}>
                  {overlayGranted ? strings.onboarding.done : strings.onboarding.enable}
                </Text>
              </Pressy>
            </View>
          </>
        )}
      </View>

      <View style={styles.footer}>
        <Pressy onPress={onDone} style={styles.continueBtn} accessibilityLabel={strings.onboarding.continue}>
          <Text style={styles.continueText}>{strings.onboarding.continue}</Text>
          <ArrowRight size={icon.sm} strokeWidth={icon.stroke} color={colors.onAccent} />
        </Pressy>
      </View>
    </SafeAreaView>
  );
}

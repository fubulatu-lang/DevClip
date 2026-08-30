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

export default function OnboardingScreen({ onDone }: { onDone: () => void }) {
  const { colors, radii, spacing, text } = useTheme();
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
      backgroundColor: colors.accentSoft,
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
        <Text style={styles.heading}>Set up DevClip</Text>
        <Text style={styles.sub}>
          Three quick permissions and you're set. You can always change these later from Settings.
        </Text>

        <View style={styles.card}>
          <View style={styles.iconWrap}>
            <Bell size={18} strokeWidth={1.5} color={colors.accent} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardTitle}>Notifications</Text>
            <Text style={styles.cardBody}>Shows a quiet, permanent notification while capture is active.</Text>
          </View>
          <View style={[styles.actionBtn, notifAsked && styles.actionBtnDone]}>
            <Text style={[styles.actionText, notifAsked && styles.actionTextDone]}>
              {notifAsked ? 'Done' : '…'}
            </Text>
          </View>
        </View>

        {isNativeOverlayAvailable() && (
          <>
            <View style={styles.card}>
              <View style={styles.iconWrap}>
                <ShieldCheck size={18} strokeWidth={1.5} color={colors.accent} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.cardTitle}>Background capture</Text>
                <Text style={styles.cardBody}>
                  Lets DevClip save copies made in any app automatically, and paste a saved clip
                  directly into whatever field you were using. Android will show a broader
                  permission screen for this — that's expected.
                </Text>
              </View>
              <Pressy
                onPress={requestAccessibilityPermission}
                style={[styles.actionBtn, accessibilityGranted && styles.actionBtnDone]}
                accessibilityLabel={accessibilityGranted ? 'Background capture is enabled' : 'Enable background capture'}
              >
                <Text style={[styles.actionText, accessibilityGranted && styles.actionTextDone]}>
                  {accessibilityGranted ? 'Done' : 'Enable'}
                </Text>
              </Pressy>
            </View>

            <View style={styles.card}>
              <View style={styles.iconWrap}>
                <CircleDot size={18} strokeWidth={1.5} color={colors.accent} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.cardTitle}>Floating bubble</Text>
                <Text style={styles.cardBody}>Lets DevClip draw the bubble on top of other apps.</Text>
              </View>
              <Pressy
                onPress={requestOverlayPermission}
                style={[styles.actionBtn, overlayGranted && styles.actionBtnDone]}
                accessibilityLabel={overlayGranted ? 'Floating bubble is enabled' : 'Enable floating bubble'}
              >
                <Text style={[styles.actionText, overlayGranted && styles.actionTextDone]}>
                  {overlayGranted ? 'Done' : 'Enable'}
                </Text>
              </Pressy>
            </View>
          </>
        )}
      </View>

      <View style={styles.footer}>
        <Pressy onPress={onDone} style={styles.continueBtn} accessibilityLabel="Continue">
          <Text style={styles.continueText}>Continue</Text>
          <ArrowRight size={18} strokeWidth={1.5} color={colors.onAccent} />
        </Pressy>
      </View>
    </SafeAreaView>
  );
}

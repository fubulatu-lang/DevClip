import React, { useMemo, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Settings as SettingsIcon } from 'lucide-react-native';
import ClipListView from './ClipListView';
import SettingsScreen from './SettingsScreen';
import OnboardingScreen from './OnboardingScreen';
import Pressy from '../components/Pressy';
import { useTheme } from '../theme/ThemeContext';
import { useSettingsStore } from '../store/settingsStore';
import { strings } from '../strings';

/**
 * The full-screen app, opened from the launcher.
 *
 * Window sizing lives entirely in the overlay now: the bubble opens a mini
 * window that can expand to a half-screen sheet, and native owns both
 * geometries. So this screen no longer carries size tabs — it is simply
 * the whole app at whatever size the system gives it.
 */
export default function PopupScreen() {
  const { colors, radii, spacing, text, icon } = useTheme();
  const [showSettings, setShowSettings] = useState(false);
  const hasOnboarded = useSettingsStore((s) => s.hasOnboarded);
  const setOnboarded = useSettingsStore((s) => s.setOnboarded);

  const styles = useMemo(
    () =>
      StyleSheet.create({
        screen: { flex: 1, backgroundColor: colors.bg },
        hero: {
          paddingHorizontal: spacing.keyline,
          paddingTop: spacing.lg,
          paddingBottom: spacing.md,
          backgroundColor: colors.surface,
          borderBottomWidth: 1,
          borderBottomColor: colors.divider,
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'space-between',
        },
        heroLeft: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
        logoDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.accent },
        title: { ...text.title, color: colors.ink },
        gearBtn: {
          width: 48,
          height: 48,
          borderRadius: radii.pill,
          backgroundColor: colors.surfaceSunken,
          alignItems: 'center',
          justifyContent: 'center',
        },
      }),
    [colors, radii, spacing, text]
  );

  if (!hasOnboarded) {
    return <OnboardingScreen onDone={setOnboarded} />;
  }

  return (
    <SafeAreaView style={styles.screen} edges={['top', 'bottom']}>
      <View style={styles.hero}>
        <View style={styles.heroLeft}>
          <View style={styles.logoDot} />
          <Text style={styles.title}>{strings.app.name}</Text>
        </View>
        <Pressy
          onPress={() => setShowSettings(true)}
          style={styles.gearBtn}
          accessibilityLabel={strings.settings.open}
        >
          <SettingsIcon size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />
        </Pressy>
      </View>

      {showSettings ? <SettingsScreen onBack={() => setShowSettings(false)} /> : <ClipListView />}
    </SafeAreaView>
  );
}

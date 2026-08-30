import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, AppState } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CircleDot, Settings as SettingsIcon } from 'lucide-react-native';
import ClipListView from './ClipListView';
import SettingsScreen from './SettingsScreen';
import OnboardingScreen from './OnboardingScreen';
import Pressy from '../components/Pressy';
import { PopupState } from '../types/clip';
import {
  resizePopupWindow,
  isNativeOverlayAvailable,
  startBubble,
  stopBubble,
  requestOverlayPermission,
  isOverlayPermissionGranted,
  isBubbleRunning,
} from '../native/OverlayModule';
import { useTheme } from '../theme/ThemeContext';
import { useSettingsStore } from '../store/settingsStore';

const SIZES: Record<PopupState, { width: number; height: number }> = {
  small: { width: 300, height: 400 },
  expanded: { width: 360, height: 640 },
  full: { width: -1, height: -1 },
};

const TABS: { key: PopupState; label: string }[] = [
  { key: 'small', label: 'Small' },
  { key: 'expanded', label: 'Expanded' },
  { key: 'full', label: 'Full App' },
];

export default function PopupScreen() {
  const { colors, radii, spacing, shadow, text } = useTheme();
  const [state, setState] = useState<PopupState>('small');
  const [showSettings, setShowSettings] = useState(false);
  const [bubbleRunning, setBubbleRunning] = useState(false);
  const hasOnboarded = useSettingsStore((s) => s.hasOnboarded);
  const setOnboarded = useSettingsStore((s) => s.setOnboarded);

  useEffect(() => {
    isBubbleRunning().then(setBubbleRunning);
    const sub = AppState.addEventListener('change', (appState) => {
      if (appState === 'active') isBubbleRunning().then(setBubbleRunning);
    });
    return () => sub.remove();
  }, []);

  const changeState = (next: PopupState) => {
    setState(next);
    if (isNativeOverlayAvailable()) {
      const { width, height } = SIZES[next];
      resizePopupWindow(width, height);
    }
  };

  const toggleBubble = async () => {
    if (bubbleRunning) {
      stopBubble();
      setBubbleRunning(false);
      return;
    }
    const granted = (await isOverlayPermissionGranted()) || (await requestOverlayPermission());
    if (granted) {
      startBubble();
      setBubbleRunning(true);
    }
  };

  const floating = state !== 'full';

  const styles = useMemo(
    () =>
      StyleSheet.create({
        outer: { flex: 1, backgroundColor: colors.bg },
        outerFloating: {
          padding: 5,
          borderRadius: radii.container + 5,
          backgroundColor: colors.divider,
          ...shadow.floating,
        },
        inner: { flex: 1, backgroundColor: colors.bg },
        innerFloating: { borderRadius: radii.container, overflow: 'hidden' },
        hero: {
          paddingHorizontal: spacing.keyline,
          paddingTop: spacing.lg,
          paddingBottom: spacing.md,
          backgroundColor: colors.surface,
          borderBottomWidth: 1,
          borderBottomColor: colors.divider,
        },
        heroTopRow: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: spacing.md,
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
        switcherRow: { flexDirection: 'row', gap: spacing.sm, alignItems: 'center' },
        bubbleBtn: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: spacing.xs,
          paddingHorizontal: spacing.md,
          borderRadius: radii.pill,
          backgroundColor: colors.surfaceSunken,
          minHeight: 48,
        },
        bubbleBtnActive: { backgroundColor: colors.accentSoft },
        bubbleBtnText: { ...text.micro, color: colors.inkSoft },
        bubbleBtnTextActive: { color: colors.accent },
        tabBar: {
          flex: 1,
          flexDirection: 'row',
          backgroundColor: colors.surfaceSunken,
          borderRadius: radii.pill,
          padding: 4,
        },
        tab: {
          flex: 1,
          justifyContent: 'center',
          borderRadius: radii.pill,
          alignItems: 'center',
          minHeight: 40,
          paddingVertical: spacing.xs,
        },
        tabActive: { backgroundColor: colors.surface },
        tabText: { ...text.micro, color: colors.inkFaint },
        tabTextActive: { color: colors.ink, fontWeight: '500' },
      }),
    [colors, radii, spacing, shadow, text]
  );

  if (!hasOnboarded) {
    return <OnboardingScreen onDone={setOnboarded} />;
  }

  return (
    <View style={[styles.outer, floating && styles.outerFloating]}>
      <SafeAreaView
        style={[styles.inner, floating && styles.innerFloating]}
        edges={floating ? [] : ['top', 'bottom']}
      >
        <View style={styles.hero}>
          <View style={styles.heroTopRow}>
            <View style={styles.heroLeft}>
              <View style={styles.logoDot} />
              <Text style={styles.title}>DevClip</Text>
            </View>
            <Pressy
              onPress={() => setShowSettings(true)}
              style={styles.gearBtn}
              accessibilityLabel="Settings"
            >
              <SettingsIcon size={24} strokeWidth={1.5} color={colors.ink} />
            </Pressy>
          </View>

          <View style={styles.switcherRow}>
            {isNativeOverlayAvailable() && (
              <Pressy
                onPress={toggleBubble}
                style={[styles.bubbleBtn, bubbleRunning && styles.bubbleBtnActive]}
                accessibilityLabel={bubbleRunning ? 'Turn off floating bubble' : 'Turn on floating bubble'}
                accessibilityState={{ checked: bubbleRunning }}
                accessibilityRole="switch"
              >
                <CircleDot size={18} strokeWidth={1.5} color={bubbleRunning ? colors.accent : colors.inkFaint} />
                <Text style={[styles.bubbleBtnText, bubbleRunning && styles.bubbleBtnTextActive]}>
                  {bubbleRunning ? 'Bubble on' : 'Bubble off'}
                </Text>
              </Pressy>
            )}
            <View style={styles.tabBar}>
              {TABS.map((tab) => {
                const active = state === tab.key;
                return (
                  <Pressy
                    key={tab.key}
                    onPress={() => changeState(tab.key)}
                    style={[styles.tab, active && styles.tabActive]}
                    accessibilityLabel={`${tab.label} view`}
                    accessibilityRole="tab"
                    accessibilityState={{ selected: active }}
                    hitSlop={{ top: 4, bottom: 4, left: 0, right: 0 }}
                  >
                    <Text style={[styles.tabText, active && styles.tabTextActive]}>{tab.label}</Text>
                  </Pressy>
                );
              })}
            </View>
          </View>
        </View>

        {showSettings ? <SettingsScreen onBack={() => setShowSettings(false)} /> : <ClipListView />}
      </SafeAreaView>
    </View>
  );
}

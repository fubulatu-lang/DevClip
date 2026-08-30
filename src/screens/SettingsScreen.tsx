import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, AppState, Alert, ScrollView, BackHandler } from 'react-native';
import {
  ArrowLeft,
  ShieldCheck,
  CircleDot,
  Bell,
  Sun,
  Moon,
  SmartphoneCharging,
  Trash2,
  MessageSquareWarning,
  Power,
  Layers,
  Download,
} from 'lucide-react-native';
import {
  isNativeOverlayAvailable,
  requestOverlayPermission,
  requestAccessibilityPermission,
  requestNotificationPermission,
  isAccessibilityServiceEnabled,
  isOverlayPermissionGranted,
  isNotificationPermissionGranted,
  startBubble,
  stopBubble,
} from '../native/OverlayModule';
import { exportBackup } from '../utils/backup';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import { useSettingsStore, ThemeMode, BubbleSize } from '../store/settingsStore';
import { useClipStore } from '../store/clipStore';
import Pressy from '../components/Pressy';
import { strings } from '../strings';

/**
 * Settings is opened and closed from the clip list rather than pushed onto a
 * navigation stack, so it unmounts every time and would otherwise reopen at
 * the top. Remembering the offset for the life of the process restores the
 * position the user left, which is what returning to a screen should do.
 */
let lastScrollOffset = 0;

const MAX_CLIPS_OPTIONS = [
  { value: 100, label: '100' },
  { value: 500, label: '500' },
  { value: 1000, label: '1000' },
  { value: 0, label: strings.settings.noLimit },
];

export default function SettingsScreen({ onBack }: { onBack: () => void }) {
  const { colors, radii, spacing, text, icon } = useTheme();
  const { gutter } = useAdaptiveLayout();
  const [accessibilityOn, setAccessibilityOn] = useState(false);
  const [overlayOn, setOverlayOn] = useState(false);
  const [notifOn, setNotifOn] = useState(false);
  const [bubbleRunning, setBubbleRunning] = useState(false);
  const [exporting, setExporting] = useState(false);

  const themeMode = useSettingsStore((s) => s.themeMode);
  const setThemeMode = useSettingsStore((s) => s.setThemeMode);
  const bubbleSize = useSettingsStore((s) => s.bubbleSize);
  const setBubbleSize = useSettingsStore((s) => s.setBubbleSize);
  const autoStartOnBoot = useSettingsStore((s) => s.autoStartOnBoot);
  const setAutoStartOnBoot = useSettingsStore((s) => s.setAutoStartOnBoot);
  const confirmBeforePaste = useSettingsStore((s) => s.confirmBeforePaste);
  const setConfirmBeforePaste = useSettingsStore((s) => s.setConfirmBeforePaste);
  const maxClips = useSettingsStore((s) => s.maxClips);
  const setMaxClips = useSettingsStore((s) => s.setMaxClips);
  const clearAll = useClipStore((s) => s.clearAll);

  const refreshStatus = useCallback(async () => {
    setAccessibilityOn(await isAccessibilityServiceEnabled());
    setOverlayOn(await isOverlayPermissionGranted());
    setNotifOn(await isNotificationPermissionGranted());
  }, []);

  useEffect(() => {
    refreshStatus();
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') refreshStatus();
    });
    return () => sub.remove();
  }, [refreshStatus]);

  useEffect(() => {
    // Without this, the system/gesture Back button exits the popup entirely
    // instead of returning to the clip list, since Settings is just a
    // conditionally-rendered view rather than a real navigation stack entry.
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      onBack();
      return true;
    });
    return () => sub.remove();
  }, [onBack]);

  const styles = useMemo(() => StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.bg },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.md,
      paddingHorizontal: gutter,
      paddingVertical: spacing.xs,
      minHeight: 56,
    },
    backBtn: {
      width: 48,
      height: 48,
      borderRadius: radii.pill,
      alignItems: 'center',
      justifyContent: 'center',
    },
    headerTitle: { ...text.title, color: colors.ink },
    section: {
      marginHorizontal: gutter,
      marginTop: spacing.md,
      backgroundColor: colors.surface,
      borderRadius: radii.md,
      padding: spacing.lg,
      gap: spacing.md,
    },
    // One UI section headers are sentence case, not letter-spaced caps.
    sectionTitle: { ...text.secondary, fontWeight: '500', color: colors.inkSoft, marginBottom: spacing.xs },
    note: { ...text.secondary, color: colors.inkSoft, lineHeight: 22 },
    row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: spacing.md },
    // A label above its control, so segmented groups get the full width and
    // every segment can reach a 48dp target inside a 300dp popup.
    stackRow: { gap: spacing.sm },
    rowLeft: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, flexShrink: 1 },
    rowLabel: { ...text.body, color: colors.ink, flexShrink: 1 },
    statusDot: { width: 8, height: 8, borderRadius: 4 },
    statusText: { ...text.caption, fontWeight: '500' },
    pillGroup: {
      flexDirection: 'row',
      backgroundColor: colors.surfaceSunken,
      borderRadius: radii.pill,
      padding: 4,
      gap: spacing.xs,
    },
    pill: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 40,
      paddingHorizontal: spacing.sm,
      borderRadius: radii.pill,
    },
    pillActive: { backgroundColor: colors.accent },
    pillText: { ...text.secondary, color: colors.inkSoft },
    pillTextActive: { color: colors.onAccent },
    actionBtn: {
      backgroundColor: colors.surfaceSunken,
      paddingHorizontal: spacing.lg,
      minHeight: 48,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: radii.pill,
    },
    actionBtnActive: { backgroundColor: colors.accentSoft },
    actionBtnText: { ...text.secondary, fontWeight: '500', color: colors.inkSoft },
    actionBtnTextActive: { color: colors.accent },
    exportBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      backgroundColor: colors.accentSoft,
      minHeight: 48,
      borderRadius: radii.pill,
      justifyContent: 'center',
    },
    exportText: { ...text.button, color: colors.accent },
    dangerBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      backgroundColor: colors.dangerSoft,
      minHeight: 48,
      borderRadius: radii.pill,
      justifyContent: 'center',
    },
    dangerText: { ...text.button, color: colors.danger },
  }), [colors, radii, spacing, text, gutter]);

  const handleClearAll = () => {
    Alert.alert(strings.settings.clearAllTitle, strings.settings.clearAllBody, [
      { text: strings.common.cancel, style: 'cancel' },
      { text: strings.settings.clearAll, style: 'destructive', onPress: clearAll },
    ]);
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      await exportBackup();
    } catch (e) {
      Alert.alert(strings.settings.exportFailedTitle, strings.settings.exportFailedBody);
    } finally {
      setExporting(false);
    }
  };

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <Pressy onPress={onBack} style={styles.backBtn} accessibilityLabel={strings.settings.back}>
          <ArrowLeft size={icon.md} strokeWidth={icon.stroke} color={colors.ink} />
        </Pressy>
        <Text style={styles.headerTitle}>{strings.settings.title}</Text>
      </View>

      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={{ paddingBottom: spacing.xl }}
        contentOffset={{ x: 0, y: lastScrollOffset }}
        onScroll={(e) => {
          lastScrollOffset = e.nativeEvent.contentOffset.y;
        }}
        scrollEventThrottle={16}
      >
        {/* Permissions status */}
        {isNativeOverlayAvailable() && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>{strings.settings.permissions}</Text>
            <PermissionRow label={strings.settings.backgroundCapture} granted={accessibilityOn} colors={colors} styles={styles} />
            <PermissionRow label={strings.settings.floatingBubble} granted={overlayOn} colors={colors} styles={styles} />
            <PermissionRow label={strings.settings.notifications} granted={notifOn} colors={colors} styles={styles} />
          </View>
        )}

        {/* Appearance */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{strings.settings.appearance}</Text>
          <View style={styles.stackRow}>
            <View style={styles.rowLeft}>
              {themeMode === 'dark' ? (
                <Moon size={icon.md} strokeWidth={icon.stroke} color={colors.accent} />
              ) : (
                <Sun size={icon.md} strokeWidth={icon.stroke} color={colors.accent} />
              )}
              <Text style={styles.rowLabel}>{strings.settings.theme}</Text>
            </View>
            <ThreeWayPill
              groupLabel={strings.settings.theme}
              options={[
                { value: 'light', label: strings.settings.themeLight },
                { value: 'dark', label: strings.settings.themeDark },
                { value: 'system', label: strings.settings.themeAuto, a11yLabel: strings.settings.themeAutoA11y },
              ]}
              value={themeMode}
              onChange={(v) => setThemeMode(v as ThemeMode)}
              styles={styles}
            />
          </View>
        </View>

        {/* Capture behavior */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{strings.settings.capture}</Text>

          {!isNativeOverlayAvailable() ? (
            <Text style={styles.note}>{strings.settings.devClientNote}</Text>
          ) : (
            <>
              <SettingRow
                icon={<ShieldCheck size={icon.md} strokeWidth={icon.stroke} color={accessibilityOn ? colors.accent : colors.inkFaint} />}
                label={strings.settings.backgroundCapture}
                active={accessibilityOn}
                buttonLabel={accessibilityOn ? strings.settings.manage : strings.settings.enable}
                onPress={requestAccessibilityPermission}
                styles={styles}
              />
              <SettingRow
                icon={<Bell size={icon.md} strokeWidth={icon.stroke} color={notifOn ? colors.accent : colors.inkFaint} />}
                label={strings.settings.notifications}
                active={notifOn}
                buttonLabel={notifOn ? strings.settings.manage : strings.settings.enable}
                onPress={async () => setNotifOn(await requestNotificationPermission())}
                styles={styles}
              />
              <SettingRow
                icon={<CircleDot size={icon.md} strokeWidth={icon.stroke} color={bubbleRunning ? colors.accent : colors.inkFaint} />}
                label={strings.settings.floatingBubble}
                active={bubbleRunning}
                buttonLabel={bubbleRunning ? strings.settings.stop : strings.settings.start}
                onPress={async () => {
                  if (bubbleRunning) {
                    stopBubble();
                    setBubbleRunning(false);
                  } else {
                    const granted = overlayOn || (await requestOverlayPermission());
                    if (granted) {
                      startBubble();
                      setBubbleRunning(true);
                    }
                  }
                }}
                styles={styles}
              />

              <View style={styles.stackRow}>
                <View style={styles.rowLeft}>
                  <Layers size={icon.md} strokeWidth={icon.stroke} color={colors.inkFaint} />
                  <Text style={styles.rowLabel}>{strings.settings.bubbleSize}</Text>
                </View>
                <ThreeWayPill
                  groupLabel={strings.settings.bubbleSize}
                  options={[
                    { value: 'small', label: strings.settings.bubbleSmall, a11yLabel: strings.settings.bubbleSmallA11y },
                    { value: 'medium', label: strings.settings.bubbleMedium, a11yLabel: strings.settings.bubbleMediumA11y },
                    { value: 'large', label: strings.settings.bubbleLarge, a11yLabel: strings.settings.bubbleLargeA11y },
                  ]}
                  value={bubbleSize}
                  onChange={(v) => setBubbleSize(v as BubbleSize)}
                  styles={styles}
                />
              </View>

              <View style={styles.row}>
                <View style={styles.rowLeft}>
                  <Power size={icon.md} strokeWidth={icon.stroke} color={colors.inkFaint} />
                  <Text style={styles.rowLabel}>{strings.settings.autoStart}</Text>
                </View>
                <Pressy
                  onPress={() => setAutoStartOnBoot(!autoStartOnBoot)}
                  style={[styles.actionBtn, autoStartOnBoot && styles.actionBtnActive]}
                  accessibilityLabel={strings.settings.autoStart}
                  accessibilityRole="switch"
                  accessibilityState={{ checked: autoStartOnBoot }}
                >
                  <Text style={[styles.actionBtnText, autoStartOnBoot && styles.actionBtnTextActive]}>
                    {autoStartOnBoot ? strings.settings.on : strings.settings.off}
                  </Text>
                </Pressy>
              </View>
            </>
          )}

          <View style={styles.row}>
            <View style={styles.rowLeft}>
              <MessageSquareWarning size={icon.md} strokeWidth={icon.stroke} color={colors.inkFaint} />
              <Text style={styles.rowLabel}>{strings.settings.confirmPaste}</Text>
            </View>
            <Pressy
              onPress={() => setConfirmBeforePaste(!confirmBeforePaste)}
              style={[styles.actionBtn, confirmBeforePaste && styles.actionBtnActive]}
              accessibilityLabel={strings.settings.confirmPaste}
              accessibilityRole="switch"
              accessibilityState={{ checked: confirmBeforePaste }}
            >
              <Text style={[styles.actionBtnText, confirmBeforePaste && styles.actionBtnTextActive]}>
                {confirmBeforePaste ? strings.settings.on : strings.settings.off}
              </Text>
            </Pressy>
          </View>
        </View>

        {/* Storage */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{strings.settings.storage}</Text>
          <View style={styles.stackRow}>
            <View style={styles.rowLeft}>
              <SmartphoneCharging size={icon.md} strokeWidth={icon.stroke} color={colors.inkFaint} />
              <Text style={styles.rowLabel}>{strings.settings.keepAtMost}</Text>
            </View>
            <View style={styles.pillGroup}>
              {MAX_CLIPS_OPTIONS.map((opt) => {
                const active = maxClips === opt.value;
                return (
                  <Pressy
                    key={opt.value}
                    onPress={() => setMaxClips(opt.value)}
                    style={[styles.pill, active && styles.pillActive]}
                    accessibilityLabel={strings.settings.keepAtMostA11y(opt.label)}
                    accessibilityRole="radio"
                    accessibilityState={{ selected: active, checked: active }}
                    hitSlop={{ top: 4, bottom: 4, left: 0, right: 0 }}
                  >
                    <Text style={[styles.pillText, active && styles.pillTextActive]}>{opt.label}</Text>
                  </Pressy>
                );
              })}
            </View>
          </View>

          <Pressy onPress={handleExport} style={styles.exportBtn} accessibilityLabel={strings.settings.exportBackup}>
            <Download size={icon.sm} strokeWidth={icon.stroke} color={colors.accent} />
            <Text style={styles.exportText}>{exporting ? strings.settings.exporting : strings.settings.exportBackup}</Text>
          </Pressy>

          <Pressy onPress={handleClearAll} style={styles.dangerBtn} accessibilityLabel={strings.settings.clearAll}>
            <Trash2 size={icon.sm} strokeWidth={icon.stroke} color={colors.danger} />
            <Text style={styles.dangerText}>{strings.settings.clearAll}</Text>
          </Pressy>
        </View>
      </ScrollView>
    </View>
  );
}

function PermissionRow({
  label,
  granted,
  colors,
  styles,
}: {
  label: string;
  granted: boolean;
  colors: any;
  styles: any;
}) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <View style={styles.rowLeft}>
        <View style={[styles.statusDot, { backgroundColor: granted ? colors.success : colors.danger }]} />
        <Text style={[styles.statusText, { color: granted ? colors.success : colors.danger }]}>
          {granted ? strings.settings.granted : strings.settings.notGranted}
        </Text>
      </View>
    </View>
  );
}

function SettingRow({
  icon,
  label,
  active,
  buttonLabel,
  onPress,
  styles,
}: {
  icon: React.ReactNode;
  label: string;
  active: boolean;
  buttonLabel: string;
  onPress: () => void;
  styles: any;
}) {
  return (
    <View style={styles.row}>
      <View style={styles.rowLeft}>
        {icon}
        <Text style={styles.rowLabel}>{label}</Text>
      </View>
      <Pressy onPress={onPress} style={[styles.actionBtn, active && styles.actionBtnActive]}>
        <Text style={[styles.actionBtnText, active && styles.actionBtnTextActive]}>{buttonLabel}</Text>
      </Pressy>
    </View>
  );
}

function ThreeWayPill({
  options,
  value,
  onChange,
  styles,
  groupLabel,
}: {
  /** `a11yLabel` carries the full name when `label` is an abbreviation. */
  options: { value: string; label: string; a11yLabel?: string }[];
  value: string;
  onChange: (v: string) => void;
  styles: any;
  groupLabel: string;
}) {
  return (
    <View style={styles.pillGroup}>
      {options.map((opt) => {
        const active = value === opt.value;
        return (
          <Pressy
            key={opt.value}
            onPress={() => onChange(opt.value)}
            style={[styles.pill, active && styles.pillActive]}
            accessibilityLabel={`${groupLabel}: ${opt.a11yLabel ?? opt.label}`}
            accessibilityRole="radio"
            accessibilityState={{ selected: active, checked: active }}
            hitSlop={{ top: 4, bottom: 4, left: 0, right: 0 }}
          >
            <Text style={[styles.pillText, active && styles.pillTextActive]}>{opt.label}</Text>
          </Pressy>
        );
      })}
    </View>
  );
}

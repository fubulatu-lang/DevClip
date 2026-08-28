import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, StyleSheet, AppState, Alert, ScrollView } from 'react-native';
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
import { useTheme } from '../theme/ThemeContext';
import { useSettingsStore, ThemeMode, BubbleSize } from '../store/settingsStore';
import { useClipStore } from '../store/clipStore';
import Pressy from '../components/Pressy';

const MAX_CLIPS_OPTIONS = [
  { value: 100, label: '100' },
  { value: 500, label: '500' },
  { value: 1000, label: '1000' },
  { value: 0, label: 'No limit' },
];

export default function SettingsScreen({ onBack }: { onBack: () => void }) {
  const { colors, radii, spacing, type } = useTheme();
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

  const styles = StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.bg },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      paddingHorizontal: spacing.md,
      paddingTop: spacing.md,
      paddingBottom: spacing.sm,
    },
    backBtn: {
      width: 32,
      height: 32,
      borderRadius: radii.pill,
      backgroundColor: colors.surfaceSunken,
      alignItems: 'center',
      justifyContent: 'center',
    },
    headerTitle: { fontFamily: type.extrabold, fontSize: 17, color: colors.ink },
    section: {
      marginHorizontal: spacing.md,
      marginTop: spacing.sm,
      backgroundColor: colors.surface,
      borderRadius: radii.md,
      padding: spacing.md,
      gap: spacing.sm,
    },
    sectionTitle: {
      fontFamily: type.bold,
      fontSize: 10,
      letterSpacing: 1,
      textTransform: 'uppercase',
      color: colors.inkFaint,
      marginBottom: 2,
    },
    note: { fontFamily: type.medium, fontSize: 12, color: colors.inkFaint, lineHeight: 17 },
    row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    rowLeft: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, flexShrink: 1 },
    rowLabel: { fontFamily: type.semibold, fontSize: 13, color: colors.ink },
    statusDot: { width: 8, height: 8, borderRadius: 4 },
    statusText: { fontFamily: type.semibold, fontSize: 11 },
    pillGroup: { flexDirection: 'row', backgroundColor: colors.surfaceSunken, borderRadius: radii.pill, padding: 3, gap: 2 },
    pill: { paddingVertical: 5, paddingHorizontal: 10, borderRadius: radii.pill },
    pillActive: { backgroundColor: colors.accent },
    pillText: { fontFamily: type.semibold, fontSize: 11, color: colors.inkSoft },
    pillTextActive: { color: colors.bg },
    actionBtn: { backgroundColor: colors.surfaceSunken, paddingHorizontal: spacing.md, paddingVertical: 6, borderRadius: radii.pill },
    actionBtnActive: { backgroundColor: colors.accentSoft },
    actionBtnText: { fontFamily: type.semibold, fontSize: 11, color: colors.inkSoft },
    actionBtnTextActive: { color: colors.accent },
    exportBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
      backgroundColor: colors.accentSoft,
      paddingVertical: 9,
      borderRadius: radii.pill,
      justifyContent: 'center',
    },
    exportText: { fontFamily: type.semibold, fontSize: 12.5, color: colors.accent },
    dangerBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: colors.dangerSoft, paddingVertical: 9, borderRadius: radii.pill, justifyContent: 'center' },
    dangerText: { fontFamily: type.semibold, fontSize: 12.5, color: colors.danger },
  });

  const handleClearAll = () => {
    Alert.alert('Clear all clips?', 'This deletes everything in your history. This cannot be undone.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Clear all', style: 'destructive', onPress: clearAll },
    ]);
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      await exportBackup();
    } catch (e) {
      Alert.alert('Export failed', 'Could not create the backup file. Try again.');
    } finally {
      setExporting(false);
    }
  };

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <Pressy onPress={onBack} style={styles.backBtn}>
          <ArrowLeft size={16} strokeWidth={1.75} color={colors.ink} />
        </Pressy>
        <Text style={styles.headerTitle}>Settings</Text>
      </View>

      <ScrollView style={{ flex: 1 }} contentContainerStyle={{ paddingBottom: spacing.xl }}>
        {/* Permissions status */}
        {isNativeOverlayAvailable() && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Permissions</Text>
            <PermissionRow label="Background capture" granted={accessibilityOn} colors={colors} styles={styles} />
            <PermissionRow label="Floating bubble" granted={overlayOn} colors={colors} styles={styles} />
            <PermissionRow label="Notifications" granted={notifOn} colors={colors} styles={styles} />
          </View>
        )}

        {/* Appearance */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Appearance</Text>
          <View style={styles.row}>
            <View style={styles.rowLeft}>
              {themeMode === 'dark' ? (
                <Moon size={16} strokeWidth={1.5} color={colors.accent} />
              ) : (
                <Sun size={16} strokeWidth={1.5} color={colors.accent} />
              )}
              <Text style={styles.rowLabel}>Theme</Text>
            </View>
            <ThreeWayPill
              options={[
                { value: 'light', label: 'Light' },
                { value: 'dark', label: 'Dark' },
                { value: 'system', label: 'Auto' },
              ]}
              value={themeMode}
              onChange={(v) => setThemeMode(v as ThemeMode)}
              styles={styles}
            />
          </View>
        </View>

        {/* Capture behavior */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Capture</Text>

          {!isNativeOverlayAvailable() ? (
            <Text style={styles.note}>
              Background capture and the floating bubble need the custom dev client build — see
              SETUP_GUIDE.md.
            </Text>
          ) : (
            <>
              <SettingRow
                icon={<ShieldCheck size={16} strokeWidth={1.5} color={accessibilityOn ? colors.accent : colors.inkFaint} />}
                label="Background capture"
                active={accessibilityOn}
                buttonLabel={accessibilityOn ? 'Manage' : 'Enable'}
                onPress={requestAccessibilityPermission}
                styles={styles}
              />
              <SettingRow
                icon={<Bell size={16} strokeWidth={1.5} color={notifOn ? colors.accent : colors.inkFaint} />}
                label="Notifications"
                active={notifOn}
                buttonLabel={notifOn ? 'On' : 'Enable'}
                onPress={async () => setNotifOn(await requestNotificationPermission())}
                styles={styles}
              />
              <SettingRow
                icon={<CircleDot size={16} strokeWidth={1.5} color={bubbleRunning ? colors.accent : colors.inkFaint} />}
                label="Floating bubble"
                active={bubbleRunning}
                buttonLabel={bubbleRunning ? 'Stop' : 'Start'}
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

              <View style={styles.row}>
                <View style={styles.rowLeft}>
                  <Layers size={16} strokeWidth={1.5} color={colors.inkFaint} />
                  <Text style={styles.rowLabel}>Bubble size</Text>
                </View>
                <ThreeWayPill
                  options={[
                    { value: 'small', label: 'S' },
                    { value: 'medium', label: 'M' },
                    { value: 'large', label: 'L' },
                  ]}
                  value={bubbleSize}
                  onChange={(v) => setBubbleSize(v as BubbleSize)}
                  styles={styles}
                />
              </View>

              <View style={styles.row}>
                <View style={styles.rowLeft}>
                  <Power size={16} strokeWidth={1.5} color={colors.inkFaint} />
                  <Text style={styles.rowLabel}>Auto-start after reboot</Text>
                </View>
                <Pressy
                  onPress={() => setAutoStartOnBoot(!autoStartOnBoot)}
                  style={[styles.actionBtn, autoStartOnBoot && styles.actionBtnActive]}
                >
                  <Text style={[styles.actionBtnText, autoStartOnBoot && styles.actionBtnTextActive]}>
                    {autoStartOnBoot ? 'On' : 'Off'}
                  </Text>
                </Pressy>
              </View>
            </>
          )}

          <View style={styles.row}>
            <View style={styles.rowLeft}>
              <MessageSquareWarning size={16} strokeWidth={1.5} color={colors.inkFaint} />
              <Text style={styles.rowLabel}>Confirm before paste</Text>
            </View>
            <Pressy
              onPress={() => setConfirmBeforePaste(!confirmBeforePaste)}
              style={[styles.actionBtn, confirmBeforePaste && styles.actionBtnActive]}
            >
              <Text style={[styles.actionBtnText, confirmBeforePaste && styles.actionBtnTextActive]}>
                {confirmBeforePaste ? 'On' : 'Off'}
              </Text>
            </Pressy>
          </View>
        </View>

        {/* Storage */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Storage</Text>
          <View style={styles.row}>
            <View style={styles.rowLeft}>
              <SmartphoneCharging size={16} strokeWidth={1.5} color={colors.inkFaint} />
              <Text style={styles.rowLabel}>Keep at most</Text>
            </View>
            <View style={styles.pillGroup}>
              {MAX_CLIPS_OPTIONS.map((opt) => {
                const active = maxClips === opt.value;
                return (
                  <Pressy key={opt.value} onPress={() => setMaxClips(opt.value)} style={[styles.pill, active && styles.pillActive]}>
                    <Text style={[styles.pillText, active && styles.pillTextActive]}>{opt.label}</Text>
                  </Pressy>
                );
              })}
            </View>
          </View>

          <Pressy onPress={handleExport} style={styles.exportBtn}>
            <Download size={14} strokeWidth={1.75} color={colors.accent} />
            <Text style={styles.exportText}>{exporting ? 'Exporting…' : 'Export backup'}</Text>
          </Pressy>

          <Pressy onPress={handleClearAll} style={styles.dangerBtn}>
            <Trash2 size={14} strokeWidth={1.75} color={colors.danger} />
            <Text style={styles.dangerText}>Clear all clips</Text>
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
        <View style={[styles.statusDot, { backgroundColor: granted ? '#3FB950' : colors.danger }]} />
        <Text style={[styles.statusText, { color: granted ? '#3FB950' : colors.danger }]}>
          {granted ? 'Granted' : 'Off'}
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
}: {
  options: { value: string; label: string }[];
  value: string;
  onChange: (v: string) => void;
  styles: any;
}) {
  return (
    <View style={styles.pillGroup}>
      {options.map((opt) => {
        const active = value === opt.value;
        return (
          <Pressy key={opt.value} onPress={() => onChange(opt.value)} style={[styles.pill, active && styles.pillActive]}>
            <Text style={[styles.pillText, active && styles.pillTextActive]}>{opt.label}</Text>
          </Pressy>
        );
      })}
    </View>
  );
}

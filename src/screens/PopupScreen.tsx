import React, { useMemo, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Settings as SettingsIcon } from 'lucide-react-native';
import ClipListView from './ClipListView';
import EditClipSheet from '../components/EditClipSheet';
import SettingsScreen from './SettingsScreen';
import SetupScreen from './SetupScreen';
import PermissionBanner from '../components/PermissionBanner';
import Pressy from '../components/Pressy';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import { useSettingsStore } from '../store/settingsStore';
import { useClipStore } from '../store/clipStore';
import { useEditStore } from '../store/editStore';
import { usePermissions, allGranted, hasRegressed } from '../hooks/usePermissions';
import { isNativeOverlayAvailable } from '../native/OverlayModule';
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
  const { gutter } = useAdaptiveLayout();
  const [showSettings, setShowSettings] = useState(false);
  const hasOnboarded = useSettingsStore((s) => s.hasOnboarded);
  const permissionSkip = useSettingsStore((s) => s.permissionSkip);
  const skipPermissions = useSettingsStore((s) => s.skipPermissions);
  const clearPermissionSkip = useSettingsStore((s) => s.clearPermissionSkip);
  const { permissions, refresh: refreshPermissions } = usePermissions();
  const updateClip = useClipStore((s) => s.updateClip);
  const deleteClip = useClipStore((s) => s.deleteClip);
  const closeEditor = useEditStore((s) => s.close);

  const styles = useMemo(
    () =>
      StyleSheet.create({
        screen: { flex: 1, backgroundColor: colors.bg },
        hero: {
          // The bar spans the window, but its content lines up with the
          // content below it; a title hard against the edge while the list
          // sits 12.5% in reads as two unrelated layouts.
          paddingHorizontal: gutter,
          paddingVertical: spacing.xs,
          minHeight: 56,
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
          alignItems: 'center',
          justifyContent: 'center',
        },
      }),
    [colors, radii, spacing, text, gutter]
  );

  /**
   * The wall goes up on first run, and again whenever something that was
   * granted has since been taken away.
   *
   * Regression, not difference: granting one more permission changes the
   * picture too, and throwing the user back at setup for making progress
   * would be absurd. Android revokes permissions on its own for apps left
   * unopened for months and announces nothing, which is the case this exists
   * for.
   *
   * In Expo Go there is no native module and none of this can be satisfied,
   * so there is nothing to gate on — the wall would be unskippable-looking
   * and pointless.
   */
  const showSetup =
    isNativeOverlayAvailable() &&
    !allGranted(permissions) &&
    (!hasOnboarded || permissionSkip === null || hasRegressed(permissionSkip, permissions));

  if (showSetup || !hasOnboarded) {
    return (
      <SetupScreen
        permissions={permissions}
        refresh={refreshPermissions}
        onSkip={() => skipPermissions(permissions)}
      />
    );
  }

  // Settings is a whole screen with its own back control and title. Keeping
  // the app bar above it stacked two headers and offered two ways out of one
  // place, so the app bar belongs to the clip list only.
  if (showSettings) {
    return (
      <SafeAreaView style={styles.screen} edges={['top', 'bottom']}>
        <SettingsScreen onBack={() => setShowSettings(false)} />
      </SafeAreaView>
    );
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

      <PermissionBanner
        permissions={permissions}
        onFix={clearPermissionSkip}
      />

      <ClipListView />

      {/*
        Last child, so it draws over the app bar as well as the list. It is an
        ordinary view rather than a Modal precisely so that it shares this
        window — and this window's adjustResize — with everything under it.
      */}
      <EditClipSheet
        onSave={async (id, content, title) => {
          await updateClip(id, content, title);
          closeEditor();
        }}
        onDelete={async (id) => {
          await deleteClip(id);
          closeEditor();
        }}
      />
    </SafeAreaView>
  );
}

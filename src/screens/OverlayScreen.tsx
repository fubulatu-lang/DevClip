import React, { useEffect, useMemo } from 'react';
import { View, Text, StyleSheet, FlatList } from 'react-native';
import { Inbox, X, Expand } from 'lucide-react-native';
import { useClipStore } from '../store/clipStore';
import { useClipSync } from '../hooks/useClipSync';
import { useSettingsStore } from '../store/settingsStore';
import ClipListItem from '../components/ClipListItem';
import Pressy from '../components/Pressy';
import Snackbar from '../components/Snackbar';
import { useTheme } from '../theme/ThemeContext';
import { hidePopup, openFullApp } from '../native/OverlayModule';
import { strings } from '../strings';

/**
 * The floating list, mounted by OverlayService in its own window.
 *
 * One shape, tethered to the bubble. It used to have a second, expanded
 * half-screen shape as well; cutting it removed the trickiest geometry in the
 * service and left this as the only floating surface, which is why it is now
 * sized to be worth opening rather than to be the smaller of two options.
 *
 * Paste only. No search — that lives in the full app, where there is room for
 * a keyboard and a result list. No capture button either: the overlay window
 * is deliberately non-focusable, and since Android 10 an app without focus
 * cannot read the clipboard, so the button here could only ever have saved
 * nothing. Capture happens by tapping the bubble with text selected.
 *
 * Native owns the geometry — it is the only side that knows where the bubble
 * sits and where the system bars are — so this screen simply lays itself out
 * to whatever window it is given.
 */
/** Mini is paste-only, so the row's edit hook goes nowhere. */
const noop = () => {};

export default function OverlayScreen() {
  const { colors, radii, spacing, text, icon } = useTheme();
  const clips = useClipStore((s) => s.clips);
  const initialised = useClipStore((s) => s.initialised);
  const error = useClipStore((s) => s.error);
  const trimToMax = useClipStore((s) => s.trimToMax);
  const dismissError = useClipStore((s) => s.dismissError);
  const maxClips = useSettingsStore((s) => s.maxClips);

  useClipSync();

  useEffect(() => {
    trimToMax(maxClips);
  }, [maxClips, clips.length, trimToMax]);

  const styles = useMemo(
    () =>
      StyleSheet.create({
        shell: {
          flex: 1,
          backgroundColor: colors.bg,
          borderRadius: radii.container,
          overflow: 'hidden',
          borderWidth: 1,
          borderColor: colors.border,
        },
        header: {
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing.sm,
          paddingLeft: spacing.lg,
          paddingRight: spacing.sm,
          paddingVertical: spacing.sm,
          backgroundColor: colors.surface,
          borderBottomWidth: 1,
          borderBottomColor: colors.divider,
        },
        logoDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.accent },
        title: { ...text.body, fontWeight: '500', color: colors.ink, flex: 1 },
        headerBtn: {
          width: 40,
          height: 40,
          borderRadius: radii.pill,
          alignItems: 'center',
          justifyContent: 'center',
        },
        list: {
          flexGrow: 1,
          paddingTop: spacing.sm,
          paddingBottom: spacing.sm,
          paddingHorizontal: spacing.lg,
        },
        empty: {
          alignItems: 'center',
          justifyContent: 'center',
          paddingVertical: spacing.xl,
          gap: spacing.sm,
          paddingHorizontal: spacing.lg,
        },
        emptyText: { ...text.caption, color: colors.inkFaint, textAlign: 'center' },
        errorBanner: {
          marginHorizontal: spacing.lg,
          marginTop: spacing.sm,
          backgroundColor: colors.dangerSoft,
          paddingVertical: spacing.sm,
          paddingHorizontal: spacing.md,
          borderRadius: radii.md,
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing.sm,
        },
        errorText: { flex: 1, ...text.caption, color: colors.danger },
      }),
    [colors, radii, spacing, text]
  );

  return (
    <View style={styles.shell}>
      <View style={styles.header}>
        <View style={styles.logoDot} />
        <Text style={styles.title} numberOfLines={1}>
          {strings.app.name}
        </Text>

        <Pressy
          onPress={openFullApp}
          style={styles.headerBtn}
          accessibilityLabel={strings.overlay.openFullApp}
          hitSlop={4}
        >
          <Expand size={icon.sm} strokeWidth={icon.stroke} color={colors.inkSoft} />
        </Pressy>

        <Pressy
          onPress={hidePopup}
          style={styles.headerBtn}
          accessibilityLabel={strings.overlay.close}
          hitSlop={4}
        >
          <X size={icon.sm} strokeWidth={icon.stroke} color={colors.inkSoft} />
        </Pressy>
      </View>

      {error ? (
        <View style={styles.errorBanner}>
          <Text style={styles.errorText}>{error}</Text>
          <Pressy onPress={dismissError} accessibilityLabel={strings.common.dismissError} hitSlop={16}>
            <Text style={[styles.errorText, { flex: 0, fontWeight: '500' }]}>
              {strings.common.dismiss}
            </Text>
          </Pressy>
        </View>
      ) : null}

      <FlatList
        data={clips}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => <ClipListItem clip={item} variant="mini" onLongPress={noop} />}
        ListEmptyComponent={
          <View style={styles.empty}>
            {initialised && (
              <Inbox size={icon.md} strokeWidth={icon.stroke} color={colors.inkDisabled} />
            )}
            <Text style={styles.emptyText}>
              {initialised ? strings.clips.emptyTitle : strings.clips.loading}
            </Text>
          </View>
        }
      />

      <Snackbar />
    </View>
  );
}

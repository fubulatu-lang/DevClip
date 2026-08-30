import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, FlatList } from 'react-native';
import { ClipboardPaste, Inbox, Maximize2, Minimize2, X, Expand } from 'lucide-react-native';
import { useClipStore } from '../store/clipStore';
import { useSettingsStore } from '../store/settingsStore';
import ClipListItem from '../components/ClipListItem';
import SearchBar from '../components/SearchBar';
import SortMenu from '../components/SortMenu';
import Pressy from '../components/Pressy';
import Snackbar from '../components/Snackbar';
import { readSystemClipboard } from '../utils/clipboardCapture';
import { useTheme } from '../theme/ThemeContext';
import { setOverlayMode, hideOverlay, openFullApp, OverlayMode } from '../native/OverlayModule';
import { strings } from '../strings';

/**
 * The floating overlay, mounted by OverlayService in its own window.
 *
 * Two shapes. `mini` is tethered to the bubble and is paste-only: tap a clip
 * and it pastes, and nothing else is reachable from here. `expanded` is a
 * half-height sheet across the bottom with search, sort and editing.
 *
 * Native owns both geometries — it is the only side that knows where the
 * bubble sits and where the system bars are — so this screen asks for a
 * shape by name and lays itself out to whatever window it is given.
 */
export default function OverlayScreen() {
  const { colors, radii, spacing, text, icon } = useTheme();
  const [mode, setMode] = useState<OverlayMode>('mini');
  const {
    clips, search, sort, loading, error,
    init, setSearch, setSort, addClip, trimToMax, dismissError,
  } = useClipStore();
  const maxClips = useSettingsStore((s) => s.maxClips);

  useEffect(() => { init(); }, []);
  useEffect(() => { trimToMax(maxClips); }, [maxClips, clips.length]);

  const changeMode = (next: OverlayMode) => {
    setMode(next);
    setOverlayMode(next);
  };

  const handleCapture = async () => {
    const text = await readSystemClipboard();
    if (text) await addClip(text);
  };

  const mini = mode === 'mini';

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
        actionBar: {
          paddingHorizontal: spacing.lg,
          paddingTop: spacing.sm,
          paddingBottom: spacing.md,
          borderTopWidth: 1,
          borderTopColor: colors.divider,
        },
        captureBtn: {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: spacing.sm,
          backgroundColor: colors.accent,
          minHeight: 48,
          paddingHorizontal: spacing.lg,
          borderRadius: radii.pill,
        },
        captureText: { ...text.button, color: colors.onAccent },
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
          onPress={() => changeMode(mini ? 'expanded' : 'mini')}
          style={styles.headerBtn}
          accessibilityLabel={mini ? strings.overlay.expand : strings.overlay.collapse}
          hitSlop={4}
        >
          {mini ? (
            <Maximize2 size={icon.sm} strokeWidth={icon.stroke} color={colors.inkSoft} />
          ) : (
            <Minimize2 size={icon.sm} strokeWidth={icon.stroke} color={colors.inkSoft} />
          )}
        </Pressy>

        {!mini && (
          <Pressy
            onPress={openFullApp}
            style={styles.headerBtn}
            accessibilityLabel={strings.overlay.openFullApp}
            hitSlop={4}
          >
            <Expand size={icon.sm} strokeWidth={icon.stroke} color={colors.inkSoft} />
          </Pressy>
        )}

        <Pressy
          onPress={hideOverlay}
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

      {!mini && (
        <>
          <SearchBar value={search} onChange={setSearch} />
          <SortMenu value={sort} onChange={setSort} />
        </>
      )}

      <FlatList
        data={clips}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        renderItem={({ item, index }) => (
          <ClipListItem
            clip={item}
            variant={mini ? 'mini' : 'full'}
            isManualSort={!mini && sort === 'manual'}
            isFirst={index === 0}
            isLast={index === clips.length - 1}
            onLongPress={() => {}}
            onMoveUp={() => {}}
            onMoveDown={() => {}}
          />
        )}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Inbox size={icon.md} strokeWidth={icon.stroke} color={colors.inkDisabled} />
            <Text style={styles.emptyText}>
              {search ? strings.clips.noMatches(search) : strings.clips.emptyTitle}
            </Text>
          </View>
        }
      />

      <View style={styles.actionBar}>
        <Pressy
          onPress={handleCapture}
          style={styles.captureBtn}
          accessibilityLabel={strings.clips.captureA11y}
        >
          <ClipboardPaste size={icon.sm} strokeWidth={icon.stroke} color={colors.onAccent} />
          <Text style={styles.captureText}>
            {loading ? strings.clips.capturing : strings.clips.capture}
          </Text>
        </Pressy>
      </View>

      <Snackbar />
    </View>
  );
}

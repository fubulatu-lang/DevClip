import React, { useMemo } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { MoreVertical } from 'lucide-react-native';
import { Clip } from '../types/clip';
import { pasteClip } from '../utils/clipboardCapture';
import { useTheme } from '../theme/ThemeContext';
import { useSettingsStore } from '../store/settingsStore';
import { useSnackbarStore } from '../store/snackbarStore';
import { usePasteArmStore } from '../store/pasteArmStore';
import Pressy from './Pressy';
import { strings } from '../strings';

interface Props {
  clip: Clip;
  /**
   * Position in the list, newest first. A label, not an identity: it
   * renumbers as clips arrive and are deleted, which is intended — "the third
   * one down" is what the user is looking at, and it is stable for exactly as
   * long as the list they are looking at is.
   */
  position: number;
  onLongPress: (clip: Clip) => void;
  /**
   * `mini` is the tethered bubble window: paste only, at the smaller type
   * scale. It drops both routes to edit — the more button and the long press.
   * Dropping them together is deliberate: leaving the long press behind would
   * make a gesture the only way to reach editing, which is the accessibility
   * failure the more button was added to fix.
   */
  variant?: 'full' | 'mini';
}

function ClipListItem({ clip, position, onLongPress, variant = 'full' }: Props) {
  const { colors, radii, spacing, shadow, text, miniText, icon } = useTheme();
  const confirmBeforePaste = useSettingsStore((s) => s.confirmBeforePaste);
  const showSnackbar = useSnackbarStore((s) => s.show);
  const armed = usePasteArmStore((s) => s.armedId === clip.id);
  const arm = usePasteArmStore((s) => s.arm);
  const disarm = usePasteArmStore((s) => s.disarm);

  const mini = variant === 'mini';
  const type = mini ? miniText : text;

  const styles = useMemo(
    () =>
      StyleSheet.create({
        card: {
          backgroundColor: colors.surface,
          borderRadius: radii.md,
          marginBottom: spacing.sm,
          flex: 1,
          padding: mini ? spacing.md : spacing.lg,
          ...shadow.card,
        },
        // A ring rather than a colour swap: the row has to stay readable
        // while it says the next tap will paste it.
        cardArmed: {
          borderWidth: 2,
          borderColor: colors.accent,
          padding: (mini ? spacing.md : spacing.lg) - 2,
        },
        row: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.md },
        numberWrap: {
          minWidth: mini ? 24 : 32,
          height: mini ? 24 : 32,
          paddingHorizontal: 4,
          borderRadius: radii.sm,
          backgroundColor: colors.surfaceSunken,
          alignItems: 'center',
          justifyContent: 'center',
          marginTop: 2,
        },
        number: { ...type.caption, fontWeight: '500', color: colors.inkSoft },
        title: { ...type.body, fontWeight: '500', color: colors.ink, marginBottom: 2 },
        content: { ...type.secondary, color: colors.inkSoft },
        meta: { ...type.caption, color: colors.inkFaint, marginTop: spacing.sm },
        metaArmed: { ...type.caption, color: colors.accent, fontWeight: '500', marginTop: spacing.sm },
        moreBtn: {
          width: 32,
          height: 32,
          borderRadius: radii.pill,
          alignItems: 'center',
          justifyContent: 'center',
        },
      }),
    [colors, radii, spacing, shadow, type, mini]
  );

  const doPaste = async () => {
    const result = await pasteClip(clip.content);
    if (result === 'copiedOnly') {
      // News, not a decision: a dialog here interrupts the paste it just
      // half-completed. Say what happened and let the user carry on.
      showSnackbar(strings.paste.copiedBody);
    }
  };

  const handleTap = () => {
    if (!confirmBeforePaste) {
      doPaste();
      return;
    }
    if (armed) {
      disarm();
      doPaste();
      return;
    }
    arm(clip.id);
  };

  return (
    <Pressy
      onPress={handleTap}
      onLongPress={mini ? undefined : () => onLongPress(clip)}
      style={[styles.card, armed && styles.cardArmed]}
      accessibilityLabel={`${position}. ${clip.title ? clip.title + ': ' : ''}${clip.content}`}
      accessibilityHint={armed ? strings.clips.pasteArmedHint : strings.clips.pasteHint}
    >
      <View style={styles.row}>
        {/*
          The number replaces the copy glyph rather than sitting beside it.
          Every row had the same icon, which told the user nothing; the
          position tells them where they are in the list.
        */}
        <View style={styles.numberWrap} importantForAccessibility="no">
          <Text style={styles.number}>{position}</Text>
        </View>

        <View style={{ flex: 1 }}>
          {clip.title ? <Text style={styles.title}>{clip.title}</Text> : null}
          {/*
            A preview, never the whole clip. The full text is in the database
            and comes out whole on paste; rendering half a megabyte of it here
            would make the list crawl.
          */}
          <Text style={styles.content} numberOfLines={2}>
            {clip.content}
          </Text>
          <Text style={armed ? styles.metaArmed : styles.meta}>
            {armed ? strings.paste.armed : formatWhen(clip.createdAt)}
          </Text>
        </View>

        {/*
          Long press is a shortcut, not the only route: switch control and
          many motor-impairment setups cannot perform one, so edit and delete
          need a control that can simply be activated.
        */}
        {!mini && (
          <Pressy
            onPress={() => onLongPress(clip)}
            style={styles.moreBtn}
            accessibilityLabel={strings.clips.moreOptions(clip.title || strings.clips.fallbackTitle)}
            accessibilityHint={strings.clips.moreOptionsHint}
            hitSlop={8}
          >
            <MoreVertical size={icon.sm} strokeWidth={icon.stroke} color={colors.inkFaint} />
          </Pressy>
        )}
      </View>
    </Pressy>
  );
}

/**
 * A list of up to 1000 rows re-rendered every one of them whenever anything
 * in the store moved. Every prop here is either a primitive or a callback
 * the list keeps stable, so the default shallow comparison is enough: a row
 * re-renders when its own clip or position changes, and not otherwise.
 */
export default React.memo(ClipListItem);

function formatWhen(ts: number): string {
  const diffMs = Date.now() - ts;
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return strings.clips.when.justNow;
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return new Date(ts).toLocaleDateString();
}

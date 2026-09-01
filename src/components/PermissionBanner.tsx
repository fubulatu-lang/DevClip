import React, { useMemo } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { TriangleAlert } from 'lucide-react-native';
import { useTheme, useAdaptiveLayout } from '../theme/ThemeContext';
import { PermissionState } from '../hooks/usePermissions';
import { isNativeOverlayAvailable } from '../native/OverlayModule';
import Pressy from './Pressy';
import { strings } from '../strings';

interface Props {
  permissions: PermissionState;
  /** Puts the setup wall back up. */
  onFix: () => void;
}

/**
 * Says what DevClip cannot currently do, and offers the way to fix it.
 *
 * The setup wall can be walked past, which is the right call — Android makes
 * two of the three permissions a trip into system Settings, and holding
 * someone out of their own app until they have made that trip is not
 * something an app gets to do. The cost of letting them past is that the app
 * is then quietly crippled, and this codebase has been bitten by quiet
 * repeatedly: the empty overlay, the dead Capture button, the dialog that
 * never appeared. So it is said out loud, in the words of what is lost rather
 * than the names of the permissions.
 */
export default function PermissionBanner({ permissions, onFix }: Props) {
  const { colors, radii, spacing, text, icon } = useTheme();
  const { gutter } = useAdaptiveLayout();

  const missing: string[] = [];
  if (!permissions.accessibility) missing.push(strings.setup.missingCapture);
  if (!permissions.overlay) missing.push(strings.setup.missingBubble);
  if (!permissions.notifications) missing.push(strings.setup.missingNotification);

  const styles = useMemo(
    () =>
      StyleSheet.create({
        banner: {
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing.md,
          marginHorizontal: gutter,
          marginTop: spacing.sm,
          paddingVertical: spacing.md,
          paddingHorizontal: spacing.lg,
          borderRadius: radii.md,
          backgroundColor: colors.warningSoft,
        },
        message: { flex: 1, ...text.secondary, color: colors.warning, lineHeight: 22 },
        action: { minHeight: 48, justifyContent: 'center' },
        actionText: { ...text.secondary, fontWeight: '500', color: colors.warning },
      }),
    [colors, radii, spacing, text, gutter]
  );

  if (!isNativeOverlayAvailable() || missing.length === 0) return null;

  const list =
    missing.length === 1
      ? missing[0]
      : `${missing.slice(0, -1).join(', ')} or ${missing[missing.length - 1]}`;

  return (
    <View style={styles.banner} accessibilityLiveRegion="polite">
      <TriangleAlert size={icon.md} strokeWidth={icon.stroke} color={colors.warning} />
      <Text style={styles.message}>{strings.setup.crippled(list)}</Text>
      <Pressy
        onPress={onFix}
        style={styles.action}
        accessibilityLabel={strings.setup.crippledAction}
        hitSlop={8}
      >
        <Text style={styles.actionText}>{strings.setup.crippledAction}</Text>
      </Pressy>
    </View>
  );
}

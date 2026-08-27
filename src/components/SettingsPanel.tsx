import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, AppState } from 'react-native';
import {
  isNativeOverlayAvailable,
  requestOverlayPermission,
  requestAccessibilityPermission,
  isAccessibilityServiceEnabled,
  startBubble,
  stopBubble,
} from '../native/OverlayModule';

export default function SettingsPanel() {
  const [accessibilityOn, setAccessibilityOn] = useState(false);
  const [bubbleRunning, setBubbleRunning] = useState(false);

  const refreshStatus = useCallback(async () => {
    setAccessibilityOn(await isAccessibilityServiceEnabled());
  }, []);

  useEffect(() => {
    refreshStatus();
    // Re-check whenever the user comes back from the Settings app, since
    // permission screens are separate Activities we can't get a callback from.
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') refreshStatus();
    });
    return () => sub.remove();
  }, [refreshStatus]);

  if (!isNativeOverlayAvailable()) {
    return (
      <View style={styles.box}>
        <Text style={styles.note}>
          Background capture & the floating bubble need the custom dev client build (Phase 2).
          You're currently running in Expo Go / a build without it — see SETUP_GUIDE.md.
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.box}>
      <Row
        label={accessibilityOn ? 'Background capture: ON' : 'Background capture: OFF'}
        buttonLabel={accessibilityOn ? 'Manage' : 'Enable'}
        onPress={requestAccessibilityPermission}
      />
      <Row
        label="Floating bubble"
        buttonLabel={bubbleRunning ? 'Stop' : 'Start'}
        onPress={async () => {
          if (bubbleRunning) {
            stopBubble();
            setBubbleRunning(false);
          } else {
            const granted = await requestOverlayPermission();
            if (granted) {
              startBubble();
              setBubbleRunning(true);
            }
            // If not granted yet, the permission screen just opened — the
            // user needs to flip the switch, then tap Start again.
          }
        }}
      />
    </View>
  );
}

function Row({
  label,
  buttonLabel,
  onPress,
}: {
  label: string;
  buttonLabel: string;
  onPress: () => void;
}) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <TouchableOpacity style={styles.rowBtn} onPress={onPress}>
        <Text style={styles.rowBtnText}>{buttonLabel}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  box: { paddingHorizontal: 12, paddingVertical: 8, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#ddd' },
  note: { fontSize: 11, color: '#888' },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginVertical: 2 },
  rowLabel: { fontSize: 12, color: '#333' },
  rowBtn: { backgroundColor: '#4a6cf7', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 6 },
  rowBtnText: { color: '#fff', fontSize: 11, fontWeight: '600' },
});

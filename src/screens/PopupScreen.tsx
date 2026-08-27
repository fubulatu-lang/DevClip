import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native';
import ClipListView from './ClipListView';
import SettingsPanel from '../components/SettingsPanel';
import { PopupState } from '../types/clip';
import { resizePopupWindow, isNativeOverlayAvailable } from '../native/OverlayModule';

// Sizes are approximate dp values for the native floating window (Phase 2).
// In Expo Go / a plain foreground app these are visual-only (the app itself
// just renders differently); the native resize call is a no-op until the
// custom dev client (Phase 2) is built.
const SIZES: Record<PopupState, { width: number; height: number }> = {
  small: { width: 300, height: 400 },
  expanded: { width: 360, height: 640 },
  full: { width: -1, height: -1 }, // -1 = fill screen, handled natively
};

export default function PopupScreen() {
  const [state, setState] = useState<PopupState>('small');

  const changeState = (next: PopupState) => {
    setState(next);
    if (isNativeOverlayAvailable()) {
      const { width, height } = SIZES[next];
      resizePopupWindow(width, height);
    }
  };

  return (
    <SafeAreaView style={[styles.container, state !== 'full' && styles.floating]}>
      <View style={styles.header}>
        <Text style={styles.title}>DevClip</Text>
        <View style={styles.toggleRow}>
          <ToggleButton label="Small" active={state === 'small'} onPress={() => changeState('small')} />
          <ToggleButton
            label="Expanded"
            active={state === 'expanded'}
            onPress={() => changeState('expanded')}
          />
          <ToggleButton label="Full App" active={state === 'full'} onPress={() => changeState('full')} />
        </View>
      </View>
      {state === 'full' && <SettingsPanel />}
      <ClipListView />
    </SafeAreaView>
  );
}

function ToggleButton({
  label,
  active,
  onPress,
}: {
  label: string;
  active: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity style={[styles.toggleBtn, active && styles.toggleBtnActive]} onPress={onPress}>
      <Text style={[styles.toggleText, active && styles.toggleTextActive]}>{label}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  floating: {
    borderRadius: 16,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#ccc',
    overflow: 'hidden',
  },
  header: {
    paddingHorizontal: 12,
    paddingTop: 10,
    paddingBottom: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#ddd',
  },
  title: { fontSize: 16, fontWeight: '700', marginBottom: 6 },
  toggleRow: { flexDirection: 'row', gap: 6 },
  toggleBtn: { paddingVertical: 4, paddingHorizontal: 10, borderRadius: 12, backgroundColor: '#eee' },
  toggleBtnActive: { backgroundColor: '#4a6cf7' },
  toggleText: { fontSize: 11, color: '#333' },
  toggleTextActive: { color: '#fff', fontWeight: '600' },
});

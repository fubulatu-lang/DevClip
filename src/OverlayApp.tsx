import React from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import OverlayScreen from './screens/OverlayScreen';
import { ThemeProvider } from './theme/ThemeContext';

/**
 * Root for the floating overlay window.
 *
 * Deliberately not the same tree as the launcher app: the overlay has no
 * status bar to style, no onboarding to run, and no navigation of its own.
 * It shares the theme and the stores, and nothing else.
 */
export default function OverlayApp() {
  return (
    <SafeAreaProvider>
      <ThemeProvider>
        <OverlayScreen />
      </ThemeProvider>
    </SafeAreaProvider>
  );
}

import React from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import OverlayScreen from './screens/OverlayScreen';
import ErrorBoundary from './components/ErrorBoundary';
import { ThemeProvider } from './theme/ThemeContext';

/**
 * Root for the floating overlay window.
 *
 * Deliberately not the same tree as the launcher app: the overlay has no
 * status bar to style, no onboarding to run, and no navigation of its own.
 * It shares the theme and the stores, and nothing else.
 *
 * The boundary is outermost so that a throw from the theme provider — or from
 * anything the screen does on mount — still leaves something on screen. There
 * is no console to check from inside a floating window.
 */
export default function OverlayApp() {
  return (
    <ErrorBoundary surface="the floating list">
      <SafeAreaProvider>
        <ThemeProvider>
          <OverlayScreen />
        </ThemeProvider>
      </SafeAreaProvider>
    </ErrorBoundary>
  );
}

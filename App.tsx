import React from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import PopupScreen from './src/screens/PopupScreen';
import { useDevClipFonts } from './src/theme/fonts';
import { ThemeProvider, useTheme } from './src/theme/ThemeContext';

function Root() {
  const { mode } = useTheme();
  return (
    <>
      <StatusBar style={mode === 'dark' ? 'light' : 'dark'} />
      <PopupScreen />
    </>
  );
}

export default function App() {
  const [fontsLoaded] = useDevClipFonts();

  if (!fontsLoaded) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color="#3D4CF0" />
      </View>
    );
  }

  return (
    <SafeAreaProvider>
      <ThemeProvider>
        <Root />
      </ThemeProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#F4F4F6' },
});

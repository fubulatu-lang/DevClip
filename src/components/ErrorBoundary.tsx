import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';

interface Props {
  /** Named in the message, so a report says which surface died. */
  surface: string;
  children: React.ReactNode;
}

interface State {
  error: Error | null;
}

/**
 * Renders the error instead of rendering nothing.
 *
 * React unmounts the whole tree when a render throws and puts nothing in its
 * place. Inside the floating window that is invisible: the window is there,
 * the service thinks it drew, and the user sees an empty coloured rectangle
 * with no clue why. Anything that throws while the overlay mounts — opening
 * SQLite is the likeliest — used to look exactly like a bubble tap that did
 * nothing.
 *
 * Deliberately plain: no theme, no store, no hooks. A boundary that depends
 * on the things that might have thrown cannot report that they threw.
 */
export default class ErrorBoundary extends React.Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error) {
    console.error('DevClip render error', error);
  }

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;

    return (
      <ScrollView style={styles.scroll} contentContainerStyle={styles.wrap}>
        <Text style={styles.heading}>DevClip couldn’t draw {this.props.surface}.</Text>
        <Text style={styles.body}>{error.message || String(error)}</Text>
      </ScrollView>
    );
  }
}

const styles = StyleSheet.create({
  // Hardcoded colours on purpose: this renders when the theme may be what
  // failed, so it cannot read a token. High contrast in both light and dark.
  scroll: { flex: 1, backgroundColor: '#3A2323' },
  wrap: { padding: 16, gap: 8 },
  heading: { fontSize: 15, fontWeight: '500', color: '#FFFFFF' },
  body: { fontSize: 13, color: '#FFD9D6' },
});

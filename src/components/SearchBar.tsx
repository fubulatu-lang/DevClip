import React from 'react';
import { View, TextInput, StyleSheet } from 'react-native';

interface Props {
  value: string;
  onChange: (text: string) => void;
}

export default function SearchBar({ value, onChange }: Props) {
  return (
    <View style={styles.wrap}>
      <TextInput
        style={styles.input}
        placeholder="Search title or content..."
        placeholderTextColor="#888"
        value={value}
        onChangeText={onChange}
        autoCorrect={false}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { paddingHorizontal: 10, paddingTop: 8, paddingBottom: 4 },
  input: {
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 14,
  },
});

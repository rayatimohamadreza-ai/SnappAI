import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

// This file is the entry point of your app. When you give the AI instructions in the chat,
// it edits this file (and new files) to build your app.
export default function App() {
  const [count, setCount] = useState(0);
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Hello, Palmagine!</Text>
      <Text style={styles.subtitle}>
        Tell the AI in the chat tab what app you want to build
      </Text>
      <Pressable style={styles.button} onPress={() => setCount((c) => c + 1)}>
        <Text style={styles.buttonText}>Taps: {count}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    backgroundColor: '#f6f7fb',
    padding: 24,
  },
  title: { fontSize: 24, fontWeight: 'bold', color: '#1f2430' },
  subtitle: { fontSize: 14, color: '#6b7280', textAlign: 'center' },
  button: {
    marginTop: 12,
    backgroundColor: '#4f46e5',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 999,
  },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 16 },
});

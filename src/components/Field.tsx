import React from 'react';
import { KeyboardTypeOptions, StyleSheet, Text, TextInput, View } from 'react-native';
import { colors } from '../theme/colors';
import { radius } from '../theme/type';

interface Props {
  label?: string;
  value: string;
  onChangeText: (v: string) => void;
  placeholder?: string;
  prefix?: string;
  keyboardType?: KeyboardTypeOptions;
  secureTextEntry?: boolean;
}

/** Ports components.jsx `Field`. */
export function DhanField({ label, value, onChangeText, placeholder, prefix, keyboardType, secureTextEntry }: Props) {
  return (
    <View style={styles.wrap}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <View style={styles.inputRow}>
        {prefix ? <Text style={styles.prefix}>{prefix}</Text> : null}
        <TextInput
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor={colors.fg3}
          keyboardType={keyboardType}
          secureTextEntry={secureTextEntry}
          style={styles.input}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { marginBottom: 14 },
  label: { fontSize: 12, fontWeight: '600', color: colors.fg2, marginBottom: 6 },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    height: 52,
    borderWidth: 1,
    borderColor: colors.borderDefault,
    borderRadius: radius.input,
    paddingHorizontal: 12,
    backgroundColor: '#fff',
  },
  prefix: { color: colors.fg2, fontWeight: '600' },
  input: { flex: 1, fontSize: 15, color: colors.fg1 },
});

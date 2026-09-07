import React, { PropsWithChildren } from 'react';
import { Pressable, StyleSheet, View, ViewStyle } from 'react-native';
import { colors } from '../theme/colors';
import { radius } from '../theme/type';

interface Props {
  onPress?: () => void;
  style?: ViewStyle;
  padding?: number;
}

/** Ports components.jsx `Card`. */
export function DhanCard({ children, onPress, style, padding = 16 }: PropsWithChildren<Props>) {
  const content = <View style={[styles.card, { padding }, style]}>{children}</View>;
  if (!onPress) return content;
  return <Pressable onPress={onPress}>{content}</Pressable>;
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.bgElevated,
    borderRadius: radius.card,
    shadowColor: colors.navy,
    shadowOpacity: 0.06,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
});

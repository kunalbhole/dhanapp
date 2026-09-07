import React from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, ViewStyle } from 'react-native';
import { colors } from '../theme/colors';
import { radius } from '../theme/type';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'destructive';

const VARIANTS: Record<ButtonVariant, { bg: string; fg: string; border?: string }> = {
  primary: { bg: colors.navy, fg: '#fff' },
  secondary: { bg: colors.bgSurface, fg: colors.navy },
  ghost: { bg: 'transparent', fg: colors.navy },
  outline: { bg: '#fff', fg: colors.navy, border: colors.borderDefault },
  destructive: { bg: colors.expenseBg, fg: colors.expense },
};

interface Props {
  text: string;
  onPress: () => void;
  variant?: ButtonVariant;
  full?: boolean;
  disabled?: boolean;
  loading?: boolean;
  size?: 'sm' | 'md' | 'lg';
  style?: ViewStyle;
}

const HEIGHTS = { sm: 36, md: 48, lg: 56 };
const FONT_SIZES = { sm: 13, md: 15, lg: 16 };

/** Ports components.jsx `Button`. Primary CTAs are always navy per the source project's
 *  CTA-color audit — gold is reserved for decorative accents only. */
export function DhanButton({ text, onPress, variant = 'primary', full, disabled, loading, size = 'md', style }: Props) {
  const v = VARIANTS[variant];
  return (
    <Pressable
      onPress={disabled || loading ? undefined : onPress}
      style={[
        styles.base,
        {
          backgroundColor: v.bg,
          borderColor: v.border,
          borderWidth: v.border ? 1 : 0,
          height: HEIGHTS[size],
          opacity: disabled ? 0.5 : 1,
          alignSelf: full ? 'stretch' : 'flex-start',
        },
        style,
      ]}>
      {loading ? <ActivityIndicator color={v.fg} /> : <Text style={[styles.text, { color: v.fg, fontSize: FONT_SIZES[size] }]}>{text}</Text>}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    borderRadius: radius.control,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  text: { fontWeight: '600' },
});

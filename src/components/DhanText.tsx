import React from 'react';
import { StyleSheet, Text, TextProps, TextStyle } from 'react-native';

const WEIGHT_TO_FAMILY: Record<string, string> = {
  '400': 'Poppins-Regular',
  normal: 'Poppins-Regular',
  '500': 'Poppins-Medium',
  '600': 'Poppins-SemiBold',
  '700': 'Poppins-Bold',
  bold: 'Poppins-Bold',
};

/**
 * Drop-in replacement for RN's Text — maps this app's existing fontWeight values (400/500/
 * 600/700, set via src/theme/type.ts or a component's own StyleSheet) onto the four bundled
 * Poppins weights, and drops fontWeight itself. Mixing a numeric fontWeight with a static
 * per-weight font file makes Android synthesize a second, overly-heavy bold on top of a
 * file that's already bold — dropping it avoids that. Every screen/component already sets
 * fontWeight consistently, so switching call sites to import this instead of RN's Text
 * (`import { DhanText as Text } from '.../components/DhanText'`) needed no per-style edits.
 */
export function DhanText({ style, ...rest }: TextProps) {
  const flat = StyleSheet.flatten(style) as TextStyle | undefined;
  const weightKey = String(flat?.fontWeight ?? '400');
  const fontFamily = WEIGHT_TO_FAMILY[weightKey] ?? 'Poppins-Regular';
  return <Text {...rest} style={[style, { fontFamily, fontWeight: undefined }]} />;
}

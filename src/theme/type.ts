import { TextStyle } from 'react-native';

// No custom font bundling in this rewrite (Poppins would need native font-asset linking,
// which is exactly the kind of extra native-build risk this rewrite is trying to avoid).
// System font stands in; weights still follow the source design's 400/600/700 scale.
export const type = {
  display: { fontSize: 32, lineHeight: 40, fontWeight: '700', letterSpacing: -0.4 } as TextStyle,
  h1: { fontSize: 24, lineHeight: 32, fontWeight: '700' } as TextStyle,
  h2: { fontSize: 20, lineHeight: 28, fontWeight: '600' } as TextStyle,
  h3: { fontSize: 18, lineHeight: 26, fontWeight: '600' } as TextStyle,
  body: { fontSize: 15, lineHeight: 22, fontWeight: '400' } as TextStyle,
  bodySm: { fontSize: 13, lineHeight: 20, fontWeight: '400' } as TextStyle,
  caption: { fontSize: 12, lineHeight: 16, fontWeight: '400' } as TextStyle,
  label: { fontSize: 11, lineHeight: 14, fontWeight: '700', letterSpacing: 0.6 } as TextStyle,
};

export const radius = {
  input: 8,
  control: 12,
  cardSm: 14,
  card: 16,
  cardLg: 20,
  sheet: 24,
  pill: 999,
};

export const space = { s1: 4, s2: 8, s3: 12, s4: 16, s5: 20, s6: 24, s7: 32, s8: 40 };

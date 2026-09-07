import { TextStyle } from 'react-native';

// Poppins is bundled and linked natively (src/assets/fonts/, see react-native.config.js);
// DhanText (src/components/DhanText.tsx) maps these fontWeight values onto the four
// Poppins weight files, so this table only needs to keep choosing the right weight per
// role. h1 is the actual "page heading" role used across every screen's title (Budget,
// Transactions, Bills, More, Welcome) — sized/weighted to the design system's locked
// spec (20px Poppins Medium), not the 24px Bold it was carrying before. h2 is a separate,
// unrelated role (Home's greeting, the merchant name on transaction detail) and keeps its
// own weight.
export const type = {
  display: { fontSize: 32, lineHeight: 40, fontWeight: '700', letterSpacing: -0.4 } as TextStyle,
  h1: { fontSize: 20, lineHeight: 28, fontWeight: '500' } as TextStyle,
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

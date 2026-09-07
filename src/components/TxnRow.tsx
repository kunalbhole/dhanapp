import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, categoryFromKey } from '../theme/colors';
import { formatINR } from '../utils/format';
import { CategoryIcon } from './CategoryIcon';

interface Props {
  merchant: string;
  meta: string;
  amount: number;
  category: string;
  last?: boolean;
  onPress?: () => void;
}

/** Ports components.jsx `TxnRow`. amount > 0 renders as incoming (green, "+"). */
export function TxnRow({ merchant, meta, amount, category, last, onPress }: Props) {
  const isIn = amount > 0;
  return (
    <Pressable onPress={onPress} style={[styles.row, !last && styles.divider]}>
      <CategoryIcon category={categoryFromKey(category)} />
      <View style={styles.middle}>
        <Text style={styles.merchant} numberOfLines={1}>{merchant}</Text>
        <Text style={styles.meta} numberOfLines={1}>{meta}</Text>
      </View>
      <Text style={[styles.amount, { color: isIn ? colors.income : colors.fg1 }]}>
        {(isIn ? '+' : '−') + formatINR(Math.abs(amount))}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  divider: { borderBottomWidth: 1, borderBottomColor: colors.borderSubtle },
  middle: { flex: 1, minWidth: 0 },
  merchant: { fontSize: 14, fontWeight: '600', color: colors.fg1 },
  meta: { fontSize: 12, color: colors.fg3, marginTop: 2 },
  amount: { fontSize: 15, fontWeight: '700' },
});

import React, { PropsWithChildren } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { colors } from '../theme/colors';
import { radius } from '../theme/type';

export type StatusTone = 'info' | 'income' | 'expense' | 'warning' | 'neutral';

const TONES: Record<StatusTone, { bg: string; fg: string }> = {
  info: { bg: colors.infoBg, fg: colors.info },
  income: { bg: colors.incomeBg, fg: colors.income },
  expense: { bg: colors.expenseBg, fg: colors.expense },
  warning: { bg: colors.warningBg, fg: colors.warning },
  neutral: { bg: colors.bgSurface, fg: colors.fg2 },
};

export function StatusPill({ children, tone = 'info' }: PropsWithChildren<{ tone?: StatusTone }>) {
  const t = TONES[tone];
  return (
    <Text style={[styles.pill, { backgroundColor: t.bg, color: t.fg }]}>{children}</Text>
  );
}

export function DhanChip({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={[styles.chip, { backgroundColor: active ? colors.navy : colors.bgSurface }]}>
      <Text style={{ color: active ? '#fff' : colors.fg2, fontWeight: '600', fontSize: 13 }}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pill: {
    fontSize: 10.5,
    fontWeight: '700',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: radius.pill,
    overflow: 'hidden',
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: radius.pill,
  },
});

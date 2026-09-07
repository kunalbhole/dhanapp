import React, { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { colors, CATEGORIES, CategoryKey, categoryFromKey } from '../theme/colors';
import { type } from '../theme/type';
import { db, Budget, Transaction } from '../native/DhanDb';
import { formatINR, monthKey, monthRange } from '../utils/format';
import { DhanCard } from '../components/Card';
import { StatusPill } from '../components/Chips';
import { DhanChip } from '../components/Chips';
import { CategoryIcon } from '../components/CategoryIcon';
import { DhanButton } from '../components/Button';

const EDITABLE_CATEGORIES = (Object.keys(CATEGORIES) as CategoryKey[]).filter((c) => c !== 'income');

/** Reserved "category" key for the overall monthly cap, stored in the same budgets table
 *  as per-category caps so no native/schema change is needed. Never rendered as a category. */
const TOTAL_BUDGET_KEY = '__total__';

export function BudgetScreen() {
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [txns, setTxns] = useState<Transaction[]>([]);
  const [pickCategory, setPickCategory] = useState<CategoryKey>('food');
  const [amountText, setAmountText] = useState('');
  const [totalText, setTotalText] = useState('');

  const load = useCallback(() => {
    Promise.all([db.getBudgets(monthKey()), db.getTransactions()]).then(([b, t]) => {
      setBudgets(b);
      setTxns(t);
      const totalRow = b.find((x) => x.category === TOTAL_BUDGET_KEY);
      setTotalText(totalRow ? String(totalRow.limitAmount) : '');
    });
  }, []);

  useFocusEffect(load);

  const categoryBudgets = budgets.filter((b) => b.category !== TOTAL_BUDGET_KEY);
  const totalOverride = budgets.find((b) => b.category === TOTAL_BUDGET_KEY)?.limitAmount;
  const categorySum = categoryBudgets.reduce((s, b) => s + b.limitAmount, 0);
  const totalCap = totalOverride ?? categorySum;

  const [monthStart, monthEnd] = monthRange();
  const spendByCategory = new Map<string, number>();
  for (const t of txns) {
    if (t.amount >= 0 || t.timestampMillis < monthStart || t.timestampMillis >= monthEnd) continue;
    spendByCategory.set(t.category, (spendByCategory.get(t.category) ?? 0) - t.amount);
  }

  const totalSpent = categoryBudgets.reduce((s, b) => s + (spendByCategory.get(b.category) ?? 0), 0);
  const pct = totalCap > 0 ? Math.round((totalSpent / totalCap) * 100) : 0;

  const saveBudget = async () => {
    const amount = Number(amountText);
    if (!amount || amount <= 0) return;
    await db.setBudget(pickCategory, monthKey(), amount);
    setAmountText('');
    load();
  };

  const saveTotalBudget = async () => {
    const amount = Number(totalText);
    if (!amount || amount <= 0) return;
    if (categorySum > amount) {
      Alert.alert(
        'Allocations exceed this total',
        `Your category caps add up to ${formatINR(categorySum)}, more than the ${formatINR(amount)} total you're setting. Category caps won't change automatically — you may want to adjust them.`,
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Set total anyway', onPress: () => commitTotalBudget(amount) },
        ],
      );
      return;
    }
    await commitTotalBudget(amount);
  };

  const commitTotalBudget = async (amount: number) => {
    await db.setBudget(TOTAL_BUDGET_KEY, monthKey(), amount);
    load();
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Budget</Text>

      <DhanCard style={{ marginBottom: 14 }}>
        {categoryBudgets.length === 0 && !totalOverride ? (
          <Text style={styles.empty}>No budget set for this month yet. Add your first category cap below.</Text>
        ) : (
          <>
            <Text style={styles.spentAmount}>{formatINR(totalSpent)} <Text style={styles.of}>of {formatINR(totalCap)}</Text></Text>
            <View style={styles.barTrack}>
              <View style={[styles.barFill, { width: `${Math.min(100, pct)}%`, backgroundColor: pct > 100 ? colors.expense : colors.navy }]} />
            </View>
          </>
        )}
      </DhanCard>

      <Text style={styles.sectionLabel}>TOTAL BUDGET</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <Text style={styles.totalHint}>
          Set an overall monthly cap directly. This won't change your category allocations below.
        </Text>
        <View style={styles.amountRow}>
          <Text style={styles.rupee}>₹</Text>
          <TextInput
            value={totalText}
            onChangeText={setTotalText}
            placeholder="Overall monthly budget"
            placeholderTextColor={colors.fg3}
            keyboardType="numeric"
            style={styles.amountInput}
          />
        </View>
        <DhanButton text="Save total budget" onPress={saveTotalBudget} full />
      </DhanCard>

      <Text style={styles.sectionLabel}>ADD / UPDATE A CAP</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <View style={styles.chipWrap}>
          {EDITABLE_CATEGORIES.map((c) => (
            <DhanChip key={c} label={CATEGORIES[c].name} active={pickCategory === c} onPress={() => setPickCategory(c)} />
          ))}
        </View>
        <View style={styles.amountRow}>
          <Text style={styles.rupee}>₹</Text>
          <TextInput
            value={amountText}
            onChangeText={setAmountText}
            placeholder="Monthly cap"
            placeholderTextColor={colors.fg3}
            keyboardType="numeric"
            style={styles.amountInput}
          />
        </View>
        <DhanButton text="Save cap" onPress={saveBudget} full />
      </DhanCard>

      {categoryBudgets.map((b) => {
        const cat = categoryFromKey(b.category);
        const meta = CATEGORIES[cat];
        const spent = spendByCategory.get(b.category) ?? 0;
        const pctCat = b.limitAmount > 0 ? Math.round((spent / b.limitAmount) * 100) : 0;
        const over = spent > b.limitAmount;
        return (
          <DhanCard key={b.id} style={{ marginBottom: 10 }}>
            <View style={styles.catRow}>
              <View style={styles.catLeft}>
                <CategoryIcon category={cat} size={32} tint />
                <View>
                  <Text style={styles.catName}>{meta.name}</Text>
                  <Text style={styles.catSub}>{formatINR(spent)} of {formatINR(b.limitAmount)}</Text>
                </View>
              </View>
              <StatusPill tone={over ? 'expense' : pctCat >= 80 ? 'warning' : 'income'}>
                {over ? 'Overspent' : pctCat >= 80 ? 'Warning' : 'On track'}
              </StatusPill>
            </View>
            <View style={[styles.barTrack, { height: 6, marginTop: 10 }]}>
              <View style={[styles.barFill, { width: `${Math.min(100, pctCat)}%`, backgroundColor: over ? colors.expense : meta.color }]} />
            </View>
          </DhanCard>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 16, paddingBottom: 40 },
  title: { ...type.h1, color: colors.fg1, marginBottom: 14 },
  empty: { color: colors.fg3, fontSize: 13, lineHeight: 19 },
  spentAmount: { fontSize: 24, fontWeight: '700', color: colors.fg1 },
  of: { fontSize: 14, fontWeight: '400', color: colors.fg3 },
  barTrack: { height: 8, backgroundColor: colors.bgSurface, borderRadius: 999, overflow: 'hidden', marginTop: 12 },
  barFill: { height: '100%', borderRadius: 999 },
  catRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  catLeft: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  catName: { fontSize: 14, fontWeight: '700', color: colors.fg1 },
  catSub: { fontSize: 11, color: colors.fg3, marginTop: 1 },
  sectionLabel: { ...type.label, color: colors.fg3, marginBottom: 8, marginLeft: 4 },
  totalHint: { fontSize: 12, color: colors.fg3, marginBottom: 12, lineHeight: 17 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 14 },
  amountRow: {
    flexDirection: 'row', alignItems: 'center', gap: 8, height: 48, borderWidth: 1,
    borderColor: colors.borderDefault, borderRadius: 8, paddingHorizontal: 12, marginBottom: 14, backgroundColor: '#fff',
  },
  rupee: { color: colors.fg2, fontWeight: '600' },
  amountInput: { flex: 1, fontSize: 15, color: colors.fg1 },
});

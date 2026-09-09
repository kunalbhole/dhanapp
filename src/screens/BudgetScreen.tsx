import React, { useCallback, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { DhanText as Text } from '../components/DhanText';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { DotsThreeVerticalIcon } from 'phosphor-react-native';
import { colors, CATEGORIES, categoryFromKey } from '../theme/colors';
import { type } from '../theme/type';
import { db, Budget, BudgetDef } from '../native/DhanDb';
import { Transaction } from '../native/DhanDb';
import { formatINR, monthKey, monthRange } from '../utils/format';
import { DhanCard } from '../components/Card';
import { StatusPill } from '../components/Chips';
import { CategoryIcon } from '../components/CategoryIcon';
import { DhanButton } from '../components/Button';
import { resolveFramework, FrameworkBucket, TOTAL_BUDGET_CATEGORY_KEY } from '../data/frameworks';
import { RootStackParamList } from '../navigation/RootNavigator';

type Nav = NativeStackNavigationProp<RootStackParamList>;

/**
 * "Your budgets" — Personal plus any Project budgets, as expandable accordion cards
 * (decided over tabs or a dropdown). Each card shows a read-only summary + framework
 * breakdown; all editing (total, framework, per-category allocations) lives in
 * EditBudgetScreen, reached via the card's 3-dot menu — never inline here.
 */
export function BudgetScreen() {
  const navigation = useNavigation<Nav>();
  const [budgetDefs, setBudgetDefs] = useState<BudgetDef[]>([]);
  const [budgetsByDefId, setBudgetsByDefId] = useState<Map<number, Budget[]>>(new Map());
  const [txns, setTxns] = useState<Transaction[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const load = useCallback(() => {
    Promise.all([db.getBudgetDefs(), db.getTransactions()]).then(async ([defs, t]) => {
      setBudgetDefs(defs);
      setTxns(t);
      const key = monthKey();
      const entries = await Promise.all(defs.map(async (d): Promise<[number, Budget[]]> => [d.id, await db.getBudgets(d.id, key)]));
      setBudgetsByDefId(new Map(entries));
    });
  }, []);

  useFocusEffect(load);

  const [monthStart, monthEnd] = monthRange();
  const spendByCategory = new Map<string, number>();
  for (const t of txns) {
    if (t.amount >= 0 || t.timestampMillis < monthStart || t.timestampMillis >= monthEnd) continue;
    spendByCategory.set(t.category, (spendByCategory.get(t.category) ?? 0) - t.amount);
  }

  const openMenu = (def: BudgetDef) => {
    const buttons: { text: string; onPress?: () => void; style?: 'destructive' | 'cancel' }[] = [
      { text: 'Edit', onPress: () => navigation.navigate('EditBudget', { budgetDefId: def.id }) },
    ];
    if (def.type !== 'PERSONAL') {
      buttons.push({
        text: 'Delete',
        style: 'destructive',
        onPress: () =>
          Alert.alert('Delete this budget?', `"${def.name}" and its allocations will be removed. Transactions aren't affected.`, [
            { text: 'Cancel', style: 'cancel' },
            {
              text: 'Delete',
              style: 'destructive',
              onPress: async () => {
                await db.deleteBudgetDef(def.id);
                load();
              },
            },
          ]),
      });
    }
    buttons.push({ text: 'Cancel', style: 'cancel' });
    Alert.alert(def.name, undefined, buttons);
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Budget</Text>
      <Text style={styles.sectionLabel}>YOUR BUDGETS</Text>

      {budgetDefs.map((def) => {
        const framework = resolveFramework(def.frameworkKey, def.customFrameworkJson);
        const allRows = budgetsByDefId.get(def.id) ?? [];
        const categoryRows = allRows.filter((b) => b.category !== TOTAL_BUDGET_CATEGORY_KEY);
        const totalOverride = allRows.find((b) => b.category === TOTAL_BUDGET_CATEGORY_KEY)?.limitAmount;
        const categorySum = categoryRows.reduce((s, b) => s + b.limitAmount, 0);
        const totalCap = totalOverride ?? categorySum;
        const totalSpent = categoryRows.reduce((s, b) => s + (spendByCategory.get(b.category) ?? 0), 0);
        const pct = totalCap > 0 ? Math.round((totalSpent / totalCap) * 100) : 0;
        const expanded = expandedId === def.id;

        return (
          <DhanCard key={def.id} style={styles.budgetCard} onPress={() => setExpandedId(expanded ? null : def.id)}>
            <View style={styles.cardHeader}>
              <View style={{ flex: 1 }}>
                <View style={styles.nameRow}>
                  <Text style={styles.budgetName}>{def.name}</Text>
                  <StatusPill tone="neutral">{framework.name}</StatusPill>
                </View>
                {categoryRows.length === 0 && !totalOverride ? (
                  <Text style={styles.empty}>No allocations yet — tap ⋮ then Edit to set a total budget.</Text>
                ) : (
                  <>
                    <Text style={styles.spentAmount}>
                      {formatINR(totalSpent)} <Text style={styles.of}>of {formatINR(totalCap)}</Text>
                    </Text>
                    <View style={styles.barTrack}>
                      <View style={[styles.barFill, { width: `${Math.min(100, pct)}%`, backgroundColor: pct > 100 ? colors.expense : colors.navy }]} />
                    </View>
                  </>
                )}
              </View>
              <Pressable hitSlop={12} onPress={() => openMenu(def)} style={styles.menuButton}>
                <DotsThreeVerticalIcon size={20} color={colors.fg3} weight="bold" />
              </Pressable>
            </View>

            {expanded && (
              <View style={styles.bucketsWrap}>
                {framework.buckets.map((bucket) => (
                  <BucketBreakdown key={bucket.key} bucket={bucket} totalCap={totalCap} categoryRows={categoryRows} spendByCategory={spendByCategory} />
                ))}
              </View>
            )}
          </DhanCard>
        );
      })}

      <DhanButton text="+ Create new budget" variant="secondary" full onPress={() => navigation.navigate('CreateBudget')} style={{ marginTop: 8 }} />
    </ScrollView>
  );
}

function BucketBreakdown({
  bucket,
  totalCap,
  categoryRows,
  spendByCategory,
}: {
  bucket: FrameworkBucket;
  totalCap: number;
  categoryRows: Budget[];
  spendByCategory: Map<string, number>;
}) {
  const categoryCapByKey = new Map(categoryRows.map((b) => [b.category, b.limitAmount]));
  const hasCategories = bucket.categories.length > 0;
  const bucketAmount = hasCategories
    ? bucket.categories.reduce((s, c) => s + (categoryCapByKey.get(c) ?? 0), 0)
    : (totalCap * bucket.percent) / 100;

  return (
    <View style={styles.bucket}>
      <View style={styles.bucketHeaderRow}>
        <Text style={styles.bucketName}>{bucket.name}</Text>
        <Text style={styles.bucketAmount}>
          {formatINR(bucketAmount)} <Text style={styles.bucketPercent}>({bucket.percent}%)</Text>
        </Text>
      </View>
      {hasCategories &&
        bucket.categories.map((c) => {
          const cat = categoryFromKey(c);
          const meta = CATEGORIES[cat];
          const cap = categoryCapByKey.get(c) ?? 0;
          const spent = spendByCategory.get(c) ?? 0;
          const pctCat = cap > 0 ? Math.round((spent / cap) * 100) : 0;
          const over = cap > 0 && spent > cap;
          return (
            <View key={c} style={styles.catRow}>
              <View style={styles.catRowTop}>
                <View style={styles.catLeft}>
                  <CategoryIcon category={cat} size={28} tint />
                  <View>
                    <Text style={styles.catName}>{meta.name}</Text>
                    <Text style={styles.catSub}>
                      {formatINR(spent)} of {formatINR(cap)}
                    </Text>
                  </View>
                </View>
                <StatusPill tone={over ? 'expense' : pctCat >= 80 ? 'warning' : 'income'}>
                  {over ? 'Overspent' : pctCat >= 80 ? 'Warning' : 'On track'}
                </StatusPill>
              </View>
              {/* Distinct color per category (same palette as everywhere else), not a
                  single flat tone — falls back to the expense color only when over cap. */}
              <View style={[styles.barTrack, { height: 5, marginTop: 6 }]}>
                <View style={[styles.barFill, { width: `${Math.min(100, pctCat)}%`, backgroundColor: over ? colors.expense : meta.color }]} />
              </View>
            </View>
          );
        })}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 16, paddingBottom: 40 },
  title: { ...type.h1, color: colors.fg1, marginBottom: 14 },
  sectionLabel: { ...type.label, color: colors.fg3, marginBottom: 8, marginLeft: 4 },
  empty: { color: colors.fg3, fontSize: 13, lineHeight: 19, marginTop: 4 },
  budgetCard: { marginBottom: 10 },
  cardHeader: { flexDirection: 'row', alignItems: 'flex-start', gap: 8 },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8, flexWrap: 'wrap' },
  budgetName: { fontSize: 16, fontWeight: '700', color: colors.fg1 },
  menuButton: { padding: 4, marginTop: -4, marginRight: -4 },
  spentAmount: { fontSize: 22, fontWeight: '700', color: colors.fg1 },
  of: { fontSize: 13, fontWeight: '400', color: colors.fg3 },
  barTrack: { height: 8, backgroundColor: colors.bgSurface, borderRadius: 999, overflow: 'hidden', marginTop: 10 },
  barFill: { height: '100%', borderRadius: 999 },
  bucketsWrap: { marginTop: 16, borderTopWidth: 1, borderTopColor: colors.borderSubtle, paddingTop: 14, gap: 16 },
  bucket: { gap: 10 },
  bucketHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  bucketName: { fontSize: 13, fontWeight: '700', color: colors.fg2, textTransform: 'uppercase', letterSpacing: 0.4 },
  bucketAmount: { fontSize: 13, fontWeight: '700', color: colors.fg1 },
  bucketPercent: { fontWeight: '400', color: colors.fg3 },
  catRow: { marginTop: 2 },
  catRowTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  catLeft: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  catName: { fontSize: 14, fontWeight: '700', color: colors.fg1 },
  catSub: { fontSize: 11, color: colors.fg3, marginTop: 1 },
});

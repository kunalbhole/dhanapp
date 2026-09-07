import React, { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { DhanText as Text } from '../components/DhanText';
import { useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { colors, CATEGORIES, categoryFromKey } from '../theme/colors';
import { type } from '../theme/type';
import { db, Budget, BudgetDef } from '../native/DhanDb';
import { formatINR, monthKey } from '../utils/format';
import { DhanCard } from '../components/Card';
import { DhanButton } from '../components/Button';
import { DhanChip } from '../components/Chips';
import { CategoryIcon } from '../components/CategoryIcon';
import { computeDefaultAllocations, FRAMEWORKS, Framework, resolveFramework, TOTAL_BUDGET_CATEGORY_KEY } from '../data/frameworks';
import { RootStackParamList } from '../navigation/RootNavigator';


type Props = NativeStackScreenProps<RootStackParamList, 'EditBudget'>;

/**
 * All editing for a budget lives here — total amount, framework, and per-category
 * allocations — reached only from the Budget screen's 3-dot menu. The main Budget page
 * itself is read-only; "Change framework" specifically only exists on this screen.
 */
export function EditBudgetScreen({ route, navigation }: Props) {
  const { budgetDefId } = route.params;
  const [def, setDef] = useState<BudgetDef | null>(null);
  const [categoryRows, setCategoryRows] = useState<Budget[]>([]);
  const [totalText, setTotalText] = useState('');
  const [amountDrafts, setAmountDrafts] = useState<Record<string, string>>({});
  const [showFrameworkPicker, setShowFrameworkPicker] = useState(false);
  const [savingTotal, setSavingTotal] = useState(false);
  const [savingCategories, setSavingCategories] = useState(false);

  const load = useCallback(() => {
    const key = monthKey();
    Promise.all([db.getBudgetDefs(), db.getBudgets(budgetDefId, key)]).then(([defs, rows]) => {
      setDef(defs.find((d) => d.id === budgetDefId) ?? null);
      const catRows = rows.filter((b) => b.category !== TOTAL_BUDGET_CATEGORY_KEY);
      setCategoryRows(catRows);
      const totalRow = rows.find((b) => b.category === TOTAL_BUDGET_CATEGORY_KEY);
      setTotalText(totalRow ? String(totalRow.limitAmount) : '');
      const drafts: Record<string, string> = {};
      catRows.forEach((b) => {
        drafts[b.category] = String(b.limitAmount);
      });
      setAmountDrafts(drafts);
    });
  }, [budgetDefId]);

  useFocusEffect(load);

  if (!def) {
    return (
      <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
        <Text style={styles.hint}>Loading…</Text>
      </ScrollView>
    );
  }

  const framework = resolveFramework(def.frameworkKey, def.customFrameworkJson);
  const categorySum = categoryRows.reduce((s, b) => s + b.limitAmount, 0);

  const seedFrameworkDefaults = async (fw: Framework, total: number) => {
    const key = monthKey();
    const allocations = computeDefaultAllocations(fw, total);
    await Promise.all(allocations.map((a) => db.setBudget(budgetDefId, a.category, key, a.amount)));
  };

  const saveTotal = async () => {
    const newTotal = Number(totalText);
    if (!newTotal || newTotal <= 0) return;
    setSavingTotal(true);
    try {
      const key = monthKey();
      if (categoryRows.length > 0 && categorySum > 0) {
        // Auto-scale every existing allocation proportionally so they still sum to the
        // new total, rather than leaving stale amounts or requiring a manual rebalance.
        const ratio = newTotal / categorySum;
        await Promise.all(categoryRows.map((b) => db.setBudget(budgetDefId, b.category, key, Math.round(b.limitAmount * ratio))));
      } else {
        await seedFrameworkDefaults(framework, newTotal);
      }
      await db.setBudget(budgetDefId, TOTAL_BUDGET_CATEGORY_KEY, key, newTotal);
      load();
    } finally {
      setSavingTotal(false);
    }
  };

  const saveCategoryAmounts = async () => {
    setSavingCategories(true);
    try {
      const key = monthKey();
      const writes = Object.entries(amountDrafts)
        .filter(([, v]) => v !== '')
        .map(([category, v]) => db.setBudget(budgetDefId, category, key, Number(v) || 0));
      await Promise.all(writes);
      load();
    } finally {
      setSavingCategories(false);
    }
  };

  const applyFrameworkChange = (newKey: string, customJson: string | null) => {
    Alert.alert(
      'Switch framework?',
      "Switching frameworks will reset your category allocations to the new framework's defaults. Your total budget stays the same.",
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Switch',
          onPress: async () => {
            const key = monthKey();
            await db.updateBudgetDefFramework(budgetDefId, newKey, customJson);
            await db.clearBudgetCategories(budgetDefId, key);
            const total = Number(totalText) || 0;
            if (total > 0) {
              await seedFrameworkDefaults(resolveFramework(newKey, customJson), total);
            }
            setShowFrameworkPicker(false);
            load();
          },
        },
      ],
    );
  };

  const pickFramework = (newKey: string) => {
    if (newKey === def.frameworkKey) {
      setShowFrameworkPicker(false);
      return;
    }
    if (newKey === 'custom') {
      navigation.navigate('CustomFrameworkBuilder', {
        mode: 'edit',
        budgetDefId,
        existingCustomJson: def.customFrameworkJson,
        currentTotal: Number(totalText) || 0,
      });
      setShowFrameworkPicker(false);
      return;
    }
    applyFrameworkChange(newKey, null);
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Edit {def.name}</Text>

      <Text style={styles.sectionLabel}>TOTAL BUDGET</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <Text style={styles.hint}>Changing this scales your existing category allocations proportionally.</Text>
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
        <DhanButton text="Save total" onPress={saveTotal} loading={savingTotal} full />
      </DhanCard>

      <Text style={styles.sectionLabel}>FRAMEWORK</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <View style={styles.frameworkRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.frameworkName}>{framework.name}</Text>
            <Text style={styles.hint}>{framework.description}</Text>
          </View>
          <DhanButton text="Change" size="sm" variant="secondary" onPress={() => setShowFrameworkPicker((v) => !v)} />
        </View>
        {showFrameworkPicker && (
          <View style={styles.frameworkPicker}>
            {FRAMEWORKS.map((fw) => (
              <DhanChip key={fw.key} label={fw.name} active={fw.key === def.frameworkKey} onPress={() => pickFramework(fw.key)} />
            ))}
            <DhanChip label="Custom…" active={def.frameworkKey === 'custom'} onPress={() => pickFramework('custom')} />
          </View>
        )}
      </DhanCard>

      <Text style={styles.sectionLabel}>ALLOCATIONS</Text>
      {framework.buckets.map((bucket) => {
        if (bucket.categories.length === 0) {
          const amount = (Number(totalText) * bucket.percent) / 100 || 0;
          return (
            <DhanCard key={bucket.key} style={{ marginBottom: 10 }}>
              <View style={styles.bucketHeaderRow}>
                <Text style={styles.bucketName}>{bucket.name}</Text>
                <Text style={styles.bucketAmount}>
                  {formatINR(amount)} <Text style={styles.hint}>({bucket.percent}%)</Text>
                </Text>
              </View>
              <Text style={styles.hint}>Not a spending category — nothing to allocate here directly.</Text>
            </DhanCard>
          );
        }
        return (
          <DhanCard key={bucket.key} style={{ marginBottom: 10 }}>
            <Text style={[styles.bucketName, { marginBottom: 10 }]}>
              {bucket.name} <Text style={styles.hint}>({bucket.percent}%)</Text>
            </Text>
            {bucket.categories.map((c) => {
              const cat = categoryFromKey(c);
              const meta = CATEGORIES[cat];
              return (
                <View key={c} style={styles.catEditRow}>
                  <CategoryIcon category={cat} size={28} tint />
                  <Text style={styles.catEditName}>{meta.name}</Text>
                  <View style={styles.catEditAmountRow}>
                    <Text style={styles.rupee}>₹</Text>
                    <TextInput
                      value={amountDrafts[c] ?? ''}
                      onChangeText={(v) => setAmountDrafts((prev) => ({ ...prev, [c]: v }))}
                      placeholder="0"
                      placeholderTextColor={colors.fg3}
                      keyboardType="numeric"
                      style={styles.catEditInput}
                    />
                  </View>
                </View>
              );
            })}
          </DhanCard>
        );
      })}
      <DhanButton text="Save allocations" onPress={saveCategoryAmounts} loading={savingCategories} full style={{ marginTop: 8 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 16, paddingBottom: 40 },
  title: { ...type.h1, color: colors.fg1, marginBottom: 14 },
  sectionLabel: { ...type.label, color: colors.fg3, marginBottom: 8, marginLeft: 4 },
  hint: { fontSize: 12, color: colors.fg3, lineHeight: 17, marginBottom: 10 },
  amountRow: {
    flexDirection: 'row', alignItems: 'center', gap: 8, height: 48, borderWidth: 1,
    borderColor: colors.borderDefault, borderRadius: 8, paddingHorizontal: 12, marginBottom: 14, backgroundColor: '#fff',
  },
  rupee: { color: colors.fg2, fontWeight: '600' },
  amountInput: { flex: 1, fontSize: 15, color: colors.fg1, fontFamily: 'Poppins-Regular' },
  frameworkRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  frameworkName: { fontSize: 15, fontWeight: '700', color: colors.fg1, marginBottom: 2 },
  frameworkPicker: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 14, paddingTop: 14, borderTopWidth: 1, borderTopColor: colors.borderSubtle },
  bucketHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 },
  bucketName: { fontSize: 13, fontWeight: '700', color: colors.fg2, textTransform: 'uppercase', letterSpacing: 0.4 },
  bucketAmount: { fontSize: 13, fontWeight: '700', color: colors.fg1 },
  catEditRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 10 },
  catEditName: { flex: 1, fontSize: 14, fontWeight: '600', color: colors.fg1 },
  catEditAmountRow: {
    flexDirection: 'row', alignItems: 'center', gap: 4, height: 38, borderWidth: 1,
    borderColor: colors.borderDefault, borderRadius: 8, paddingHorizontal: 10, backgroundColor: '#fff', width: 110,
  },
  catEditInput: { flex: 1, fontSize: 14, color: colors.fg1, fontFamily: 'Poppins-Regular' },
});

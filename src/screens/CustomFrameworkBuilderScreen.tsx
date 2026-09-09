import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { DhanText as Text } from '../components/DhanText';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { TrashIcon } from 'phosphor-react-native';
import { colors, CATEGORIES, CategoryKey } from '../theme/colors';
import { type } from '../theme/type';
import { db } from '../native/DhanDb';
import { monthKey } from '../utils/format';
import { DhanField } from '../components/Field';
import { DhanButton } from '../components/Button';
import { DhanChip } from '../components/Chips';
import { CustomFramework, FrameworkBucket, computeDefaultAllocations, TOTAL_BUDGET_CATEGORY_KEY } from '../data/frameworks';
import { RootStackParamList } from '../navigation/RootNavigator';

type Props = NativeStackScreenProps<RootStackParamList, 'CustomFrameworkBuilder'>;

const SPENDING_CATEGORIES = (Object.keys(CATEGORIES) as CategoryKey[]).filter((c) => c !== 'income');

interface DraftBucket {
  localId: string;
  name: string;
  percentText: string;
  categories: CategoryKey[];
}

function draftFromBucket(b: FrameworkBucket, i: number): DraftBucket {
  return { localId: `${i}-${b.key}`, name: b.name, percentText: String(b.percent), categories: b.categories };
}

/**
 * The custom framework builder — its exact UX isn't specified anywhere in the design
 * brief, so this is a straightforward, functional first pass (named buckets, a percent
 * each, categories assigned via chips, validated to sum to 100) rather than a guess at
 * pixel-exact behavior. Flagged in PROJECT_STATUS.md as the piece most likely to need
 * revision once a real visual reference for it exists.
 */
export function CustomFrameworkBuilderScreen({ route, navigation }: Props) {
  const params = route.params;
  const [name, setName] = useState(params.mode === 'edit' ? 'Custom' : 'Custom');
  const [buckets, setBuckets] = useState<DraftBucket[]>(() => {
    if (params.mode === 'edit' && params.existingCustomJson) {
      try {
        const parsed = JSON.parse(params.existingCustomJson) as CustomFramework;
        return parsed.buckets.map(draftFromBucket);
      } catch {
        // fall through to a single starter bucket below
      }
    }
    return [{ localId: 'b0', name: 'Bucket 1', percentText: '100', categories: [] }];
  });
  const [saving, setSaving] = useState(false);

  const addBucket = () => {
    setBuckets((prev) => [...prev, { localId: `b${prev.length}-${Date.now()}`, name: `Bucket ${prev.length + 1}`, percentText: '0', categories: [] }]);
  };

  const removeBucket = (localId: string) => {
    setBuckets((prev) => prev.filter((b) => b.localId !== localId));
  };

  const updateBucket = (localId: string, patch: Partial<DraftBucket>) => {
    setBuckets((prev) => prev.map((b) => (b.localId === localId ? { ...b, ...patch } : b)));
  };

  const toggleCategory = (localId: string, category: CategoryKey) => {
    setBuckets((prev) =>
      prev.map((b) => {
        if (b.localId !== localId) return b;
        const has = b.categories.includes(category);
        return { ...b, categories: has ? b.categories.filter((c) => c !== category) : [...b.categories, category] };
      }),
    );
  };

  const percentTotal = buckets.reduce((s, b) => s + (Number(b.percentText) || 0), 0);

  const save = async () => {
    if (!name.trim()) {
      Alert.alert('Name required', 'Give this framework a name.');
      return;
    }
    if (buckets.length === 0) {
      Alert.alert('Add a bucket', 'A framework needs at least one bucket.');
      return;
    }
    if (percentTotal !== 100) {
      Alert.alert('Percentages must add up to 100', `Your buckets currently add up to ${percentTotal}%.`);
      return;
    }

    const customFramework: CustomFramework = {
      name: name.trim(),
      buckets: buckets.map((b) => ({ key: b.localId, name: b.name.trim() || 'Bucket', percent: Number(b.percentText) || 0, categories: b.categories })),
    };
    const customJson = JSON.stringify(customFramework);
    const resolvedForSeeding = { key: 'custom', name: customFramework.name, description: '', buckets: customFramework.buckets };

    setSaving(true);
    try {
      const key = monthKey();
      if (params.mode === 'create') {
        const id = await db.createBudgetDef(params.pendingName, 'PROJECT', 'custom', customJson);
        const allocations = computeDefaultAllocations(resolvedForSeeding, params.pendingTotal);
        await Promise.all([
          db.setBudget(id, TOTAL_BUDGET_CATEGORY_KEY, key, params.pendingTotal),
          ...allocations.map((a) => db.setBudget(id, a.category, key, a.amount)),
        ]);
        navigation.popToTop();
      } else {
        await db.updateBudgetDefFramework(params.budgetDefId, 'custom', customJson);
        await db.clearBudgetCategories(params.budgetDefId, key);
        if (params.currentTotal > 0) {
          const allocations = computeDefaultAllocations(resolvedForSeeding, params.currentTotal);
          await Promise.all(allocations.map((a) => db.setBudget(params.budgetDefId, a.category, key, a.amount)));
        }
        navigation.goBack();
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <DhanField label="Framework name" value={name} onChangeText={setName} placeholder="e.g. My Framework" />

      <View style={styles.percentRow}>
        <Text style={styles.sectionLabel}>BUCKETS</Text>
        <Text style={[styles.percentTotal, percentTotal !== 100 && { color: colors.expense }]}>{percentTotal}% of 100%</Text>
      </View>

      {buckets.map((b) => (
        <View key={b.localId} style={styles.bucketCard}>
          <View style={styles.bucketTopRow}>
            <TextInput value={b.name} onChangeText={(v) => updateBucket(b.localId, { name: v })} style={styles.bucketNameInput} placeholder="Bucket name" placeholderTextColor={colors.fg3} />
            <View style={styles.percentInputRow}>
              <TextInput
                value={b.percentText}
                onChangeText={(v) => updateBucket(b.localId, { percentText: v })}
                keyboardType="numeric"
                style={styles.percentInput}
                placeholder="0"
                placeholderTextColor={colors.fg3}
              />
              <Text style={styles.percentSign}>%</Text>
            </View>
            <Pressable onPress={() => removeBucket(b.localId)} hitSlop={8} style={{ marginLeft: 8 }}>
              <TrashIcon size={18} color={colors.fg3} />
            </Pressable>
          </View>
          <View style={styles.chipWrap}>
            {SPENDING_CATEGORIES.map((c) => (
              <DhanChip key={c} label={CATEGORIES[c].name} active={b.categories.includes(c)} onPress={() => toggleCategory(b.localId, c)} />
            ))}
          </View>
        </View>
      ))}

      <DhanButton text="+ Add bucket" variant="secondary" full onPress={addBucket} style={{ marginBottom: 20 }} />
      <DhanButton text="Save framework" full size="lg" loading={saving} onPress={save} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 20, paddingBottom: 40 },
  sectionLabel: { ...type.label, color: colors.fg3 },
  percentRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10, marginTop: 4 },
  percentTotal: { fontSize: 12, fontWeight: '700', color: colors.fg2 },
  bucketCard: { backgroundColor: colors.bgSurface, borderRadius: 12, padding: 14, marginBottom: 12 },
  bucketTopRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  bucketNameInput: { flex: 1, fontSize: 14, fontWeight: '700', color: colors.fg1, fontFamily: 'Poppins-SemiBold' },
  percentInputRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 8, borderWidth: 1, borderColor: colors.borderDefault, paddingHorizontal: 8, height: 34, width: 64 },
  percentInput: { flex: 1, fontSize: 13, color: colors.fg1, fontFamily: 'Poppins-Regular', textAlign: 'right' },
  percentSign: { fontSize: 13, color: colors.fg2, marginLeft: 2 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
});

import React, { useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { DhanText as Text } from '../components/DhanText';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { db } from '../native/DhanDb';
import { monthKey } from '../utils/format';
import { DhanField } from '../components/Field';
import { DhanButton } from '../components/Button';
import { DhanChip } from '../components/Chips';
import { computeDefaultAllocations, DEFAULT_FRAMEWORK_KEY, FRAMEWORKS, TOTAL_BUDGET_CATEGORY_KEY } from '../data/frameworks';
import { RootStackParamList } from '../navigation/RootNavigator';

type Props = NativeStackScreenProps<RootStackParamList, 'CreateBudget'>;

/** Project budgets inherit the exact same framework structure as Personal — there is no
 *  separate picker or code path for them; this screen just calls the same createBudgetDef
 *  API with type: 'PROJECT'. */
export function CreateBudgetScreen({ navigation }: Props) {
  const [name, setName] = useState('');
  const [frameworkKey, setFrameworkKey] = useState(DEFAULT_FRAMEWORK_KEY);
  const [totalText, setTotalText] = useState('');
  const [saving, setSaving] = useState(false);

  const create = async () => {
    if (!name.trim()) {
      Alert.alert('Name required', 'Give this budget a name, e.g. "Wedding" or "Goa Trip".');
      return;
    }
    const total = Number(totalText);
    if (!total || total <= 0) {
      Alert.alert('Total budget required', 'Set a starting total so categories start with real numbers — you can change it anytime.');
      return;
    }
    if (frameworkKey === 'custom') {
      navigation.navigate('CustomFrameworkBuilder', { mode: 'create', pendingName: name.trim(), pendingTotal: total });
      return;
    }
    setSaving(true);
    try {
      const id = await db.createBudgetDef(name.trim(), 'PROJECT', frameworkKey, null);
      const fw = FRAMEWORKS.find((f) => f.key === frameworkKey)!;
      const key = monthKey();
      const allocations = computeDefaultAllocations(fw, total);
      await Promise.all([
        db.setBudget(id, TOTAL_BUDGET_CATEGORY_KEY, key, total),
        ...allocations.map((a) => db.setBudget(id, a.category, key, a.amount)),
      ]);
      navigation.goBack();
    } finally {
      setSaving(false);
    }
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <DhanField label="Budget name" value={name} onChangeText={setName} placeholder="e.g. Wedding, Goa Trip" />
      <DhanField label="Total budget" value={totalText} onChangeText={setTotalText} placeholder="0" prefix="₹" keyboardType="numeric" />

      <Text style={styles.label}>Framework</Text>
      <View style={styles.chipWrap}>
        {FRAMEWORKS.map((fw) => (
          <DhanChip key={fw.key} label={fw.name} active={frameworkKey === fw.key} onPress={() => setFrameworkKey(fw.key)} />
        ))}
        <DhanChip label="Custom…" active={frameworkKey === 'custom'} onPress={() => setFrameworkKey('custom')} />
      </View>

      <DhanButton text="Create budget" full size="lg" loading={saving} onPress={create} style={{ marginTop: 10 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 20, paddingBottom: 40 },
  label: { fontSize: 12, fontWeight: '600', color: colors.fg2, marginBottom: 8 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 20 },
});

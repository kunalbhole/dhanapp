import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View, Pressable } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { colors, CATEGORIES, CategoryKey } from '../theme/colors';
import { db } from '../native/DhanDb';
import { DhanButton } from '../components/Button';
import { DhanField } from '../components/Field';
import { CategoryIcon } from '../components/CategoryIcon';
import { RootStackParamList } from '../navigation/RootNavigator';

type Props = NativeStackScreenProps<RootStackParamList, 'AddTransaction'>;

const EXPENSE_CATEGORIES = (Object.keys(CATEGORIES) as CategoryKey[]).filter((c) => c !== 'income');

export function AddTransactionScreen({ route, navigation }: Props) {
  const [isIncome, setIsIncome] = useState(!!route.params?.defaultIsIncome);
  const [amountText, setAmountText] = useState('');
  const [category, setCategory] = useState<CategoryKey>('other');
  const [note, setNote] = useState('');

  const save = async () => {
    const amount = Number(amountText);
    if (!amount || amount <= 0) return;
    const signed = isIncome ? amount : -amount;
    const resolvedCategory = isIncome ? 'income' : category;
    const merchant = note.trim() || (isIncome ? 'Income' : CATEGORIES[category].name);
    await db.addTransaction(merchant, note.trim() || null, signed, resolvedCategory, Date.now(), 'MANUAL');
    navigation.goBack();
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <View style={styles.toggleRow}>
        {(['Expense', 'Income'] as const).map((label, i) => {
          const active = isIncome === (i === 1);
          return (
            <Pressable key={label} onPress={() => setIsIncome(i === 1)} style={[styles.toggleBtn, active && styles.toggleBtnActive]}>
              <Text style={[styles.toggleText, active && { color: colors.navy }]}>{label}</Text>
            </Pressable>
          );
        })}
      </View>

      <DhanField label="Amount" value={amountText} onChangeText={setAmountText} placeholder="0" prefix="₹" keyboardType="numeric" />

      {!isIncome && (
        <>
          <Text style={styles.label}>Category</Text>
          <View style={styles.grid}>
            {EXPENSE_CATEGORIES.map((c) => (
              <Pressable key={c} onPress={() => setCategory(c)} style={[styles.gridItem, category === c && styles.gridItemActive]}>
                <CategoryIcon category={c} size={28} tint />
                <Text style={styles.gridLabel}>{CATEGORIES[c].name}</Text>
              </Pressable>
            ))}
          </View>
        </>
      )}

      <DhanField label="Note (optional)" value={note} onChangeText={setNote} placeholder="Add a note" />

      <DhanButton text={isIncome ? 'Save income' : 'Save expense'} full size="lg" onPress={save} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 20, paddingBottom: 40 },
  toggleRow: { flexDirection: 'row', backgroundColor: colors.bgSurface, borderRadius: 12, padding: 3, marginBottom: 20 },
  toggleBtn: { flex: 1, height: 38, alignItems: 'center', justifyContent: 'center', borderRadius: 9 },
  toggleBtnActive: { backgroundColor: '#fff' },
  toggleText: { fontWeight: '700', fontSize: 14, color: colors.fg3 },
  label: { fontSize: 12, fontWeight: '700', color: colors.fg2, marginBottom: 8 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 14 },
  gridItem: { width: '22%', alignItems: 'center', gap: 6, paddingVertical: 10, borderRadius: 12, backgroundColor: colors.bgSurface },
  gridItemActive: { backgroundColor: colors.navy05, borderWidth: 1, borderColor: colors.navy },
  gridLabel: { fontSize: 10, fontWeight: '600', color: colors.fg2 },
});

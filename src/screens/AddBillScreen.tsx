import React, { useState } from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { db, BillStatus } from '../native/DhanDb';
import { DhanButton } from '../components/Button';
import { DhanField } from '../components/Field';
import { RootStackParamList } from '../navigation/RootNavigator';

type Props = NativeStackScreenProps<RootStackParamList, 'AddBill'>;

export function AddBillScreen({ navigation }: Props) {
  const [name, setName] = useState('');
  const [amountText, setAmountText] = useState('');
  const [dueInDaysText, setDueInDaysText] = useState('7');

  const save = async () => {
    const amount = Number(amountText);
    const dueInDays = Number(dueInDaysText) || 0;
    if (!name.trim() || !amount || amount <= 0) return;
    const dueDateMillis = Date.now() + dueInDays * 24 * 60 * 60 * 1000;
    const status: BillStatus = dueInDays <= 3 ? 'DUE_SOON' : 'UPCOMING';
    await db.addBill(name.trim(), amount, dueDateMillis, status, true, 'bills');
    navigation.goBack();
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <DhanField label="Bill name" value={name} onChangeText={setName} placeholder="e.g. Netflix" />
      <DhanField label="Amount" value={amountText} onChangeText={setAmountText} placeholder="0" prefix="₹" keyboardType="numeric" />
      <DhanField label="Due in (days)" value={dueInDaysText} onChangeText={setDueInDaysText} placeholder="7" keyboardType="numeric" />
      <DhanButton text="Save bill" full size="lg" onPress={save} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 20, paddingBottom: 40 },
});

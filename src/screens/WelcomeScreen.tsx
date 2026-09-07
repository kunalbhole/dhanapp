import React, { useState } from 'react';
import { SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { type } from '../theme/type';
import { DhanField } from '../components/Field';
import { DhanButton } from '../components/Button';
import { userPrefs } from '../native/UserPrefs';
import { permissions } from '../native/DhanPermissions';
import { db } from '../native/DhanDb';
import { monthKey } from '../utils/format';
import { RootStackParamList } from '../navigation/RootNavigator';

type Props = NativeStackScreenProps<RootStackParamList, 'Welcome'>;

/**
 * Consolidates the source design's Splash + Onboarding + SignUp + Permissions + Login
 * screens into one screen. This rewrite is scoped to the core money-tracking loop, not
 * a full re-port of all 26 screens a second time — see the project README.
 */
export function WelcomeScreen({ navigation }: Props) {
  const [name, setName] = useState('');
  const [incomeText, setIncomeText] = useState('');
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('');

  const finish = async () => {
    setBusy(true);
    try {
      await userPrefs.setUserName(name.trim() || 'there');

      const income = Number(incomeText);
      if (income > 0) {
        await db.addTransaction('Salary', 'Monthly income', income, 'income', Date.now(), 'MANUAL');
      }

      setStatus('Requesting SMS permission…');
      const granted = await permissions.requestSmsPermission();
      if (granted) {
        setStatus('Scanning SMS for past transactions…');
        await db.scanHistoricalSms();
        await seedDefaultBudgets();
      }

      await userPrefs.setOnboarded();
      navigation.reset({ index: 0, routes: [{ name: 'Main' }] });
    } finally {
      setBusy(false);
      setStatus('');
    }
  };

  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.content}>
        <Text style={styles.title}>Welcome to Dhan</Text>
        <Text style={styles.subtitle}>
          Track what comes in and goes out. Dhan can read your bank/UPI SMS and notifications
          to log transactions automatically — you can turn that on now or later in Settings.
        </Text>
        <DhanField label="Your name" value={name} onChangeText={setName} placeholder="e.g. Priya" />
        <DhanField
          label="Monthly income (optional)"
          value={incomeText}
          onChangeText={setIncomeText}
          placeholder="0"
          prefix="₹"
          keyboardType="numeric"
        />
        <Text style={styles.note}>
          Tapping continue will ask for SMS permission and, if granted, scan your existing SMS for
          past bank/UPI transactions so your balance and budgets start out real instead of empty.
          If you skip permission now, you can grant it later from Settings and scan then.
        </Text>
        <DhanButton text="Continue" onPress={finish} full loading={busy} size="lg" />
        {busy && !!status && <Text style={styles.status}>{status}</Text>}
      </View>
    </SafeAreaView>
  );
}

async function seedDefaultBudgets(): Promise<void> {
  const all = await db.getTransactions();
  const spendByCategory = new Map<string, number>();
  for (const t of all) {
    if (t.amount >= 0) continue;
    spendByCategory.set(t.category, (spendByCategory.get(t.category) ?? 0) - t.amount);
  }
  const key = monthKey();
  await Promise.all(
    Array.from(spendByCategory.entries())
      .filter(([, amount]) => amount > 0)
      .map(([category, amount]) => db.setBudget(category, key, Math.round(amount))),
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { flex: 1, justifyContent: 'center', padding: 24 },
  title: { ...type.h1, color: colors.navy, marginBottom: 8 },
  subtitle: { ...type.body, color: colors.fg2, marginBottom: 24 },
  note: { ...type.caption, color: colors.fg3, marginBottom: 20 },
  status: { ...type.caption, color: colors.fg3, marginTop: 10, textAlign: 'center' },
});

import React, { useCallback, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { DhanText as Text } from '../components/DhanText';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { type } from '../theme/type';
import { db, Transaction } from '../native/DhanDb';
import { userPrefs } from '../native/UserPrefs';
import { formatINR, monthRange } from '../utils/format';
import { DhanCard } from '../components/Card';
import { DhanButton } from '../components/Button';
import { TxnRow } from '../components/TxnRow';
import { RootStackParamList } from '../navigation/RootNavigator';

type Nav = NativeStackNavigationProp<RootStackParamList>;

export function HomeScreen() {
  const navigation = useNavigation<Nav>();
  const [name, setName] = useState('there');
  const [txns, setTxns] = useState<Transaction[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    const [n, all] = await Promise.all([userPrefs.getUserName(), db.getTransactions()]);
    setName(n ?? 'there');
    setTxns(all);
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const onRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  const [monthStart, monthEnd] = monthRange();
  const thisMonth = txns.filter((t) => t.timestampMillis >= monthStart && t.timestampMillis < monthEnd);
  const income = thisMonth.filter((t) => t.amount > 0).reduce((s, t) => s + t.amount, 0);
  const expense = thisMonth.filter((t) => t.amount < 0).reduce((s, t) => s - t.amount, 0);
  const balance = income - expense;
  const recent = txns.slice(0, 4);

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}>
      <Text style={styles.greeting}>Hi, {name} 👋</Text>

      <View style={styles.balanceCard}>
        <Text style={styles.balanceLabel}>BALANCE · THIS MONTH</Text>
        <Text style={styles.balanceAmount}>{formatINR(balance)}</Text>
        <View style={styles.pillRow}>
          <Text style={[styles.pill, { backgroundColor: 'rgba(254,217,119,0.15)', color: colors.goldSoft }]}>
            + {formatINR(income)}
          </Text>
          <Text style={[styles.pill, { backgroundColor: 'rgba(255,255,255,0.1)', color: '#fff' }]}>
            − {formatINR(expense)}
          </Text>
        </View>
      </View>

      <View style={styles.actionsRow}>
        <DhanButton text="+ Expense" onPress={() => navigation.navigate('AddTransaction', { defaultIsIncome: false })} variant="secondary" style={styles.actionBtn} />
        <DhanButton text="+ Income" onPress={() => navigation.navigate('AddTransaction', { defaultIsIncome: true })} variant="secondary" style={styles.actionBtn} />
      </View>

      <Text style={styles.sectionTitle}>Recent transactions</Text>
      <DhanCard padding={8}>
        {recent.length === 0 ? (
          <Text style={styles.empty}>No transactions yet — add one, or turn on SMS/notification capture in Settings.</Text>
        ) : (
          recent.map((t, i) => (
            <TxnRow
              key={t.id}
              merchant={t.merchant}
              meta={new Date(t.timestampMillis).toLocaleString('en-IN', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}
              amount={t.amount}
              category={t.category}
              last={i === recent.length - 1}
              onPress={() => navigation.navigate('TxnDetail', { id: t.id })}
            />
          ))
        )}
      </DhanCard>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 16, paddingBottom: 40 },
  greeting: { ...type.h2, color: colors.fg1, marginBottom: 16 },
  balanceCard: { backgroundColor: colors.navy, borderRadius: 20, padding: 20, marginBottom: 16 },
  balanceLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 11, fontWeight: '700', letterSpacing: 1 },
  balanceAmount: { color: '#fff', fontSize: 34, fontWeight: '700', marginVertical: 6 },
  pillRow: { flexDirection: 'row', gap: 8, marginTop: 8 },
  pill: { fontSize: 12, fontWeight: '600', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999, overflow: 'hidden' },
  actionsRow: { flexDirection: 'row', gap: 10, marginBottom: 20 },
  actionBtn: { flex: 1 },
  sectionTitle: { ...type.h3, color: colors.fg1, marginBottom: 10 },
  empty: { color: colors.fg3, fontSize: 13, padding: 16, textAlign: 'center' },
});

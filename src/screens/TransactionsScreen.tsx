import React, { useCallback, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { type } from '../theme/type';
import { db, Transaction } from '../native/DhanDb';
import { dayGroupLabel, timeLabel } from '../utils/format';
import { DhanCard } from '../components/Card';
import { DhanChip } from '../components/Chips';
import { TxnRow } from '../components/TxnRow';
import { RootStackParamList } from '../navigation/RootNavigator';

type Nav = NativeStackNavigationProp<RootStackParamList>;
type Filter = 'all' | 'in' | 'out';

export function TransactionsScreen() {
  const navigation = useNavigation<Nav>();
  const [txns, setTxns] = useState<Transaction[]>([]);
  const [filter, setFilter] = useState<Filter>('all');

  useFocusEffect(useCallback(() => { db.getTransactions().then(setTxns); }, []));

  const filtered = txns.filter((t) => (filter === 'in' ? t.amount > 0 : filter === 'out' ? t.amount < 0 : true));
  const groups = groupByDay(filtered);

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>Transactions</Text>
        <View style={styles.filterRow}>
          <DhanChip label="All" active={filter === 'all'} onPress={() => setFilter('all')} />
          <DhanChip label="Income" active={filter === 'in'} onPress={() => setFilter('in')} />
          <DhanChip label="Expenses" active={filter === 'out'} onPress={() => setFilter('out')} />
        </View>
      </View>

      <FlatList
        contentContainerStyle={styles.list}
        data={groups}
        keyExtractor={(g) => g.day}
        renderItem={({ item }) => (
          <View style={styles.group}>
            <Text style={styles.groupLabel}>{item.day.toUpperCase()}</Text>
            <DhanCard padding={8}>
              {item.items.map((t, i) => (
                <TxnRow
                  key={t.id}
                  merchant={t.merchant}
                  meta={(t.note ?? t.sourceApp ?? t.category) + ' · ' + timeLabel(t.timestampMillis)}
                  amount={t.amount}
                  category={t.category}
                  last={i === item.items.length - 1}
                  onPress={() => navigation.navigate('TxnDetail', { id: t.id })}
                />
              ))}
            </DhanCard>
          </View>
        )}
        ListEmptyComponent={<Text style={styles.empty}>No transactions match.</Text>}
      />

      <Pressable style={styles.fab} onPress={() => navigation.navigate('AddTransaction', undefined)}>
        <Text style={styles.fabText}>+</Text>
      </Pressable>
    </View>
  );
}

function groupByDay(txns: Transaction[]): { day: string; items: Transaction[] }[] {
  const map = new Map<string, Transaction[]>();
  for (const t of txns) {
    const label = dayGroupLabel(t.timestampMillis);
    if (!map.has(label)) map.set(label, []);
    map.get(label)!.push(t);
  }
  return Array.from(map.entries()).map(([day, items]) => ({ day, items }));
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  header: { padding: 16, paddingBottom: 8 },
  title: { ...type.h1, color: colors.fg1, marginBottom: 12 },
  filterRow: { flexDirection: 'row', gap: 8 },
  list: { paddingHorizontal: 16, paddingBottom: 100 },
  group: { marginBottom: 14 },
  groupLabel: { ...type.label, color: colors.fg3, marginBottom: 6, marginLeft: 4 },
  empty: { textAlign: 'center', color: colors.fg3, padding: 40 },
  fab: {
    position: 'absolute', right: 20, bottom: 24, width: 56, height: 56, borderRadius: 28,
    backgroundColor: colors.navy, alignItems: 'center', justifyContent: 'center',
    shadowColor: colors.navy, shadowOpacity: 0.35, shadowRadius: 10, shadowOffset: { width: 0, height: 6 }, elevation: 6,
  },
  fabText: { color: '#fff', fontSize: 28, fontWeight: '600', marginTop: -2 },
});

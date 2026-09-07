import React, { useCallback, useState } from 'react';
import { FlatList, Pressable, StyleSheet, View } from 'react-native';
import { DhanText as Text } from '../components/DhanText';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { type } from '../theme/type';
import { db, Bill } from '../native/DhanDb';
import { formatINR, dateLabel } from '../utils/format';
import { DhanCard } from '../components/Card';
import { StatusPill, StatusTone } from '../components/Chips';
import { DhanButton } from '../components/Button';
import { RootStackParamList } from '../navigation/RootNavigator';

type Nav = NativeStackNavigationProp<RootStackParamList>;

const STATUS_META: Record<string, { tone: StatusTone; label: string }> = {
  UPCOMING: { tone: 'info', label: 'Upcoming' },
  DUE_SOON: { tone: 'warning', label: 'Due soon' },
  OVERDUE: { tone: 'expense', label: 'Overdue' },
  PAID: { tone: 'income', label: 'Paid' },
};

export function BillsScreen() {
  const navigation = useNavigation<Nav>();
  const [bills, setBills] = useState<Bill[]>([]);

  const load = useCallback(() => { db.getBills().then(setBills); }, []);
  useFocusEffect(load);

  const unpaidTotal = bills.filter((b) => b.status !== 'PAID').reduce((s, b) => s + b.amount, 0);
  const dueSoon = bills.filter((b) => b.status === 'DUE_SOON');

  const markPaid = async (id: number) => {
    await db.setBillStatus(id, 'PAID');
    load();
  };

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>Bills & subs</Text>
        <View style={styles.heroCard}>
          <Text style={styles.heroLabel}>STILL DUE THIS MONTH</Text>
          <Text style={styles.heroAmount}>{formatINR(unpaidTotal)}</Text>
          <Text style={styles.heroSub}>{bills.filter((b) => b.status === 'PAID').length} of {bills.length} paid</Text>
        </View>
      </View>

      <FlatList
        contentContainerStyle={styles.list}
        data={bills}
        keyExtractor={(b) => String(b.id)}
        ListHeaderComponent={dueSoon.length > 0 ? <Text style={styles.sectionLabel}>DUE SOON</Text> : undefined}
        renderItem={({ item }) => {
          const meta = STATUS_META[item.status];
          return (
            <DhanCard style={{ marginBottom: 8 }}>
              <View style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.billName}>{item.name}</Text>
                  <Text style={styles.billDue}>Due {dateLabel(item.dueDateMillis)}</Text>
                </View>
                <View style={{ alignItems: 'flex-end' }}>
                  <Text style={styles.billAmount}>{formatINR(item.amount)}</Text>
                  <StatusPill tone={meta.tone}>{meta.label}</StatusPill>
                </View>
              </View>
              {item.status !== 'PAID' && (
                <DhanButton text="Mark as paid" size="sm" full onPress={() => markPaid(item.id)} style={{ marginTop: 10 }} />
              )}
            </DhanCard>
          );
        }}
        ListEmptyComponent={<Text style={styles.empty}>No bills yet. Tap + to add one.</Text>}
      />

      <Pressable style={styles.fab} onPress={() => navigation.navigate('AddBill')}>
        <Text style={styles.fabText}>+</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  header: { padding: 16 },
  title: { ...type.h1, color: colors.fg1, marginBottom: 12 },
  heroCard: { backgroundColor: colors.navy, borderRadius: 16, padding: 16 },
  heroLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 10, fontWeight: '700', letterSpacing: 1 },
  heroAmount: { color: '#fff', fontSize: 26, fontWeight: '700', marginTop: 2 },
  heroSub: { color: 'rgba(255,255,255,0.7)', fontSize: 11, marginTop: 2 },
  list: { paddingHorizontal: 16, paddingBottom: 100 },
  sectionLabel: { ...type.label, color: colors.fg3, marginBottom: 8, marginLeft: 4 },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  billName: { fontSize: 14, fontWeight: '700', color: colors.fg1 },
  billDue: { fontSize: 12, color: colors.fg3, marginTop: 2 },
  billAmount: { fontSize: 14, fontWeight: '700', color: colors.fg1, marginBottom: 4 },
  empty: { textAlign: 'center', color: colors.fg3, padding: 40 },
  fab: {
    position: 'absolute', right: 20, bottom: 24, width: 56, height: 56, borderRadius: 28,
    backgroundColor: colors.navy, alignItems: 'center', justifyContent: 'center',
    shadowColor: colors.navy, shadowOpacity: 0.35, shadowRadius: 10, shadowOffset: { width: 0, height: 6 }, elevation: 6,
  },
  fabText: { color: '#fff', fontSize: 28, fontWeight: '600', marginTop: -2 },
});

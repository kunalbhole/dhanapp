import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { colors, categoryFromKey, CATEGORIES } from '../theme/colors';
import { type } from '../theme/type';
import { db, Transaction } from '../native/DhanDb';
import { formatINR, dateLabel, timeLabel } from '../utils/format';
import { DhanCard } from '../components/Card';
import { CategoryIcon } from '../components/CategoryIcon';
import { DhanButton } from '../components/Button';
import { RootStackParamList } from '../navigation/RootNavigator';

type Props = NativeStackScreenProps<RootStackParamList, 'TxnDetail'>;

export function TxnDetailScreen({ route, navigation }: Props) {
  const { id } = route.params;
  const [txn, setTxn] = useState<Transaction | null>(null);

  useEffect(() => {
    db.getTransactions().then((all) => setTxn(all.find((t) => t.id === id) ?? null));
  }, [id]);

  if (!txn) {
    return (
      <View style={styles.screen}>
        <Text style={styles.notFound}>Loading…</Text>
      </View>
    );
  }

  const cat = categoryFromKey(txn.category);
  const isIncome = txn.amount > 0;

  const remove = async () => {
    await db.deleteTransaction(txn.id);
    navigation.goBack();
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <View style={styles.hero}>
        <CategoryIcon category={cat} size={64} tint />
        <Text style={styles.merchant}>{txn.merchant}</Text>
        <Text style={styles.date}>{dateLabel(txn.timestampMillis)} · {timeLabel(txn.timestampMillis)}</Text>
        <Text style={[styles.amount, { color: isIncome ? colors.income : colors.fg1 }]}>
          {(isIncome ? '+' : '−') + formatINR(Math.abs(txn.amount))}
        </Text>
      </View>

      <DhanCard style={{ marginBottom: 14 }}>
        <DetailRow label="Category" value={CATEGORIES[cat].name} />
        <DetailRow label="Logged via" value={txn.source === 'MANUAL' ? 'Manual entry' : txn.source === 'SMS' ? 'SMS auto-capture' : 'Notification auto-capture'} />
        {txn.sourceApp ? <DetailRow label="Source" value={txn.sourceApp} /> : null}
        {txn.accountHint ? <DetailRow label="Account" value={txn.accountHint} last /> : null}
      </DhanCard>

      {txn.rawText ? (
        <>
          <Text style={styles.sectionLabel}>DETECTED FROM {txn.sourceApp ?? txn.source}</Text>
          <DhanCard style={{ marginBottom: 14 }}>
            <Text style={styles.rawText}>“{txn.rawText}”</Text>
          </DhanCard>
        </>
      ) : null}

      <DhanButton text="Delete transaction" variant="destructive" full onPress={remove} />
    </ScrollView>
  );
}

function DetailRow({ label, value, last }: { label: string; value: string; last?: boolean }) {
  return (
    <View style={[styles.detailRow, !last && styles.divider]}>
      <Text style={styles.detailLabel}>{label}</Text>
      <Text style={styles.detailValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 16, paddingBottom: 40 },
  notFound: { textAlign: 'center', marginTop: 40, color: colors.fg3 },
  hero: { alignItems: 'center', paddingVertical: 20 },
  merchant: { ...type.h2, color: colors.fg1, marginTop: 12 },
  date: { fontSize: 12, color: colors.fg3, marginTop: 2 },
  amount: { fontSize: 38, fontWeight: '700', marginTop: 14 },
  detailRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 12 },
  divider: { borderBottomWidth: 1, borderBottomColor: colors.borderSubtle },
  detailLabel: { fontSize: 13, fontWeight: '600', color: colors.fg3 },
  detailValue: { fontSize: 13, fontWeight: '600', color: colors.fg1 },
  sectionLabel: { ...type.label, color: colors.fg3, marginBottom: 8, marginLeft: 4 },
  rawText: { fontSize: 12, color: colors.fg2, lineHeight: 18 },
});

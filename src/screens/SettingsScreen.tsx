import React, { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Switch, View } from 'react-native';
import { DhanText as Text } from '../components/DhanText';
import { useFocusEffect, useNavigation, CommonActions } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { type } from '../theme/type';
import { db, CaptureEvent } from '../native/DhanDb';
import { permissions } from '../native/DhanPermissions';
import { userPrefs } from '../native/UserPrefs';
import { entitlement, EntitlementStatus } from '../native/Entitlement';
import { DhanCard } from '../components/Card';
import { DhanButton } from '../components/Button';
import { dateLabel, timeLabel, dayGroupLabel } from '../utils/format';
import { RootStackParamList } from '../navigation/RootNavigator';

type Nav = NativeStackNavigationProp<RootStackParamList>;

export function SettingsScreen() {
  const navigation = useNavigation<Nav>();
  const [name, setName] = useState('');
  const [smsGranted, setSmsGranted] = useState(false);
  const [notifGranted, setNotifGranted] = useState(false);
  const [events, setEvents] = useState<CaptureEvent[]>([]);
  const [scanning, setScanning] = useState(false);
  const [relabeling, setRelabeling] = useState(false);
  const [plusStatus, setPlusStatus] = useState<EntitlementStatus | null>(null);

  const load = useCallback(() => {
    userPrefs.getUserName().then((n) => setName(n ?? ''));
    permissions.hasSmsPermission().then(setSmsGranted);
    permissions.isNotificationListenerEnabled().then(setNotifGranted);
    db.getCaptureEvents().then(setEvents);
    entitlement.getStatus().then(setPlusStatus);
  }, []);

  const toggleSimulatedPlus = async (next: boolean) => {
    await entitlement.setSubscribedForTesting(next);
    load();
  };

  useFocusEffect(load);

  const requestSms = async () => {
    const granted = await permissions.requestSmsPermission();
    setSmsGranted(granted);
  };

  const scanSmsHistory = async () => {
    setScanning(true);
    try {
      const matched = await db.scanHistoricalSms();
      Alert.alert(
        'Scan complete',
        matched > 0
          ? `Found ${matched} transaction${matched === 1 ? '' : 's'} in your SMS history.`
          : 'No bank/UPI transactions found in your SMS history.',
      );
      load();
    } finally {
      setScanning(false);
    }
  };

  const relabelTransactions = async () => {
    setRelabeling(true);
    try {
      const updated = await db.relabelSmsTransactions();
      Alert.alert(
        'Clean-up complete',
        updated > 0
          ? `Updated ${updated} transaction${updated === 1 ? '' : 's'} with better names.`
          : 'Every transaction already has its best available name.',
      );
      load();
    } finally {
      setRelabeling(false);
    }
  };

  const signOut = () => {
    Alert.alert('Sign out of Dhan?', 'Your data stays on this device.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Sign out',
        style: 'destructive',
        onPress: async () => {
          await userPrefs.clear();
          navigation.dispatch(CommonActions.reset({ index: 0, routes: [{ name: 'Welcome' }] }));
        },
      },
    ]);
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>More</Text>

      <DhanCard style={{ marginBottom: 18 }}>
        <Text style={styles.name}>{name || 'Dhan user'}</Text>
      </DhanCard>

      <Text style={styles.sectionLabel}>CAPTURE</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <View style={styles.row}>
          <View style={{ flex: 1 }}>
            <Text style={styles.rowLabel}>SMS capture</Text>
            <Text style={styles.rowValue}>{smsGranted ? 'On' : 'Off'}</Text>
          </View>
          {!smsGranted && <DhanButton text="Grant" size="sm" onPress={requestSms} />}
        </View>
        <View style={[styles.row, { marginTop: 14 }]}>
          <View style={{ flex: 1 }}>
            <Text style={styles.rowLabel}>Notification capture</Text>
            <Text style={styles.rowValue}>{notifGranted ? 'On' : 'Off'}</Text>
          </View>
          {!notifGranted && <DhanButton text="Grant" size="sm" onPress={permissions.openNotificationListenerSettings} />}
        </View>
        {smsGranted && (
          <View style={[styles.row, { marginTop: 14 }]}>
            <View style={{ flex: 1 }}>
              <Text style={styles.rowLabel}>SMS history</Text>
              <Text style={styles.rowValue}>Scan past messages for transactions missed before</Text>
            </View>
            <DhanButton text="Scan now" size="sm" variant="secondary" loading={scanning} onPress={scanSmsHistory} />
          </View>
        )}
        <View style={[styles.row, { marginTop: 14 }]}>
          <View style={{ flex: 1 }}>
            <Text style={styles.rowLabel}>Transaction labels</Text>
            <Text style={styles.rowValue}>Clean up any old rows still showing a raw SMS sender ID as the title</Text>
          </View>
          <DhanButton text="Clean up" size="sm" variant="secondary" loading={relabeling} onPress={relabelTransactions} />
        </View>
      </DhanCard>

      <Text style={styles.sectionLabel}>DHAN PLUS</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <View style={styles.row}>
          <View style={{ flex: 1 }}>
            <Text style={styles.rowLabel}>Status</Text>
            <Text style={styles.rowValue}>{plusStatus ? describePlusStatus(plusStatus) : '…'}</Text>
          </View>
        </View>
        <View style={[styles.row, { marginTop: 14 }]}>
          <View style={{ flex: 1 }}>
            <Text style={styles.rowLabel}>Simulate Dhan Plus subscription</Text>
            <Text style={styles.rowValue}>Dev-only stand-in for Razorpay — not a real payment.</Text>
          </View>
          <Switch value={plusStatus?.subscribed ?? false} onValueChange={toggleSimulatedPlus} trackColor={{ true: colors.navy }} />
        </View>
      </DhanCard>

      <Text style={styles.sectionLabel}>DATA & PRIVACY</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <View style={styles.row}>
          <View style={{ flex: 1 }}>
            <Text style={styles.rowLabel}>Backup</Text>
            <Text style={styles.rowValue}>Back up to Google Drive — free for everyone</Text>
          </View>
          <DhanButton text="Open" size="sm" variant="secondary" onPress={() => navigation.navigate('BackupSettings')} />
        </View>
      </DhanCard>

      <Text style={styles.sectionLabel}>ACTIVITY ({events.length} scanned)</Text>
      <DhanCard style={{ marginBottom: 18 }} padding={8}>
        {events.length === 0 ? (
          <Text style={styles.empty}>No activity yet. Turn on capture above to see scanned messages here.</Text>
        ) : (
          events.slice(0, 15).map((e, i) => (
            <View key={e.id} style={[styles.eventRow, i !== Math.min(events.length, 15) - 1 && styles.divider]}>
              <Text style={styles.eventTitle} numberOfLines={1}>
                {e.matched ? '✅ Transaction detected' : '· Not a transaction'} · {e.sourceApp ?? e.source}
              </Text>
              <Text style={styles.eventTime}>{dayGroupLabel(e.timestampMillis)} {timeLabel(e.timestampMillis)}</Text>
            </View>
          ))
        )}
      </DhanCard>

      <DhanButton text="Sign out" variant="destructive" full onPress={signOut} />
    </ScrollView>
  );
}

function describePlusStatus(s: EntitlementStatus): string {
  if (s.subscribed) {
    return s.billingStartsMillis && Date.now() < s.billingStartsMillis
      ? `Dhan Plus — bonus period, billing starts ${dateLabel(s.billingStartsMillis)}`
      : 'Dhan Plus';
  }
  if (s.inTrial) {
    const daysLeft = Math.max(0, Math.ceil((s.trialEndsMillis - Date.now()) / (1000 * 60 * 60 * 24)));
    return `Free trial — ${daysLeft} day${daysLeft === 1 ? '' : 's'} left`;
  }
  return 'Free';
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 16, paddingBottom: 40 },
  title: { ...type.h1, color: colors.fg1, marginBottom: 14 },
  name: { fontSize: 16, fontWeight: '700', color: colors.fg1 },
  sectionLabel: { ...type.label, color: colors.fg3, marginBottom: 8, marginLeft: 4 },
  row: { flexDirection: 'row', alignItems: 'center' },
  rowLabel: { fontSize: 14, fontWeight: '600', color: colors.fg1 },
  rowValue: { fontSize: 12, color: colors.fg3, marginTop: 2 },
  empty: { color: colors.fg3, fontSize: 13, padding: 16, textAlign: 'center' },
  eventRow: { paddingVertical: 10 },
  divider: { borderBottomWidth: 1, borderBottomColor: colors.borderSubtle },
  eventTitle: { fontSize: 13, fontWeight: '600', color: colors.fg1 },
  eventTime: { fontSize: 11, color: colors.fg3, marginTop: 2 },
});

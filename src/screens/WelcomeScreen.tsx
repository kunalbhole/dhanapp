import React, { useState } from 'react';
import { Alert, Pressable, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { type } from '../theme/type';
import { DhanField } from '../components/Field';
import { DhanButton } from '../components/Button';
import { userPrefs } from '../native/UserPrefs';
import { permissions } from '../native/DhanPermissions';
import { db } from '../native/DhanDb';
import { backup, BackupErrorCode } from '../native/DhanBackup';
import { entitlement } from '../native/Entitlement';
import { dayGroupLabel, formatBytes, monthKey, timeLabel } from '../utils/format';
import { RootStackParamList } from '../navigation/RootNavigator';

type Props = NativeStackScreenProps<RootStackParamList, 'Welcome'>;

/**
 * Consolidates the source design's Splash + Onboarding + SignUp + Permissions + Login
 * screens into one screen. This rewrite is scoped to the core money-tracking loop, not
 * a full re-port of all 26 screens a second time — see the project README.
 *
 * Restore-from-backup is offered here as an optional secondary path (sign in -> check ->
 * confirm -> enter passphrase), not a mandatory login gate — the app has no account system
 * otherwise, and forcing Google sign-in on every fresh install would contradict that.
 */
export function WelcomeScreen({ navigation }: Props) {
  const [name, setName] = useState('');
  const [incomeText, setIncomeText] = useState('');
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('');

  const [showRestoreEntry, setShowRestoreEntry] = useState(false);
  const [restorePassphrase, setRestorePassphrase] = useState('');
  const [restoring, setRestoring] = useState(false);
  const [restoreStatus, setRestoreStatus] = useState('');

  /** Shared by normal setup and the "backup had no transactions" restore fallback —
   *  requests SMS permission and, if granted, runs the one-time historical scan. */
  const runCaptureSetup = async () => {
    setStatus('Requesting SMS permission…');
    const granted = await permissions.requestSmsPermission();
    if (granted) {
      setStatus('Scanning SMS for past transactions…');
      await db.scanHistoricalSms();
      await seedDefaultBudgets();
    }
  };

  const finish = async () => {
    setBusy(true);
    try {
      await userPrefs.setUserName(name.trim() || 'there');

      const income = Number(incomeText);
      if (income > 0) {
        await db.addTransaction('Salary', 'Monthly income', income, 'income', Date.now(), 'MANUAL');
      }

      await runCaptureSetup();

      await entitlement.ensureTrialStarted();
      await userPrefs.setOnboarded();
      navigation.reset({ index: 0, routes: [{ name: 'Main' }] });
    } finally {
      setBusy(false);
      setStatus('');
    }
  };

  const startRestore = async () => {
    setRestoring(true);
    setRestoreStatus('Signing in with Google…');
    try {
      await backup.signIn();
      setRestoreStatus('Checking for an existing backup…');
      const info = await backup.checkForExistingBackup();
      setRestoring(false);
      setRestoreStatus('');
      if (!info) {
        Alert.alert('No backup found', 'This Google account has no Dhan backup yet. You can continue with normal setup below.');
        return;
      }
      Alert.alert(
        'Restore your data?',
        `A backup from ${dayGroupLabel(info.modifiedTimeMillis)}, ${timeLabel(info.modifiedTimeMillis)} (${formatBytes(info.sizeBytes)}) was found for this account.`,
        [
          { text: 'Not now', style: 'cancel' },
          { text: 'Restore', onPress: () => setShowRestoreEntry(true) },
        ],
      );
    } catch {
      setRestoring(false);
      setRestoreStatus('');
      Alert.alert('Could not check for a backup', 'Sign-in or network error. You can try again or continue with normal setup below.');
    }
  };

  const confirmRestore = async () => {
    if (!restorePassphrase) return;
    setRestoring(true);
    setRestoreStatus('Restoring your data…');
    try {
      const result = await backup.restoreFromBackup(restorePassphrase);
      await userPrefs.setUserName(name.trim() || 'there');
      if (result.restoredTransactionCount === 0) {
        // Backup existed but had no transactions in it — fall back to the normal capture
        // setup rather than leaving the app in an empty state with onboarding "done".
        await runCaptureSetup();
      }
      await entitlement.ensureTrialStarted();
      await userPrefs.setOnboarded();
      navigation.reset({ index: 0, routes: [{ name: 'Main' }] });
    } catch (e) {
      const code = (e as { code?: string }).code;
      if (code === BackupErrorCode.DECRYPT_FAILED) {
        Alert.alert('Wrong passphrase', 'That passphrase could not decrypt the backup. Try again, or continue with normal setup instead.');
      } else {
        Alert.alert('Restore failed', 'Could not restore your backup. You can continue with normal setup instead.');
      }
    } finally {
      setRestoring(false);
      setRestoreStatus('');
    }
  };

  if (showRestoreEntry) {
    return (
      <SafeAreaView style={styles.screen}>
        <View style={styles.content}>
          <Text style={styles.title}>Enter backup passphrase</Text>
          <Text style={styles.subtitle}>Enter the passphrase you set when you turned on backup, to decrypt your data.</Text>
          <DhanField label="Backup passphrase" value={restorePassphrase} onChangeText={setRestorePassphrase} placeholder="Passphrase" secureTextEntry />
          <DhanButton text="Restore" onPress={confirmRestore} full loading={restoring} size="lg" />
          {restoring && !!restoreStatus && <Text style={styles.status}>{restoreStatus}</Text>}
          <Pressable onPress={() => setShowRestoreEntry(false)} style={{ marginTop: 16 }}>
            <Text style={styles.link}>Cancel and set up fresh instead</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }

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

        <Pressable onPress={startRestore} disabled={restoring} style={{ marginTop: 18 }}>
          <Text style={styles.link}>{restoring ? restoreStatus || 'Checking…' : 'Already have a Dhan backup? Restore instead'}</Text>
        </Pressable>
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
  link: { ...type.caption, color: colors.navy, textAlign: 'center', fontWeight: '600' },
});

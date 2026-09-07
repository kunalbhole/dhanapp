import React, { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { colors } from '../theme/colors';
import { type } from '../theme/type';
import { backup, BackupErrorCode, BackupFrequency, LastBackupStatus } from '../native/DhanBackup';
import { dayGroupLabel, formatBytes, timeLabel } from '../utils/format';
import { DhanCard } from '../components/Card';
import { DhanButton } from '../components/Button';
import { DhanChip } from '../components/Chips';
import { DhanField } from '../components/Field';

const FREQUENCIES: { key: BackupFrequency; label: string }[] = [
  { key: 'DAILY', label: 'Daily' },
  { key: 'WEEKLY', label: 'Weekly' },
  { key: 'MONTHLY', label: 'Monthly' },
  { key: 'MANUAL', label: 'Manual only' },
];

export function BackupSettingsScreen() {
  const [email, setEmail] = useState<string | null>(null);
  const [signingIn, setSigningIn] = useState(false);
  const [hasPassphrase, setHasPassphrase] = useState(false);
  const [showPassphraseForm, setShowPassphraseForm] = useState(false);
  const [passphrase, setPassphrase] = useState('');
  const [confirmPassphrase, setConfirmPassphrase] = useState('');
  const [frequency, setFrequency] = useState<BackupFrequency>('MANUAL');
  const [wifiOnly, setWifiOnly] = useState(true);
  const [lastStatus, setLastStatus] = useState<LastBackupStatus | null>(null);
  const [backingUp, setBackingUp] = useState(false);

  const load = useCallback(() => {
    backup.getSignedInEmail().then(setEmail);
    backup.hasPassphraseSet().then(setHasPassphrase);
    backup.getSettings().then((s) => {
      setFrequency(s.frequency);
      setWifiOnly(s.wifiOnly);
    });
    backup.getLastStatus().then(setLastStatus);
  }, []);

  useFocusEffect(load);

  const signIn = async () => {
    setSigningIn(true);
    try {
      const signedInEmail = await backup.signIn();
      setEmail(signedInEmail);
    } catch {
      Alert.alert('Sign-in failed', 'Could not sign in with Google. Please try again.');
    } finally {
      setSigningIn(false);
    }
  };

  const signOut = () => {
    Alert.alert('Sign out of Google?', 'Scheduled and manual backups will stop until you sign in again.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Sign out',
        style: 'destructive',
        onPress: async () => {
          await backup.signOut();
          setEmail(null);
        },
      },
    ]);
  };

  const savePassphrase = async () => {
    if (passphrase.length < 6) {
      Alert.alert('Too short', 'Use at least 6 characters for your backup passphrase.');
      return;
    }
    if (passphrase !== confirmPassphrase) {
      Alert.alert("Passphrases don't match", 'Re-enter both fields to match.');
      return;
    }
    await backup.setPassphrase(passphrase);
    setPassphrase('');
    setConfirmPassphrase('');
    setShowPassphraseForm(false);
    setHasPassphrase(true);
    backupNow();
  };

  const backupNow = async () => {
    if (!email) {
      Alert.alert('Sign in required', 'Sign in with Google first to back up to Drive.');
      return;
    }
    if (!hasPassphrase) {
      Alert.alert('Set a passphrase first', 'Set a backup passphrase below before backing up.');
      setShowPassphraseForm(true);
      return;
    }
    setBackingUp(true);
    try {
      await backup.backupNow();
      Alert.alert('Backed up', 'Your data was backed up to Google Drive.');
    } catch (e) {
      const code = (e as { code?: string }).code;
      if (code === BackupErrorCode.NOT_SIGNED_IN) {
        Alert.alert('Sign in required', 'Sign in with Google first to back up to Drive.');
      } else if (code === BackupErrorCode.PASSPHRASE_NOT_SET) {
        Alert.alert('Set a passphrase first', 'Set a backup passphrase below before backing up.');
      } else {
        Alert.alert('Backup failed', 'Check your connection and try again.');
      }
    } finally {
      setBackingUp(false);
      load();
    }
  };

  const changeFrequency = async (next: BackupFrequency) => {
    setFrequency(next);
    await backup.setSettings(next, wifiOnly);
  };

  const changeWifiOnly = async (next: boolean) => {
    setWifiOnly(next);
    await backup.setSettings(frequency, next);
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.freeNote}>Backup is free for everyone — none of this is gated behind Dhan Plus.</Text>

      <Text style={styles.sectionLabel}>GOOGLE ACCOUNT</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        {email ? (
          <View style={styles.row}>
            <View style={{ flex: 1 }}>
              <Text style={styles.rowLabel}>Signed in</Text>
              <Text style={styles.rowValue}>{email}</Text>
            </View>
            <DhanButton text="Sign out" size="sm" variant="secondary" onPress={signOut} />
          </View>
        ) : (
          <>
            <Text style={styles.rowValue}>Sign in to back up your data to your own Google Drive.</Text>
            <DhanButton text="Sign in with Google" onPress={signIn} loading={signingIn} full style={{ marginTop: 12 }} />
          </>
        )}
      </DhanCard>

      <Text style={styles.sectionLabel}>BACKUP PASSPHRASE</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        {!showPassphraseForm ? (
          <View style={styles.row}>
            <View style={{ flex: 1 }}>
              <Text style={styles.rowLabel}>{hasPassphrase ? 'Passphrase set' : 'Not set'}</Text>
              <Text style={styles.rowValue}>Encrypts your backup before it leaves the device.</Text>
            </View>
            <DhanButton text={hasPassphrase ? 'Change' : 'Set up'} size="sm" onPress={() => setShowPassphraseForm(true)} />
          </View>
        ) : (
          <>
            <Text style={styles.warning}>
              If you forget this passphrase, your backup cannot be recovered — not by anyone, including us.
              Write it down somewhere safe.
            </Text>
            <DhanField label="New passphrase" value={passphrase} onChangeText={setPassphrase} placeholder="At least 6 characters" secureTextEntry />
            <DhanField label="Confirm passphrase" value={confirmPassphrase} onChangeText={setConfirmPassphrase} placeholder="Re-enter" secureTextEntry />
            <View style={{ flexDirection: 'row', gap: 8 }}>
              <DhanButton text="Cancel" variant="secondary" style={{ flex: 1 }} onPress={() => setShowPassphraseForm(false)} />
              <DhanButton text="Save" style={{ flex: 1 }} onPress={savePassphrase} />
            </View>
          </>
        )}
      </DhanCard>

      <Text style={styles.sectionLabel}>FREQUENCY</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <View style={styles.chipWrap}>
          {FREQUENCIES.map((f) => (
            <DhanChip key={f.key} label={f.label} active={frequency === f.key} onPress={() => changeFrequency(f.key)} />
          ))}
        </View>
      </DhanCard>

      <Text style={styles.sectionLabel}>NETWORK</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        <View style={styles.row}>
          <View style={{ flex: 1 }}>
            <Text style={styles.rowLabel}>Wi-Fi only</Text>
            <Text style={styles.rowValue}>Scheduled backups wait for Wi-Fi. "Back up now" always runs.</Text>
          </View>
          <Switch value={wifiOnly} onValueChange={changeWifiOnly} trackColor={{ true: colors.navy }} />
        </View>
      </DhanCard>

      <Text style={styles.sectionLabel}>STATUS</Text>
      <DhanCard style={{ marginBottom: 18 }}>
        {lastStatus && lastStatus.timestampMillis > 0 ? (
          <Text style={styles.rowValue}>
            Last backed up: {dayGroupLabel(lastStatus.timestampMillis)}, {timeLabel(lastStatus.timestampMillis)} ·{' '}
            {formatBytes(lastStatus.sizeBytes)}
          </Text>
        ) : (
          <Text style={styles.rowValue}>Never backed up yet.</Text>
        )}
        {lastStatus?.failed && (
          <DhanButton text="Last backup failed — tap to retry" variant="destructive" size="sm" onPress={backupNow} style={{ marginTop: 10 }} />
        )}
      </DhanCard>

      <DhanButton text="Back up now" full size="lg" loading={backingUp} onPress={backupNow} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.appBg },
  content: { padding: 16, paddingBottom: 40 },
  freeNote: { ...type.caption, color: colors.fg3, marginBottom: 16, textAlign: 'center' },
  sectionLabel: { ...type.label, color: colors.fg3, marginBottom: 8, marginLeft: 4 },
  row: { flexDirection: 'row', alignItems: 'center' },
  rowLabel: { fontSize: 14, fontWeight: '600', color: colors.fg1 },
  rowValue: { fontSize: 12, color: colors.fg3, marginTop: 2, lineHeight: 17 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  warning: { fontSize: 12, color: colors.expense, backgroundColor: colors.expenseBg, padding: 10, borderRadius: 8, marginBottom: 12, lineHeight: 17 },
});

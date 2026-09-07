import { NativeModules } from 'react-native';

const { DhanBackup } = NativeModules;

export type BackupFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'MANUAL';

export interface BackupSettings {
  frequency: BackupFrequency;
  wifiOnly: boolean;
}

export interface BackupResult {
  timestampMillis: number;
  sizeBytes: number;
}

export interface ExistingBackupInfo {
  modifiedTimeMillis: number;
  sizeBytes: number;
}

export interface RestoreResult {
  restoredTransactionCount: number;
}

export interface LastBackupStatus {
  timestampMillis: number;
  sizeBytes: number;
  failed: boolean;
  errorMessage: string | null;
}

/** Error codes the native side rejects promises with — check `(err as { code?: string }).code`. */
export const BackupErrorCode = {
  NOT_SIGNED_IN: 'dhan_not_signed_in',
  BACKUP_NOT_FOUND: 'dhan_backup_not_found',
  DECRYPT_FAILED: 'dhan_decrypt_failed',
  PASSPHRASE_NOT_SET: 'dhan_passphrase_not_set',
  GENERIC: 'dhan_backup_error',
} as const;

/** Typed wrapper over the native DhanBackup module (Google Sign-In + Drive appDataFolder
 *  backup/restore). See android/app/src/main/java/com/dhan/app/backup/ for the native side
 *  and PROJECT_STATUS.md for the architecture (local SQLite is always the source of truth;
 *  Drive is a backup/restore destination only, never a live sync target). */
export const backup = {
  isSignedIn: (): Promise<boolean> => DhanBackup.isSignedIn(),
  getSignedInEmail: (): Promise<string | null> => DhanBackup.getSignedInEmail(),
  signIn: (): Promise<string> => DhanBackup.signIn(),
  signOut: (): Promise<void> => DhanBackup.signOut(),

  hasPassphraseSet: (): Promise<boolean> => DhanBackup.hasBackupPassphraseSet(),
  setPassphrase: (passphrase: string): Promise<void> => DhanBackup.setBackupPassphrase(passphrase),

  /** Uses the passphrase already cached on this device — rejects with
   *  BackupErrorCode.PASSPHRASE_NOT_SET if setPassphrase hasn't been called here yet. */
  backupNow: (): Promise<BackupResult> => DhanBackup.backupNow(),
  checkForExistingBackup: (): Promise<ExistingBackupInfo | null> => DhanBackup.checkForExistingBackup(),
  /** For restoring on a fresh device, where nothing is cached yet — passphrase must be
   *  typed in by hand. */
  restoreFromBackup: (passphrase: string): Promise<RestoreResult> => DhanBackup.restoreFromBackup(passphrase),

  getSettings: (): Promise<BackupSettings> => DhanBackup.getBackupSettings(),
  setSettings: (frequency: BackupFrequency, wifiOnly: boolean): Promise<void> =>
    DhanBackup.setBackupSettings(frequency, wifiOnly),
  getLastStatus: (): Promise<LastBackupStatus> => DhanBackup.getLastBackupStatus(),
};

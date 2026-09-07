import { NativeModules } from 'react-native';

const { DhanDb } = NativeModules;

export type TxSource = 'MANUAL' | 'SMS' | 'NOTIFICATION';

export interface Transaction {
  id: number;
  merchant: string;
  note: string | null;
  amount: number; // signed: + income, - expense
  category: string;
  timestampMillis: number;
  source: TxSource;
  sourceApp: string | null;
  rawText: string | null;
  accountHint: string | null;
}

export interface Budget {
  id: number;
  category: string;
  monthKey: string;
  limitAmount: number;
}

export type BillStatus = 'UPCOMING' | 'DUE_SOON' | 'OVERDUE' | 'PAID';

export interface Bill {
  id: number;
  name: string;
  amount: number;
  dueDateMillis: number;
  status: BillStatus;
  repeatMonthly: number;
  category: string;
}

export interface Goal {
  id: number;
  name: string;
  targetAmount: number;
  savedAmount: number;
  targetDateMillis: number | null;
}

export interface Friend {
  id: number;
  name: string;
  balance: number;
}

export interface CaptureEvent {
  id: number;
  source: TxSource;
  sourceApp: string | null;
  rawText: string;
  timestampMillis: number;
  matched: number;
  createdTxnId: number | null;
}

/** Typed wrapper over the native DhanDb module (Kotlin/SQLite) — every native read comes
 *  back as a JSON string, parsed here so the rest of the app deals in real types. */
export const db = {
  getTransactions: async (): Promise<Transaction[]> => JSON.parse(await DhanDb.getTransactions()),
  addTransaction: (merchant: string, note: string | null, amount: number, category: string, timestampMillis: number, source: TxSource): Promise<number> =>
    DhanDb.addTransaction(merchant, note, amount, category, timestampMillis, source),
  deleteTransaction: (id: number): Promise<void> => DhanDb.deleteTransaction(id),

  getCaptureEvents: async (): Promise<CaptureEvent[]> => JSON.parse(await DhanDb.getCaptureEvents()),

  getBudgets: async (monthKey: string): Promise<Budget[]> => JSON.parse(await DhanDb.getBudgets(monthKey)),
  setBudget: (category: string, monthKey: string, limitAmount: number): Promise<void> =>
    DhanDb.setBudget(category, monthKey, limitAmount),

  getBills: async (): Promise<Bill[]> => JSON.parse(await DhanDb.getBills()),
  addBill: (name: string, amount: number, dueDateMillis: number, status: BillStatus, repeatMonthly: boolean, category: string): Promise<number> =>
    DhanDb.addBill(name, amount, dueDateMillis, status, repeatMonthly, category),
  setBillStatus: (id: number, status: BillStatus): Promise<void> => DhanDb.setBillStatus(id, status),

  getGoals: async (): Promise<Goal[]> => JSON.parse(await DhanDb.getGoals()),
  addGoal: (name: string, targetAmount: number, savedAmount: number): Promise<number> =>
    DhanDb.addGoal(name, targetAmount, savedAmount),
  updateGoalSaved: (id: number, savedAmount: number): Promise<void> => DhanDb.updateGoalSaved(id, savedAmount),

  getFriends: async (): Promise<Friend[]> => JSON.parse(await DhanDb.getFriends()),

  /** One-time scan of the device's existing SMS inbox. Returns how many messages matched
   *  and became transactions. Requires SMS permission to already be granted. */
  scanHistoricalSms: (): Promise<number> => DhanDb.scanHistoricalSms(),
};

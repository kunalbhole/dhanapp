import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY_TRIAL_START = 'dhan.entitlement.trialStartMillis';
const KEY_SUBSCRIBED = 'dhan.entitlement.subscribed';
const KEY_SUBSCRIPTION_START = 'dhan.entitlement.subscriptionStartMillis';

const TRIAL_MONTHS = 3;
const BONUS_MONTHS = 3;

export interface EntitlementStatus {
  /** Whichever of trial or an active subscription is true — the single check every
   *  Plus-gated feature (Currency multi-select, Edit categories, unlimited groups, ...)
   *  should use. */
  isPlusUser: boolean;
  inTrial: boolean;
  trialEndsMillis: number;
  subscribed: boolean;
  /** When regular billing would start (subscription start + bonus months) — display-only
   *  until real payments exist; nothing here actually charges anyone. */
  billingStartsMillis: number | null;
}

function addMonths(millis: number, months: number): number {
  const d = new Date(millis);
  d.setMonth(d.getMonth() + months);
  return d.getTime();
}

/**
 * Local-only Dhan Plus entitlement state — no backend call, matching the rest of this
 * app's local-first architecture. Implements the product model from CLAUDE.md: every new
 * user gets a 3-month free trial with no payment required; if they subscribe (whenever
 * that happens, in or after the trial), they get 3 more bonus months before regular
 * ₹199/month billing would start; non-subscribers just revert to the free tier once their
 * trial ends. Razorpay isn't wired up yet — [setSubscribedForTesting] is a stand-in so
 * Plus-gated UI can be built and tested now; swap it for a real webhook-driven subscribed
 * flag when payments land. Trial/subscription state lives in AsyncStorage like UserPrefs,
 * so (like the display name) it is device-local and not included in Drive backup/restore
 * — restoring onto a new device currently starts a fresh trial there, a known limitation.
 */
export const entitlement = {
  /** Call once, at the moment onboarding completes (any path — fresh setup or restore) —
   *  a no-op if the trial has already started on this device. */
  ensureTrialStarted: async (): Promise<void> => {
    const existing = await AsyncStorage.getItem(KEY_TRIAL_START);
    if (existing == null) {
      await AsyncStorage.setItem(KEY_TRIAL_START, String(Date.now()));
    }
  },

  getStatus: async (): Promise<EntitlementStatus> => {
    const [trialStartStr, subscribedStr, subStartStr] = await Promise.all([
      AsyncStorage.getItem(KEY_TRIAL_START),
      AsyncStorage.getItem(KEY_SUBSCRIBED),
      AsyncStorage.getItem(KEY_SUBSCRIPTION_START),
    ]);
    const trialStart = trialStartStr ? Number(trialStartStr) : Date.now();
    const trialEndsMillis = addMonths(trialStart, TRIAL_MONTHS);
    const subscribed = subscribedStr === '1';
    const subscriptionStart = subStartStr ? Number(subStartStr) : null;
    const billingStartsMillis = subscribed && subscriptionStart ? addMonths(subscriptionStart, BONUS_MONTHS) : null;
    const inTrial = Date.now() < trialEndsMillis;
    return {
      isPlusUser: subscribed || inTrial,
      inTrial,
      trialEndsMillis,
      subscribed,
      billingStartsMillis,
    };
  },

  /** STUB for the real Razorpay subscribe/cancel flow — see module doc comment. */
  setSubscribedForTesting: async (subscribed: boolean): Promise<void> => {
    await AsyncStorage.setItem(KEY_SUBSCRIBED, subscribed ? '1' : '0');
    if (subscribed) {
      const existing = await AsyncStorage.getItem(KEY_SUBSCRIPTION_START);
      if (existing == null) {
        await AsyncStorage.setItem(KEY_SUBSCRIPTION_START, String(Date.now()));
      }
    } else {
      await AsyncStorage.removeItem(KEY_SUBSCRIPTION_START);
    }
  },
};

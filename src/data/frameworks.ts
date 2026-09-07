import { CategoryKey, CATEGORIES } from '../theme/colors';

export interface FrameworkBucket {
  key: string;
  name: string;
  percent: number;
  /** Spending categories that roll up into this bucket. Empty for a non-spending bucket
   *  (e.g. "Savings", "Retirement") that represents money set aside rather than spent. */
  categories: CategoryKey[];
}

export interface Framework {
  key: string;
  name: string;
  description: string;
  buckets: FrameworkBucket[];
  /** Envelope/Zero-Based don't split by percentage — every spending category is its own
   *  bucket with a directly-set amount. EditBudgetScreen shows a plain amount field per
   *  bucket instead of a percent, and Zero-Based additionally shows an "unallocated"
   *  total check. */
  isPerCategory?: boolean;
}

const SPENDING_CATEGORIES = (Object.keys(CATEGORIES) as CategoryKey[]).filter((c) => c !== 'income');

function perCategoryBuckets(): FrameworkBucket[] {
  const evenPercent = Math.round((100 / SPENDING_CATEGORIES.length) * 100) / 100;
  return SPENDING_CATEGORIES.map((c) => ({
    key: c,
    name: CATEGORIES[c].name,
    percent: evenPercent,
    categories: [c],
  }));
}

/**
 * Predefined budgeting frameworks. 50/30/20 is the default; the spec calls for "five
 * alternative framework options" without naming them, so this set (70/20/10, 80/20 Pay
 * Yourself First, the 60% Solution, Envelope System, Zero-Based) is a reasonable,
 * well-known real-world choice standing in for whatever the actual design spec names —
 * flagged in PROJECT_STATUS.md, easy to swap once the real list is known.
 */
export const FRAMEWORKS: Framework[] = [
  {
    key: '50-30-20',
    name: '50/30/20',
    description: '50% needs, 30% wants, 20% savings — the default framework.',
    buckets: [
      { key: 'needs', name: 'Needs', percent: 50, categories: ['rent', 'bills', 'groceries', 'health', 'transport'] },
      { key: 'wants', name: 'Wants', percent: 30, categories: ['food', 'shopping', 'ent', 'travel', 'edu'] },
      { key: 'savings', name: 'Savings', percent: 20, categories: [] },
    ],
  },
  {
    key: '70-20-10',
    name: '70/20/10',
    description: '70% living expenses, 20% savings, 10% debt payoff or giving.',
    buckets: [
      { key: 'living', name: 'Living Expenses', percent: 70, categories: ['rent', 'bills', 'groceries', 'health', 'transport', 'food'] },
      { key: 'savings', name: 'Savings', percent: 20, categories: [] },
      { key: 'debt-giving', name: 'Debt & Giving', percent: 10, categories: [] },
    ],
  },
  {
    key: '80-20',
    name: '80/20 (Pay Yourself First)',
    description: 'Set aside 20% in savings first, spend the remaining 80% freely.',
    buckets: [
      { key: 'savings-first', name: 'Savings First', percent: 20, categories: [] },
      { key: 'everything-else', name: 'Everything Else', percent: 80, categories: SPENDING_CATEGORIES },
    ],
  },
  {
    key: '60-percent-solution',
    name: '60% Solution',
    description: '60% committed expenses, the rest split across saving and fun.',
    buckets: [
      { key: 'committed', name: 'Committed Expenses', percent: 60, categories: ['rent', 'bills', 'groceries', 'health', 'transport'] },
      { key: 'retirement', name: 'Retirement', percent: 10, categories: [] },
      { key: 'long-term-savings', name: 'Long-Term Savings', percent: 10, categories: [] },
      { key: 'short-term-savings', name: 'Short-Term Savings', percent: 10, categories: [] },
      { key: 'fun', name: 'Fun', percent: 10, categories: ['food', 'shopping', 'ent', 'travel', 'edu'] },
    ],
  },
  {
    key: 'envelope',
    name: 'Envelope System',
    description: 'Every category is its own envelope with a directly-set amount.',
    buckets: perCategoryBuckets(),
    isPerCategory: true,
  },
  {
    key: 'zero-based',
    name: 'Zero-Based',
    description: 'Assign every rupee somewhere — allocations should add up to the total.',
    buckets: perCategoryBuckets(),
    isPerCategory: true,
  },
];

export const DEFAULT_FRAMEWORK_KEY = '50-30-20';

/** Reserved "category" key for a budget's overall monthly cap override, stored in the
 *  same `budgets` table as per-category caps. Never rendered as a category. Must match
 *  the literal hardcoded in DhanDb.kt's clearBudgetCategories. */
export const TOTAL_BUDGET_CATEGORY_KEY = '__total__';

export interface CustomFramework {
  name: string;
  buckets: FrameworkBucket[];
}

/** Splits a total budget amount across a framework's buckets/categories per their
 *  percentages — the "pre-filled default categories per framework, never starts empty"
 *  rule. Shared by Create, Edit (on first total-set or framework switch), and the Custom
 *  builder so the math lives in exactly one place. Non-spending buckets (no categories)
 *  contribute nothing here since there's no category to attach an amount to. */
export function computeDefaultAllocations(fw: Framework, total: number): { category: CategoryKey; amount: number }[] {
  const out: { category: CategoryKey; amount: number }[] = [];
  for (const bucket of fw.buckets) {
    if (bucket.categories.length === 0) continue;
    const perCategory = Math.round(((total * bucket.percent) / 100) / bucket.categories.length);
    for (const c of bucket.categories) {
      out.push({ category: c, amount: perCategory });
    }
  }
  return out;
}

/** Resolves a budget's framework — predefined by key, or a custom one parsed from the
 *  JSON stored on its budget_defs row. Falls back to the default if a custom framework's
 *  JSON is missing/corrupt, rather than crashing the Budget screen. */
export function resolveFramework(frameworkKey: string, customFrameworkJson: string | null): Framework {
  if (frameworkKey === 'custom') {
    if (customFrameworkJson) {
      try {
        const parsed = JSON.parse(customFrameworkJson) as CustomFramework;
        return { key: 'custom', name: parsed.name || 'Custom', description: 'Custom framework', buckets: parsed.buckets };
      } catch {
        // fall through to default below
      }
    }
    return FRAMEWORKS[0];
  }
  return FRAMEWORKS.find((f) => f.key === frameworkKey) ?? FRAMEWORKS[0];
}

// Dhan design system — colors. Ported 1:1 from the source design's colors_and_type.css.
export const colors = {
  navy: '#141C41',
  navy80: '#2A3158',
  navy60: '#6A7091',
  navy40: '#A8ACC1',
  navy20: '#D4D6E0',
  navy10: '#E8EAF0',
  navy05: '#F3F4F8',

  gold: '#C9A84C',
  goldSoft: '#FED977',
  goldBg: '#FBF5E3',

  bgBase: '#FFFFFF',
  bgSurface: '#F5F6FA',
  bgElevated: '#FFFFFF',
  bgOverlay: 'rgba(20,28,65,0.48)',
  appBg: '#EDEEF4',

  fg1: '#141C41',
  fg2: '#4A5172',
  fg3: '#8A90A8',
  fg4: '#B8BCCB',
  fgOnDark: '#FFFFFF',

  borderSubtle: '#EEF0F5',
  borderDefault: '#E1E3EC',
  borderStrong: '#C6CAD8',

  income: '#2E7D5B',
  incomeBg: '#E6F2EC',
  expense: '#C94A3B',
  expenseBg: '#FBEAE7',
  warning: '#D89838',
  warningBg: '#FBF2DF',
  info: '#3B6FD4',
  infoBg: '#E6EEFB',

  catFood: '#E88B5C',
  catTransport: '#6A8FD4',
  catShopping: '#C97BB6',
  catBills: '#7C9B5F',
  catEnt: '#B079D9',
  catHealth: '#5CB4A8',
  catEducation: '#D4A84C',
  catGroceries: '#7FB36B',
  catRent: '#6A8FD4',
  catTravel: '#4F8FAF',
  catOther: '#8A90A8',
} as const;

export type CategoryKey =
  | 'food' | 'transport' | 'shopping' | 'bills' | 'ent' | 'health'
  | 'edu' | 'groceries' | 'rent' | 'travel' | 'income' | 'other';

export const CATEGORIES: Record<CategoryKey, { name: string; color: string }> = {
  food: { name: 'Food', color: colors.catFood },
  transport: { name: 'Transport', color: colors.catTransport },
  shopping: { name: 'Shopping', color: colors.catShopping },
  bills: { name: 'Bills', color: colors.catBills },
  ent: { name: 'Entertainment', color: colors.catEnt },
  health: { name: 'Health', color: colors.catHealth },
  edu: { name: 'Education', color: colors.catEducation },
  groceries: { name: 'Groceries', color: colors.catGroceries },
  rent: { name: 'Rent', color: colors.catRent },
  travel: { name: 'Travel', color: colors.catTravel },
  income: { name: 'Income', color: colors.income },
  other: { name: 'Other', color: colors.catOther },
};

export function categoryFromKey(key: string | null | undefined): CategoryKey {
  const k = (key ?? 'other').toLowerCase();
  return (k in CATEGORIES ? k : 'other') as CategoryKey;
}

import React from 'react';
import { StyleSheet, View } from 'react-native';
import {
  AirplaneIcon,
  ArrowDownIcon,
  BankIcon,
  BasketIcon,
  CarIcon,
  FilmSlateIcon,
  ForkKnifeIcon,
  GraduationCapIcon,
  HouseIcon,
  type Icon,
  PillIcon,
  ReceiptIcon,
  ShoppingBagIcon,
} from 'phosphor-react-native';
import { CATEGORIES, CategoryKey } from '../theme/colors';

interface Props {
  category: CategoryKey;
  size?: number;
  tint?: boolean;
}

// 'other' gets a Bank icon rather than a generic mark — most transactions that land here
// are raw bank/UPI SMS the merchant-name regexes couldn't classify further, so a bank
// glyph reads more true than a catch-all dot.
const CATEGORY_ICONS: Record<CategoryKey, Icon> = {
  food: ForkKnifeIcon,
  transport: CarIcon,
  shopping: ShoppingBagIcon,
  bills: ReceiptIcon,
  ent: FilmSlateIcon,
  health: PillIcon,
  edu: GraduationCapIcon,
  groceries: BasketIcon,
  rent: HouseIcon,
  travel: AirplaneIcon,
  income: ArrowDownIcon,
  other: BankIcon,
};

/** Real Phosphor icons (SVG via react-native-svg — no font-asset linking needed) in a
 *  colored circle, replacing the earlier emoji-glyph stand-in. */
export function CategoryIcon({ category, size = 40, tint = false }: Props) {
  const meta = CATEGORIES[category];
  const IconComponent = CATEGORY_ICONS[category];
  const bg = tint ? meta.color + '22' : meta.color;
  const iconColor = tint ? meta.color : '#fff';
  return (
    <View style={[styles.circle, { width: size, height: size, borderRadius: size * 0.3, backgroundColor: bg }]}>
      <IconComponent size={size * 0.52} color={iconColor} weight="regular" />
    </View>
  );
}

const styles = StyleSheet.create({
  circle: { alignItems: 'center', justifyContent: 'center' },
});

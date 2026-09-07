import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { CATEGORIES, CategoryKey } from '../theme/colors';

interface Props {
  category: CategoryKey;
  size?: number;
  tint?: boolean;
}

/** Emoji glyph in a colored circle — stands in for the source design's Phosphor icon
 *  set. No icon-font library here deliberately: react-native-vector-icons needs native
 *  font-asset gradle wiring, which is exactly the kind of extra native-build risk this
 *  rewrite is trying to avoid. */
export function CategoryIcon({ category, size = 40, tint = false }: Props) {
  const meta = CATEGORIES[category];
  const bg = tint ? meta.color + '22' : meta.color;
  return (
    <View style={[styles.circle, { width: size, height: size, borderRadius: size * 0.3, backgroundColor: bg }]}>
      <Text style={{ fontSize: size * 0.48 }}>{meta.glyph}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  circle: { alignItems: 'center', justifyContent: 'center' },
});

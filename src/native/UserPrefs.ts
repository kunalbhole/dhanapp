import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY_USER_NAME = 'dhan.userName';
const KEY_ONBOARDED = 'dhan.hasOnboarded';

export const userPrefs = {
  getUserName: () => AsyncStorage.getItem(KEY_USER_NAME),
  setUserName: (name: string) => AsyncStorage.setItem(KEY_USER_NAME, name),
  hasOnboarded: async () => (await AsyncStorage.getItem(KEY_ONBOARDED)) === '1',
  setOnboarded: () => AsyncStorage.setItem(KEY_ONBOARDED, '1'),
  clear: () => AsyncStorage.removeMany([KEY_USER_NAME, KEY_ONBOARDED]),
};

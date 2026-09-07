import { NativeModules, PermissionsAndroid, Platform } from 'react-native';

const { DhanPermissions } = NativeModules;

/** SMS is a normal Android runtime permission — request it via RN's built-in
 *  PermissionsAndroid, no native bridge needed. Notification-listener access has no
 *  PermissionsAndroid entry (it's not a runtime permission), so that half goes through
 *  the native DhanPermissions module. */
export const permissions = {
  hasSmsPermission: async (): Promise<boolean> => {
    if (Platform.OS !== 'android') return false;
    const receive = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.RECEIVE_SMS);
    const read = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.READ_SMS);
    return receive && read;
  },
  requestSmsPermission: async (): Promise<boolean> => {
    const result = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.RECEIVE_SMS,
      PermissionsAndroid.PERMISSIONS.READ_SMS,
    ]);
    return (
      result[PermissionsAndroid.PERMISSIONS.RECEIVE_SMS] === PermissionsAndroid.RESULTS.GRANTED &&
      result[PermissionsAndroid.PERMISSIONS.READ_SMS] === PermissionsAndroid.RESULTS.GRANTED
    );
  },
  isNotificationListenerEnabled: (): Promise<boolean> => DhanPermissions.isNotificationListenerEnabled(),
  openNotificationListenerSettings: (): void => DhanPermissions.openNotificationListenerSettings(),
};

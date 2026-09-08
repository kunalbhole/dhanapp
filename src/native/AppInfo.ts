import { NativeModules } from 'react-native';

// Backed by the native DhanPermissions module (see its getAppVersion doc comment for why
// the version string lives there, and why it reads the installed APK's own PackageInfo
// rather than app.json).
const { DhanPermissions } = NativeModules;

export type AppVersionInfo = { versionName: string; versionCode: number };

export const appInfo = {
  getAppVersion: async (): Promise<AppVersionInfo> => JSON.parse(await DhanPermissions.getAppVersion()),
};

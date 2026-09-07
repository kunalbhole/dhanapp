import React, { useEffect, useState } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { ActivityIndicator, View } from 'react-native';
import { CalendarCheckIcon, ChartPieIcon, GearIcon, HouseIcon, type Icon, ListBulletsIcon } from 'phosphor-react-native';
import { colors } from '../theme/colors';
import { userPrefs } from '../native/UserPrefs';

import { WelcomeScreen } from '../screens/WelcomeScreen';
import { HomeScreen } from '../screens/HomeScreen';
import { TransactionsScreen } from '../screens/TransactionsScreen';
import { BudgetScreen } from '../screens/BudgetScreen';
import { BillsScreen } from '../screens/BillsScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { TxnDetailScreen } from '../screens/TxnDetailScreen';
import { AddTransactionScreen } from '../screens/AddTransactionScreen';
import { AddBillScreen } from '../screens/AddBillScreen';
import { BackupSettingsScreen } from '../screens/BackupSettingsScreen';
import { EditBudgetScreen } from '../screens/EditBudgetScreen';
import { CreateBudgetScreen } from '../screens/CreateBudgetScreen';
import { CustomFrameworkBuilderScreen } from '../screens/CustomFrameworkBuilderScreen';

export type RootStackParamList = {
  Welcome: undefined;
  Main: undefined;
  TxnDetail: { id: number };
  AddTransaction: { defaultIsIncome?: boolean } | undefined;
  AddBill: undefined;
  BackupSettings: undefined;
  EditBudget: { budgetDefId: number };
  CreateBudget: undefined;
  CustomFrameworkBuilder:
    | { mode: 'create'; pendingName: string; pendingTotal: number }
    | { mode: 'edit'; budgetDefId: number; existingCustomJson: string | null; currentTotal: number };
};

export type MainTabParamList = {
  Home: undefined;
  Transactions: undefined;
  Budget: undefined;
  Bills: undefined;
  Settings: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();

const TAB_ICONS: Record<keyof MainTabParamList, Icon> = {
  Home: HouseIcon,
  Transactions: ListBulletsIcon,
  Budget: ChartPieIcon,
  Bills: CalendarCheckIcon,
  Settings: GearIcon,
};

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.navy,
        tabBarInactiveTintColor: colors.fg3,
        tabBarIcon: ({ focused, color, size }) => {
          const IconComponent = TAB_ICONS[route.name as keyof MainTabParamList];
          return <IconComponent color={color} size={size} weight={focused ? 'fill' : 'regular'} />;
        },
      })}>
      <Tab.Screen name="Home" component={HomeScreen} />
      <Tab.Screen name="Transactions" component={TransactionsScreen} options={{ title: 'Txns' }} />
      <Tab.Screen name="Budget" component={BudgetScreen} />
      <Tab.Screen name="Bills" component={BillsScreen} />
      <Tab.Screen name="Settings" component={SettingsScreen} options={{ title: 'More' }} />
    </Tab.Navigator>
  );
}

export function RootNavigator() {
  const [initialRoute, setInitialRoute] = useState<'Welcome' | 'Main' | null>(null);

  useEffect(() => {
    userPrefs.hasOnboarded().then((done) => setInitialRoute(done ? 'Main' : 'Welcome'));
  }, []);

  if (!initialRoute) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.appBg }}>
        <ActivityIndicator color={colors.navy} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName={initialRoute}
        screenOptions={{
          headerStyle: { backgroundColor: colors.appBg },
          headerTintColor: colors.navy,
          headerShadowVisible: false,
        }}>
        <Stack.Screen name="Welcome" component={WelcomeScreen} options={{ headerShown: false }} />
        <Stack.Screen name="Main" component={MainTabs} options={{ headerShown: false }} />
        <Stack.Screen name="TxnDetail" component={TxnDetailScreen} options={{ title: 'Transaction' }} />
        <Stack.Screen name="AddTransaction" component={AddTransactionScreen} options={{ title: 'Add transaction', presentation: 'modal' }} />
        <Stack.Screen name="AddBill" component={AddBillScreen} options={{ title: 'Add bill', presentation: 'modal' }} />
        <Stack.Screen name="BackupSettings" component={BackupSettingsScreen} options={{ title: 'Backup' }} />
        <Stack.Screen name="EditBudget" component={EditBudgetScreen} options={{ title: 'Edit budget' }} />
        <Stack.Screen name="CreateBudget" component={CreateBudgetScreen} options={{ title: 'New budget', presentation: 'modal' }} />
        <Stack.Screen name="CustomFrameworkBuilder" component={CustomFrameworkBuilderScreen} options={{ title: 'Custom framework' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

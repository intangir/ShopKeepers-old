package com.nisovin.shopkeepers;

import java.lang.reflect.Field;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;

public class Settings {
	
	public static boolean disableOtherVillagers = true;
	public static boolean blockVillagerSpawns = false;
	public static boolean enableSpawnVerifier = false;
	public static boolean enablePurchaseLogging = false;
	public static boolean saveInstantly = true;
	
	public static boolean enableWorldGuardRestrictions = false;
	public static boolean enableTownyRestrictions = false;	
	
	public static boolean requireChestRecentlyPlaced = true;
	public static boolean createPlayerShopWithCommand = false;
	public static boolean simulateRightClickOnCommand = true;
	public static boolean protectChests = true;
	public static boolean deleteShopkeeperOnBreakChest = false;
	public static int maxShopsPerPlayer = 0;
	public static int maxChestDistance = 15;
	public static int playerShopkeeperInactiveDays = 0;
	public static boolean preventTradingWithOwnShop = true;
	
	public static int shopCreationItem = 383;
	public static int shopCreationItemData = 120;
	public static String shopCreationItemName = "";
	public static boolean deletingPlayerShopReturnsEgg = false;

	public static boolean enableVillagerShops = true;	
	public static boolean enableSignShops = true;
	public static boolean enableWitchShops = true;
	
	public static String signShopFirstLine = "[SHOP]";
	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;
	public static String nameplatePrefix = "&a";
	
	public static boolean enableBlockShops = true;
	public static int blockShopType = 0;

	public static String editorTitle = "Shopkeeper Editor";
	public static int saveItem = Material.EMERALD_BLOCK.getId();
	public static int nameItem = Material.ANVIL.getId();
	public static int deleteItem = Material.FIRE.getId();
	
	public static int currencyItem = Material.EMERALD.getId();
	public static short currencyItemData = 0;
	public static String currencyItemName = null;
	public static int zeroItem = Material.SLIME_BALL.getId();
	
	public static int highCurrencyItem = Material.EMERALD_BLOCK.getId();
	public static short highCurrencyItemData = 0;
	public static String highCurrencyItemName = null;
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;
	public static int highZeroItem = Material.SLIME_BALL.getId();
	
	public static String msgButtonName = "&aSet Shop Name";
	public static String msgButtonType = "&aChoose Appearance";
	public static String msgButtonDelete = "&4Delete";
	
	public static String msgSelectedNormalShop = "&aNormal shopkeeper selected (sells items to players).";
	public static String msgSelectedBookShop = "&aBook shopkeeper selected (sell books).";
	public static String msgSelectedBuyShop = "&aBuying shopkeeper selected (buys items from players).";
	public static String msgSelectedTradeShop = "&aTrading shopkeeper selected (trade items with players).";
	
	public static String msgSelectedVillagerShop = "&aVillager shop selected.";
	public static String msgSelectedSignShop = "&aSign shop selected.";
	public static String msgSelectedWitchShop = "&aWitch shop selected.";
	
	public static String msgSelectedChest = "&aChest selected! Right click a block to place your shopkeeper.";
	public static String msgMustSelectChest = "&aYou must right-click a chest before placing your shopkeeper.";
	public static String msgChestTooFar = "&aThe shopkeeper's chest is too far away!";
	public static String msgChestNotPlaced = "&aYou must select a chest you have recently placed.";
	public static String msgTypeNewName = "&aPlease type the shop's name into the chat.\n  &aType a dash (-) to remove the name.";
	public static String msgNameSet = "&aThe shop's name has been set!";
	
	public static String msgPlayerShopCreated = "&aShopkeeper created!\n&aAdd items you want to sell to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgBookShopCreated = "&aShopkeeper created!\n&aAdd written books and blank books to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgBuyShopCreated = "&aShopkeeper created!\n&aAdd one of each item you want to sell to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgTradeShopCreated = "&aShopkeeper created!\n&aAdd items you want to sell to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgAdminShopCreated = "&aShopkeeper created!\n&aRight-click the shop while sneaking to modify trades.";
	public static String msgShopCreateFail = "&aYou cannot create a shopkeeper there.";
	public static String msgTooManyShops = "&aYou have too many shops.";

	public static String fileEncoding = "";
	
	public static void loadConfiguration(Configuration config) {
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				String configKey = field.getName().replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();
				if (field.getType() == String.class) {
					field.set(null, config.getString(configKey, (String)field.get(null)));
				} else if (field.getType() == int.class) {
					field.set(null, config.getInt(configKey, field.getInt(null)));
				} else if (field.getType() == short.class) {
					field.set(null, (short)config.getInt(configKey, field.getShort(null)));
				} else if (field.getType() == boolean.class) {
					field.set(null, config.getBoolean(configKey, field.getBoolean(null)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (maxChestDistance > 50) maxChestDistance = 50;
		if (highCurrencyValue <= 0) highCurrencyItem = 0;
	}
	
	public static void loadLanguageConfiguration(Configuration config) {
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.getType() == String.class && field.getName().startsWith("msg")) {
					String configKey = field.getName().replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();
					field.set(null, config.getString(configKey, (String)field.get(null)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

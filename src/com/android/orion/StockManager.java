package com.android.orion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.orion.database.DatabaseContract;
import com.android.orion.database.Stock;
import com.android.orion.database.StockData;
import com.android.orion.database.StockDatabaseManager;
import com.android.orion.pinyin.Pinyin;
import com.android.orion.utility.Utility;

public class StockManager {
	Context mContext;

	ContentResolver mContentResolver = null;
	LocalBroadcastManager mLocalBroadcastManager = null;
	protected StockDatabaseManager mStockDatabaseManager = null;
	ArrayMap<String, Stock> mStockArrayMapFavorite = null;

	// ConnectivityManager mConnectivityManager = null;

	public StockManager(Context context) {
		mContext = context;

		if (mContentResolver == null) {
			mContentResolver = mContext.getContentResolver();
		}

		if (mLocalBroadcastManager == null) {
			mLocalBroadcastManager = LocalBroadcastManager
					.getInstance(mContext);
		}

		if (mStockDatabaseManager == null) {
			mStockDatabaseManager = StockDatabaseManager.getInstance(mContext);
		}

		if (mStockArrayMapFavorite == null) {
			mStockArrayMapFavorite = new ArrayMap<String, Stock>();
		}

		/*
		 * if (mConnectivityManager == null) { mConnectivityManager =
		 * (ConnectivityManager) mContext
		 * .getSystemService(Context.CONNECTIVITY_SERVICE); }
		 */
	}

	/*
	 * boolean isConnected() { NetworkInfo networkInfo = null;
	 * 
	 * if (mConnectivityManager != null) { networkInfo = mConnectivityManager
	 * .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	 * 
	 * if (networkInfo != null && networkInfo.isConnected()) { return true; }
	 * 
	 * networkInfo = mConnectivityManager
	 * .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	 * 
	 * if (networkInfo != null && networkInfo.isConnected()) { return true; } }
	 * 
	 * return false; }
	 */

	/*
	 * boolean isConnected() { boolean result = false; boolean bWifiOnly =
	 * false;
	 * 
	 * String settingValue;
	 * 
	 * State wifiState; State mobileState;
	 * 
	 * try { openDatabase();
	 * 
	 * settingValue = mStockDatabaseManager
	 * .getSettingValue(Constants.SETTING_CONNECTION_WIFI_ONLY);
	 * 
	 * if (settingValue.equals("true")) { bWifiOnly = true; } else if
	 * (settingValue.equals("false")) { bWifiOnly = false; } else { bWifiOnly =
	 * false; } } catch (Exception e) { e.printStackTrace(); } finally {
	 * closeDatabase(); }
	 * 
	 * if (mConnectivityManager != null) { wifiState =
	 * mConnectivityManager.getNetworkInfo(
	 * ConnectivityManager.TYPE_WIFI).getState(); mobileState =
	 * mConnectivityManager.getNetworkInfo(
	 * ConnectivityManager.TYPE_MOBILE).getState();
	 * 
	 * if (bWifiOnly) { return (wifiState == NetworkInfo.State.CONNECTED); }
	 * 
	 * if (wifiState == NetworkInfo.State.CONNECTED) { result = true; } else if
	 * (mobileState == NetworkInfo.State.CONNECTED) { result = true; } }
	 * 
	 * return result; }
	 */

	/*
	 * boolean isConnected() { NetworkInfo networkInfo = null;
	 * 
	 * if (mConnectivityManager != null) { networkInfo = mConnectivityManager
	 * .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	 * 
	 * if (!networkInfo.isAvailable()) { networkInfo = mConnectivityManager
	 * .getNetworkInfo(ConnectivityManager.TYPE_MOBILE); } }
	 * 
	 * return networkInfo == null ? false : networkInfo.isConnected(); }
	 */

	void initStockIndexes() {
		if (mStockDatabaseManager == null) {
			return;
		}

		try {
			if (mStockDatabaseManager.getStockCount(null, null, null) == 0) {
				insertStockIndexes(Constants.STOCK_SE_SH,
						Constants.STOCK_INDEXES_CODE_BASE_SH, 1);
				insertStockIndexes(Constants.STOCK_SE_SH,
						Constants.STOCK_INDEXES_CODE_BASE_SH, 300);
				insertStockIndexes(Constants.STOCK_SE_SZ,
						Constants.STOCK_INDEXES_CODE_BASE_SZ, 5);
				insertStockIndexes(Constants.STOCK_SE_SZ,
						Constants.STOCK_INDEXES_CODE_BASE_SZ, 6);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void insertStockIndexes(String se, String baseCode, int index) {
		String now = Utility.getCurrentDateTimeString();
		Stock stock = Stock.obtain();

		if (mStockDatabaseManager == null) {
			return;
		}

		stock.init();
		stock.setSE(se);
		stock.setCode(String.format("%06d", Integer.valueOf(baseCode) + index));
		stock.setClasses(Constants.STOCK_FLAG_CLASS_INDEXES);
		stock.setCreated(now);
		stock.setModified(now);
		try {
			mStockDatabaseManager.insertStock(stock);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void fixPinyin() {
		String name = "";
		String selection = null;
		List<Stock> stockList = null;

		if (mStockDatabaseManager == null) {
			return;
		}

		for (Entry<String, String> entry : Pinyin.fixedPinyinMap.entrySet()) {
			selection = DatabaseContract.COLUMN_NAME + " LIKE '%"
					+ entry.getKey() + "%'" + " AND "
					+ DatabaseContract.Stock.COLUMN_PINYIN_FIXED
					+ " NOT LIKE '" + Constants.STOCK_FLAG_PINYIN_FIXED + "'";
			stockList = loadStockList(selection, null, null);
			if (stockList == null) {
				return;
			}

			for (Stock stock : stockList) {
				name = stock.getName().replaceAll(entry.getKey(),
						entry.getValue());
				stock.setPinyin(Pinyin.toPinyin(mContext, name));
				stock.setPinyinFixed(Constants.STOCK_FLAG_PINYIN_FIXED);
				try {
					mStockDatabaseManager.updateStock(stock,
							stock.getContentValues());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected List<Stock> loadStockList(String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor cursor = null;
		List<Stock> stockList = null;

		if (mStockDatabaseManager == null) {
			return stockList;
		}

		stockList = new ArrayList<Stock>();

		try {
			stockList.clear();
			cursor = mStockDatabaseManager.queryStock(selection, selectionArgs,
					sortOrder);
			if ((cursor != null) && (cursor.getCount() > 0)) {
				while (cursor.moveToNext()) {
					Stock stock = Stock.obtain();
					stock.set(cursor);
					stockList.add(stock);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mStockDatabaseManager.closeCursor(cursor);
		}

		return stockList;
	}

	void loadStockArrayMap(String selection, String[] selectionArgs,
			String sortOrder, ArrayMap<String, Stock> stockArrayMap) {
		Cursor cursor = null;

		if ((mStockDatabaseManager == null) || (stockArrayMap == null)) {
			return;
		}

		try {
			stockArrayMap.clear();
			cursor = mStockDatabaseManager.queryStock(selection, selectionArgs,
					sortOrder);
			if ((cursor != null) && (cursor.getCount() > 0)) {
				while (cursor.moveToNext()) {
					Stock stock = Stock.obtain();
					stock.set(cursor);
					stockArrayMap.put(stock.getSE() + stock.getCode(), stock);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mStockDatabaseManager.closeCursor(cursor);
		}
	}

	void loadStockDataList(Stock stock, String period,
			ArrayList<StockData> stockDataList) {
		int index = 0;

		Cursor cursor = null;
		String selection = null;
		String sortOrder = null;

		if ((stock == null) || TextUtils.isEmpty(period)
				|| (stockDataList == null)) {
			return;
		}

		if (mStockDatabaseManager == null) {
			return;
		}

		try {
			stockDataList.clear();

			selection = mStockDatabaseManager.getStockDataSelection(
					stock.getId(), period);
			sortOrder = mStockDatabaseManager.getStockDataOrder();
			cursor = mStockDatabaseManager.queryStockData(selection, null,
					sortOrder);
			if ((cursor != null) && (cursor.getCount() > 0)) {
				while (cursor.moveToNext()) {
					StockData stockData = StockData.obtain(period);
					stockData.set(cursor);
					index = stockDataList.size();
					stockData.setIndex(index);
					stockData.setIndexStart(index);
					stockData.setIndexEnd(index);
					stockData.setAction(Constants.STOCK_ACTION_NONE);
					// __TEST_CASE__
					// stockData.setAction(String.valueOf(stockData.getIndex()));
					// __TEST_CASE__

					stockDataList.add(stockData);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mStockDatabaseManager.closeCursor(cursor);
		}
	}

	ArrayList<StockData> getStockDataList(Stock stock, String period) {
		ArrayList<StockData> stockDataList = null;

		if (period.equals(Constants.PERIOD_MIN1)) {
			stockDataList = stock.mStockDataListMin1;
		} else if (period.equals(Constants.PERIOD_MIN5)) {
			stockDataList = stock.mStockDataListMin5;
		} else if (period.equals(Constants.PERIOD_MIN15)) {
			stockDataList = stock.mStockDataListMin15;
		} else if (period.equals(Constants.PERIOD_MIN30)) {
			stockDataList = stock.mStockDataListMin30;
		} else if (period.equals(Constants.PERIOD_MIN60)) {
			stockDataList = stock.mStockDataListMin60;
		} else if (period.equals(Constants.PERIOD_DAY)) {
			stockDataList = stock.mStockDataListDay;
		} else if (period.equals(Constants.PERIOD_WEEK)) {
			stockDataList = stock.mStockDataListWeek;
		} else if (period.equals(Constants.PERIOD_MONTH)) {
			stockDataList = stock.mStockDataListMonth;
		} else if (period.equals(Constants.PERIOD_QUARTER)) {
			stockDataList = stock.mStockDataListQuarter;
		} else if (period.equals(Constants.PERIOD_YEAR)) {
			stockDataList = stock.mStockDataListYear;
		}
		return stockDataList;
	}

	protected String selectStock(String mark) {
		return DatabaseContract.Stock.COLUMN_MARK + " = '" + mark + "'";
	}

	String selectStockHSA() {
		return DatabaseContract.Stock.COLUMN_CLASSES + " = '"
				+ Constants.STOCK_FLAG_CLASS_HSA + "'";
	}

	boolean isStockHSAEmpty() {
		boolean result = false;

		String selection = selectStockHSA();

		if (mStockDatabaseManager == null) {
			return result;
		}

		if (mStockDatabaseManager.getStockCount(selection, null, null) == 0) {
			result = true;
		}

		return result;
	}

	int getPeriodMinutes(String period) {
		int minutes = 0;

		if (period.equals(Constants.PERIOD_MIN1)) {
			minutes = 1;
		} else if (period.equals(Constants.PERIOD_MIN5)) {
			minutes = 5;
		} else if (period.equals(Constants.PERIOD_MIN15)) {
			minutes = 15;
		} else if (period.equals(Constants.PERIOD_MIN30)) {
			minutes = 30;
		} else if (period.equals(Constants.PERIOD_MIN60)) {
			minutes = 60;
		} else if (period.equals(Constants.PERIOD_DAY)) {
			minutes = 240;
		} else if (period.equals(Constants.PERIOD_WEEK)) {
			minutes = 1680;
		} else if (period.equals(Constants.PERIOD_MONTH)) {
			minutes = 7200;
		} else if (period.equals(Constants.PERIOD_QUARTER)) {
			minutes = 28800;
		} else if (period.equals(Constants.PERIOD_YEAR)) {
			minutes = 115200;
		} else {
		}

		return minutes;
	}

	int getPeriodCoefficient(String period) {
		int result = 1;

		if (period.equals(Constants.PERIOD_MIN1)) {
			result = 240;
		} else if (period.equals(Constants.PERIOD_MIN5)) {
			result = 48;
		} else if (period.equals(Constants.PERIOD_MIN15)) {
			result = 16;
		} else if (period.equals(Constants.PERIOD_MIN30)) {
			result = 8;
		} else if (period.equals(Constants.PERIOD_MIN60)) {
			result = 4;
		} else if (period.equals(Constants.PERIOD_DAY)) {
			result = 1;
		} else if (period.equals(Constants.PERIOD_WEEK)) {
			result = 1;
		} else if (period.equals(Constants.PERIOD_MONTH)) {
			result = 1;
		} else if (period.equals(Constants.PERIOD_QUARTER)) {
			result = 1;
		} else if (period.equals(Constants.PERIOD_YEAR)) {
			result = 1;
		}

		return result;
	}
}

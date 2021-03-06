package com.android.orion;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.android.orion.curve.BezierCurve;
import com.android.orion.database.DatabaseContract;
import com.android.orion.database.FinancialData;
import com.android.orion.database.ShareBonus;
import com.android.orion.database.Stock;
import com.android.orion.database.StockData;
import com.android.orion.database.StockDeal;
import com.android.orion.database.TotalShare;
import com.android.orion.indicator.MACD;
import com.android.orion.utility.Preferences;
import com.android.orion.utility.StopWatch;
import com.android.orion.utility.Utility;

public class StockAnalyzer extends StockManager {
	static final String TAG = Constants.TAG + " "
			+ StockAnalyzer.class.getSimpleName();

	public StockAnalyzer(Context context) {
		super(context);
	}

	void analyze(Stock stock) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		if (stock == null) {
			return;
		}

		try {
			if (!isFinancialAnalyzed(stock)) {
				analyzeFinancial(stock);
			}

			setupStockFinancialData(stock);
			setupStockShareBonus(stock);

			updateDatabase(stock);
			updateNotification(stock);
		} catch (Exception e) {
			e.printStackTrace();
		}

		stopWatch.stop();
		Log.d(TAG, "analyze:" + stock.getName() + " " + stopWatch.getInterval()
				+ "s");
	}

	void analyze(Stock stock, String period, ArrayList<StockData> stockDataList) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		if ((stock == null) || (stockDataList == null)) {
			return;
		}

		try {
			setupStockShareBonus(stock);
			setupStockFinancialData(stock);

			loadStockDataList(stock, period, stockDataList);
			// if (stockDataList.size() < Constants.STOCK_VERTEX_TYPING_SIZE) {
			// return;
			// }

			analyzeStockData(stock, period, stockDataList);

			updateDatabase(stock, period, stockDataList);
			updateNotification(stock);
		} catch (Exception e) {
			e.printStackTrace();
		}

		stopWatch.stop();
		Log.d(TAG, "analyze:" + stock.getName() + " " + period + " "
				+ stopWatch.getInterval() + "s");
	}
	
	boolean isFinancialAnalyzed(Stock stock) {
		boolean result = false;
		
		FinancialData financialData = new FinancialData();
		financialData.setStockId(stock.getId());
		mStockDatabaseManager.getFinancialData(stock, financialData);

		if (financialData.getCreated().contains(
				Utility.getCurrentDateString())
				|| financialData.getModified().contains(
						Utility.getCurrentDateString())) {
			if (financialData.getRate() != 0) {
				result = true;
			}
		}
		
		return result;
	}

	void analyzeFinancial(Stock stock) {
		ArrayList<FinancialData> financialDataList = new ArrayList<FinancialData>();
		ArrayList<TotalShare> totalShareList = new ArrayList<TotalShare>();
		ArrayList<ShareBonus> shareBonusList = new ArrayList<ShareBonus>();
		ArrayList<StockData> stockDataList = new ArrayList<StockData>();
		String sortOrder = DatabaseContract.COLUMN_DATE + " DESC ";

		mStockDatabaseManager.getFinancialDataList(stock, financialDataList,
				sortOrder);
		mStockDatabaseManager.getTotalShareList(stock, totalShareList,
				sortOrder);
		mStockDatabaseManager.getShareBonusList(stock, shareBonusList,
				sortOrder);
		mStockDatabaseManager.getStockDataList(stock, Constants.PERIOD_MONTH,
				stockDataList, sortOrder);

		setupTotalShare(financialDataList, totalShareList);
		setupNetProfitPerShare(financialDataList);
		setupNetProfitPerShareInYear(financialDataList);
		setupRate(financialDataList);
		setupRoe(financialDataList);

		for (FinancialData financialData : financialDataList) {
			financialData.setModified(Utility.getCurrentDateTimeString());
			mStockDatabaseManager.updateFinancialData(financialData,
					financialData.getContentValues());
		}

		setupRoi(stockDataList, financialDataList);

		for (StockData stockData : stockDataList) {
			stockData.setModified(Utility.getCurrentDateTimeString());
			mStockDatabaseManager.updateStockData(stockData,
					stockData.getContentValues());
		}
	}

	void setupTotalShare(ArrayList<FinancialData> financialDataList,
			ArrayList<TotalShare> totalShareList) {
		if (financialDataList == null || totalShareList == null) {
			return;
		}

		int j = 0;
		for (FinancialData financialData : financialDataList) {
			while (j < totalShareList.size()) {
				TotalShare totalShare = totalShareList.get(j);
				if (Utility.stringToCalendar(financialData.getDate(),
						Utility.CALENDAR_DATE_FORMAT).after(
						Utility.stringToCalendar(totalShare.getDate(),
								Utility.CALENDAR_DATE_FORMAT))) {
					financialData.setTotalShare(totalShare.getTotalShare());
					break;
				} else {
					j++;
				}
			}
		}
	}

	void setupNetProfitPerShare(ArrayList<FinancialData> financialDataList) {
		if (financialDataList == null) {
			return;
		}

		for (FinancialData financialData : financialDataList) {
			financialData.setupDebtToNetAssetsRatio();
			financialData.setupNetProfitPerShare();
		}
	}

	void setupNetProfitPerShareInYear(ArrayList<FinancialData> financialDataList) {
		double netProfitPerShareInYear = 0;
		double netProfitPerShare = 0;

		if (financialDataList == null) {
			return;
		}

		if (financialDataList.size() < Constants.SEASONS_IN_A_YEAR + 1) {
			return;
		}

		for (int i = 0; i < financialDataList.size()
				- Constants.SEASONS_IN_A_YEAR; i++) {
			netProfitPerShareInYear = 0;
			for (int j = 0; j < Constants.SEASONS_IN_A_YEAR; j++) {
				FinancialData current = financialDataList.get(i + j);
				FinancialData prev = financialDataList.get(i + j + 1);

				if (current == null || prev == null) {
					continue;
				}

				if (current.getTotalShare() == 0) {
					continue;
				}

				netProfitPerShare = 0;
				if (current.getDate().contains("03-31")) {
					netProfitPerShare = current.getNetProfit()
							/ current.getTotalShare();
				} else {
					netProfitPerShare = (current.getNetProfit() - prev
							.getNetProfit()) / current.getTotalShare();
				}
				netProfitPerShareInYear += netProfitPerShare;

			}

			FinancialData financialData = financialDataList.get(i);
			financialData.setNetProfitPerShareInYear(netProfitPerShareInYear);
		}
	}

	void setupRate(ArrayList<FinancialData> financialDataList) {
		double rate = 0;

		if (financialDataList == null) {
			return;
		}

		if (financialDataList.size() < Constants.SEASONS_IN_A_YEAR + 1) {
			return;
		}

		for (int i = 0; i < financialDataList.size()
				- Constants.SEASONS_IN_A_YEAR; i++) {
			FinancialData financialData = financialDataList.get(i);
			FinancialData prev = financialDataList.get(i
					+ Constants.SEASONS_IN_A_YEAR);

			if (prev == null || prev.getNetProfitPerShareInYear() == 0) {
				continue;
			}

			rate = Utility.Round(financialData.getNetProfitPerShareInYear()
					/ prev.getNetProfitPerShareInYear(),
					Constants.DOUBLE_FIXED_DECIMAL);

			financialData.setRate(rate);
		}
	}

	void setupRoe(ArrayList<FinancialData> financialDataList) {
		double roe = 0;

		if (financialDataList == null) {
			return;
		}

		if (financialDataList.size() < Constants.SEASONS_IN_A_YEAR + 1) {
			return;
		}

		for (int i = 0; i < financialDataList.size()
				- Constants.SEASONS_IN_A_YEAR; i++) {
			FinancialData financialData = financialDataList.get(i);
			FinancialData prev = financialDataList.get(i
					+ Constants.SEASONS_IN_A_YEAR);

			if (prev == null || prev.getBookValuePerShare() == 0) {
				continue;
			}

			roe = Utility.Round(
					100.0 * financialData.getNetProfitPerShareInYear()
							/ prev.getBookValuePerShare(),
					Constants.DOUBLE_FIXED_DECIMAL);
			if (roe < 0) {
				roe = 0;
			}

			financialData.setRoe(roe);
		}
	}

	void setupRoi(ArrayList<StockData> stockDataList,
			ArrayList<FinancialData> financialDataList) {
		double price = 0;
		double pe = 0;
		double pb = 0;
		double roi = 0;

		if (stockDataList == null || financialDataList == null) {
			return;
		}

		int j = 0;
		for (StockData stockData : stockDataList) {
			price = stockData.getClose();
			if (price == 0) {
				continue;
			}

			while (j < financialDataList.size()) {
				FinancialData financialData = financialDataList.get(j);
				if (Utility.stringToCalendar(stockData.getDate(),
						Utility.CALENDAR_DATE_FORMAT).after(
						Utility.stringToCalendar(financialData.getDate(),
								Utility.CALENDAR_DATE_FORMAT))) {
					pe = Utility.Round(
							100.0 * financialData.getNetProfitPerShareInYear()
									/ price, Constants.DOUBLE_FIXED_DECIMAL);

					if (financialData.getBookValuePerShare() != 0) {
						pb = Utility.Round(
								price / financialData.getBookValuePerShare(),
								Constants.DOUBLE_FIXED_DECIMAL);
					}

					roi = Utility.Round(financialData.getRoe() * pe
							* Constants.ROI_COEFFICIENT,
							Constants.DOUBLE_FIXED_DECIMAL);
					if (roi < 0) {
						roi = 0;
					}

					stockData.setPe(pe);
					stockData.setPb(pb);
					stockData.setRoi(roi);
					break;
				} else {
					j++;
				}
			}
		}
	}

	void setupStockFinancialData(Stock stock) {
		String sortOrder = DatabaseContract.COLUMN_DATE + " DESC ";
		FinancialData financialData = new FinancialData();
		ArrayList<FinancialData> financialDataList = new ArrayList<FinancialData>();

		financialData.setStockId(stock.getId());

		mStockDatabaseManager.getFinancialData(stock, financialData);
		mStockDatabaseManager.getFinancialDataList(stock, financialDataList,
				sortOrder);

		stock.setTotalAssets(financialData.getTotalAssets());
		stock.setTotalLongTermLiabilities(financialData
				.getTotalLongTermLiabilities());
		stock.setBookValuePerShare(financialData.getBookValuePerShare());
		stock.setCashFlowPerShare(financialData.getCashFlowPerShare());
		stock.setNetProfit(financialData.getNetProfit());

		stock.setupMarketValue();
		stock.setupNetProfitPerShare();
		stock.setupNetProfitPerShareInYear(financialDataList);
		stock.setupNetProfitPerShareLastYear(financialDataList);
		stock.setupRate();
		stock.setupDebtToNetAssetsRatio();
		stock.setupRoe(financialDataList);
		stock.setupPE();
		stock.setupPB();
		stock.setupRoi();
	}

	void setupStockShareBonus(Stock stock) {
		double totalDivident = 0;

		String yearString = "";
		String prevYearString = "";
		String sortOrder = DatabaseContract.COLUMN_DATE + " DESC ";

		ArrayList<ShareBonus> shareBonusList = new ArrayList<ShareBonus>();

		mStockDatabaseManager.getShareBonusList(stock, shareBonusList,
				sortOrder);

		int i = 0;
		for (ShareBonus shareBonus : shareBonusList) {
			String dateString = shareBonus.getDate();
			if (!TextUtils.isEmpty(dateString)) {
				String[] strings = dateString.split("-");
				if (strings != null && strings.length > 0) {
					yearString = strings[0];
				}

				if (!TextUtils.isEmpty(prevYearString)) {
					if (!prevYearString.equals(yearString)) {
						break;
					}
				}
			}

			totalDivident += shareBonus.getDividend();

			if (i == 0) {
				stock.setDate(shareBonus.getDate());
				stock.setRDate(shareBonus.getRDate());
			}
			stock.setDividend(Utility.Round(totalDivident,
					Constants.DOUBLE_FIXED_DECIMAL));
			stock.setupBonus();
			stock.setupYield();
			stock.setupDividendRatio();

			prevYearString = yearString;
			i++;
		}
	}

	private void setMACD(Stock stock, String period,
			ArrayList<StockData> stockDataList) {
		int size = 0;
		int grade = 0;
		int beginIndex = 0;

		double t = 0;
		double average = 0;
		double velocity = 0;
		double acceleration = 0;

		double average5 = 0;
		double average10 = 0;
		double dif = 0;
		double dea = 0;
		double histogram = 0;
		double histogram_1 = 0;
		double histogram_2 = 0;

		String action = Constants.STOCK_ACTION_NONE;

		MACD macd = new MACD();
		BezierCurve bezierCurve = new BezierCurve();

		size = stockDataList.size();

		if (size < Constants.STOCK_VERTEX_TYPING_SIZE) {
			return;
		}

		macd.mPriceList.clear();

		for (int i = 0; i < size; i++) {
			// macd.mPriceList.add(stockDataList.get(i).getClose());
			macd.mPriceList
					.add((stockDataList.get(i).getVertexHigh() + stockDataList
							.get(i).getVertexLow()) / 2.0);
		}

		macd.calculate();

		grade = size - 1;

		if (size > Constants.BENZIER_CURVE_GRADE_MAX) {
			grade = Constants.BENZIER_CURVE_GRADE_MAX;
		}

		bezierCurve.init(grade);

		beginIndex = size - grade - 1;

		for (int i = 0; i < size; i++) {
			average5 = macd.mEMAAverage5List.get(i);
			average10 = macd.mEMAAverage10List.get(i);
			dif = macd.mDIFList.get(i);
			dea = macd.mDEAList.get(i);
			histogram = macd.mHistogramList.get(i);

			stockDataList.get(i).setAverage5(average5);
			stockDataList.get(i).setAverage10(average10);
			stockDataList.get(i).setDIF(dif);
			stockDataList.get(i).setDEA(dea);
			stockDataList.get(i).setHistogram(histogram);
			stockDataList.get(i).setAverage(average);
			stockDataList.get(i).setVelocity(velocity);
			stockDataList.get(i).setAcceleration(acceleration);

			if (i >= beginIndex) {
				bezierCurve.addControlData(i - beginIndex, histogram);
			}
		}

		for (int i = beginIndex; i < size; i++) {
			t = 1.0 * (i - beginIndex) / (size - 1 - beginIndex);
			average = bezierCurve.calculate(t);

			if (i == beginIndex) {
				velocity = 0;
				acceleration = 0;
			} else if (i == beginIndex + 1) {
				acceleration = 0;
			} else {
				velocity = 10 * (average - stockDataList.get(i - 1)
						.getAverage());
				acceleration = 2 * (velocity - stockDataList.get(i - 1)
						.getVelocity());
			}

			stockDataList.get(i).setAverage(average);
			stockDataList.get(i).setVelocity(velocity);
			stockDataList.get(i).setAcceleration(acceleration);
		}

		histogram = macd.mHistogramList.get(size - 1);
		histogram_1 = macd.mHistogramList.get(size - 2);
		histogram_2 = macd.mHistogramList.get(size - 3);

		if ((histogram_1 < histogram_2) && (histogram_1 < histogram)) {
			action = Constants.STOCK_ACTION_DX;
		}

		if ((histogram_1 > histogram_2) && (histogram_1 > histogram)) {
			action = Constants.STOCK_ACTION_GX;
		}

		stock.setAction(period, action);
	}

	void analyzeStockData(Stock stock, String period,
			ArrayList<StockData> stockDataList) {
		VertexAnalyzer vertexAnalyzer = new VertexAnalyzer();

		ArrayList<StockData> drawVertexList = new ArrayList<StockData>();
		ArrayList<StockData> drawDataList = new ArrayList<StockData>();
		ArrayList<StockData> strokeVertexList = new ArrayList<StockData>();
		ArrayList<StockData> strokeDataList = new ArrayList<StockData>();
		ArrayList<StockData> segmentVertexList = new ArrayList<StockData>();
		ArrayList<StockData> segmentDataList = new ArrayList<StockData>();
		ArrayList<StockData> overlapList = new ArrayList<StockData>();

		vertexAnalyzer.analyzeVertex(stockDataList, drawVertexList);
		vertexAnalyzer.vertexListToDataList(stockDataList, drawVertexList,
				drawDataList, false);

		setMACD(stock, period, stockDataList);

		// __TEST_CASE__
		// vertexAnalyzer.testShow(stockDataList, drawDataList);
		// __TEST_CASE__
		vertexAnalyzer.analyzeLine(stockDataList, drawDataList,
				strokeVertexList, Constants.STOCK_VERTEX_TOP_STROKE,
				Constants.STOCK_VERTEX_BOTTOM_STROKE);
		vertexAnalyzer.vertexListToDataList(stockDataList, strokeVertexList,
				strokeDataList, false);
		// __TEST_CASE__
		// vertexAnalyzer.testShow(stockDataList, strokeDataList);
		// __TEST_CASE__
		vertexAnalyzer.analyzeLine(stockDataList, strokeDataList,
				segmentVertexList, Constants.STOCK_VERTEX_TOP_SEGMENT,
				Constants.STOCK_VERTEX_BOTTOM_SEGMENT);
		vertexAnalyzer.vertexListToDataList(stockDataList, segmentVertexList,
				segmentDataList, true);

		vertexAnalyzer.analyzeOverlap(stockDataList, segmentDataList,
				overlapList);

		vertexAnalyzer.analyzeAction(stockDataList, segmentDataList,
				overlapList);

		setAction(stock, period, stockDataList, segmentDataList);
	}

	private void setAction(Stock stock, String period,
			ArrayList<StockData> stockDataList,
			ArrayList<StockData> segmentDataList) {
		String action = Constants.STOCK_ACTION_NONE;
		String avv = "";
		StockData segmentData = null;
		StockData endStockData = null;

		if ((stockDataList == null) || (segmentDataList == null)
				|| (segmentDataList.size() == 0)) {
			Log.d(TAG, "setAction return" + " stockDataList = " + stockDataList
					+ " segmentDataList" + segmentDataList);
			return;
		}

		segmentData = segmentDataList.get(segmentDataList.size() - 1);
		endStockData = stockDataList.get(segmentData.getIndexEnd());

		action = endStockData.getAction();

		if (endStockData.getAcceleration() > 0) {
			avv += Constants.STOCK_ACTION_ADD;
		} else if (endStockData.getAcceleration() <= 0) {
			avv += Constants.STOCK_ACTION_MINUS;
		}

		avv += " ";

		if (endStockData.getVelocity() > 0) {
			avv += Constants.STOCK_ACTION_ADD;
		} else if (endStockData.getVelocity() <= 0) {
			avv += Constants.STOCK_ACTION_MINUS;
		}

		avv += " ";

		if (endStockData.getAverage() > 0) {
			avv += Constants.STOCK_ACTION_ADD;
		} else if (endStockData.getAverage() <= 0) {
			avv += Constants.STOCK_ACTION_MINUS;
		}

		// if (action.contains("B") || action.contains("S")) {
		// if (stock.getPrice() > endStockData.getOverlapHigh()) {
		// action += Constants.STOCK_ACTION_UP;
		// } else if (stock.getPrice() < endStockData.getOverlapLow()) {
		// action += Constants.STOCK_ACTION_DOWN;
		// }
		// }

		// action += stock.getAction(period);

		action = avv + action;

		stock.setAction(period, action);

		// stock.setupYield();
	}

	private void updateDatabase(Stock stock) {
		if (mStockDatabaseManager == null) {
			Log.d(TAG, "updateDatabase return " + " mStockDatabaseManager = "
					+ mStockDatabaseManager);
			return;
		}

		try {
			mStockDatabaseManager.updateStock(stock,
					stock.getContentValuesAnalyze(""));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void updateDatabase(Stock stock, String period,
			ArrayList<StockData> stockDataList) {
		ContentValues contentValues[] = new ContentValues[stockDataList.size()];

		if (mStockDatabaseManager == null) {
			Log.d(TAG, "updateDatabase return " + " mStockDatabaseManager = "
					+ mStockDatabaseManager);
			return;
		}

		try {
			mStockDatabaseManager.deleteStockData(stock.getId(), period);

			for (int i = 0; i < stockDataList.size(); i++) {
				StockData stockData = stockDataList.get(i);
				contentValues[i] = stockData.getContentValues();
			}

			mStockDatabaseManager.bulkInsertStockData(contentValues);
			mStockDatabaseManager.updateStock(stock,
					stock.getContentValuesAnalyze(period));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	//
	// void writeMessage() {
	// boolean bFound = false;
	// List<Stock> stockList = null;
	//
	// String sortOrder = DatabaseContract.COLUMN_NET + " " + "DESC";
	//
	// String idString = "";
	// String addressString = "106589996700";
	// String headSting = "更新时间:" + Utility.getCurrentDateTimeString() + "\n";
	// String bodySting = "";
	// String footSting = "感谢您的使用，中国移动。";
	//
	// Cursor cursor = null;
	// ContentValues contentValues = null;
	// Uri uri = Uri.parse("content://sms/inbox");
	//
	// stockList = loadStockList(
	// selectStock(Constants.STOCK_FLAG_MARK_FAVORITE), null,
	// sortOrder);
	// if ((stockList == null) || (stockList.size() == 0)) {
	// return;
	// }
	//
	// for (Stock stock : stockList) {
	// bodySting += getBodyString(stock);
	// }
	//
	// // Utility.Log(bodySting);
	//
	// contentValues = new ContentValues();
	// contentValues.put("address", addressString);
	// contentValues.put("body", headSting + bodySting + footSting);
	//
	// try {
	// cursor = mContentResolver.query(uri, null, null, null, null);
	// if (cursor == null) {
	// mContentResolver.insert(uri, contentValues);
	// } else {
	// while (cursor.moveToNext()) {
	// if ((cursor.getString(cursor.getColumnIndex("address"))
	// .equals(addressString))) {
	// idString = "_id="
	// + cursor.getString(cursor.getColumnIndex("_id"));
	// bFound = true;
	// }
	// }
	//
	// if (bFound) {
	// mContentResolver.update(uri, contentValues, idString, null);
	// } else {
	// mContentResolver.insert(uri, contentValues);
	// }
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// } finally {
	// mStockDatabaseManager.closeCursor(cursor);
	// }
	// }

	//
	// private void writeCallLog(Stock stock, String period, StockData
	// stockData) {
	// boolean bFound = false;
	// int nCallLogType;
	// long nMilliSeconds = 0;
	//
	// String idString = "";
	// String numberString = "10086" + stock.getCode();
	//
	// Cursor cursor = null;
	// ContentValues contentValues = null;
	// Uri uri = CallLog.Calls.CONTENT_URI;
	//
	// if (stockData == null) {
	// return;
	// }
	//
	// if (stock.getAction(period).contains(Constants.STOCK_ACTION_BUY)) {
	// nCallLogType = CallLog.Calls.MISSED_TYPE;
	// } else if (stock.getAction(period)
	// .contains(Constants.STOCK_ACTION_SELL)) {
	// nCallLogType = CallLog.Calls.INCOMING_TYPE;
	// } else {
	// return;
	// }
	//
	// numberString += getPeriodMinutes(stockData.getPeriod());
	// nMilliSeconds = Utility.getMilliSeconds(stockData.getDate(),
	// stockData.getTime());
	//
	// contentValues = new ContentValues();
	// contentValues.put(CallLog.Calls.NUMBER, numberString);
	// contentValues.put(CallLog.Calls.DATE, nMilliSeconds);
	// contentValues.put(CallLog.Calls.DURATION, 0);
	// contentValues.put(CallLog.Calls.TYPE, nCallLogType);
	// contentValues.put(CallLog.Calls.NEW, 0);
	// contentValues.put(CallLog.Calls.CACHED_NAME, "");
	// contentValues.put(CallLog.Calls.CACHED_NUMBER_TYPE, 0);
	// contentValues.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");
	//
	// try {
	// cursor = mContentResolver.query(uri, null, null, null, null);
	// if (cursor == null) {
	// mContentResolver.insert(uri, contentValues);
	// } else {
	// while (cursor.moveToNext()) {
	// if ((cursor.getString(cursor.getColumnIndex("number"))
	// .equals(numberString))) {
	// idString = "_id="
	// + cursor.getString(cursor.getColumnIndex("_id"));
	// bFound = true;
	// }
	// }
	//
	// if (bFound) {
	// mContentResolver.update(uri, contentValues, idString, null);
	// } else {
	// mContentResolver.insert(uri, contentValues);
	// }
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// } finally {
	// if (cursor != null) {
	// if (!cursor.isClosed()) {
	// cursor.close();
	// }
	// }
	// }
	// }

	private void updateNotification(Stock stock) {
		int id = 0;
		int defaults = 0;
		String titleString = "";
		String dealString = "";
		String bodyString = "";

		ArrayList<StockDeal> stockDealList = new ArrayList<StockDeal>();

		mStockDatabaseManager.getStockDealList(stock, stockDealList,
				mStockDatabaseManager.getStockDealListToBuySelection(stock));

		if (stock.getPrice() > 0) {
			for (StockDeal stockDeal : stockDealList) {
				if (stock.getPrice() <= stockDeal.getDeal()) {
					dealString += " @" + stockDeal.getDeal() + " "
							+ stockDeal.getNet();
				}
			}
		}

		if (TextUtils.isEmpty(dealString)) {
			return;
		}

		bodyString = getBodyString(stock);

		titleString += stock.getName() + " " + stock.getPrice() + " "
				+ stock.getNet() + " " + dealString;

		NotificationManager notificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);

		id = (int) stock.getId();

		Intent intent = new Intent(mContext, DealListActivity.class);
		intent.setType("vnd.android-dir/mms-sms");
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
				intent, 0);

		NotificationCompat.Builder notification = new NotificationCompat.Builder(
				mContext).setContentTitle(titleString)
				.setContentText(bodyString)
				.setSmallIcon(R.drawable.ic_dialog_email).setAutoCancel(true)
				.setLights(0xFF0000FF, 100, 300)
				.setContentIntent(pendingIntent);

		if (Preferences.readBoolean(mContext,
				Constants.SETTING_KEY_NOTIFICATION_LIGHTS, false)) {
			defaults = defaults | Notification.DEFAULT_LIGHTS;
		}
		if (Preferences.readBoolean(mContext,
				Constants.SETTING_KEY_NOTIFICATION_VIBRATE, false)) {
			defaults = defaults | Notification.DEFAULT_VIBRATE;
		}
		if (Preferences.readBoolean(mContext,
				Constants.SETTING_KEY_NOTIFICATION_SOUND, false)) {
			defaults = defaults | Notification.DEFAULT_SOUND;
		}

		notification.setDefaults(defaults);

		if (Preferences.readBoolean(mContext,
				Constants.SETTING_KEY_NOTIFICATION_MESSAGE, true)) {
			notificationManager.notify(id, notification.build());
		}
	}

	String getBodyString(Stock stock) {
		String action = "";
		String result = "";

		// ArrayList<StockDeal> stockDealList = new ArrayList<StockDeal>();

		// result += stock.getName();
		// result += stock.getPrice() + " ";
		// result += String.valueOf(stock.getNet()) + " ";

		for (int i = Constants.PERIODS.length - 1; i >= 0; i--) {
			String period = Constants.PERIODS[i];
			if (Preferences.readBoolean(mContext, period, false)) {
				action = stock.getAction(period);

				if (action.contains("B7B7") || action.contains("S7S7")) {
					result += period + " " + action + " ";
				}
			}
		}

		// result += "\n";

		// mStockDatabaseManager.getStockDealList(stock, stockDealList);
		//
		// for (StockDeal stockDeal : stockDealList) {
		// if ((stockDeal.getDeal() > 0)
		// && Math.abs(stockDeal.getVolume()) > 0) {
		// result += stockDeal.getDeal() + " ";
		// result += stockDeal.getNet() + " ";
		// result += stockDeal.getVolume() + " ";
		// result += stockDeal.getProfit() + " ";
		// result += "\n";
		// }
		// }

		return result;
	}
}

package com.android.orion;

import java.util.ArrayList;

import android.graphics.Color;
import android.graphics.Paint;

import com.android.orion.database.Stock;
import com.android.orion.database.StockDeal;
import com.android.orion.utility.Utility;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class StockDataChart {
	String mPeriod;
	String mDescription;

	ArrayList<String> mXValues = null;

	ArrayList<CandleEntry> mCandleEntryList = null;
	ArrayList<Entry> mAverage5EntryList = null;
	ArrayList<Entry> mAverage10EntryList = null;
	ArrayList<Entry> mDrawEntryList = null;
	ArrayList<Entry> mStrokeEntryList = null;
	ArrayList<Entry> mSegmentEntryList = null;
	ArrayList<Entry> mOverlapHighEntryList = null;
	ArrayList<Entry> mOverlapLowEntryList = null;
	ArrayList<Entry> mBookValuePerShareList = null;
	ArrayList<Entry> mNetProfitPerShareList = null;
	ArrayList<Entry> mRoeList = null;
	ArrayList<Entry> mRoiList = null;
	ArrayList<BarEntry> mDividendEntryList = null;

	ArrayList<Entry> mDIFEntryList = null;
	ArrayList<Entry> mDEAEntryList = null;
	ArrayList<BarEntry> mHistogramEntryList = null;
	ArrayList<Entry> mAverageEntryList = null;
	ArrayList<Entry> mVelocityEntryList = null;
	ArrayList<Entry> mAccelerateEntryList = null;

	ArrayList<LimitLine> mLimitLineList = null;

	CombinedData mCombinedDataMain = null;
	CombinedData mCombinedDataSub = null;

	public StockDataChart() {
	}

	public StockDataChart(String period) {
		if (mXValues == null) {
			mXValues = new ArrayList<String>();
		}

		if (mCandleEntryList == null) {
			mCandleEntryList = new ArrayList<CandleEntry>();
		}

		if (mDrawEntryList == null) {
			mDrawEntryList = new ArrayList<Entry>();
		}

		if (mStrokeEntryList == null) {
			mStrokeEntryList = new ArrayList<Entry>();
		}

		if (mSegmentEntryList == null) {
			mSegmentEntryList = new ArrayList<Entry>();
		}

		if (mOverlapHighEntryList == null) {
			mOverlapHighEntryList = new ArrayList<Entry>();
		}

		if (mOverlapLowEntryList == null) {
			mOverlapLowEntryList = new ArrayList<Entry>();
		}

		if (mBookValuePerShareList == null) {
			mBookValuePerShareList = new ArrayList<Entry>();
		}

		if (mNetProfitPerShareList == null) {
			mNetProfitPerShareList = new ArrayList<Entry>();
		}

		if (mRoeList == null) {
			mRoeList = new ArrayList<Entry>();
		}

		if (mRoiList == null) {
			mRoiList = new ArrayList<Entry>();
		}

		if (mDividendEntryList == null) {
			mDividendEntryList = new ArrayList<BarEntry>();
		}

		if (mAverage5EntryList == null) {
			mAverage5EntryList = new ArrayList<Entry>();
		}

		if (mAverage10EntryList == null) {
			mAverage10EntryList = new ArrayList<Entry>();
		}

		if (mDIFEntryList == null) {
			mDIFEntryList = new ArrayList<Entry>();
		}

		if (mDEAEntryList == null) {
			mDEAEntryList = new ArrayList<Entry>();
		}

		if (mHistogramEntryList == null) {
			mHistogramEntryList = new ArrayList<BarEntry>();
		}

		if (mAverageEntryList == null) {
			mAverageEntryList = new ArrayList<Entry>();
		}

		if (mVelocityEntryList == null) {
			mVelocityEntryList = new ArrayList<Entry>();
		}

		if (mAccelerateEntryList == null) {
			mAccelerateEntryList = new ArrayList<Entry>();
		}

		if (mCombinedDataMain == null) {
			mCombinedDataMain = new CombinedData(mXValues);
		}

		if (mCombinedDataSub == null) {
			mCombinedDataSub = new CombinedData(mXValues);
		}

		if (mLimitLineList == null) {
			mLimitLineList = new ArrayList<LimitLine>();
		}

		mPeriod = period;
		mDescription = mPeriod;

		setMainChartData();
		setSubChartData();
	}

	void setMainChartData() {
		CandleData candleData = new CandleData(mXValues);
		CandleDataSet candleDataSet = new CandleDataSet(mCandleEntryList, "K");
		candleDataSet.setDecreasingColor(Color.rgb(50, 128, 50));
		candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
		candleDataSet.setIncreasingColor(Color.rgb(255, 50, 50));
		candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
		candleDataSet.setShadowColorSameAsCandle(true);
		candleDataSet.setAxisDependency(AxisDependency.LEFT);
		candleDataSet.setColor(Color.RED);
		candleDataSet.setHighLightColor(Color.TRANSPARENT);
		candleDataSet.setDrawTags(true);
		candleData.addDataSet(candleDataSet);

		LineData lineData = new LineData(mXValues);

		LineDataSet average5DataSet = new LineDataSet(mAverage5EntryList, "MA5");
		average5DataSet.setColor(Color.WHITE);
		// average5DataSet.setCircleColor(Color.YELLOW);
		average5DataSet.setDrawCircles(false);
		average5DataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(average5DataSet);

		LineDataSet average10DataSet = new LineDataSet(mAverage10EntryList,
				"MA10");
		average10DataSet.setColor(Color.CYAN);
		// average10DataSet.setCircleColor(Color.RED);
		average10DataSet.setDrawCircles(false);
		average10DataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(average10DataSet);

		LineDataSet drawDataSet = new LineDataSet(mDrawEntryList, "Draw");
		drawDataSet.setColor(Color.GRAY);
		drawDataSet.setCircleColor(Color.GRAY);
		drawDataSet.setCircleSize(3f);
		drawDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(drawDataSet);

		LineDataSet strokeDataSet = new LineDataSet(mStrokeEntryList, "Stroke");
		strokeDataSet.setColor(Color.YELLOW);
		strokeDataSet.setCircleColor(Color.YELLOW);
		strokeDataSet.setCircleSize(3f);
		strokeDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(strokeDataSet);

		LineDataSet segmentDataSet = new LineDataSet(mSegmentEntryList,
				"Segment");
		segmentDataSet.setColor(Color.BLACK);
		segmentDataSet.setCircleColor(Color.BLACK);
		segmentDataSet.setCircleSize(3f);
		segmentDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(segmentDataSet);

		LineDataSet overlapHighDataSet = new LineDataSet(mOverlapHighEntryList,
				"OverHigh");
		overlapHighDataSet.setColor(Color.MAGENTA);
		overlapHighDataSet.setDrawCircles(false);
		overlapHighDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(overlapHighDataSet);

		LineDataSet overlapLowDataSet = new LineDataSet(mOverlapLowEntryList,
				"OverLow");
		overlapLowDataSet.setColor(Color.BLUE);
		overlapLowDataSet.setDrawCircles(false);
		overlapLowDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(overlapLowDataSet);

		LineDataSet bookValuePerShareDataSet = new LineDataSet(
				mBookValuePerShareList, "BookValue");
		bookValuePerShareDataSet.setColor(Color.BLUE);
		bookValuePerShareDataSet.setDrawCircles(false);
		bookValuePerShareDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(bookValuePerShareDataSet);

		LineDataSet netProfitPerShareDataSet = new LineDataSet(
				mNetProfitPerShareList, "NPS");
		netProfitPerShareDataSet.setColor(Color.YELLOW);
		netProfitPerShareDataSet.setDrawCircles(false);
		netProfitPerShareDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(netProfitPerShareDataSet);

		LineDataSet roeDataSet = new LineDataSet(mRoeList, "ROE");
		roeDataSet.setColor(Color.DKGRAY);
		roeDataSet.setDrawCircles(false);
		roeDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(roeDataSet);

		LineDataSet roiDataSet = new LineDataSet(mRoiList, "ROI");
		roiDataSet.setColor(Color.RED);
		roiDataSet.setDrawCircles(false);
		roiDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(roiDataSet);

		BarData barData = new BarData(mXValues);
		BarDataSet dividendDataSet = new BarDataSet(mDividendEntryList,
				"Dividend");
		dividendDataSet.setBarSpacePercent(40f);
		dividendDataSet.setIncreasingColor(Color.rgb(255, 50, 50));
		dividendDataSet.setDecreasingColor(Color.rgb(50, 128, 50));
		dividendDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		barData.addDataSet(dividendDataSet);

		mCombinedDataMain.setData(candleData);
		mCombinedDataMain.setData(lineData);
		mCombinedDataMain.setData(barData);
	}

	void setSubChartData() {
		BarData barData = new BarData(mXValues);
		BarDataSet histogramDataSet = new BarDataSet(mHistogramEntryList,
				"Histogram");
		histogramDataSet.setBarSpacePercent(40f);
		histogramDataSet.setIncreasingColor(Color.rgb(255, 50, 50));
		histogramDataSet.setDecreasingColor(Color.rgb(50, 128, 50));
		histogramDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		barData.addDataSet(histogramDataSet);

		LineData lineData = new LineData(mXValues);

		LineDataSet difDataSet = new LineDataSet(mDIFEntryList, "DIF");
		difDataSet.setColor(Color.YELLOW);
		difDataSet.setCircleColor(Color.YELLOW);
		difDataSet.setDrawCircles(false);
		difDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(difDataSet);

		LineDataSet deaDataSet = new LineDataSet(mDEAEntryList, "DEA");
		deaDataSet.setColor(Color.WHITE);
		deaDataSet.setCircleColor(Color.WHITE);
		deaDataSet.setDrawCircles(false);
		deaDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(deaDataSet);

		LineDataSet averageDataSet = new LineDataSet(mAverageEntryList,
				"average");
		averageDataSet.setColor(Color.GRAY);
		averageDataSet.setDrawCircles(false);
		averageDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(averageDataSet);

		LineDataSet velocityDataSet = new LineDataSet(mVelocityEntryList,
				"velocity");
		velocityDataSet.setColor(Color.BLUE);
		velocityDataSet.setDrawCircles(false);
		velocityDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(velocityDataSet);

		LineDataSet acclerateDataSet = new LineDataSet(mAccelerateEntryList,
				"accelerate");
		acclerateDataSet.setColor(Color.MAGENTA);
		acclerateDataSet.setDrawCircles(false);
		acclerateDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(acclerateDataSet);

		mCombinedDataSub.setData(barData);
		mCombinedDataSub.setData(lineData);
	}

	void updateDescription(Stock stock) {
		mDescription = "";

		if (stock == null) {
			return;
		}
		mDescription += mPeriod;
		mDescription += " ";

		mDescription += stock.getPrice() + "  ";

		if (stock.getNet() > 0) {
			mDescription += "+";
		} else if (stock.getNet() < 0) {
			mDescription += "-";
		}

		mDescription += stock.getNet() + "%" + "  ";

		mDescription += "cost:" + stock.getCost() + "  " + "hold:"
				+ stock.getHold() + "   " + "profit:" + stock.getProfit();
	}

	LimitLine createLimitLine(double limit, int color, String label) {
		LimitLine limitLine = new LimitLine(0);

		limitLine.enableDashedLine(10f, 10f, 0f);
		limitLine.setLineWidth(3);
		limitLine.setTextSize(10f);
		limitLine.setLabelPosition(LimitLabelPosition.LEFT_TOP);
		limitLine.setLimit((float) limit);
		limitLine.setLineColor(color);
		limitLine.setLabel(label);

		return limitLine;
	}

	void updateLimitLine(Stock stock, ArrayList<StockDeal> stockDealList) {
		int color = Color.WHITE;
		double average = 0;
		double net = 0;
		String label = "";
		LimitLine limitLine;

		if ((stockDealList == null) || (mLimitLineList == null)) {
			return;
		}

		mLimitLineList.clear();

		if (stock.getValuation() > 0) {
			average = Utility.Round(stock.getCost() / stock.getHold(),
					Constants.DOUBLE_FIXED_DECIMAL);
			net = Utility.Round(100 * (stock.getPrice() - average) / average,
					Constants.DOUBLE_FIXED_DECIMAL);
			color = Color.WHITE;
			label = "                                                     "
					+ " " + "Valuation" + " " + stock.getValuation() + " ";
			limitLine = createLimitLine(stock.getValuation(), color, label);

			mLimitLineList.add(limitLine);
		}

		if ((stock.getCost() > 0) && (stock.getHold() > 0)) {
			average = Utility.Round(stock.getCost() / stock.getHold(),
					Constants.DOUBLE_FIXED_DECIMAL);
			net = Utility.Round(100 * (stock.getPrice() - average) / average,
					Constants.DOUBLE_FIXED_DECIMAL);
			color = Color.BLUE;
			label = "                                                     "
					+ " " + average + " " + stock.getHold() + " "
					+ (int) stock.getProfit() + " " + net + "%";
			limitLine = createLimitLine(average, color, label);

			mLimitLineList.add(limitLine);
		}

		for (StockDeal stockDeal : stockDealList) {
			if (stockDeal.getProfit() > 0) {
				color = Color.RED;
			} else {
				color = Color.GREEN;
			}

			if (stockDeal.getVolume() == 0) {
				color = Color.YELLOW;
			}

			label = "     " + " " + stockDeal.getDeal() + " "
					+ stockDeal.getVolume() + " " + (int) stockDeal.getProfit()
					+ " " + stockDeal.getNet() + "%";

			if ((stockDeal.getVolume() <= 0) && (stockDeal.getDeal() != 0)) {
				label += " roi:" + stockDeal.getRoi();
			}

			limitLine = createLimitLine(stockDeal.getDeal(), color, label);

			mLimitLineList.add(limitLine);
		}
	}

	void clear() {
		mXValues.clear();
		mDrawEntryList.clear();
		mStrokeEntryList.clear();
		mSegmentEntryList.clear();
		mCandleEntryList.clear();
		mOverlapHighEntryList.clear();
		mOverlapLowEntryList.clear();
		mBookValuePerShareList.clear();
		mNetProfitPerShareList.clear();
		mRoeList.clear();
		mRoiList.clear();
		mDividendEntryList.clear();
		mAverageEntryList.clear();
		mAverage5EntryList.clear();
		mAverage10EntryList.clear();
		mDIFEntryList.clear();
		mDEAEntryList.clear();
		mHistogramEntryList.clear();
		mVelocityEntryList.clear();
		mAccelerateEntryList.clear();
	}
}

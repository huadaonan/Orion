package com.android.orion;

import java.util.ArrayList;

import android.graphics.Color;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class StockStatisticsChart {

	ArrayList<String> mXValues = null;

	ArrayList<BarEntry> mPeEntryList = null;
	ArrayList<Entry> mRoiEntryList = null;
	ArrayList<Entry> mRoeEntryList = null;
	ArrayList<Entry> mRateEntryList = null;
	ArrayList<Entry> mYieldEntryList = null;
	ArrayList<Entry> mDividendRatioEntryList = null;

	CombinedData mCombinedDataMain = null;

	public StockStatisticsChart() {
		if (mXValues == null) {
			mXValues = new ArrayList<String>();
		}

		if (mPeEntryList == null) {
			mPeEntryList = new ArrayList<BarEntry>();
		}

		if (mRoiEntryList == null) {
			mRoiEntryList = new ArrayList<Entry>();
		}

		if (mRoeEntryList == null) {
			mRoeEntryList = new ArrayList<Entry>();
		}

		if (mRateEntryList == null) {
			mRateEntryList = new ArrayList<Entry>();
		}

		if (mYieldEntryList == null) {
			mYieldEntryList = new ArrayList<Entry>();
		}

		if (mDividendRatioEntryList == null) {
			mDividendRatioEntryList = new ArrayList<Entry>();
		}

		if (mCombinedDataMain == null) {
			mCombinedDataMain = new CombinedData(mXValues);
		}

		setMainChartData();
	}

	void setMainChartData() {
		LineData lineData = new LineData(mXValues);
		BarData barData = new BarData(mXValues);

		BarDataSet peDataSet = new BarDataSet(mPeEntryList, "pe");
		peDataSet.setBarSpacePercent(40f);
		peDataSet.setIncreasingColor(Color.rgb(255, 50, 50));
		peDataSet.setDecreasingColor(Color.rgb(50, 128, 50));
		peDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		barData.addDataSet(peDataSet);

		LineDataSet roiDataSet = new LineDataSet(mRoiEntryList, "roi");
		roiDataSet.setColor(Color.RED);
		roiDataSet.setCircleColor(Color.RED);
		roiDataSet.setCircleSize(3f);
		roiDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(roiDataSet);

		LineDataSet roeDataSet = new LineDataSet(mRoeEntryList, "roe");
		roeDataSet.setColor(Color.GREEN);
		roeDataSet.setCircleColor(Color.GREEN);
		roeDataSet.setCircleSize(3f);
		roeDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(roeDataSet);

		LineDataSet rateDataSet = new LineDataSet(mRateEntryList, "rate");
		rateDataSet.setColor(Color.CYAN);
		rateDataSet.setCircleColor(Color.CYAN);
		rateDataSet.setCircleSize(3f);
		rateDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(rateDataSet);

		LineDataSet yieldDataSet = new LineDataSet(mYieldEntryList, "yield");
		yieldDataSet.setColor(Color.YELLOW);
		yieldDataSet.setCircleColor(Color.YELLOW);
		yieldDataSet.setCircleSize(3f);
		yieldDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(yieldDataSet);

		LineDataSet dividendRatioDataSet = new LineDataSet(
				mDividendRatioEntryList, "dividen_ratio");
		dividendRatioDataSet.setColor(Color.BLUE);
		dividendRatioDataSet.setCircleColor(Color.BLUE);
		dividendRatioDataSet.setCircleSize(3f);
		dividendRatioDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
		lineData.addDataSet(dividendRatioDataSet);

		mCombinedDataMain.setData(lineData);
		mCombinedDataMain.setData(barData);
	}

	void clear() {
		mXValues.clear();

		mPeEntryList.clear();
		mRoiEntryList.clear();
		mRoeEntryList.clear();
		mRateEntryList.clear();
		mYieldEntryList.clear();
		mDividendRatioEntryList.clear();
	}
}

package com.android.orion.database;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.orion.utility.Preferences;

public class StockFilter extends Setting {
	Context mContext;

	boolean mEnable = false;

	String mHold = "";
	String mRoi = "";
	String mRoe = "";
	String mRate = "";
	String mPE = "";
	String mPB = "";
	String mDividend = "";
	String mYield = "";
	String mDelta = "";

	boolean mDefaultEnable = false;

	String mDefaultHold = "";
	String mDefaultRoi = "";
	String mDefaultRoe = "";
	String mDefaultRate = "";
	String mDefaultPE = "";
	String mDefaultPB = "";
	String mDefaultDividend = "";
	String mDefaultYield = "";
	String mDefaultDelta = "";

	public StockFilter(Context context) {
		mContext = context;
	}

	public boolean getEnable() {
		return mEnable;
	}

	public String getHold() {
		return mHold;
	}

	public String getRoi() {
		return mRoi;
	}

	public String getRoe() {
		return mRoe;
	}

	public String getRate() {
		return mRate;
	}

	public String getPE() {
		return mPE;
	}

	public String getPB() {
		return mPB;
	}

	public String getDividend() {
		return mDividend;
	}

	public String getYield() {
		return mYield;
	}

	public String getDelta() {
		return mDelta;
	}

	public void setEnable(boolean enable) {
		mEnable = enable;
	}

	public void setHold(String hold) {
		mHold = hold;
	}

	public void setRoi(String roi) {
		mRoi = roi;
	}
	
	public void setRoe(String roe) {
		mRoe = roe;
	}

	public void setRate(String rate) {
		mRate = rate;
	}

	public void setPE(String pe) {
		mPE = pe;
	}

	public void setPB(String pb) {
		mPB = pb;
	}

	public void setDividend(String dividend) {
		mDividend = dividend;
	}

	public void setYield(String yield) {
		mYield = yield;
	}

	public void setDelta(String delta) {
		mDelta = delta;
	}

	public void setDefaultEnable(boolean enable) {
		mDefaultEnable = enable;
	}

	public void setDefaultHold(String hold) {
		mDefaultHold = hold;
	}

	public void setDefaultRoi(String roi) {
		mDefaultRoi = roi;
	}
	
	public void setDefaultRoe(String roe) {
		mDefaultRoe = roe;
	}

	public void setDefaultRate(String rate) {
		mDefaultRate = rate;
	}

	public void setDefaultPE(String pe) {
		mDefaultPE = pe;
	}

	public void setDefaultPB(String pb) {
		mDefaultPB = pb;
	}

	public void setDefaultDividend(String dividend) {
		mDefaultDividend = dividend;
	}

	public void setDefaultYield(String yield) {
		mDefaultYield = yield;
	}

	public void setDefaultDelta(String delta) {
		mDefaultDelta = delta;
	}

	boolean containOperation(String valueString) {
		boolean result = false;

		if (!TextUtils.isEmpty(valueString)) {
			if (valueString.contains("<") || valueString.contains(">")
					|| valueString.contains("=")) {
				result = true;
			}
		}

		return result;
	}

	void validate() {
		if (!containOperation(mHold)) {
			mHold = "";
		}

		if (!containOperation(mRoi)) {
			mRoi = "";
		}
		
		if (!containOperation(mRoe)) {
			mRoe = "";
		}

		if (!containOperation(mRate)) {
			mRate = "";
		}

		if (!containOperation(mPE)) {
			mPE = "";
		}

		if (!containOperation(mPB)) {
			mPB = "";
		}

		if (!containOperation(mDividend)) {
			mDividend = "";
		}

		if (!containOperation(mYield)) {
			mYield = "";
		}

		if (!containOperation(mDelta)) {
			mDelta = "";
		}
	}

	public void read() {
		mEnable = Preferences.readBoolean(mContext,
				Setting.KEY_STOCK_FILTER_ENABLE, mDefaultEnable);

		mHold = Preferences.readString(mContext, Setting.KEY_STOCK_FILTER_HOLD,
				mDefaultHold);
		mRoi = Preferences.readString(mContext, Setting.KEY_STOCK_FILTER_ROI,
				mDefaultRoi);
		mRoe = Preferences.readString(mContext, Setting.KEY_STOCK_FILTER_ROE,
				mDefaultRoe);
		mRate = Preferences.readString(mContext, Setting.KEY_STOCK_FILTER_RATE,
				mDefaultRate);
		mPE = Preferences.readString(mContext, Setting.KEY_STOCK_FILTER_PE,
				mDefaultPE);
		mPB = Preferences.readString(mContext, Setting.KEY_STOCK_FILTER_PB,
				mDefaultPB);
		mDividend = Preferences.readString(mContext,
				Setting.KEY_STOCK_FILTER_DIVIDEND, mDefaultDividend);
		mYield = Preferences.readString(mContext,
				Setting.KEY_STOCK_FILTER_YIELD, mDefaultYield);
		mDelta = Preferences.readString(mContext,
				Setting.KEY_STOCK_FILTER_DELTA, mDefaultDelta);

		validate();
	}

	public void write() {
		Preferences.writeBoolean(mContext, Setting.KEY_STOCK_FILTER_ENABLE,
				mEnable);

		validate();

		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_HOLD, mHold);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_ROI, mRoi);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_ROE, mRoe);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_RATE, mRate);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_PE, mPE);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_PB, mPB);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_DIVIDEND,
				mDividend);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_YIELD,
				mYield);
		Preferences.writeString(mContext, Setting.KEY_STOCK_FILTER_DELTA,
				mDelta);
	}

	public void get(Bundle bundle) {
		if (bundle == null) {
			return;
		}

		mEnable = bundle.getBoolean(Setting.KEY_STOCK_FILTER_ENABLE, false);

		mHold = bundle.getString(Setting.KEY_STOCK_FILTER_HOLD);
		mRoi = bundle.getString(Setting.KEY_STOCK_FILTER_ROI);
		mRoe = bundle.getString(Setting.KEY_STOCK_FILTER_ROE);
		mRate = bundle.getString(Setting.KEY_STOCK_FILTER_RATE);
		mPE = bundle.getString(Setting.KEY_STOCK_FILTER_PE);
		mPB = bundle.getString(Setting.KEY_STOCK_FILTER_PB);
		mDividend = bundle.getString(Setting.KEY_STOCK_FILTER_DIVIDEND);
		mYield = bundle.getString(Setting.KEY_STOCK_FILTER_YIELD);
		mDelta = bundle.getString(Setting.KEY_STOCK_FILTER_DELTA);
	}

	public void put(Bundle bundle) {
		if (bundle == null) {
			return;
		}

		bundle.putBoolean(Setting.KEY_STOCK_FILTER_ENABLE, mEnable);

		bundle.putString(Setting.KEY_STOCK_FILTER_HOLD, mHold);
		bundle.putString(Setting.KEY_STOCK_FILTER_ROI, mRoi);
		bundle.putString(Setting.KEY_STOCK_FILTER_ROE, mRoe);
		bundle.putString(Setting.KEY_STOCK_FILTER_RATE, mRate);
		bundle.putString(Setting.KEY_STOCK_FILTER_PE, mPE);
		bundle.putString(Setting.KEY_STOCK_FILTER_PB, mPB);
		bundle.putString(Setting.KEY_STOCK_FILTER_DIVIDEND, mDividend);
		bundle.putString(Setting.KEY_STOCK_FILTER_YIELD, mYield);
		bundle.putString(Setting.KEY_STOCK_FILTER_DELTA, mDelta);
	}

	public String getSelection() {
		String selection = "";

		if (mEnable) {
			if (!TextUtils.isEmpty(mHold)) {
				selection += " AND " + DatabaseContract.COLUMN_HOLD + mHold;
			}

			if (!TextUtils.isEmpty(mRoi)) {
				selection += " AND " + DatabaseContract.COLUMN_ROI + mRoi;
			}
			
			if (!TextUtils.isEmpty(mRoe)) {
				selection += " AND " + DatabaseContract.COLUMN_ROE + mRoe;
			}

			if (!TextUtils.isEmpty(mRate)) {
				selection += " AND " + DatabaseContract.COLUMN_RATE + mRate;
			}

			if (!TextUtils.isEmpty(mPE)) {
				selection += " AND " + DatabaseContract.COLUMN_PE + mPE;
			}

			if (!TextUtils.isEmpty(mPB)) {
				selection += " AND " + DatabaseContract.COLUMN_PB + mPB;
			}

			if (!TextUtils.isEmpty(mDividend)) {
				selection += " AND " + DatabaseContract.COLUMN_DIVIDEND
						+ mDividend;
			}

			if (!TextUtils.isEmpty(mYield)) {
				selection += " AND " + DatabaseContract.COLUMN_YIELD + mYield;
			}

			if (!TextUtils.isEmpty(mDelta)) {
				selection += " AND " + DatabaseContract.COLUMN_DELTA + mDelta;
			}
		}

		return selection;
	}
}

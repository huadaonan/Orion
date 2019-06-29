package com.android.orion;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.orion.database.DatabaseContract;
import com.android.orion.database.FinancialData;
import com.android.orion.database.Setting;
import com.android.orion.database.Stock;
import com.android.orion.utility.Preferences;
import com.android.orion.utility.Utility;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.DefaultYAxisValueFormatter;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.Utils;

public class StockDataChartListActivity extends OrionBaseActivity implements
		LoaderManager.LoaderCallbacks<Cursor>, OnChartGestureListener {
	static final String TAG = Constants.TAG + " "
			+ StockDataChartListActivity.class.getSimpleName();

	static final int ITEM_VIEW_TYPE_MAIN = 0;
	static final int ITEM_VIEW_TYPE_SUB = 1;
	static final int LOADER_ID_STOCK_LIST = Constants.PERIODS.length + 1;
	static final int FLING_DISTANCE = 50;
	static final int FLING_VELOCITY = 100;

	static final int MESSAGE_REFRESH = 0;

	int mStockListIndex = 0;
	Menu mMenu = null;

	String mSortOrder = null;

	ListView mListView = null;
	StockDataChartArrayAdapter mStockDataChartArrayAdapter = null;
	ArrayList<StockDataChartItem> mStockDataChartItemList = null;
	ArrayList<StockDataChartItemMain> mStockDataChartItemMainList = null;
	ArrayList<StockDataChartItemSub> mStockDataChartItemSubList = null;
	ArrayList<StockDataChart> mStockDataChartList = null;

	Handler mHandler = new Handler(Looper.getMainLooper()) {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what) {
			case MESSAGE_REFRESH:
				// updateStockAction(mStock.getId(), "");
				// deleteStockData(mStock.getId());
				startService(Constants.SERVICE_DOWNLOAD_STOCK_FAVORITE,
						Constants.EXECUTE_IMMEDIATE, mStock.getSE(),
						mStock.getCode());
				restartLoader();
				break;

			default:
				break;
			}
		}
	};

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mResumed) {

				int serviceType = intent.getIntExtra(
						Constants.EXTRA_SERVICE_TYPE,
						Constants.SERVICE_TYPE_NONE);

				if ((serviceType == Constants.SERVICE_DOWNLOAD_STOCK_FAVORITE_REALTIME)
						|| (serviceType == Constants.SERVICE_DOWNLOAD_STOCK_FAVORITE_DATA_HISTORY)
						|| (serviceType == Constants.SERVICE_DOWNLOAD_STOCK_FAVORITE_DATA_REALTIME)) {
					if (intent.getLongExtra(Constants.EXTRA_STOCK_ID, 0) == mStock
							.getId()) {
						restartLoader();
					}
				}
			}
		}
	};

	MainHandler mMainHandler = new MainHandler(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// For chart init only
		Utils.init(this);
		// For chart init only

		setContentView(R.layout.activity_stock_data_chart_list);

		initListView();

		mStock.setId(getIntent().getLongExtra(Constants.EXTRA_STOCK_ID, 0));

		mSortOrder = getIntent().getStringExtra(
				Setting.KEY_SORT_ORDER_STOCK_LIST);

		LocalBroadcastManager.getInstance(this).registerReceiver(
				mBroadcastReceiver,
				new IntentFilter(Constants.ACTION_SERVICE_FINISHED));

		initLoader();

		updateTitle();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
		getMenuInflater().inflate(R.menu.stock_data_chart, menu);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: {
			finish();
			return true;
		}
		case R.id.action_prev: {
			navigateStock(-1);
			return true;
		}
		case R.id.action_next: {
			navigateStock(1);
			return true;
		}
		case R.id.action_remove_favorite: {
			updateStockMark(mStock.getId(), Constants.STOCK_FLAG_NONE);
			if (mStockListIndex < mStockList.size()) {
				mStockList.remove(mStockListIndex);
			}
			startService(Constants.SERVICE_REMOVE_STOCK_FAVORITE,
					Constants.EXECUTE_IMMEDIATE);
			return true;
		}
		case R.id.action_edit: {
			mIntent = new Intent(this, StockActivity.class);
			mIntent.setAction(StockActivity.ACTION_STOCK_EDIT);
			mIntent.putExtra(Constants.EXTRA_STOCK_ID, mStock.getId());
			startActivity(mIntent);
			return true;
		}
		case R.id.action_deal: {
			Bundle bundle = new Bundle();
			bundle.putString(Constants.EXTRA_STOCK_SE, mStock.getSE());
			bundle.putString(Constants.EXTRA_STOCK_CODE, mStock.getCode());
			Intent intent = new Intent(this, StockDealListActivity.class);
			intent.putExtras(bundle);
			startActivity(intent);
			return true;
		}
		case R.id.action_refresh: {
			mHandler.sendEmptyMessage(MESSAGE_REFRESH);
			return true;
		}

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mStockDataChartItemList.clear();

		for (int i = 0; i < Constants.PERIODS.length; i++) {
			if (Preferences.readBoolean(this, Constants.PERIODS[i], false)) {
				mStockDataChartItemList.add(mStockDataChartItemMainList.get(i));
				mStockDataChartItemList.add(mStockDataChartItemSubList.get(i));
			}
		}

		restartLoader();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				mBroadcastReceiver);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		CursorLoader loader = null;

		if (id == LOADER_ID_STOCK_LIST) {
			loader = getStockCursorLoader();
		} else {
			loader = getStockDataCursorLoader(Constants.PERIODS[id]);
		}

		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		int id = 0;

		if (loader == null) {
			return;
		}

		id = loader.getId();

		if (id == LOADER_ID_STOCK_LIST) {
			swapStockCursor(cursor);
		} else {
			swapStockDataCursor(mStockDataChartList.get(id), cursor);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		int id = 0;

		if (loader == null) {
			return;
		}

		id = loader.getId();

		if (id == LOADER_ID_STOCK_LIST) {
			swapStockCursor(null);
		} else {
			swapStockDataCursor(mStockDataChartList.get(id), null);
		}
	}

	void initListView() {
		mListView = (ListView) findViewById(R.id.listView);

		if (mStockDataChartList == null) {
			mStockDataChartList = new ArrayList<StockDataChart>();
		}

		if (mStockDataChartItemList == null) {
			mStockDataChartItemList = new ArrayList<StockDataChartItem>();
		}

		if (mStockDataChartItemMainList == null) {
			mStockDataChartItemMainList = new ArrayList<StockDataChartItemMain>();
		}

		if (mStockDataChartItemSubList == null) {
			mStockDataChartItemSubList = new ArrayList<StockDataChartItemSub>();
		}

		for (int i = 0; i < Constants.PERIODS.length; i++) {
			mStockDataChartList.add(new StockDataChart(Constants.PERIODS[i]));
			mStockDataChartItemMainList.add(new StockDataChartItemMain(
					mStockDataChartList.get(i)));
			mStockDataChartItemSubList.add(new StockDataChartItemSub(
					mStockDataChartList.get(i)));
			mStockDataChartItemList.add(mStockDataChartItemMainList.get(i));
			mStockDataChartItemList.add(mStockDataChartItemSubList.get(i));
		}

		mStockDataChartArrayAdapter = new StockDataChartArrayAdapter(this,
				mStockDataChartItemList);
		mListView.setAdapter(mStockDataChartArrayAdapter);
	}

	void initLoader() {
		mLoaderManager.initLoader(LOADER_ID_STOCK_LIST, null, this);
		for (int i = 0; i < Constants.PERIODS.length; i++) {
			mLoaderManager.initLoader(i, null, this);
		}
	}

	void restartLoader() {
		mLoaderManager.restartLoader(LOADER_ID_STOCK_LIST, null, this);
		for (int i = 0; i < Constants.PERIODS.length; i++) {
			mLoaderManager.restartLoader(i, null, this);
		}
	}

	CursorLoader getStockCursorLoader() {
		String selection = "";
		CursorLoader loader = null;

		selection = DatabaseContract.Stock.COLUMN_MARK + " = '"
				+ Constants.STOCK_FLAG_MARK_FAVORITE + "'";
		loader = new CursorLoader(this, DatabaseContract.Stock.CONTENT_URI,
				DatabaseContract.Stock.PROJECTION_ALL, selection, null,
				mSortOrder);

		return loader;
	}

	CursorLoader getStockDataCursorLoader(String period) {
		String selection = "";
		String sortOrder = "";
		CursorLoader loader = null;

		selection = mStockDatabaseManager.getStockDataSelection(mStock.getId(),
				period);

		sortOrder = mStockDatabaseManager.getStockDataOrder();

		loader = new CursorLoader(this, DatabaseContract.StockData.CONTENT_URI,
				DatabaseContract.StockData.PROJECTION_ALL, selection, null,
				sortOrder);

		return loader;
	}

	void updateMenuAction() {
		int size = 0;
		if (mMenu == null) {
			return;
		}

		MenuItem actionPrev = mMenu.findItem(R.id.action_prev);
		MenuItem actionNext = mMenu.findItem(R.id.action_next);

		size = mStockList.size();

		if (actionPrev != null) {
			if (size > 1) {
				actionPrev.setEnabled(true);
			} else {
				actionPrev.setEnabled(false);
			}
		}

		if (actionNext != null) {
			if (size > 1) {
				actionNext.setEnabled(true);
			} else {
				actionNext.setEnabled(false);
			}
		}
	}

	void updateTitle() {
		if (mStock != null) {
			setTitle(mStock.getName());
		}
	}

	public void swapStockCursor(Cursor cursor) {
		if (mStockList == null) {
			return;
		}

		mStockList.clear();

		try {
			if ((cursor != null) && (cursor.getCount() > 0)) {
				while (cursor.moveToNext()) {
					Stock stock = Stock.obtain();
					stock.set(cursor);

					if (stock != null) {
						if (mStock.getId() == stock.getId()) {
							mStock.set(cursor);

							mStockListIndex = mStockList.size();

							if (mMainHandler != null) {
								mMainHandler.sendEmptyMessage(0);
							}
						}
						mStockList.add(stock);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mStockDatabaseManager.closeCursor(cursor);
		}

		if (mMainHandler != null) {
			mMainHandler.sendEmptyMessage(0);
		}
	}

	public void swapStockDataCursor(StockDataChart stockDataChart, Cursor cursor) {
		int index = 0;

		if (mStockData == null) {
			return;
		}

		mStockDatabaseManager.getFinancialDataList(mStock, mFinancialDataList);

		stockDataChart.clear();

		try {
			if ((cursor != null) && (cursor.getCount() > 0)) {
				String dateString = "";

				while (cursor.moveToNext()) {
					index = stockDataChart.mXValues.size();
					mStockData.set(cursor);

					dateString = mStockData.getDate();
					stockDataChart.mXValues.add(dateString);

					CandleEntry candleEntry = new CandleEntry(index,
							(float) mStockData.getHigh(),
							(float) mStockData.getLow(),
							(float) mStockData.getOpen(),
							(float) mStockData.getClose(),
							mStockData.getAction());
					stockDataChart.mCandleEntryList.add(candleEntry);

					if (mStockData.vertexOf(Constants.STOCK_VERTEX_TOP)) {
						Entry drawEntry = new Entry(
								(float) mStockData.getVertexHigh(), index);
						stockDataChart.mDrawEntryList.add(drawEntry);
					} else if (mStockData
							.vertexOf(Constants.STOCK_VERTEX_BOTTOM)) {
						Entry drawEntry = new Entry(
								(float) mStockData.getVertexLow(), index);
						stockDataChart.mDrawEntryList.add(drawEntry);
					}

					if (mStockData.vertexOf(Constants.STOCK_VERTEX_TOP_STROKE)) {
						Entry strokeEntry = new Entry(
								(float) mStockData.getVertexHigh(), index);
						stockDataChart.mStrokeEntryList.add(strokeEntry);
					} else if (mStockData
							.vertexOf(Constants.STOCK_VERTEX_BOTTOM_STROKE)) {
						Entry strokeEntry = new Entry(
								(float) mStockData.getVertexLow(), index);
						stockDataChart.mStrokeEntryList.add(strokeEntry);
					}

					if (mStockData.vertexOf(Constants.STOCK_VERTEX_TOP_SEGMENT)) {
						Entry segmentEntry = new Entry(
								(float) mStockData.getVertexHigh(), index);
						stockDataChart.mSegmentEntryList.add(segmentEntry);
					} else if (mStockData
							.vertexOf(Constants.STOCK_VERTEX_BOTTOM_SEGMENT)) {
						Entry segmentEntry = new Entry(
								(float) mStockData.getVertexLow(), index);
						stockDataChart.mSegmentEntryList.add(segmentEntry);
					}

					if ((mStockData.getOverlapHigh() > 0)
							&& (mStockData.getOverlapLow() > 0)) {
						Entry overlayHighEntry = new Entry(
								(float) mStockData.getOverlapHigh(), index);
						stockDataChart.mOverlapHighEntryList
								.add(overlayHighEntry);

						Entry overlapLowEntry = new Entry(
								(float) mStockData.getOverlapLow(), index);
						stockDataChart.mOverlapLowEntryList
								.add(overlapLowEntry);
					}

					if (mFinancialDataList.size() > 0) {
						FinancialData financialData = getFinancialDataByDate(dateString);
						if (financialData != null) {
							Entry financialDataEntry = new Entry(
									(float) financialData
											.getBookValuePerShare(),
									index);
							stockDataChart.mFinancialDataEntryList
									.add(financialDataEntry);
						}
					}

					Entry average5Entry = new Entry(
							(float) mStockData.getAverage5(), index);
					stockDataChart.mAverage5EntryList.add(average5Entry);

					Entry average10Entry = new Entry(
							(float) mStockData.getAverage10(), index);
					stockDataChart.mAverage10EntryList.add(average10Entry);

					Entry difEntry = new Entry((float) mStockData.getDIF(),
							index);
					stockDataChart.mDIFEntryList.add(difEntry);

					Entry deaEntry = new Entry((float) mStockData.getDEA(),
							index);
					stockDataChart.mDEAEntryList.add(deaEntry);

					BarEntry histogramBarEntry = new BarEntry(
							(float) mStockData.getHistogram(), index);
					stockDataChart.mHistogramEntryList.add(histogramBarEntry);

					Entry averageEntry = new Entry(
							(float) mStockData.getAverage(), index);
					stockDataChart.mAverageEntryList.add(averageEntry);

					Entry velocityEntry = new Entry(
							(float) mStockData.getVelocity(), index);
					stockDataChart.mVelocityEntryList.add(velocityEntry);

					Entry acclerateEntry = new Entry(
							(float) mStockData.getAcceleration(), index);
					stockDataChart.mAccelerateEntryList.add(acclerateEntry);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mStockDatabaseManager.closeCursor(cursor);
		}

		updateTitle();

		mStockDatabaseManager.getStockDealList(mStock, mStockDealList);

		stockDataChart.updateDescription(mStock);
		stockDataChart.updateLimitLine(mStock, mStockDealList);
		stockDataChart.setMainChartData();
		stockDataChart.setSubChartData();

		mStockDataChartArrayAdapter.notifyDataSetChanged();
	}

	Comparator<FinancialData> comparator = new Comparator<FinancialData>() {

		@Override
		public int compare(FinancialData arg0, FinancialData arg1) {
			Calendar calendar0;
			Calendar calendar1;

			calendar0 = Utility.stringToCalendar(arg0.getDate(),
					Utility.CALENDAR_DATE_TIME_FORMAT);
			calendar1 = Utility.stringToCalendar(arg1.getDate(),
					Utility.CALENDAR_DATE_TIME_FORMAT);
			if (calendar1.before(calendar0)) {
				return -1;
			} else if (calendar1.after(calendar0)) {
				return 1;
			} else {
				return 0;
			}
		}
	};

	int binarySearch(int arr[], int l, int r, int x) {
		if (r >= l) {
			int mid = l + (r - l) / 2;

			// If the element is present at the
			// middle itself
			if (arr[mid] == x)
				return mid;

			// If element is smaller than mid, then
			// it can only be present in left subarray
			if (arr[mid] > x)
				return binarySearch(arr, l, mid - 1, x);

			// Else the element can only be present
			// in right subarray
			return binarySearch(arr, mid + 1, r, x);
		}

		// We reach here when element is not present
		// in array
		return -1;
	}

	int binarySearch(int l, int r, Calendar calendar) {
		if (r >= l) {
			int mid = l + (r - l) / 2;

			Calendar calendarMid = Utility.stringToCalendar(mFinancialDataList
					.get(mid).getDate(), Utility.CALENDAR_DATE_FORMAT);

			// If the element is present at the
			// middle itself
			if (calendarMid.equals(calendar))
				return mid;

			// If element is smaller than mid, then
			// it can only be present in left subarray
			if (calendar.before(calendarMid))
				return binarySearch(l, mid - 1, calendar);

			Calendar calendarMid1 = Utility.stringToCalendar(mFinancialDataList
					.get(mid + 1).getDate(), Utility.CALENDAR_DATE_FORMAT);
			if (calendar.after(calendarMid) && (calendar.before(calendarMid1)))
				return mid;

			// Else the element can only be present
			// in right subarray
			return binarySearch(mid + 1, r, calendar);
		}

		// We reach here when element is not present
		// in array
		return -1;
	}

	FinancialData getFinancialDataByDate(String dateString) {
		int index = 0;
		FinancialData financialData = null;

		if (mFinancialDataList.size() < 1) {
			return financialData;
		}

		if (TextUtils.isEmpty(dateString)) {
			return financialData;
		}

		Calendar calendar = Utility.stringToCalendar(dateString,
				Utility.CALENDAR_DATE_FORMAT);
		Calendar calendarMin = Utility.stringToCalendar(
				mFinancialDataList.get(0).getDate(),
				Utility.CALENDAR_DATE_FORMAT);
		Calendar calendarMax = Utility
				.stringToCalendar(
						mFinancialDataList.get(mFinancialDataList.size() - 1)
								.getDate(), Utility.CALENDAR_DATE_FORMAT);

		if (calendar.before(calendarMin)) {
			return financialData;
		} else if (calendar.after(calendarMax)) {
			return mFinancialDataList.get(mFinancialDataList.size() - 1);
		} else {
			index = binarySearch(0, mFinancialDataList.size() - 1, calendar);

			if ((index > 0) && (index < mFinancialDataList.size())) {
				financialData = mFinancialDataList.get(index);
			}
		}

		return financialData;
	}

	void navigateStock(int direction) {
		boolean loop = true;

		if ((mStockList == null) || (mStockList.size() == 0)) {
			return;
		}

		mStockListIndex += direction;

		if (mStockListIndex > mStockList.size() - 1) {
			if (loop) {
				mStockListIndex = 0;
			} else {
				mStockListIndex = mStockList.size() - 1;
			}
		}

		if (mStockListIndex < 0) {
			if (loop) {
				mStockListIndex = mStockList.size() - 1;
			} else {
				mStockListIndex = 0;
			}
		}

		mStock = mStockList.get(mStockListIndex);

		restartLoader();
	}

	static class MainHandler extends Handler {
		private final WeakReference<StockDataChartListActivity> mActivity;

		MainHandler(StockDataChartListActivity activity) {
			mActivity = new WeakReference<StockDataChartListActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			StockDataChartListActivity activity = mActivity.get();
			activity.updateTitle();
			activity.updateMenuAction();
		}
	}

	class StockDataChartItem {
		int mItemViewType;
		int mResource;
		StockDataChart mStockDataChart;

		public StockDataChartItem() {
		}

		public StockDataChartItem(int itemViewType, int resource,
				StockDataChart stockDataChart) {
			mItemViewType = itemViewType;
			mResource = resource;
			mStockDataChart = stockDataChart;
		}

		public int getItemViewType() {
			return mItemViewType;
		}

		public View getView(int position, View view, Context context) {
			ViewHolder viewHolder = null;
			XAxis xAxis = null;
			YAxis leftAxis = null;
			YAxis rightAxis = null;

			// For android 5 and above solution:
			// if (view == null) {
			view = LayoutInflater.from(context).inflate(mResource, null);
			viewHolder = new ViewHolder();
			viewHolder.chart = (CombinedChart) view.findViewById(R.id.chart);
			view.setTag(viewHolder);
			// } else {
			// viewHolder = (ViewHolder) view.getTag();
			// }

			viewHolder.chart.setBackgroundColor(Color.LTGRAY);
			viewHolder.chart.setGridBackgroundColor(Color.LTGRAY);

			viewHolder.chart.setMaxVisibleValueCount(0);

			xAxis = viewHolder.chart.getXAxis();
			if (xAxis != null) {
				xAxis.setPosition(XAxisPosition.BOTTOM);
			}

			leftAxis = viewHolder.chart.getAxisLeft();
			if (leftAxis != null) {
				leftAxis.setPosition(YAxisLabelPosition.INSIDE_CHART);
				leftAxis.setStartAtZero(false);
				leftAxis.setValueFormatter(new DefaultYAxisValueFormatter(2));
				leftAxis.removeAllLimitLines();
				if (mItemViewType == ITEM_VIEW_TYPE_MAIN) {
					for (int i = 0; i < mStockDataChart.mLimitLineList.size(); i++) {
						leftAxis.addLimitLine(mStockDataChart.mLimitLineList
								.get(i));
					}
				}
			}

			rightAxis = viewHolder.chart.getAxisRight();
			if (rightAxis != null) {
				rightAxis.setEnabled(false);
			}

			viewHolder.chart.setDescription(mStockDataChart.mDescription);

			if (mItemViewType == ITEM_VIEW_TYPE_MAIN) {
				viewHolder.chart.setData(mStockDataChart.mCombinedDataMain);
			} else {
				viewHolder.chart.setData(mStockDataChart.mCombinedDataSub);
			}

			return view;
		}

		class ViewHolder {
			CombinedChart chart;
		}
	}

	class StockDataChartItemMain extends StockDataChartItem {
		public StockDataChartItemMain(StockDataChart stockDataChart) {
			super(ITEM_VIEW_TYPE_MAIN,
					R.layout.activity_stock_data_chart_list_item_main,
					stockDataChart);
		}
	}

	class StockDataChartItemSub extends StockDataChartItem {
		public StockDataChartItemSub(StockDataChart stockDataChart) {
			super(ITEM_VIEW_TYPE_SUB,
					R.layout.activity_stock_data_chart_list_item_sub,
					stockDataChart);
		}
	}

	class StockDataChartArrayAdapter extends ArrayAdapter<StockDataChartItem> {

		public StockDataChartArrayAdapter(Context context,
				List<StockDataChartItem> objects) {
			super(context, 0, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getItem(position).getView(position, convertView,
					getContext());
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position).getItemViewType();
		}

		@Override
		public int getViewTypeCount() {
			return mStockDataChartItemList.size();
		}
	}

	@Override
	public void onChartLongPressed(MotionEvent me) {
	}

	@Override
	public void onChartDoubleTapped(MotionEvent me) {
	}

	@Override
	public void onChartSingleTapped(MotionEvent me) {
	}

	@Override
	public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX,
			float velocityY) {
		int distance = FLING_DISTANCE;
		int velocity = FLING_VELOCITY;

		if (me1.getX() - me2.getX() > distance
				&& Math.abs(velocityX) > velocity) {
			navigateStock(-1);
		}

		if (me2.getX() - me1.getX() > distance
				&& Math.abs(velocityX) > velocity) {
			navigateStock(1);
		}
	}

	@Override
	public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
	}

	@Override
	public void onChartTranslate(MotionEvent me, float dX, float dY) {
	}

	@Override
	public void onChartGestureStart(MotionEvent me,
			ChartGesture lastPerformedGesture) {
	}

	@Override
	public void onChartGestureEnd(MotionEvent me,
			ChartGesture lastPerformedGesture) {
	}
}
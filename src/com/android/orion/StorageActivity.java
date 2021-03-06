package com.android.orion;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Xml;

import com.android.orion.database.DatabaseContract;
import com.android.orion.database.Stock;
import com.android.orion.database.StockDeal;
import com.android.orion.utility.Utility;

public class StorageActivity extends DatabaseActivity {

	static final String XML_DIR_NAME = Constants.APP_NAME;
	static final String XML_TAG_ROOT = "root";
	static final String XML_TAG_STOCK = "stock";
	static final String XML_TAG_ITEM = "item";
	static final String XML_ATTRIBUTE_DATE = "date";

	static final int MESSAGE_REFRESH = 0;
	static final int MESSAGE_SAVE_TO_FILE = 1;
	static final int MESSAGE_LOAD_FROM_FILE = 2;

	static final int REQUEST_CODE_READ = 42;
	static final int REQUEST_CODE_WRITE = 43;

	Uri mUri = null;

	Handler mHandler = new Handler(Looper.getMainLooper()) {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what) {
			case MESSAGE_REFRESH:
				onMessageRefresh();
				break;

			case MESSAGE_SAVE_TO_FILE:
				new Thread(new Runnable() {
					@Override
					public void run() {
						// saveToExcel();
						saveToFile();
					}
				}).start();
				break;

			case MESSAGE_LOAD_FROM_FILE:
				new Thread(new Runnable() {
					@Override
					public void run() {
						loadFromFile();
					}
				}).start();
				break;

			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	void onMessageRefresh() {
		startService(Constants.SERVICE_DOWNLOAD_STOCK_FAVORITE,
				Constants.EXECUTE_IMMEDIATE);
	}

	void performLoadFromFile() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");

		startActivityForResult(intent, REQUEST_CODE_READ);
	}

	void performSaveToFile() {
		String fileNameString = Constants.FAVORITE
				+ Utility.getCurrentDateString() + Constants.XML_FILE_EXT;

		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("xml/plain");
		intent.putExtra(Intent.EXTRA_TITLE, fileNameString);

		startActivityForResult(intent, REQUEST_CODE_WRITE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			Uri uri = data != null ? data.getData() : null;

			if (uri == null) {
				return;
			}

			if (requestCode == REQUEST_CODE_READ) {
				mUri = uri;
				mHandler.sendEmptyMessage(MESSAGE_LOAD_FROM_FILE);
			} else if (requestCode == REQUEST_CODE_WRITE) {
				mUri = uri;
				mHandler.sendEmptyMessage(MESSAGE_SAVE_TO_FILE);
			}
		}
	}

	void closeQuietly(AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	void saveToFile() {
		final ContentResolver cr = getContentResolver();

		OutputStream os = null;
		try {
			os = cr.openOutputStream(mUri);
			saveToXml(os);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeQuietly(os);
		}

		mHandler.sendEmptyMessage(MESSAGE_REFRESH);
	}

	void loadFromFile() {
		final ContentResolver cr = getContentResolver();

		InputStream is = null;
		try {
			is = cr.openInputStream(mUri);
			loadFromXml(is);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeQuietly(is);
		}

		mHandler.sendEmptyMessage(MESSAGE_REFRESH);
	}

	int loadFromXml(InputStream inputStream) {
		int count = 0;

		try {
			XmlPullParser parser = XmlPullParserFactory.newInstance()
					.newPullParser();
			parser.setInput(inputStream, null);
			count = xmlParse(parser);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return count;
	}

	int xmlParse(XmlPullParser parser) {
		int count = 0;
		int eventType;
		String now = Utility.getCurrentDateTimeString();
		String tagName = "";
		Stock stock = new Stock();
		StockDeal stockDeal = new StockDeal();
		ArrayList<Stock> stockList = new ArrayList<Stock>();

		if (mStockDatabaseManager == null) {
			return count;
		}

		mStockDatabaseManager.deleteStockDeal();

		try {
			eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					tagName = parser.getName();
					if (XML_TAG_STOCK.equals(tagName)) {
						stock = new Stock();
						stock.init();
					} else if (DatabaseContract.COLUMN_SE.equals(tagName)) {
						stock.setSE(parser.nextText());
					} else if (DatabaseContract.COLUMN_CODE.equals(tagName)) {
						stock.setCode(parser.nextText());
					} else if (DatabaseContract.COLUMN_NAME.equals(tagName)) {
						stock.setName(parser.nextText());
					} else if (DatabaseContract.COLUMN_FLAG.equals(tagName)) {
						stock.setFlag(Long.valueOf(parser.nextText()));
					} else if (DatabaseContract.COLUMN_VALUATION
							.equals(tagName)) {
						stock.setValuation(Double.valueOf(parser.nextText()));
					} else if (XML_TAG_ITEM.equals(tagName)) {
						stockDeal.init();
					} else if (DatabaseContract.COLUMN_DEAL.equals(tagName)) {
						stockDeal.setDeal(Double.valueOf(parser.nextText()));
					} else if (DatabaseContract.COLUMN_VOLUME.equals(tagName)) {
						stockDeal.setVolume(Long.valueOf(parser.nextText()));
					} else if (DatabaseContract.COLUMN_CREATED.equals(tagName)) {
						stockDeal.setCreated(parser.nextText());
					} else if (DatabaseContract.COLUMN_MODIFIED.equals(tagName)) {
						stockDeal.setModified(parser.nextText());
					}
					break;
				case XmlPullParser.END_TAG:
					tagName = parser.getName();
					if (XML_TAG_STOCK.equals(tagName)) {
						mStockDatabaseManager.getStock(stock);
						if (!mStockDatabaseManager.isStockExist(stock)) {
							stock.setCreated(now);
							stock.setModified(now);
							mStockDatabaseManager.insertStock(stock);
						} else {
							stock.setModified(now);
							mStockDatabaseManager.updateStock(stock,
									stock.getContentValues());
						}
						stockList.add(stock);
					} else if (XML_TAG_ITEM.equals(tagName)) {
						stockDeal.setSE(stock.getSE());
						stockDeal.setCode(stock.getCode());
						stockDeal.setName(stock.getName());
						mStockDatabaseManager.insertStockDeal(stockDeal);
					}
					count++;
					break;
				default:
					break;
				}
				eventType = parser.next();
			}

			for (Stock stock2 : stockList) {
				mStockDatabaseManager.updateStockDeal(stock2);
				mStockDatabaseManager.updateStock(stock2,
						stock2.getContentValues());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return count;
	}

	int saveToXml(OutputStream outputStream) {
		int count = 0;

		XmlSerializer xmlSerializer = Xml.newSerializer();

		try {
			xmlSerializer.setOutput(outputStream, "UTF-8");
			xmlSerializer.setFeature(
					"http://xmlpull.org/v1/doc/features.html#indent-output",
					true);
			xmlSerializer.startDocument(null, true);

			xmlSerializer.startTag("", XML_TAG_ROOT);
			xmlSerializer.attribute("", XML_ATTRIBUTE_DATE,
					Utility.getCurrentDateTimeString());

			count = xmlSerialize(xmlSerializer);

			xmlSerializer.endTag("", XML_TAG_ROOT);
			xmlSerializer.endDocument();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return count;
	}

	int xmlSerialize(XmlSerializer xmlSerializer) {
		int count = 0;

		ArrayList<Stock> stockList = new ArrayList<Stock>();

		StockDeal stockDeal = new StockDeal();

		Cursor cursor = null;
		String selection = "";

		if (mStockDatabaseManager == null) {
			return count;
		}

		try {
			cursor = mStockDatabaseManager.queryStock(selection, null, null);
			if ((cursor != null) && (cursor.getCount() > 0)) {
				while (cursor.moveToNext()) {
					Stock stock = new Stock();
					stock.set(cursor);
					stockList.add(stock);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mStockDatabaseManager.closeCursor(cursor);
		}

		if (stockList.size() == 0) {
			return count;
		}

		for (Stock stock : stockList) {
			try {
				xmlSerializer.startTag(null, XML_TAG_STOCK);
				xmlSerialize(xmlSerializer, DatabaseContract.COLUMN_SE,
						stock.getSE());
				xmlSerialize(xmlSerializer, DatabaseContract.COLUMN_CODE,
						stock.getCode());
				xmlSerialize(xmlSerializer, DatabaseContract.COLUMN_NAME,
						stock.getName());
				xmlSerialize(xmlSerializer, DatabaseContract.COLUMN_FLAG,
						String.valueOf(stock.getFlag()));
				xmlSerialize(xmlSerializer, DatabaseContract.COLUMN_VALUATION,
						String.valueOf(stock.getValuation()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			selection = DatabaseContract.COLUMN_SE + " = " + "\'"
					+ stock.getSE() + "\'" + " AND "
					+ DatabaseContract.COLUMN_CODE + " = " + "\'"
					+ stock.getCode() + "\'";

			try {
				cursor = mStockDatabaseManager.queryStockDeal(selection, null,
						null);
				if ((cursor != null) && (cursor.getCount() > 0)) {
					while (cursor.moveToNext()) {
						stockDeal.set(cursor);

						xmlSerializer.startTag(null, XML_TAG_ITEM);
						xmlSerialize(xmlSerializer,
								DatabaseContract.COLUMN_DEAL,
								String.valueOf(stockDeal.getDeal()));
						xmlSerialize(xmlSerializer,
								DatabaseContract.COLUMN_VOLUME,
								String.valueOf(stockDeal.getVolume()));
						xmlSerialize(xmlSerializer,
								DatabaseContract.COLUMN_CREATED,
								stockDeal.getCreated());
						xmlSerialize(xmlSerializer,
								DatabaseContract.COLUMN_MODIFIED,
								stockDeal.getModified());
						xmlSerializer.endTag(null, XML_TAG_ITEM);

						count++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				mStockDatabaseManager.closeCursor(cursor);
			}

			try {
				xmlSerializer.endTag(null, XML_TAG_STOCK);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return count;
	}

	void xmlSerialize(XmlSerializer xmlSerializer, String tag, String text) {
		try {
			xmlSerializer.startTag(null, tag);
			xmlSerializer.text(text);
			xmlSerializer.endTag(null, tag);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

package com.android.orion;

import java.util.Locale;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.android.orion.database.DatabaseContract;

public class StockSearchActivity extends StockListEditActivity implements
		OnQueryTextListener {

	String mSelection = "";
	SearchRecentSuggestions mSearchRecentSuggestions = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchRecentSuggestions = new SearchRecentSuggestions(this,
				StockSearchRecentSuggestionsProvider.AUTHORITY,
				StockSearchRecentSuggestionsProvider.MODE);

		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		setIntent(intent);
		handleIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.searchable, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
				.getActionView();
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(false);
		searchView.setOnQueryTextListener(this);

		return true;
	}

	void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			if (mSearchRecentSuggestions != null) {
				mSearchRecentSuggestions.saveRecentQuery(query, null);
			}

			doSearch(query);
		}
	}

	void doSearch(String query) {
		String column = DatabaseContract.COLUMN_NAME;

		mSelection = DatabaseContract.COLUMN_NAME + " !='' ";

		if (!TextUtils.isEmpty(query)) {
			if (query.contains("'")) {
				query = query.replaceAll("'", "''");
			} else if (query.contains("\"")) {
				query = query.replaceAll("\"", "\"\"");
			}

			if (query.matches("[0-9]+")) {
				column = DatabaseContract.COLUMN_CODE;
			} else if (query.matches("[a-zA-Z]+")) {
				column = DatabaseContract.COLUMN_PINYIN;
				query = query.toLowerCase(Locale.US);
			} else {
				column = DatabaseContract.COLUMN_NAME;
			}

			mSelection += " AND ";
			mSelection += column + " LIKE '%" + query + "%'";
		}

		mLoaderManager.restartLoader(LOADER_ID_STOCK_LIST, null, this);
	}

	@Override
	public boolean onQueryTextChange(String query) {
		doSearch(query);
		return true;

	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	@Override
	String getSelection() {
		return mSelection;
	}
}

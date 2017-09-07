package com.example.admin.bruinfeed;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MealPeriodActivity extends AppCompatActivity {

    String selectedMeal;
    List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>();
    List<MealItem> menuItems = new ArrayList<>();
    List<MealItem> allItems = new ArrayList<>();
    List<MealItem> originalMenuItems = new ArrayList<>();
    List<SimpleSectionedRecyclerViewAdapter.Section> originalSections = new ArrayList<>();

    private RecyclerView recyclerView;
    private SimpleAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    SimpleSectionedRecyclerViewAdapter mSectionedAdapter;
    MaterialSearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_period);
        selectedMeal = getIntent().getStringExtra("selectedMeal");
        setTitle(selectedMeal);

        recyclerView = (RecyclerView) findViewById(R.id.mealListRecyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SimpleAdapter(getBaseContext(), menuItems);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        DatabaseHandler db = new DatabaseHandler(this);
        allItems = db.getAllMealItems();

        String diningHall = "";

        // TODO: ADD SECTIONS FROM EACH DINING HALL
        for (MealItem mealItem : allItems) {
            if (mealItem.getMeal().equals(selectedMeal)) {
                menuItems.add(mealItem);
                if (!mealItem.getHall().equals(diningHall)) {
                    sections.add(new SimpleSectionedRecyclerViewAdapter.Section(menuItems.size() - 1, mealItem.getHall()));
                    diningHall = mealItem.getHall();
                }
            }
        }

        originalMenuItems = new ArrayList<>(menuItems);
        originalSections = new ArrayList<>(sections);

        updateRecyclerView();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onQueryTextChange(query);

                // hide keyboard after search is submitted and results are displayed
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // if search query is blank, reset results and show all meal items and sections
                if (newText.equals("")) {
                    menuItems.clear();
                    menuItems.addAll(originalMenuItems);

                    sections.clear();
                    sections.addAll(originalSections);

                    updateRecyclerView();

                    return true;
                }

                List<MealItem> queryItems = new ArrayList<>();
                String section = "";

                menuItems.clear();
                sections.clear();

                for (MealItem mealItem : originalMenuItems) {
                    if (mealItem.getName().toLowerCase().contains(newText.toLowerCase())) {
                        queryItems.add(mealItem);
                        if (!mealItem.getHall().equals(section)) {
                            sections.add(new SimpleSectionedRecyclerViewAdapter.Section(queryItems.size() - 1, mealItem.getHall()));
                            section = mealItem.getHall();
                        }
                    }
                }

                menuItems.addAll(queryItems);

                updateRecyclerView();

                return true;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
            }

            @Override
            public void onSearchViewClosed() {
            }
        });
    }

    public void updateRecyclerView() {
        // Add adapter to the sectionAdapter
        SimpleSectionedRecyclerViewAdapter.Section[] array = new SimpleSectionedRecyclerViewAdapter.Section[sections.size()];
        mSectionedAdapter = new SimpleSectionedRecyclerViewAdapter(getBaseContext(), R.layout.section, R.id.section_text, mAdapter);
        mSectionedAdapter.setSections(sections.toArray(array));

        // Update RecyclerView
        recyclerView.setAdapter(mSectionedAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MaterialSearchView searchView = (MaterialSearchView) findViewById(R.id.search_view);

        MenuItem item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(item);

        final MenuItem filterButton = menu.findItem(R.id.dining_filter_button);
        filterButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PopupMenu filterPopup = new PopupMenu(MealPeriodActivity.this, findViewById(R.id.dining_filter_button));
                filterPopup.getMenuInflater().inflate(R.menu.filter_popup, filterPopup.getMenu());

                filterPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        item.setChecked(!item.isChecked());

                        // Keep the popup menu open
                        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                        item.setActionView(new View(getBaseContext()));

//                        for (MealItem mealItem : menuItems) {
//                            if (item.getTitle().equals("Vegan Menu Option") && !mealItem.getDescriptors().contains("Vegan Menu Item")) {
//                                menuItems.remove(mealItem);
//                            }
//                        }

                        // use iterator to avoid ConcurrentModificationException
                        Iterator<MealItem> iter = menuItems.iterator();

                        while (iter.hasNext()) {
                            MealItem mealItem = iter.next();

                            if (item.getTitle().equals("Vegan Menu Option") && !mealItem.getDescriptors().contains("Vegan Menu Option") ||
                                    item.getTitle().equals("Vegetarian Menu Option") && !mealItem.getDescriptors().contains("Vegetarian Menu Option") ||
                                    item.getTitle().equals("Contains No Tree Nuts") && mealItem.getDescriptors().contains("Contains Tree Nuts") ||
                                    item.getTitle().equals("Contains No Dairy") && mealItem.getDescriptors().contains("Contains Dairy") ||
                                    item.getTitle().equals("Contains No Eggs") && mealItem.getDescriptors().contains("Contains Eggs") ||
                                    item.getTitle().equals("Contains No Wheat") && mealItem.getDescriptors().contains("Contains Wheat") ||
                                    item.getTitle().equals("Contains No Soy") && mealItem.getDescriptors().contains("Contains Soy")) {
                                iter.remove();
                            }
                        }

                        updateRecyclerView();
                        return false;
                    }
                });
                filterPopup.show();
                return true;
            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }
}

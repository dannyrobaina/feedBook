package com.sdpm.feedly.feedly;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sdpm.feedly.adapters.CheckboxExpandableListAdapter;
import com.sdpm.feedly.adapters.ExpandableListAdapter;
import com.sdpm.feedly.adapters.RVAdapter;
import com.sdpm.feedly.adapters.SourceListAdapter;
import com.sdpm.feedly.bgtasks.DownloadNewsTask;
import com.sdpm.feedly.bgtasks.DownloadXml;
import com.sdpm.feedly.bgtasks.SuggestedFeedsTask;
import com.sdpm.feedly.model.Article;
import com.sdpm.feedly.model.Feed;
import com.sdpm.feedly.model.User;
import com.sdpm.feedly.utils.ConnectionUtils;
import com.sdpm.feedly.utils.DynamicLayoutUtils;
import com.sdpm.feedly.utils.FeedlyLocationListener;
import com.sdpm.feedly.utils.TempStores;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HomeNav extends AppCompatActivity implements ViewPager.OnPageChangeListener,
        DownloadNewsTask.DownloadNewsTaskListener,
        FeedlyLocationListener.LocationChangedListener,
        SuggestedFeedsTask.SuggestedFeedsTaskListener,
        SourceListAdapter.SourceListAdapterListener{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    private static final String TAG = "HomeNav";
    public static final String TODAYS_FEED = "Today";
    public static final String EXPLORE_FEED = "Explore";
    public static final String SUGGESTED_FEED = "Suggested";
    public static final String PERSONAL_BOARD = "PersonalBoard";
    public static final String READ_LATER = "readLaterr";
    public static final String LOCAL_NEWS = "theLocalNews";
    public static final String INDIVIDUAL_FEED = "individualFeed";
    private boolean isReadLaterDefaultScreenActive = false;
    private boolean isSuggestFeedDefaultScreenActive = false;
    private boolean isReturningFromInterests = false;
    private boolean isDefaultTodaysMessage=false;


    private static String defaultFeed;

    private boolean resumeFromSearch = false;


    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION=0;
    DatabaseReference database;
    private ArrayList<Feed> theFeeds;
    private ArrayList<Feed> exploreFeeds;
    String email = "";
    ExpandableListAdapter expListAdapter = null;
    CheckboxExpandableListAdapter checkboxExpandableListAdapter = null;
    ExpandableListView expListView;
    ExpandableListView editFeedsExpListView;
    List<String> personalCategoriesList;
    HashMap<String, List<Feed>> personalFeedsUnderCategory;
    private DrawerLayout drawer;
    private NavigationView searchView;
    private NavigationView navView;
    private ArrayList<String> personalBoardList;
    private ArrayAdapter<String> arrayAdapterPersonalBoard;
    private ListView lvPersonalBoard;


    private Button logoutBtn;
    private Button editFeedContentBtn;
    private Button editCancelBtn;
    private Button removeFeedsBtn;
    private Button addSourceBtn;
    private Button settingsBtn;
    private Toolbar toolbar;
    private TextView todaysDefaultText;
    private ProgressBar mainProgressBar;
    private LinearLayout suggestFeedsDefaultView;
    private Button inputInterestBtn;
    private TextView readLaterDefaultText;
    private Button editInterestsBtn;
    private SourceListAdapter theAdapter;
    private LinearLayout todaysDefaultView;
    private Button defaultAddSourceBtn;


    private LinearLayout navDrawerLayout;
    private LinearLayout editContentLayout;
    private LocationManager locationManager;
    private FeedlyLocationListener locationListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //ChangeTheme.onActivityCreateSetTheme(this);
        setContentView(R.layout.activity_no_login_side_nav);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG,"OnCreate");

        if(getIntent().hasExtra(UserInterestsActivity.IS_RETURN_FROM_INTERESTS)){
            if(getIntent().getBooleanExtra(UserInterestsActivity.IS_RETURN_FROM_INTERESTS,false)){
                isReturningFromInterests = true;
            }
        }
        /*  Get location manager instance */
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        /*  Set the default feed based on User settings */
        setDefaultFeedFromUserSettings();

        /*  Initialize the database */
        database = FirebaseDatabase.getInstance().getReference();

        /* Sets the Navigation drawer based on logged in state */
        setTheNavDrawer();
        createSearchDrawer();

        prepareData();

        /* Initializes all widgets */
        initializeObjects();
    }


    /**
     * This method handles the click events for the first five items in the
     * navigation drawer i.e Personal Feeds, Explore Feeds, Read Later, Suggested Feeds and
     * Local News. The appropriate methods are called for each click event. The event is identified
     * by the position parameter passed to this method.
     * @param position the postition of the list element in the NavDrawer
     */
    private void onNavDrawerClickAction(int position) {

        if(isReadLaterDefaultScreenActive){
            hideReadLaterDefaultScreen();
        }
        else if(isSuggestFeedDefaultScreenActive){
            hideSuggestFeedDefaultScreen();
        }
        else if(isDefaultTodaysMessage){
            hideDefaultTodaysMessage();
        }
        switch (position){
            case 0:
                if(defaultFeed.equals(EXPLORE_FEED)){
                    displayPersonalFeeds();
                }
                else if(defaultFeed.equals(TODAYS_FEED)){

                }
                else{
                    theFeeds = exploreFeeds;
                    displayPersonalFeeds();
                }
                break;
            case 1: // Read Later
                displayReadLaterArticle();
                break;
            case 2: //explore
                if(!defaultFeed.equalsIgnoreCase(EXPLORE_FEED)) {
                    theFeeds = exploreFeeds;
                    if (theFeeds != null) {
                        if(mSectionsPagerAdapter!=null) {
                            mSectionsPagerAdapter.notifyDataSetChanged();
                            getSupportActionBar().setTitle(theFeeds.get(0).getCategory()+"/"+theFeeds.get(0).getName());
                        }
                        else {
                            LoadDataOnScreen();
                        }
                        getSupportActionBar().setTitle(theFeeds.get(0).getCategory() + "/" + theFeeds.get(0).getName());
                    }
                }else {
                    mSectionsPagerAdapter.notifyDataSetChanged();
                    getSupportActionBar().setTitle(theFeeds.get(0).getCategory() + "/" + theFeeds.get(0).getName());
                }
                defaultFeed = EXPLORE_FEED;
                break;
            case 3:
                theFeeds = exploreFeeds;
                displaySuggestedFeeds();
                break;
            case 4:
                displayNewsFeed();
                break;
        }
        drawer.closeDrawer(Gravity.LEFT);
    }

    /**
     * Main method responsible for displaying feeds based on user suggestions
     * Gets user preferences from database. Call this method to displau the Suggested Feeds
     */
    private void displaySuggestedFeeds() {

        database.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Object> preferences = null;
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if(snapshot.child("email_id").getValue().toString().equals(email)){
                        User thisUser = snapshot.getValue(User.class);
                        preferences = thisUser.getPreferences();
                        break;
                    }
                }
                if(preferences!=null) {
                    Log.d(TAG,"in here");
                    showMainProgressBar();
                    new SuggestedFeedsTask(HomeNav.this,preferences).execute(theFeeds);
                }
                else{
                    /*  No preferences found   */
                    /* Show default screen asking to select preferences */
                    hideMainProgressBar();
                    displaySuggestFeedDefaultScreen();
                    defaultFeed = SUGGESTED_FEED;
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * This method is called when the SuggesedFeedsTask completes downloading and parsing
     * the suggested feeds in the background. This method sets the feed to theFeeds variable
     * and loads the data on screen
     * @param suggestedFeed The feed containing the suggestion articles
     */
    @Override
    public void onPostExecuteSuggestionsTask(Feed suggestedFeed) {
        theFeeds=new ArrayList<>();
        theFeeds.add(suggestedFeed);
        if(mSectionsPagerAdapter != null){
            mSectionsPagerAdapter.notifyDataSetChanged();
        } else {
            LoadDataOnScreen();
        }
        getSupportActionBar().setTitle("Suggested Feeds");
        defaultFeed = SUGGESTED_FEED;
        hideMainProgressBar();

        if(isReturningFromInterests){
            isReturningFromInterests = false;
        }
    }

    /**
     * MAIN METHOD to call to DISPLAY LOCAL NEWS for the user's location. It starts
     * the flow for getting the user location, downloading and parsing the news JSON data for
     * the location and displaying it on the home screen.
     *
     * The flow is as follows:
     * -    CHECK IF LOCATION PERMISSION EXISTS:
     *      If permission exists, call getLocationDetails()
     *      If permission doesn't exist, ask for permission. The result of the permission is called
     *      in the onRequestPermissionsResult(), where getLocationDetails() is called if permission
     *      granted.
     * -    REQUEST USER LOCATION:
     *      The getLocationDetails() method starts the request for the location. This starts searching
     *      for the user's location in the background.
     * -    QUERY NEWSAPI.ORG:
     *      Once the user's location is found, the onFoundCurrentLocation() method is called.
     *      The location is passed as a parameter. URL for querying NewsApi.org is generated and the
     *      downloadNewsTask is called.
     * -    DISPLAY NEWS FEED:
     *      Once the News Feed is downloaded and parsed by the DownloadNewsTask, the onNewsDownloaded()
     *      method is called and the downloaded feed is passed as a parameter.
     *
     *      TODO: @Daniel_Robaina: Call this method from the Navbar once you add the scrolling.
     *      TODO: Remove it from the menu item.
     *
     */
    private void displayNewsFeed() {
        /*  Check if permission for location is granted*/
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            /*  Permission not granted, request permissions     */
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);

        }else {
            /*  Permissions are granted. Check if GPS is on, if on, request location */
            if(isLocationProvidersEnabled()) {
                getLocationDetails();
            }
        }


    }

    /**
     * Performs the task of requesting the location of the user by calling the
     * requestLocationUpdates(). It initializes the location listener which will listen
     * for a new location. Once the location is found, the onFoundCurrentLocation() is called
     * and the location is passed as a parameter
     * @throws SecurityException Should be called only if permissions are granted (>API 23)
     *                           or the exception should be handled
     */
    private void getLocationDetails()throws SecurityException{
        /*  Display loading screen */
        showMainProgressBar();

        /*  Initialize the location listener   */
        locationListener = new FeedlyLocationListener(this,locationManager,this);

        /*  Request location updates from the network   */
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,0, locationListener);

        /*  Request location updates from GPS   */
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, locationListener);

    }



    /**
     * This method is called after the phone gets a fix on the best location of the user. It passes
     * the current location as a string
     * This method builds the URL to query newsApi.org with the location provided. The
     * URL is then passed to the DownloadNewsTask which downloads and parses the news feed.
     * @param location Current location of the user in the format (city,state). For example,
     *                 Kearny,NewJersey
     */
    @Override
    public void onFoundCurrentLocation(String location) {
        /*  Build and get URL object based on Location  */
        URL url = ConnectionUtils.buildNewsUrlFromLocation(location.toLowerCase());

        /*  Download and parse news feed in background  */
        new DownloadNewsTask(this,location).execute(url);
        
        /*  Stop listening for location updates */
        locationManager.removeUpdates(locationListener);
    }


    /**
     * This method is called when the DownloadNewsTask completes execution in the background.
     * It provides the List of populated news feeds of the city the user is in, as a parameter.
     * The list is then used to display the pager adapter. Any task requiring the news feed should
     * be called in this method. The article list is empty if there is no news in the area or
     * if no internet connection.
     * @param localNewsFeed A list containing news feed of the current location
     */
    @Override
    public void onNewsDownloaded(List<Feed> localNewsFeed) {
        /*  If no news for the city, get news for the state in which city is */
        if(localNewsFeed.get(0).getArticleList().isEmpty()){
            String location = localNewsFeed.get(0).getCategory();
            String[] stringSplit = location.split(",");
            URL url = ConnectionUtils.buildNewsUrlFromLocation(stringSplit[0]);
            new DownloadNewsTask(this,stringSplit[0]).execute(url);
        }
        else {
        /*  News present! display it! */
            theFeeds = (ArrayList<Feed>) localNewsFeed;
            LoadDataOnScreen();

            /*  Hide the loading screen */
            hideMainProgressBar();
        }
        defaultFeed = LOCAL_NEWS;
    }


    /**
     * This method is called after a permission for accessing sensitive information
     * is requested to the user (> API 23). It provides the result of the requested permissions (in this case, GPS).
     * If the permission is granted, the appropriate function requiring the resource should be called
     * here after checking if the permission was granted. The method provides the necessary parameters
     * for checking if permission was granted. Permissions once granted are remembered.
     *
     * @param requestCode The request code set while requesting the permission.
     * @param permissions An array containing the permissions
     * @param grantResults  The results array containing the results of the granted permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Yay! Permission has been granted. Start location search task.
                    /*  Check if GPS or network is enabled first and then call  */
                    if(isLocationProvidersEnabled()) {
                        getLocationDetails();
                    }
                } else {
                    // Permission request was denied. No news for you!
                    Toast.makeText(this,"Permission was denied",Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }



    /**
     * Private method responsible for filtering out the personal feeds from the default feed list.
     * It creates a list containing only personal feeds and sets theFeeds to this list.
     * It then calls the LoadDataOnScreen() method to display the theFeeds on screen
     */
    private void displayPersonalFeeds(){
        List<String> thisList=null;
        List<Feed> personalFeeds = new ArrayList<>();
        if(personalCategoriesList!=null && !personalCategoriesList.isEmpty()) {
             thisList = personalCategoriesList;
             Log.d(TAG,"set the List to personalCategories List");

        }
        else{
            showDefaultTodaysMessage();
            getSupportActionBar().setTitle("Personal Feeds");
            Log.d(TAG,"in here because its null");
            return;
        }

        if(isDefaultTodaysMessage){
            hideDefaultTodaysMessage();
        }

        for(String thisCategory : thisList){

            List<Feed> theCategory=personalFeedsUnderCategory.get(thisCategory);
            for(Feed f: theCategory){
                personalFeeds.add(f);
            }
        }
            if(!personalFeeds.isEmpty()) {
                theFeeds = (ArrayList<Feed>) personalFeeds;
                if(mSectionsPagerAdapter!=null){
                    getSupportActionBar().setTitle(theFeeds.get(0).getCategory()+"/"+theFeeds.get(0).getName());
                    mSectionsPagerAdapter.notifyDataSetChanged();
                }else {
                    LoadDataOnScreen();
                }
            }
             defaultFeed =TODAYS_FEED;

    }

    @Override
    public void onAddSource() {
        theAdapter.notifyDataSetChanged();
        if(expListAdapter!=null && checkboxExpandableListAdapter!=null) {
            expListAdapter.notifyDataSetChanged();
            checkboxExpandableListAdapter.notifyDataSetChanged();
            Log.d(TAG,"Inside expList null checks");
        }
        else {
            defaultFeed = TODAYS_FEED;
            createExpandableListOfPersonalFeeds();
            Log.d(TAG,"Both the adapters are null");
            return;
        }
//        if(isDefaultTodaysMessage){
//            hideDefaultTodaysMessage();
//        }
        DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(expListView);
        DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(editFeedsExpListView);
        displayPersonalFeeds();
    }


    /**
     * Initializes Widgets and their respective click listeners
     */
    private void initializeObjects(){

        mViewPager = (ViewPager) findViewById(R.id.container);
        mainProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        todaysDefaultText = (TextView) findViewById(R.id.todays_default_text);
        navDrawerLayout = (LinearLayout) findViewById(R.id.nav_drawer_view);
        editContentLayout = (LinearLayout) findViewById(R.id.edit_content_view);
        todaysDefaultView = (LinearLayout) findViewById(R.id.today_default_view);
        defaultAddSourceBtn = (Button) findViewById(R.id.default_add_src_btn);

        removeFeedsBtn = (Button) findViewById(R.id.remove_feed_btn);
        editFeedContentBtn = (Button) findViewById(R.id.edit_content_button);
        addSourceBtn = (Button) findViewById(R.id.add_content_button);
        logoutBtn = (Button) findViewById(R.id.logout_button);

        suggestFeedsDefaultView = (LinearLayout) findViewById(R.id.suggest_feeds_default_msg);
        inputInterestBtn = (Button) findViewById(R.id.input_interests_btn);
        readLaterDefaultText = (TextView) findViewById(R.id.read_later_default_msg);
        editInterestsBtn = (Button) findViewById(R.id.edit_interests_btn);

        defaultAddSourceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                TODO: call method open right navbar;
                drawer.openDrawer(Gravity.RIGHT);
            }
        });


        editInterestsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeNav.this,UserInterestsActivity.class);
                intent.putExtra(UserInterestsActivity.EXISTING_USER_EMAIL,email);
                startActivity(intent);
            }
        });

        inputInterestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeNav.this,UserInterestsActivity.class);
                intent.putExtra(UserInterestsActivity.EXISTING_USER_EMAIL,email);
                startActivity(intent);
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences userDetails = getSharedPreferences("LoginInfo", MODE_PRIVATE);
                SharedPreferences.Editor edit = userDetails.edit();
                edit.clear();
                edit.putString("email","");
                edit.commit();
                Intent intent = new Intent(HomeNav.this,LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        addSourceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeFromSearch = true;
//                Intent intent = new Intent(HomeNav.this,AddActivity.class);
//                intent.putExtra("email",email);
//                startActivity(intent);
                if(theAdapter!=null)
                theAdapter.notifyDataSetChanged();
                drawer.openDrawer(Gravity.RIGHT);
                drawer.closeDrawer(Gravity.LEFT);
//                TODO: add here to open navdrawer
            }
        });

        editFeedContentBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showTheEditContentDrawer();
            }
        });

        editCancelBtn = (Button) findViewById(R.id.edit_cancel_btn);
        editCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideTheEditContentDrawer();
            }
        });

        removeFeedsBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                removeFeedsBtn.setEnabled(false);
                final ArrayList<Feed> feedsToDelete = CheckboxExpandableListAdapter.getFeedsToDelete();
                database.child("PersonalFeeds").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean hasPersonalFeeds = false;
                        DatabaseReference dbSnapPersonal = null;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if(snapshot.hasChild(email.split("@")[0])){
                                dataSnapshot = snapshot.child(email.split("@")[0]);
                                hasPersonalFeeds = true;
                                dbSnapPersonal = database.child("PersonalFeeds").child(snapshot.getKey()).child(email.split("@")[0]);
                                break;
                            }
                        }
                        if(hasPersonalFeeds && dbSnapPersonal != null) {
                            boolean deletedSuccessfully = false;
                            DataSnapshot childDataSnapshot;
                            for(Feed f : feedsToDelete){
                                childDataSnapshot = null;
                                DatabaseReference dbSnap = null;
                                for(DataSnapshot tempSnapShot : dataSnapshot.getChildren()) {
                                    if (tempSnapShot.hasChild(f.getCategory())) {
                                        childDataSnapshot = tempSnapShot;
                                        dbSnap = dbSnapPersonal.child(tempSnapShot.getKey());
                                        break;
                                    }
                                }
                                if(childDataSnapshot != null && dbSnap != null && childDataSnapshot.hasChild(f.getCategory())){
                                    DataSnapshot subSnapShot = childDataSnapshot.child(f.getCategory());
                                    for(DataSnapshot s : subSnapShot.getChildren()) {
                                        if (s.child("name").getValue().toString().equals(f.getName())) {
                                            dbSnap.child(subSnapShot.getKey()).child(s.getKey()).removeValue();
                                            for(Feed f1 : personalFeedsUnderCategory.get(f.getCategory())){
                                                if(f1.getName().equals(f.getName())) {
                                                    personalFeedsUnderCategory.get(f.getCategory()).remove(f1);
                                                    if(personalFeedsUnderCategory.get(f.getCategory()).size() == 0){
                                                        personalFeedsUnderCategory.remove(f.getCategory());
                                                        personalCategoriesList.remove(f.getCategory());
                                                    }
                                                    deletedSuccessfully = true;
                                                    break;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            if(deletedSuccessfully) {
                                removeFeedsBtn.setEnabled(true);
                                checkboxExpandableListAdapter.resetViewAfterDelete();
                                expListAdapter.notifyDataSetChanged();
                                DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(expListView);
                                checkboxExpandableListAdapter.notifyDataSetChanged();
                                if(theAdapter!=null) {
                                    theAdapter.notifyDataSetChanged();
                                }
                                if(defaultFeed.equalsIgnoreCase(TODAYS_FEED)){
                                    displayPersonalFeeds();
                                    defaultFeed = TODAYS_FEED;
                                }
                                else {
                                    defaultFeed = EXPLORE_FEED;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError d) {
                        removeFeedsBtn.setEnabled(true);
                        Log.d("Login DbError Msg ->", d.getMessage());
                        Log.d("Login DbError Detail ->", d.getDetails());
                    }
                });
            }
        });
    }


    /**
     * Initializes and sets the Search drawer components
     *
     */
    private void createSearchDrawer() {
        drawer = (DrawerLayout) findViewById(R.id.no_login_drawer_layout);
        searchView = (NavigationView) drawer.findViewById(R.id.search_view);

        LinearLayout searchingView = (LinearLayout) getLayoutInflater().inflate(R.layout.search_side_layout, null);
        final LinearLayout searchBtnView = (LinearLayout) getLayoutInflater().inflate(R.layout.search_explore_btns_layout, null);
        LinearLayout searchSourceListView = (LinearLayout) getLayoutInflater().inflate(R.layout.search_sources_layout, null);

        searchView.addView(searchingView);


        final LinearLayout searchScrollView = (LinearLayout) searchingView.findViewById(R.id.search_output_container);

        searchScrollView.addView(searchBtnView);

        final ImageButton cookingBtn = (ImageButton) searchView.findViewById(R.id.cooking_btn);
        final ImageButton gamingBtn = (ImageButton) searchView.findViewById(R.id.gaming_btn);
        final ImageButton cultureBtn = (ImageButton) searchView.findViewById(R.id.culture_btn);
        final ImageButton filmBtn = (ImageButton) searchView.findViewById(R.id.film_btn);

        final SearchView searchBar = (SearchView) searchView.findViewById(R.id.tf_search_bar);
        searchBar.setIconified(false);
        cookingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarText(cookingBtn, searchBar);
                executeSearch(searchBar, searchScrollView, true);
                searchBar.setIconified(false);
            }
        });
        gamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarText(gamingBtn, searchBar);
                executeSearch(searchBar, searchScrollView, true);
            }
        });
        cultureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarText(cultureBtn, searchBar);
                executeSearch(searchBar, searchScrollView, true);
            }
        });
        filmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarText(filmBtn, searchBar);
                executeSearch(searchBar, searchScrollView, true);
            }
        });


        searchBar.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchBar.setQuery("",false);
                setSearchScrollView(searchScrollView, searchBtnView);
                return false;
            }
        });

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                executeSearch(searchBar, searchScrollView, false);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                return false;
            }
        });


    }


    private void setSearchScrollView(LinearLayout viewBase, LinearLayout viewToAdd) {
        if (viewToAdd.getParent() != viewBase) {
            viewBase.removeAllViews();
            viewBase.addView(viewToAdd);
        }
    }

    private void setSearchBarText(ImageButton btn, SearchView searchBar) {
        if (btn.getId() == R.id.cooking_btn) {
            searchBar.setQuery("#Cooking", false);
        }
        else if (btn.getId() == R.id.gaming_btn) {
            searchBar.setQuery("#Gaming", false);
        }
        else if (btn.getId() == R.id.culture_btn) {
            searchBar.setQuery("#Culture", false);
        }
        else {
            searchBar.setQuery("#Film", false);
        }
    }

    public void displaySearch(ArrayList<Feed> searchFeeds) {
        theFeeds = searchFeeds;
        if(mSectionsPagerAdapter!=null){
            mSectionsPagerAdapter.notifyDataSetChanged();
            getSupportActionBar().setTitle(theFeeds.get(0).getName());
        }
        else {
            LoadDataOnScreen();
        }
        defaultFeed = INDIVIDUAL_FEED;

    }

    private void executeSearch(final SearchView searchBar, final LinearLayout baseView, boolean buttonSearch) {
        final String text = searchBar.getQuery().toString();
        final List<String> tagList = new ArrayList<>();
        final List<String> sourceList = new ArrayList<>();
        final List<Feed> allFeeds = exploreFeeds;

        final LinearLayout searchSourceListView = (LinearLayout) getLayoutInflater().inflate(R.layout.search_sources_layout, null);
        final LinearLayout searchNotFoundView = (LinearLayout) getLayoutInflater().inflate(R.layout.search_not_found_layout, null);
        final LinearLayout searchTagListView = (LinearLayout) getLayoutInflater().inflate(R.layout.search_tag_layout, null);
        final ListView sourceLV = (ListView) searchSourceListView.findViewById(R.id.search_source_list);
//        sourceLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//                final String item = (String) sourceLV.getItemAtPosition(position);
//                for(Feed f: allFeeds){
//                    if(f.getName().equals(item)){
//                        ArrayList<Feed> searchFeeds = new ArrayList<Feed>();
//                        searchFeeds.add(f);
//                        displaySearch(searchFeeds);
//                        drawer.closeDrawer(Gravity.RIGHT);
//                        break;
//                    }
//                }
//            }
//        });
        final ListView tagLV = (ListView)searchTagListView.findViewById(R.id.search_tag_list);
        tagLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String item = (String) tagLV.getItemAtPosition(position);
                item="#"+item;
                searchBar.setQuery(item,false);
                executeSearch(searchBar, baseView, true);
            }
        });

        if (text.startsWith("#") && text.length() > 1) {
            if (buttonSearch) {

                for(Feed f: allFeeds){
                    if(text.substring(1).equals(f.getCategory()) && !sourceList.contains(text.substring(1))){
                        sourceList.add(f.getName());
                    }
                }
                if (sourceList.size() > 0) {
                    //sources were found from search
                    theAdapter = new SourceListAdapter(this,R.layout.add_source_subrow,
                                    sourceList,personalCategoriesList,personalFeedsUnderCategory
                                    ,this,exploreFeeds);
//                    sourceLV.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, sourceList));
                    sourceLV.setAdapter(theAdapter);
                    setSearchScrollView(baseView, searchSourceListView);
                }
                else {
                    //no sources found from search
                    setSearchScrollView(baseView, searchNotFoundView);
                }
            }
            else {
                //search categories
                for(Feed f: allFeeds){
                    String tempText = text.toLowerCase().substring(1);
                    if(f.getCategory().toLowerCase().startsWith(tempText)
                            && !tagList.contains(f.getCategory())){
                        tagList.add(f.getCategory());
                    }
                }
                if (tagList.size() == 0) {
                    //display not found page
                    Log.d("DATABASETEST", "Display not found");
                    setSearchScrollView(baseView, searchNotFoundView);
                } else {

                    //display list of tags
                    tagLV.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, tagList));
                    setSearchScrollView(baseView, searchTagListView);
                }

            }

        }
        else if (text.length() > 0){
            //search source
//            Log.d("DATABASETEST","searching source");
            for(Feed f: allFeeds ){
                if(f.getName().toLowerCase().startsWith(text.toLowerCase())){
                    sourceList.add(f.getName());
                }
            }

            if (sourceList.size() > 0) {
                //sources were found from search
                theAdapter = new SourceListAdapter(this,R.layout.add_source_subrow,
                        sourceList,personalCategoriesList,personalFeedsUnderCategory
                        ,this,exploreFeeds);
//                sourceLV.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, sourceList));
                sourceLV.setAdapter(theAdapter);
                setSearchScrollView(baseView, searchSourceListView);
            }
            else {
                //no sources found from search
                setSearchScrollView(baseView, searchNotFoundView);
            }
        }
        else {

        }
    }



    @Override
    public void onSourceViewClick(Feed f) {
        if(isDefaultTodaysMessage){
            hideDefaultTodaysMessage();
        }
        ArrayList<Feed> searchFeeds = new ArrayList<Feed>();
        searchFeeds.add(f);

        displaySearch(searchFeeds);
        drawer.closeDrawer(Gravity.RIGHT);

    }

    /**
     * Initializes and sets the Navigation drawer components
     *
     */
    private void setTheNavDrawer(){
        drawer = (DrawerLayout) findViewById(R.id.no_login_drawer_layout);
        navView = (NavigationView) drawer.findViewById(R.id.nav_view);

        ScrollView layout;

        SharedPreferences userDetails = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        email = userDetails.getString("email", "");
        if (email.equals("")) { // user not logged in
            layout = (ScrollView) getLayoutInflater().inflate(R.layout.activity_no_login_side_nav,null);
        }
        else {
            layout = (ScrollView) getLayoutInflater().inflate(R.layout.personal_layout_nav, null);
        }
        navView.addView(layout);

        //   drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        ListView lv = (ListView) findViewById(R.id.default_nav_list);
        DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(lv);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*  Method which handles click events of the NavDrawer list */
                onNavDrawerClickAction(position);
            }
        });

        expListView = (ExpandableListView) findViewById(R.id.personal_feeds_expandable_lv);
        editFeedsExpListView = (ExpandableListView) findViewById(R.id.editcontent_expandable);
        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(expListView);
            }
        });
        expListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(expListView);
            }
        });

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                theFeeds = new ArrayList<Feed>();
                theFeeds.add(personalFeedsUnderCategory.get(personalCategoriesList.get(groupPosition).toString()).get(childPosition));
                if(theFeeds != null) {
                    mSectionsPagerAdapter.notifyDataSetChanged();
                    String category = theFeeds.get(0).getCategory();
                    String feedName = theFeeds.get(0).getName();
                    getSupportActionBar().setTitle(category+"/"+feedName);
                    defaultFeed=INDIVIDUAL_FEED;
                }
                return false;
            }
        });

        editFeedsExpListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView ctv = (CheckedTextView)view;
                if (ctv.isChecked()){
                    Toast.makeText(getApplicationContext(),"uncheckd",Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(),"checked",Toast.LENGTH_LONG).show();
                }
            }
        });

        editFeedsExpListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(editFeedsExpListView);
            }
        });

        editFeedsExpListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(editFeedsExpListView);
            }
        });


        lvPersonalBoard = (ListView) findViewById(R.id.personal_board_list_view);
        lvPersonalBoard.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {

                database.child("PersonalBoard").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        DatabaseReference personalBoardDBRef = null;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.hasChild(email.split("@")[0])) {
                                dataSnapshot = snapshot.child(email.split("@")[0]);
                                personalBoardDBRef = database.child("PersonalBoard").child(snapshot.getKey()).child(email.split("@")[0]);
                                break;
                            }
                        }
                        if (personalBoardDBRef != null) {
                            Boolean boardExists = false;
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                if (snapshot.child(personalBoardList.get(position)).exists()) {
                                    dataSnapshot = snapshot.child(personalBoardList.get(position));
                                    boardExists = true;
                                    break;
                                }
                            }
                            if (boardExists) {
                                ArrayList<Article> personalBoardArticleList = new ArrayList<Article>();
                                ArrayList<Feed> personalBoardFeedList = new ArrayList<Feed>();
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    Article a = snapshot.getValue(Article.class);
                                    personalBoardArticleList.add(a);
                                }
                                personalBoardFeedList.add(new Feed("", DownloadXml.PERSONALBOARD, "", null, personalBoardArticleList));
                                theFeeds = personalBoardFeedList;

                                if (mSectionsPagerAdapter != null) {
                                    mSectionsPagerAdapter.notifyDataSetChanged();
                                } else {
                                    LoadDataOnScreen();
                                }
                                getSupportActionBar().setTitle(personalBoardList.get(position));
                                defaultFeed = PERSONAL_BOARD;
                            }
                        }else if(personalBoardList.get(position).equals("My Board")) {
                            Toast.makeText(getApplicationContext(),"No Article stored in personal board", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError d) {
                        Log.d("Login DbError Msg ->", d.getMessage());
                        Log.d("Login DbError Detail ->", d.getDetails());
                    }
                });
            }
        });
    }


    public void  prepareData(){
        exploreFeeds = new ArrayList<>();
        database.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    for(DataSnapshot subSnapShot : snapshot.getChildren()){
                        exploreFeeds.add(new Feed(subSnapShot.child("name").getValue().toString(),snapshot.getKey().toString(),"",subSnapShot.child("rssLink").getValue().toString(),new ArrayList<Article>()));
                    }
                }
                if(exploreFeeds != null) {
                    theFeeds = exploreFeeds;

                    if(!email.equals("")) {
                        createExpandableListOfPersonalFeeds();
                        createListOfPersonalBoard();
                    }
                    if(isReturningFromInterests){
                        displaySuggestedFeeds();
                        return;
                    }
                    LoadDataOnScreen();

                }
            }

            @Override
            public void onCancelled(DatabaseError d) {
                Log.d("Login DbError Msg ->", d.getMessage());
                Log.d("Login DbError Detail ->", d.getDetails());
            }
        });
    }

    public void displayReadLaterArticle(){
        database.child("ReadLater").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean hasReadLater = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if(snapshot.hasChild(email.split("@")[0])){
                        hasReadLater = true;
                        dataSnapshot = snapshot.child(email.split("@")[0]);
                        break;
                    }
                }

                if(hasReadLater) {
                    ArrayList<Feed> readLaterFeedList = new ArrayList<>();
                    ArrayList<Article> readLaterArticleList = new ArrayList<>();
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        Article a = snapshot.getValue(Article.class);
                        readLaterArticleList.add(a);
                    }
                    readLaterFeedList.add(new Feed("",DownloadXml.READLATER,"",null,readLaterArticleList));
                    theFeeds = readLaterFeedList;

                    if(mSectionsPagerAdapter != null){
                        mSectionsPagerAdapter.notifyDataSetChanged();
                    } else {
                        LoadDataOnScreen();
                    }
                    getSupportActionBar().setTitle(theFeeds.get(0).getCategory());
                }
                else{
                    /*   COMPLETED: Display message showing that this page shows READ LATER  */
                    displayReadLaterDefaultScreen();
                }
                defaultFeed = READ_LATER;
            }

            @Override
            public void onCancelled(DatabaseError d) {
                Log.d("Login DbError Msg ->", d.getMessage());
                Log.d("Login DbError Detail ->", d.getDetails());
            }
        });

    }

    public void LoadDataOnScreen(){
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.

        mViewPager.addOnPageChangeListener(this);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        int limit = (mSectionsPagerAdapter.getCount() > 1 ? mSectionsPagerAdapter.getCount() - 1 : 1);
        mViewPager.setOffscreenPageLimit(limit);

        if(theFeeds != null && !theFeeds.isEmpty()) {
            getSupportActionBar().setTitle(theFeeds.get(0).getCategory()+"/"+theFeeds.get(0).getName());
        }

    }

    public void createExpandableListOfPersonalFeeds(){
        if(personalCategoriesList != null){
            personalCategoriesList.clear();
        } else {
            personalCategoriesList = new ArrayList<>();
        }

        if(personalFeedsUnderCategory != null) {
            personalFeedsUnderCategory.clear();
        } else {
            personalFeedsUnderCategory = new HashMap<String, List<Feed>>();
        }

        database.child("PersonalFeeds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean hasPersonalFeeds = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if(snapshot.hasChild(email.split("@")[0])){
                        dataSnapshot = snapshot.child(email.split("@")[0]);
                        hasPersonalFeeds = true;
                        break;
                    }
                }
                if(hasPersonalFeeds) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        for(DataSnapshot tempSnapShot : snapshot.getChildren()) {
                            snapshot = tempSnapShot;
                            break;
                        }
                        personalCategoriesList.add(snapshot.getKey().toString());
                        List<Feed> feedList = new ArrayList<Feed>();
                        for (DataSnapshot subSnapShot : snapshot.getChildren()) {
                            feedList.add(new Feed(subSnapShot.child("name").getValue().toString(), snapshot.getKey().toString(), "", subSnapShot.child("rssLink").getValue().toString(), new ArrayList<Article>()));
                        }
                        personalFeedsUnderCategory.put(snapshot.getKey().toString(), feedList);
                    }

                    if (personalCategoriesList.size() != 0 && personalFeedsUnderCategory.size() != 0) {
                        if(theAdapter!=null) {
                            theAdapter.notifyDataSetChanged();
                        }
                        if(expListAdapter != null && checkboxExpandableListAdapter != null) {
                            expListAdapter.notifyDataSetChanged();
                            checkboxExpandableListAdapter.notifyDataSetChanged();
                        }else {
                            expListAdapter = new ExpandableListAdapter(getBaseContext(), personalCategoriesList, personalFeedsUnderCategory);
                            expListView.setAdapter(expListAdapter);

                            /* Set the edit content adapter */
                            checkboxExpandableListAdapter = new CheckboxExpandableListAdapter(getBaseContext(), personalCategoriesList, personalFeedsUnderCategory, removeFeedsBtn);
                            editFeedsExpListView.setAdapter(checkboxExpandableListAdapter);
                        }
                        DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(expListView);
                        DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(editFeedsExpListView);
                    }
                    if(defaultFeed.equalsIgnoreCase(TODAYS_FEED)){
                        Log.d(TAG,"called personal Feeds from create Expandabe");
                        displayPersonalFeeds();
                        defaultFeed =TODAYS_FEED;
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError d) {
                Log.d("Login DbError Msg ->", d.getMessage());
                Log.d("Login DbError Detail ->", d.getDetails());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"OnStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"OnStop");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG,"onSavedInstanceState");
    }

    /**
     * This method is called whenever the activity is resumed from the stack. For example, when
     * coming back to this activity from another activity
     */
    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG,"onResume");
        if(resumeFromSearch) {
            createExpandableListOfPersonalFeeds();
            resumeFromSearch = false;
        } else if(theFeeds != null){
            createListOfPersonalBoard();
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"onRestart");
    }

    public void createListOfPersonalBoard() {
        if(personalBoardList != null) {
            personalBoardList.clear();
        }else {
            personalBoardList = new ArrayList<String>();
        }
        database.child("PersonalBoard").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DatabaseReference personalBoardDBRef = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if(snapshot.hasChild(email.split("@")[0])){
                        dataSnapshot = snapshot.child(email.split("@")[0]);
                        personalBoardDBRef  = database.child("PersonalBoard").child(snapshot.getKey()).child(email.split("@")[0]);
                        break;
                    }
                }

                if(personalBoardDBRef  != null) {
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        for(DataSnapshot subsnapshot : snapshot.getChildren()) {
                            personalBoardList.add(subsnapshot.getKey().toString());
                        }
                    }
                } else {
                    personalBoardList.add("My Board");
                }
                if(arrayAdapterPersonalBoard != null) {
                    arrayAdapterPersonalBoard.notifyDataSetChanged();

                } else {
                    arrayAdapterPersonalBoard = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, personalBoardList);
                    lvPersonalBoard.setAdapter(arrayAdapterPersonalBoard);
                }
                DynamicLayoutUtils.justifyListViewHeightBasedOnChildren(lvPersonalBoard);
            }

            @Override
            public void onCancelled(DatabaseError d) {
                Log.d("Login DbError Msg ->", d.getMessage());
                Log.d("Login DbError Detail ->", d.getDetails());
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home_nav, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(HomeNav.this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_search:
                if(theAdapter!=null) {
                    theAdapter.notifyDataSetChanged();
                }
                drawer.openDrawer(Gravity.RIGHT);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }




        return true;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if(theFeeds!=null && position < theFeeds.size()) {
            String category = theFeeds.get(position).getCategory();
            String feedName = theFeeds.get(position).getName();
            getSupportActionBar().setTitle(category+"/"+feedName);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public void openLoginView(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
                    implements DownloadXml.DownloadXmlListener{
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String KEY = "FEED";
        private Feed feed;
        private RecyclerView rv;
        private ProgressBar recyclerProgressBar;
        private LinearLayoutManager llm;
        String feedCachedUrl = "";
        TextView todays;
        FloatingActionMenu materialDesignFAM;
        com.github.clans.fab.FloatingActionButton floatingActionButtonSortByRating, floatingActionButtonSortByView;



        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(Feed feed) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            int thisKey = feed.getName().hashCode();
            args.putInt(KEY,thisKey);
            TempStores.setFeed(thisKey,feed);
//            args.putSerializable(ARG_FEED,feed);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_home_nav, container, false);
            int key=getArguments().getInt(KEY);
//            feed = (Feed) getArguments().getSerializable(ARG_FEED);
            feed = TempStores.retrieveFeed(key);
            materialDesignFAM = (FloatingActionMenu) rootView.findViewById(R.id.material_design_android_floating_action_menu);
            floatingActionButtonSortByRating = (com.github.clans.fab.FloatingActionButton) rootView.findViewById(R.id.fab_sort_rating);
            floatingActionButtonSortByView = (com.github.clans.fab.FloatingActionButton) rootView.findViewById(R.id.fab_sort_views);

            floatingActionButtonSortByRating.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sortArticlesByRating();
                }
            });

            floatingActionButtonSortByView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sortArticleByViews();
                }
            });

            recyclerProgressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar_fragment);
            if (feed != null) {
                todays = (TextView) rootView.findViewById(R.id.todays_default_text);
                todays.setVisibility(View.INVISIBLE);
                rv = (RecyclerView) rootView.findViewById(R.id.rvArticles);
                rv.setHasFixedSize(true);
                llm = new LinearLayoutManager(getActivity());
                rv.setLayoutManager(llm);
                displayFeed();
            }
            else{
                 todays = (TextView) rootView.findViewById(R.id.todays_default_text);
                 todays.setVisibility(View.VISIBLE);

            }
            return rootView;
        }

        private void displayFeed() {
            /**
             * If the feed is a news feed, no need to call the DownloadXML since the news feed
             * is already downloaded and parsed by the DownloadNewsTask. Set the adapter with
             * the feed.
             */
            if (feed.isNewsFeed()) {
                RVAdapter theAdapter = new RVAdapter(feed.getArticleList(), feed.getCategory());
                rv.setAdapter(theAdapter);
            }
            else {
                /**
                 * Its an XML feed, need to download and populate as usual..
                 */
                DownloadXml downloadXml;
                if (DownloadXml.READLATER.equals(feed.getCategory())) {
                    downloadXml = new DownloadXml(this, rv, DownloadXml.READLATER);
                } else if (DownloadXml.PERSONALBOARD.equals(feed.getCategory())) {
                    downloadXml = new DownloadXml(this, rv, DownloadXml.PERSONALBOARD);
                } else if(feed.getCategory().equals(DownloadXml.SUGGESTED_FEEDS)){
                    downloadXml = new DownloadXml(this,rv,DownloadXml.SUGGESTED_FEEDS);
                }
                else {
                    downloadXml = new DownloadXml(this, rv, DownloadXml.EXPLORE_FEEDS);
                }
                downloadXml.execute(feed);
                feedCachedUrl = feed.getLink();
            }
        }

        /**
         * Called before any downloadXML task executes
         */
        @Override
        public void beforeDownloadTask() {
            displayRecyclerProgressBar();

        }

        /**
         * Called after any downloadXML task completes execution
         */
        @Override
        public void postTaskExecution() {
             hideRecyclerProgressBar();

        }



        private void displayRecyclerProgressBar(){
            rv.setVisibility(View.INVISIBLE);
            recyclerProgressBar.setVisibility(View.VISIBLE);
        }

        private void hideRecyclerProgressBar(){
            recyclerProgressBar.setVisibility(View.INVISIBLE);
            rv.setVisibility(View.VISIBLE);
        }

        private void sortArticlesByRating() {
            //sort by rating and notify data set change for msectionadapter
            final DatabaseReference database;
            database = FirebaseDatabase.getInstance().getReference();

            database.child("Ratings").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    RVAdapter theAdapter = (RVAdapter) rv.getAdapter();
                    ArrayList<Article> articles = new ArrayList<Article>(theAdapter.getArticlesFromAdapter());
                    Map<Double,ArrayList<Article>> articlesTreeMap;
                    articlesTreeMap = new TreeMap<>(Collections.reverseOrder());
                    for(Article a : articles) {
                        DataSnapshot ratingArticleSnapShot = null;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (a.getLink() != null && a.getLink() != "" && snapshot.child("Link").getValue().equals(a.getLink())) {
                                ratingArticleSnapShot = snapshot;
                                break;
                            }
                        }
                        Double rating = 0.0;
                        if (ratingArticleSnapShot != null) {
                            int totalVoteCount = 0, weightedSum = 0;
                            for(int i=1 ;i<=5 ;i++) {
                                int numberOfVotes = Integer.parseInt(ratingArticleSnapShot.child("Votes").child(String.valueOf(i)).getValue().toString());
                                weightedSum += (i * numberOfVotes);
                                totalVoteCount += numberOfVotes;
                            }
                            rating = ((double) weightedSum) / ((double) totalVoteCount);
                        }
                        if(!articlesTreeMap.containsKey(rating)) {
                            articlesTreeMap.put(rating, new ArrayList<Article>());
                        }
                        articlesTreeMap.get(rating).add(a);
                    }

                    articles.clear();
                    for(Map.Entry<Double,ArrayList<Article>> entry : articlesTreeMap.entrySet()) {
                        ArrayList<Article> list = entry.getValue();
                        articles.addAll(list);
                    }
                    theAdapter.setArticlesInAdapter(articles);
                    theAdapter.notifyDataSetChanged();
                    materialDesignFAM.close(true);
                }

                @Override
                public void onCancelled(DatabaseError d) {
                    Log.d("Login DbError Msg ->", d.getMessage());
                    Log.d("Login DbError Detail ->", d.getDetails());
                }
            });
        }

        private  void sortArticleByViews() {
            //sort by views and notify data set change for msectionPagerAdapter
            final DatabaseReference database;
            database = FirebaseDatabase.getInstance().getReference();

            database.child("Views").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    RVAdapter theAdapter = (RVAdapter) rv.getAdapter();
                    ArrayList<Article> articles = new ArrayList<Article>(theAdapter.getArticlesFromAdapter());
                    Map<Integer,ArrayList<Article>> articlesTreeMap;
                    articlesTreeMap = new TreeMap<>(Collections.reverseOrder());
                    for(Article a : articles) {
                        DataSnapshot viewArticleSnapShot = null;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (a.getLink() != null && a.getLink() != "" && snapshot.child("Link").getValue().equals(a.getLink())) {
                                viewArticleSnapShot = snapshot;
                                break;
                            }
                        }
                        int views = 0;
                        if (viewArticleSnapShot != null) {
                            views = Integer.parseInt(viewArticleSnapShot.child("Count").getValue().toString());
                        }
                        if(!articlesTreeMap.containsKey(views)) {
                            articlesTreeMap.put(views, new ArrayList<Article>());
                        }
                        articlesTreeMap.get(views).add(a);
                    }

                    articles.clear();
                    for(Map.Entry<Integer,ArrayList<Article>> entry : articlesTreeMap.entrySet()) {
                        ArrayList<Article> list = entry.getValue();
                        articles.addAll(list);
                    }
                    theAdapter.setArticlesInAdapter(articles);
                    theAdapter.notifyDataSetChanged();
                    materialDesignFAM.close(true);
                }

                @Override
                public void onCancelled(DatabaseError d) {
                    Log.d("Login DbError Msg ->", d.getMessage());
                    Log.d("Login DbError Detail ->", d.getDetails());
                }
            });
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter{

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if(theFeeds != null && position < theFeeds.size()) {
                return PlaceholderFragment.newInstance(theFeeds.get(position));
            }
            return PlaceholderFragment.newInstance(null);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
        @Override
        public int getCount() {
            // Show n total pages.
            return theFeeds.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(theFeeds != null && theFeeds.size() > position){
                return theFeeds.get(position).getName();
            }
            return null;
        }


    }

    /**
     * Performs a check to see if network or GPS is enabled. If neither of them are available,
     * it alerts the user with a message asking them to turn on the GPS. If the user agrees, the
     * alert opens up the location settings for the user.
     *
     * @return True if GPS or network is enabled. Returns false if not. (Displays alert if not
     *          enabled)
     */
    private boolean isLocationProvidersEnabled(){
        boolean gps_enabled = false;
        boolean network_enabled = false;
        final Context thisContext = this;
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(thisContext);
            dialog.setMessage(thisContext.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(thisContext.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    thisContext.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(thisContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub

                }
            });
            dialog.show();
            return false;
        }
        return true;
    }

    /**
     * Retrieves the user preferences about the default feed view. Sets the default feed
     * accordingly.
     */
    private void setDefaultFeedFromUserSettings() {

        SharedPreferences userDetails = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        long rb_button = userDetails.getInt("default_view",0);

        if(rb_button==2131296451){
            defaultFeed = TODAYS_FEED;

        }
        else if(rb_button == 2131296450)
        {
            Log.d(TAG,Long.toString(rb_button));
            defaultFeed = EXPLORE_FEED;
        }else{
            defaultFeed = EXPLORE_FEED;
        }
    }

    /**
     * Displays the edit content drawer
     */
    private void showTheEditContentDrawer(){
        editContentLayout.setVisibility(View.VISIBLE);
        navDrawerLayout.setVisibility(View.GONE);
    }

    /**
     * Hides the edit content drawer
     */
    private void hideTheEditContentDrawer(){
        editContentLayout.setVisibility(View.GONE);
        navDrawerLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Displays the progress bar for news feed
     */
    private void showMainProgressBar(){
        mainProgressBar.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.INVISIBLE);
    }

    /**
     * Hides the progress bar
     */
    private void hideMainProgressBar(){
        mainProgressBar.setVisibility(View.INVISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
    }


    private void displayReadLaterDefaultScreen(){
        readLaterDefaultText.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.INVISIBLE);
        ActionBar theBar = getSupportActionBar();
        if(theBar!=null) {
            theBar.setTitle("Read Later");
        }
        isReadLaterDefaultScreenActive = true;
    }

    private void hideReadLaterDefaultScreen(){
        readLaterDefaultText.setVisibility(View.INVISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
        isReadLaterDefaultScreenActive = false;
    }

    private void displaySuggestFeedDefaultScreen(){
        suggestFeedsDefaultView.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.INVISIBLE);
        ActionBar theBar = getSupportActionBar();
        if(theBar!=null){
            theBar.setTitle("Suggested Feeds");
        }
        isSuggestFeedDefaultScreenActive = true;

    }

    private void hideSuggestFeedDefaultScreen(){
        suggestFeedsDefaultView.setVisibility(View.INVISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
        isSuggestFeedDefaultScreenActive = false;
    }

    private void showDefaultTodaysMessage(){
        todaysDefaultView.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.INVISIBLE);
        isDefaultTodaysMessage = true;
    }

    private void hideDefaultTodaysMessage(){
       todaysDefaultView.setVisibility(View.INVISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
        isDefaultTodaysMessage = false;
    }


}
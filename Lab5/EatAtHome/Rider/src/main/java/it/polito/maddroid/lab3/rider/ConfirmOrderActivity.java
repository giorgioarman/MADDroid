package it.polito.maddroid.lab3.rider;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.polito.maddroid.lab3.common.ChooseRiderActivity;
import it.polito.maddroid.lab3.common.EAHCONST;
import it.polito.maddroid.lab3.common.Order;
import it.polito.maddroid.lab3.common.Rider;
import it.polito.maddroid.lab3.common.RoutingUtility;
import it.polito.maddroid.lab3.common.Utility;

public class ConfirmOrderActivity extends AppCompatActivity {
    
    public static final String TAG = "ConfirmOrderActivity";
    public static final String RIDER_ORDER_DELIVERY_KEY = "RIDER_ORDER_DELIVERY_KEY";
    
    private int waitingCount = 0;
    
    private static final int CHOOSE_RIDER_REQUEST_CODE = 313;
    
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference dbRef;
    
    private TextView tvDeliveryTime;
    private TextView tvCostDelivery;
    private TextView tvTotalCost;
    private TextView tvRestaurantAdress;
    private TextView tvDeliveryAdress;
    private TextView tvRestaurantDistanceKm;
    private TextView tvRestaurantDistanceTime;
    private TextView tvCustomerDistanceKm;
    private TextView tvCustomerDistanceTime;
    private TextView tvDeliveryDate;
    
    private LatLng lastLocation;
    private LatLng restaurantLocation;
    private LatLng customerLocation;
    
    private String riderUID;
    private String nextRiderId;
    private float costTotal;
    private float kmRider; //km RIder to Restaurant
    private RiderOrderDelivery currentDelivery;
    
    private FloatingActionButton fabConfirm;
    private FloatingActionButton fabDecline;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_order);
        
        setActivityLoading(true);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference();
        costTotal = EAHCONST.DELIVERY_COST;
        
        riderUID = currentUser.getUid();
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.confirm_order);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        
        getReferencesToViews();
        
        Intent i = getIntent();
        
        if (i.getSerializableExtra(RIDER_ORDER_DELIVERY_KEY) == null) {
            Log.e(TAG, "Cannot show null order for confirm");
            finish();
            return;
        }
        
        currentDelivery = (RiderOrderDelivery) i.getSerializableExtra(RIDER_ORDER_DELIVERY_KEY);
        
        
        fabConfirm.setOnClickListener(v -> actionConfirmOrder());
        fabDecline.setOnClickListener(v -> actionDeclineOrder());

        setDataToView();

        setActivityLoading(false);
    
        Utility.showAlertToUser(this, R.string.alert_confirm_decline_order);
    
        RiderLocationService service = RiderLocationService.getInstance();
        //to get last location from MainActivity
        if (service != null ){
            Location loc = service.getLastLocation();
        
            if (loc != null)
                lastLocation = new LatLng(loc.getLatitude(),loc.getLongitude());
            if (lastLocation == null)
                Utility.showAlertToUser(this,R.string.not_find_location);
        }
        
        getRestaurantLocations();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        
        return false;
    }
    
    private void actionDeclineOrder() {
        startChooseRiderActivity();
    }
    
    private void startChooseRiderActivity() {
        
        Intent intent = new Intent(getApplicationContext(), ChooseRiderActivity.class);
        intent.putExtra(ChooseRiderActivity.RESTAURANT_ID_KEY, currentDelivery.getRestaurantId());
        intent.putExtra(ChooseRiderActivity.LAUNCH_MODE_KEY, ChooseRiderActivity.LAUNCH_MODE_RIDER);
        intent.putExtra(ChooseRiderActivity.CURRENT_RIDER_ID_KEY, riderUID);
        startActivityForResult(intent, CHOOSE_RIDER_REQUEST_CODE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Result not ok");
            Utility.showAlertToUser(this, R.string.order_not_declined_alert);
            return;
        }
        
        if (data == null) {
            Log.e(TAG, "Data extra null");
            return;
        }
        
        if (requestCode == CHOOSE_RIDER_REQUEST_CODE) {
            Rider rider = (Rider) data.getSerializableExtra(ChooseRiderActivity.RIDER_RESULT);
            
            if (rider == null) {
                Log.e(TAG, "The received rider is null");
            } else {
                nextRiderId = rider.getId();
                saveNewRidersInfo();
            }
        }
    }
    
    private void saveNewRidersInfo() {
        setActivityLoading(true);
    
        Map<String,Object> updateMap = new HashMap<>();
    
        EAHCONST.OrderStatus orderStatus = EAHCONST.OrderStatus.DECLINED;
        String riderOrderPath = EAHCONST.generatePath(
                EAHCONST.ORDERS_RIDER_SUBTREE,
                riderUID,
                currentDelivery.getOrderId());
        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_ORDER_STATUS), orderStatus);


    
        orderStatus = EAHCONST.OrderStatus.PENDING;
        riderOrderPath = EAHCONST.generatePath(
                EAHCONST.ORDERS_RIDER_SUBTREE,
                nextRiderId,
                currentDelivery.getOrderId());
        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_ORDER_STATUS), orderStatus);
        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_ORDER_RESTAURATEUR_ID), currentDelivery.getRestaurantId());
        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_ORDER_CUSTOMER_ID), currentDelivery.getCustomerId());

    
        // perform the update
        dbRef.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
            setActivityLoading(false);
            Toast.makeText(ConfirmOrderActivity.this, R.string.declivne_order_note, Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            setActivityLoading(false);
            Utility.showAlertToUser(ConfirmOrderActivity.this, R.string.alert_error_confirming);
        });
    }
    
    private void actionConfirmOrder() {
        setActivityLoading(true);
        
        EAHCONST.OrderStatus orderStatus = EAHCONST.OrderStatus.CONFIRMED;

        String randomNumber = randomAlphaNumeric(5);
        
        Map<String,Object> updateMap = new HashMap<>();
        // now from point of view of rider
        String riderOrderPath = EAHCONST.generatePath(
                EAHCONST.ORDERS_RIDER_SUBTREE,
                riderUID,
                currentDelivery.getOrderId());
        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_ORDER_STATUS), orderStatus);
        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_KM_REST), kmRider);
        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_ORDER_DATE), currentDelivery.getDeliveryDate());


        String restaurantOrderPath = EAHCONST.generatePath(
                EAHCONST.ORDERS_REST_SUBTREE,
                currentDelivery.getRestaurantId(),
                currentDelivery.getOrderId());
        updateMap.put(EAHCONST.generatePath(restaurantOrderPath, EAHCONST.REST_ORDER_CONTROL_STRING), randomNumber);
        updateMap.put(EAHCONST.generatePath(restaurantOrderPath, EAHCONST.REST_ORDER_RIDER_ID), riderUID);
    
        String customerOrderPath = EAHCONST.generatePath(
                EAHCONST.ORDERS_CUST_SUBTREE,
                currentDelivery.getCustomerId(),
                currentDelivery.getOrderId());
        updateMap.put(EAHCONST.generatePath(customerOrderPath, EAHCONST.CUST_ORDER_RIDER_ID), riderUID);

        // perform the update
        dbRef.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
            setActivityLoading(false);
            Toast.makeText(ConfirmOrderActivity.this, R.string.confirm_order_note, Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            setActivityLoading(false);
            Utility.showAlertToUser(ConfirmOrderActivity.this, R.string.alert_error_confirming);
        });
    }
    
    
    private void setDataToView() {
        tvDeliveryTime.setText(currentDelivery.getDeliveryTime());
        tvTotalCost.setText(currentDelivery.getTotalCost());
        tvRestaurantAdress.setText(currentDelivery.getRestaurantAddress());
        tvDeliveryAdress.setText(currentDelivery.getDeliveryAddress());
        tvDeliveryDate.setText(currentDelivery.getDeliveryDate());
    }
    
    private void getReferencesToViews() {
        tvDeliveryTime = findViewById(R.id.tv_time);
        tvCostDelivery = findViewById(R.id.tv_cost_delivery);
        tvTotalCost = findViewById(R.id.tv_total_cost);
        tvRestaurantAdress = findViewById(R.id.tv_restaurant_address);
        tvDeliveryAdress = findViewById(R.id.tv_delivery_address);
        tvDeliveryDate = findViewById(R.id.tv_date);
        
        fabConfirm = findViewById(R.id.fab_accept);
        fabDecline = findViewById(R.id.fab_decline);
        
        tvRestaurantDistanceKm = findViewById(R.id.tv_restaurant_distance);
        tvRestaurantDistanceTime = findViewById(R.id.tv_restaurant_duration);
        
        tvCustomerDistanceKm = findViewById(R.id.tv_delivery_address_distance);
        tvCustomerDistanceTime = findViewById(R.id.tv_delivery_address_duration);
    }
    
    private synchronized void setActivityLoading(boolean loading) {
        // this method is necessary to show the user when the activity is doing a network operation
        // as downloading data or uploading data
        // how to use: call with loading = true to notify that a new transmission has been started
        // call with loading = false to notify end of transmission
        
        ProgressBar pbLoading = findViewById(R.id.pb_loading);
        if (loading) {
            if (waitingCount == 0)
                pbLoading.setVisibility(View.VISIBLE);
            waitingCount++;
        } else {
            waitingCount--;
            if (waitingCount == 0)
                pbLoading.setVisibility(View.INVISIBLE);
        }
    }
    
    private void getRestaurantLocations() {
        
        String restaurantUID = currentDelivery.getRestaurantId();
        
        String restaurantPath = EAHCONST.generatePath(
                EAHCONST.RESTAURANTS_SUB_TREE,restaurantUID);
        
        DatabaseReference dbRef1 = dbRef.child(restaurantPath);
        GeoFire geoFireRestaurant = new GeoFire(dbRef1);
        
        geoFireRestaurant.getLocation(EAHCONST.RESTAURANT_POSITION, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                if (location != null) {
                    restaurantLocation = new LatLng(location.latitude, location.longitude);
                    getCustomerLocation();
                } else {
                    System.out.println(String.format("There is no location for key %s in GeoFire", key));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("There was an error getting the GeoFire location: " + databaseError);
            }
        });
    }
    
    private void getCustomerLocation() {
        
        String customerPath = EAHCONST.generatePath(
                EAHCONST.ORDERS_REST_SUBTREE,currentDelivery.getRestaurantId(),currentDelivery.getOrderId());
        
        DatabaseReference dbRef2 = dbRef.child(customerPath);
        GeoFire geoFireCustomer = new GeoFire(dbRef2);
        
        geoFireCustomer.getLocation(EAHCONST.CUSTOMER_POSITION, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                if (location != null) {
                    customerLocation = new LatLng(location.latitude, location.longitude);
                    getRoutes();
                } else {
                    System.out.println(String.format("There is no location for key %s in GeoFire", key));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("There was an error getting the GeoFire location: " + databaseError);
            }
        });
    }
    
    private void getRoutes() {
        if (lastLocation != null && restaurantLocation != null && customerLocation != null) {
            // get Rider to Restaurant Routes

            new RoutingUtility(this, lastLocation, restaurantLocation, new RoutingUtility.GetRouteCaller() {
                @Override
                public void routeCallback(List<List<HashMap<String, String>>> route, String[] distances, int minutes) {
                    String restaurantDist = distances[0];
                    String[] splits = distances[0].split(" ");
                    kmRider = Float.parseFloat(splits[0]);
                    addCost(kmRider);
                    tvRestaurantDistanceKm.setText(restaurantDist);
                    String restaurantTime = distances[1];
                    tvRestaurantDistanceTime.setText(restaurantTime);
                }
    
                @Override
                public void routeErrorCallback(Exception e) {
                    Log.e(TAG, "Exception in routing: " + e.getMessage());
                }
            });
            
            new RoutingUtility(this, restaurantLocation, customerLocation, new RoutingUtility.GetRouteCaller() {
                @Override
                public void routeCallback(List<List<HashMap<String, String>>> route, String[] distances, int minutes) {
                    String customerDist = distances[0];
                    tvCustomerDistanceKm.setText(customerDist);
                    String[] splits = distances[0].split(" ");
                    float kmRider1 = Float.parseFloat(splits[0]);
                    addCost(kmRider1);
                    String customerTime = distances[1];
                    tvCustomerDistanceTime.setText(customerTime);
                }
    
                @Override
                public void routeErrorCallback(Exception e) {
                    Log.e(TAG, "Exception in routing: " + e.getMessage());
                }
            });
        }
    }

    private synchronized void addCost(float kmRider) {

        if(costTotal != 2 )
        {
            costTotal = (float) (costTotal + (0.50 * kmRider));
            tvCostDelivery.setText(String.format(Locale.US,"%.02f",costTotal) + " €");
        }
        else
            costTotal = (float) (costTotal + (0.50 * kmRider));

        currentDelivery.setDeliveryCost(costTotal);
        Map<String,Object> updateMap = new HashMap<>();
        EAHCONST.OrderStatus orderStatus = EAHCONST.OrderStatus.COMPLETED;
        String riderOrderPath = EAHCONST.generatePath(
                EAHCONST.ORDERS_RIDER_SUBTREE,
                currentUser.getUid(),
                currentDelivery.getOrderId());

        updateMap.put(EAHCONST.generatePath(riderOrderPath, EAHCONST.RIDER_INCOME), currentDelivery.getDeliveryCost());


        dbRef.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
            setActivityLoading(false);
        }).addOnFailureListener(e -> {
            setActivityLoading(false);
            Log.e(TAG, "ERROR: "+e.getMessage());
        });
    }



    public static String randomAlphaNumeric(int count) {

        StringBuilder builder = new StringBuilder();

        while (count-- != 0) {

            int character = (int)(Math.random()*EAHCONST.ALPHA_NUMERIC_STRING.length());

            builder.append(EAHCONST.ALPHA_NUMERIC_STRING.charAt(character));

        }

        return builder.toString();

    }

}

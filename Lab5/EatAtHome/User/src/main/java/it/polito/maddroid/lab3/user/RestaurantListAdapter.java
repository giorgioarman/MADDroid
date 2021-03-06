package it.polito.maddroid.lab3.user;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import it.polito.maddroid.lab3.common.EAHCONST;
import it.polito.maddroid.lab3.common.Restaurant;
import it.polito.maddroid.lab3.common.Utility;


public class RestaurantListAdapter extends ListAdapter<Restaurant, RestaurantListAdapter.MyViewHolder> {
    
    private static StorageReference storageReference;
    private ItemClickListener clickListener;
    
    public static final String MODE_HORIZONTAL = "MODE_HORIZONTAL";
    private String currentMode = "";
    
    protected RestaurantListAdapter(@NonNull DiffUtil.ItemCallback<Restaurant> diffCallback, ItemClickListener clickListener) {
        super(diffCallback);
        storageReference = FirebaseStorage.getInstance().getReference();
        this.clickListener = clickListener;
    }
    
    protected RestaurantListAdapter(@NonNull DiffUtil.ItemCallback<Restaurant> diffCallback, ItemClickListener clickListener, String mode) {
        super(diffCallback);
        storageReference = FirebaseStorage.getInstance().getReference();
        this.clickListener = clickListener;
        this.currentMode = mode;
    }
    
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.restaurant_list_item_3, parent, false);
    
        // create view holder and pass main view to it
        RestaurantListAdapter.MyViewHolder vh = new RestaurantListAdapter.MyViewHolder(v);
        return vh;
    }
    
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setupRestaurant(getItem(position), clickListener, currentMode);
    }
    
    public interface ItemClickListener {
        void onItemClickListener(Restaurant restaurant);
    }
    
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        
        private View itemView;
        private TextView tvRestaurantName;
        private TextView tvRestaurantDescription;
        private ImageView ivRestaurantPhoto;
        private TextView tvRating;
        private RatingBar ratingBar;
    
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
    
            this.itemView = itemView;
            
            tvRestaurantName = itemView.findViewById(R.id.tv_restaurant_name);
            tvRestaurantDescription = itemView.findViewById(R.id.tv_restaurant_description);
            ivRestaurantPhoto = itemView.findViewById(R.id.iv_restaurant_photo);
            tvRating = itemView.findViewById(R.id.tv_rating);
            ratingBar = itemView.findViewById(R.id.rating_bar);
        }
        
        public void setupRestaurant(Restaurant restaurant, ItemClickListener itemClickListener, String currentMode) {
            tvRestaurantName.setText(restaurant.getName());
            tvRestaurantDescription.setText(restaurant.getDescription());
            
            if (currentMode.equals(MODE_HORIZONTAL)) {
                ViewGroup.LayoutParams params = itemView.getLayoutParams();
                if (Utility.isTablet(itemView.getContext())) {
                    
                    params.width = Utility.getPixelsFromDP(itemView.getContext(), 450);
                } else
                
//                    params.width = Utility.getPixelsFromDP(itemView.getContext(), 300);
                    params.width = (int) ((0.8) * itemView.getContext().getResources().getDisplayMetrics().widthPixels);
                this.itemView.setLayoutParams(params);
            }
    
            StorageReference riversRef = storageReference.child("avatar_" + restaurant.getRestaurantID() +".jpg");
    
            int count = restaurant.getReviewCount();
            String review = tvRating.getContext().getString(R.string.review);
            String reviews = tvRating.getContext().getString(R.string.reviews);
            String rating;
            
            if (count == 1)
                rating = count + " " + review;
            else
                rating = count + " " + reviews;
            
            tvRating.setText(rating);
            ratingBar.setRating(restaurant.getReviewAvg());
            
            int height = ivRestaurantPhoto.getHeight();
            int width = ivRestaurantPhoto.getWidth();
    
    
            GlideApp.with(ivRestaurantPhoto.getContext())
                    .load(riversRef)
                    .apply(RequestOptions.centerCropTransform())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.round_logo)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .override(EAHCONST.DEFAULT_IMAGE_SIZE, EAHCONST.DEFAULT_IMAGE_SIZE)
                    .into(ivRestaurantPhoto);
            
            itemView.setOnClickListener(v -> itemClickListener.onItemClickListener(restaurant));
        }
        
    }
}

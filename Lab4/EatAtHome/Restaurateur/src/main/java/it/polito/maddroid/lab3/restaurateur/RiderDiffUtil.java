package it.polito.maddroid.lab3.restaurateur;


import androidx.annotation.NonNull;


public class RiderDiffUtil extends androidx.recyclerview.widget.DiffUtil.ItemCallback<Rider> {
    @Override
    public boolean areItemsTheSame(@NonNull Rider oldItem, @NonNull Rider newItem) {
        return oldItem.getId().equals(newItem.getId());
    }
    
    @Override
    public boolean areContentsTheSame(@NonNull Rider oldItem, @NonNull Rider newItem) {
        return oldItem.getName().equals(newItem.getName()) && oldItem.getEmail().equals(newItem.getEmail()) && (Math.abs(oldItem.getDistance() - newItem.getDistance()) < 0.0001f);
    }
}

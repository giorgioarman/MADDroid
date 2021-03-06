package it.polito.maddroid.lab3.common;

import androidx.annotation.NonNull;

import it.polito.maddroid.lab3.common.Order;

public class OrderDiffUtilCallBack extends androidx.recyclerview.widget.DiffUtil.ItemCallback<Order> {

    @Override
    public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
        return oldItem.getOrderId() == newItem.getOrderId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
        return oldItem.getOrderId().equals(newItem.getOrderId()) &&
                oldItem.getDeliveryTime().equals(newItem.getDeliveryTime()) &&
                        oldItem.getOrderStatus() == newItem.getOrderStatus();
    }


}

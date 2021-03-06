package com.nta.latecoffee.database;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface CartDataSource {

//    Flowable<List<CartItem>> getAllCart(String uid);
//
//    Single<Integer> countItemInCart(String uid);
//
//    Single<Double> sumPriceInCart(String uid);
//
//    Single<CartItem> getItemInCart(String foodId, String uid);
//
//    Completable insertOrReplaceAll(CartItem... cartItems);
//
//    Single<Integer> updateCartItems(CartItem cartItem);
//
//    Single<Integer> deleteCartItem(CartItem cartItem);
//
//    Single<Integer> clearCart(String uid);
//
//    Single<CartItem> getItemWithAllOptionsInCart(String uid, String categoryId, String foodId, String foodSize, String foodAddon);

    Flowable<List<CartItem>> getAllCart(String uid, String restaurantId);

    Single<Integer> countItemInCart(String uid, String restaurantId);

    Single<Double> sumPriceInCart(String uid, String restaurantId);

    Single<CartItem> getItemInCart(String foodId, String uid, String restaurantId);

    Completable insertOrReplaceAll(CartItem... cartItems);

    Single<Integer> updateCartItems(CartItem cartItem);

    Single<Integer> deleteCartItem(CartItem cartItem);

    Single<Integer> clearCart(String uid, String restaurantId);

    Single<CartItem> getItemWithAllOptionsInCart(String uid, String categoryId, String foodId, String foodSize, String foodAddon, String restaurantId);
}

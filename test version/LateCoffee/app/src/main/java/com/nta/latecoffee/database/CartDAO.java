package com.nta.latecoffee.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface CartDAO {
    //one restaurant
//    @Query("SELECT * FROM Cart WHERE uid=:uid")
//    Flowable<List<CartItem>> getAllCart(String uid);

    //multi restaurant
    @Query("SELECT * FROM Cart WHERE uid=:uid AND restaurantId=:restaurantId")
    Flowable<List<CartItem>> getAllCart(String uid, String restaurantId);

    //one restaurant
//    @Query("SELECT SUM(foodQuantity) FROM Cart WHERE uid=:uid")
//    Single<Integer> countItemInCart(String uid);

    //multi restaurant
    @Query("SELECT SUM(foodQuantity) FROM Cart WHERE uid=:uid AND restaurantId=:restaurantId")
    Single<Integer> countItemInCart(String uid, String restaurantId);

    //one restaurant
//    @Query("SELECT SUM((foodPrice+foodExtraPrice) * foodQuantity) FROM Cart WHERE uid=:uid")
//    Single<Double> sumPriceInCart(String uid);

    //multi restaurant
    @Query("SELECT SUM((foodPrice+foodExtraPrice) * foodQuantity) FROM Cart WHERE uid=:uid AND restaurantId=:restaurantId")
    Single<Double> sumPriceInCart(String uid, String restaurantId);

    //one restaurant
//    @Query("SELECT * FROM Cart WHERE foodId=:foodId AND uid=:uid")
//    Single<CartItem> getItemInCart(String foodId, String uid);

    //multi restaurant
    @Query("SELECT * FROM Cart WHERE foodId=:foodId AND uid=:uid AND restaurantId=:restaurantId")
    Single<CartItem> getItemInCart(String foodId, String uid, String restaurantId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrReplaceAll(CartItem... cartItems);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    Single<Integer> updateCartItems(CartItem cartItem);

    @Delete
    Single<Integer> deleteCartItem(CartItem cartItem);

//    @Query("DELETE FROM Cart WHERE uid=:uid")
//    Single<Integer> clearCart(String uid);

    @Query("DELETE FROM Cart WHERE uid=:uid AND restaurantId=:restaurantId")
    Single<Integer> clearCart(String uid, String restaurantId);

//    @Query("SELECT * FROM Cart WHERE uid=:uid AND categoryId=:categoryId AND foodId=:foodId AND foodSize=:foodSize AND foodAddon=:foodAddon")
//    Single<CartItem> getItemWithAllOptionsInCart(String uid, String categoryId, String foodId, String foodSize, String foodAddon);

    @Query("SELECT * FROM Cart WHERE uid=:uid AND categoryId=:categoryId AND foodId=:foodId AND foodSize=:foodSize AND foodAddon=:foodAddon AND restaurantId=:restaurantId")
    Single<CartItem> getItemWithAllOptionsInCart(String uid, String categoryId, String foodId, String foodSize, String foodAddon, String restaurantId);

}

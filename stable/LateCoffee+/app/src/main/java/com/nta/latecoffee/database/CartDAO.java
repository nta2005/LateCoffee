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

// DAO(Data Access Object) định nghĩa cho method truy cập database, dùng annotation để liên kết SQL với từng phương thức.
// Có 4 annotation: @Query, @Insert, @Update, @Delete
// @Query là annotation để thực hiện thao tác đọc tên database
@Dao
public interface CartDAO {
    @Query("SELECT * FROM Cart WHERE uid=:uid")
    Flowable<List<CartItem>> getAllCart(String uid);

    @Query("SELECT SUM(foodQuantity) FROM Cart WHERE uid=:uid")
    Single<Integer> countItemInCart(String uid);

    @Query("SELECT SUM((foodPrice+foodExtraPrice) * foodQuantity) FROM Cart WHERE uid=:uid")
    Single<Double> sumPriceInCart(String uid);

    @Query("SELECT * FROM Cart WHERE foodId=:foodId AND uid=:uid")
    Single<CartItem> getItemInCart(String foodId, String uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrReplaceAll(CartItem... cartItems);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    Single<Integer> updateCartItems(CartItem cartItem);

    @Delete
    Single<Integer> deleteCartItem(CartItem cartItem);

    @Query("DELETE FROM Cart WHERE uid=:uid")
    Single<Integer> clearCart(String uid);

    @Query("SELECT * FROM Cart WHERE uid=:uid AND categoryId=:categoryId AND foodId=:foodId AND foodSize=:foodSize AND foodAddon=:foodAddon")
    Single<CartItem> getItemWithAllOptionsInCart(String uid, String categoryId, String foodId, String foodSize, String foodAddon);
}

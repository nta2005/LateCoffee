package com.nta.latecoffee.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Database là class chủ sử dụng annotation để xác định các list của entity và database version. Nội dung class này xác định danh sách các DAO.
@Database(version = 1, entities = CartItem.class, exportSchema = false)

//Class annotated với @Database annotation.
// Nó sử dụng phương pháp singleton* cho database, nên ta cần tạo 1 static method chúng sẽ trả về instance của CartDatabase.
//*singleton là phương pháp hạn chế sự khởi tạo của lớp đối tượng.
public abstract class CartDatabase extends RoomDatabase {

    private static CartDatabase instance;

    public static CartDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, CartDatabase.class, "LateCoffeeDB").build();
        }
        return instance;
    }

    public abstract CartDAO cartDAO();
}

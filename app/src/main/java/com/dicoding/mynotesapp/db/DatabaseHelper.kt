package com.dicoding.mynotesapp.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dicoding.mynotesapp.db.DatabaseContract.NoteColumns.Companion.TABLE_NAME

// SQLiteOpenHelper adalah kelas helper Android untuk mengelola pembuatan dan pembaruan database.
// Dengan mewarisinya, kita tidak perlu mengurus koneksi database secara manual.
internal class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // onCreate dipanggil HANYA SEKALI saat database belum pernah ada di perangkat.
    // Di sinilah kita membuat struktur tabel untuk pertama kali.
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE_NOTE)
    }

    // onUpgrade dipanggil saat DATABASE_VERSION dinaikkan.
    // Strategi di sini adalah drop-and-recreate — cocok untuk development,
    // tapi di produksi sebaiknya gunakan ALTER TABLE agar data pengguna tidak hilang.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "dbnoteapp"   // Nama file .db yang tersimpan di storage internal
        private const val DATABASE_VERSION = 1          // Naikkan angka ini setiap kali ada perubahan skema tabel

        // Query SQL untuk membuat tabel. Disimpan sebagai konstanta agar mudah dibaca dan di-debug.
        // AUTOINCREMENT memastikan setiap catatan mendapat ID unik yang tidak pernah diulang.
        private const val SQL_CREATE_TABLE_NOTE = "CREATE TABLE $TABLE_NAME" +
                " (${DatabaseContract.NoteColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                "${DatabaseContract.NoteColumns.TITLE} TEXT NOT NULL," +
                "${DatabaseContract.NoteColumns.DESCRIPTION} TEXT NOT NULL," +
                "${DatabaseContract.NoteColumns.DATE} TEXT NOT NULL)"
    }
}
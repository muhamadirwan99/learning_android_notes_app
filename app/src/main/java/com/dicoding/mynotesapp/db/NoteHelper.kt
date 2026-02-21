package com.dicoding.mynotesapp.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.dicoding.mynotesapp.db.DatabaseContract.NoteColumns.Companion.TABLE_NAME
import com.dicoding.mynotesapp.db.DatabaseContract.NoteColumns.Companion._ID
import java.sql.SQLException
import kotlin.jvm.Throws

// NoteHelper adalah lapisan Repository/DAO — bertanggung jawab penuh atas semua
// operasi CRUD ke database. Activity tidak perlu tahu cara kerja SQLite secara detail.
class NoteHelper(context: Context) {
    private var databaseHelper: DatabaseHelper = DatabaseHelper(context)
    private lateinit var database: SQLiteDatabase // lateinit karena koneksi baru dibuka lewat open()

    // open() harus dipanggil sebelum melakukan operasi apapun.
    // Memisahkan open/close dari constructor memberi kontrol penuh kapan koneksi aktif,
    // sehingga tidak boros resource saat NoteHelper belum dipakai.
    @Throws(SQLException::class)
    fun open() {
        database = databaseHelper.writableDatabase
    }

    // close() wajib dipanggil saat Activity selesai agar koneksi database tidak bocor (resource leak).
    // INSTANCE di-null-kan agar singleton bisa dibuat ulang dengan benar di sesi berikutnya.
    fun close() {
        databaseHelper.close()

        if (database.isOpen) {
            database.close()
        }

        INSTANCE = null
    }

    // Mengambil semua catatan, diurutkan berdasarkan _ID ascending
    // agar urutan tampilan konsisten dengan urutan data dimasukkan.
    fun queryAll(): Cursor {
        return database.query(
            DATABASE_TABLE, null, null, null, null, null, "$_ID ASC"
        )
    }

    // Mengambil satu catatan berdasarkan ID — dipakai untuk keperluan edit spesifik.
    // Menggunakan "?" sebagai placeholder untuk mencegah SQL Injection.
    fun queryById(id: String): Cursor {
        return database.query(
            DATABASE_TABLE,
            null,
            "$_ID = ?",   // Kondisi WHERE — hanya ambil baris dengan ID yang cocok
            arrayOf(id),  // Nilai pengganti "?" — diescaping otomatis oleh SQLite
            null,
            null,
            null,
            null,
        )
    }

    // insert() mengembalikan rowId baris yang baru dibuat (> 0 = sukses, -1 = gagal).
    // Nilai ini penting untuk langsung meng-update objek Note dengan ID aslinya dari database.
    fun insert(values: ContentValues?): Long {
        return database.insert(DATABASE_TABLE, null, values)
    }

    // update() mengembalikan jumlah baris yang terpengaruh (> 0 = sukses).
    // WHERE clause menggunakan ID agar hanya catatan yang dituju yang berubah.
    fun update(id: String, values: ContentValues?): Int {
        return database.update(DATABASE_TABLE, values, "$_ID = ?", arrayOf(id))
    }

    // deleteById() menghapus satu baris berdasarkan ID-nya.
    // Mengembalikan jumlah baris yang dihapus, dipakai untuk validasi apakah penghapusan berhasil.
    fun deleteById(id: String): Int {
        return database.delete(DATABASE_TABLE, "$_ID = $id", null)
    }

    companion object {
        private const val DATABASE_TABLE = TABLE_NAME
        private var INSTANCE: NoteHelper? = null

        // Pola Singleton — memastikan hanya ada SATU instance NoteHelper di seluruh aplikasi.
        // synchronized(this) mencegah race condition jika ada dua thread membuat instance sekaligus.
        fun getInstance(context: Context): NoteHelper = INSTANCE ?: synchronized(this) {
            INSTANCE ?: NoteHelper(context).also { INSTANCE = it }
        }
    }
}
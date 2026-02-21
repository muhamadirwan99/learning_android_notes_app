package com.dicoding.mynotesapp.db

import android.provider.BaseColumns

// Kelas ini berfungsi sebagai "kontrak" database — satu tempat terpusat untuk menyimpan
// semua nama tabel dan kolom sebagai konstanta, sehingga tidak ada typo saat dipakai di banyak tempat.
internal class DatabaseContract {

    // NoteColumns mewarisi BaseColumns agar secara otomatis mendapat kolom standar Android (_ID),
    // yang dibutuhkan oleh beberapa komponen Android seperti CursorAdapter.
    internal class NoteColumns : BaseColumns {
        companion object {
            const val TABLE_NAME = "note"       // Nama tabel di SQLite
            const val _ID = "_id"               // Primary key — nama "_id" adalah konvensi wajib Android
            const val TITLE = "title"           // Kolom untuk judul catatan
            const val DESCRIPTION = "description" // Kolom untuk isi/deskripsi catatan
            const val DATE = "date"             // Kolom untuk tanggal pembuatan catatan
        }
    }
}
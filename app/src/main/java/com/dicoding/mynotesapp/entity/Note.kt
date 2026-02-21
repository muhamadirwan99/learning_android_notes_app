package com.dicoding.mynotesapp.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// @Parcelize secara otomatis men-generate kode Parcelable boilerplate.
// Parcelable diperlukan agar objek Note bisa dikirim antar Activity melalui Intent
// (lebih efisien dari Serializable karena tidak menggunakan refleksi Java).
@Parcelize
data class Note(
    var id: Int = 0,              // Default 0 — ID asli akan diisi oleh SQLite saat insert
    var title: String? = null,    // Nullable agar bisa dibuat objek Note kosong sebelum diisi user
    var description: String? = null,
    var date: String? = null,     // Diisi otomatis saat simpan, bukan oleh user
) : Parcelable
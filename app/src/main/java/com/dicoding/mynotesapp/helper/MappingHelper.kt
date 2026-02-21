package com.dicoding.mynotesapp.helper

import android.database.Cursor
import com.dicoding.mynotesapp.db.DatabaseContract
import com.dicoding.mynotesapp.entity.Note

// object (Singleton) dipakai karena MappingHelper tidak butuh menyimpan state apapun —
// hanya berisi fungsi utilitas murni yang bisa langsung dipanggil tanpa membuat instance.
object MappingHelper {

    // Fungsi ini memisahkan tanggung jawab konversi data dari Activity/ViewModel.
    // Cursor adalah "pointer" ke hasil query SQLite — kita iterasi baris per baris
    // lalu ubah setiap baris menjadi objek Note yang lebih mudah dipakai di UI.
    fun mapCursorToArrayList(notesCursor: Cursor?): ArrayList<Note> {
        val notesList = ArrayList<Note>()

        // apply{} dipakai agar kita bisa langsung memanggil method Cursor (moveToNext, getInt, dll.)
        // tanpa mengulang nama variabel. Jika notesCursor null, blok ini dilewati otomatis.
        notesCursor?.apply {
            while (moveToNext()) {
                // getColumnIndexOrThrow lebih aman dari getColumnIndex karena langsung melempar
                // exception jika nama kolom tidak ditemukan, sehingga bug terdeteksi lebih awal.
                val id = getInt(getColumnIndexOrThrow(DatabaseContract.NoteColumns._ID))
                val title = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.TITLE))
                val description = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.DESCRIPTION))
                val date = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.DATE))
                notesList.add(Note(id, title, description, date))
            }
        }
        return notesList
    }
}
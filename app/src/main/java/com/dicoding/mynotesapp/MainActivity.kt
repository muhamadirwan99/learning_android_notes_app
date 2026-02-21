package com.dicoding.mynotesapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.mynotesapp.adapter.NoteAdapter
import com.dicoding.mynotesapp.databinding.ActivityMainBinding
import com.dicoding.mynotesapp.db.NoteHelper
import com.dicoding.mynotesapp.entity.Note
import com.dicoding.mynotesapp.helper.MappingHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.compareTo

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter

    // registerForActivityResult menggantikan startActivityForResult yang sudah deprecated.
    // Callback ini menangani hasil dari NoteAddUpdateActivity tanpa perlu override onActivityResult,
    // sehingga kode lebih bersih dan sesuai dengan lifecycle-aware API modern Android.
    val resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            when (result.resultCode) {
                NoteAddUpdateActivity.RESULT_ADD -> {
                    // Pengecekan versi Android diperlukan karena API getParcelableExtra
                    // yang lama (tanpa parameter Class) sudah deprecated sejak API 33 (TIRAMISU).
                    val note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getParcelableExtra<Note>(
                            NoteAddUpdateActivity.EXTRA_NOTE, Note::class.java
                        ) as Note
                    } else {
                        @Suppress("Deprecation") result.data?.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
                    }

                    // addItem() + smoothScrollToPosition memberikan feedback visual langsung
                    // bahwa data baru sudah masuk ke urutan paling bawah list.
                    adapter.addItem(note)
                    binding.rvNotes.smoothScrollToPosition(adapter.itemCount - 1)
                    showSnackbarMessage("Satu item berhasil ditambahkan")
                }

                NoteAddUpdateActivity.RESULT_UPDATE -> {
                    val note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getParcelableExtra<Note>(
                            NoteAddUpdateActivity.EXTRA_NOTE, Note::class.java
                        ) as Note
                    } else {
                        @Suppress("Deprecation") result.data?.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
                    }
                    // position dikirim kembali dari NoteAddUpdateActivity agar kita tahu
                    // tepat di index mana RecyclerView harus memperbarui tampilan item-nya.
                    val position = result.data?.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0) as Int

                    adapter.updateItem(position, note)
                    binding.rvNotes.smoothScrollToPosition(position)
                    showSnackbarMessage("Satu item berhasil diubah")
                }

                NoteAddUpdateActivity.RESULT_DELETE -> {
                    val position = result.data?.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0) as Int
                    adapter.removeItem(position) // Hapus dari Adapter (UI), bukan dari database — database sudah dihapus di NoteAddUpdateActivity
                    showSnackbarMessage("Satu item berhasil dihapus")
                }

            }

        }
    }

    companion object {
        // Kunci ini dipakai untuk menyimpan dan memulihkan state list saat Activity di-recreate
        // (misalnya saat rotasi layar), agar data tidak hilang dan tidak perlu query ulang ke DB.
        private const val EXTRA_STATE = "EXTRA_STATE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // setOnApplyWindowInsetsListener memastikan konten tidak tertutup oleh status bar atau navigation bar
        // saat mode edge-to-edge diaktifkan — padding disesuaikan secara dinamis oleh sistem.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.title = "Notes"

        // LinearLayoutManager mengatur item ditampilkan secara vertikal satu per satu (seperti ListView).
        // setHasFixedSize(true) mengoptimalkan performa — memberi tahu RecyclerView bahwa ukurannya
        // tidak berubah meski isi datanya berubah.
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.setHasFixedSize(true)

        // Anonymous object dipakai agar implementasi OnItemClickCallback bisa langsung ditulis di sini
        // tanpa membuat kelas terpisah, sekaligus tetap memiliki akses ke context MainActivity.
        adapter = NoteAdapter(object : NoteAdapter.OnItemClickCallback {
            override fun onItemClicked(selectedNote: Note?, position: Int?) {
                // Kirim note dan posisinya ke NoteAddUpdateActivity sebagai data yang akan diedit.
                val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_NOTE, selectedNote)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_POSITION, position)
                resultLauncher.launch(intent)
            }
        })
        binding.rvNotes.adapter = adapter

        // FAB (Floating Action Button) untuk membuka form tambah catatan baru — tanpa mengirim EXTRA_NOTE,
        // sehingga NoteAddUpdateActivity tahu ini mode "tambah" bukan "edit".
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
            resultLauncher.launch(intent)
        }


        if (savedInstanceState == null) {
            // savedInstanceState null berarti Activity baru pertama kali dibuat (bukan hasil rotasi/recreate),
            // sehingga kita perlu memuat data dari database.
            loadNotesAsync()
        } else {
            // savedInstanceState tidak null berarti Activity di-recreate (misalnya rotasi layar).
            // Data list yang sudah ada dipulihkan dari bundle agar tidak perlu query ulang ke DB.
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelableArrayList<Note>(
                    EXTRA_STATE, Note::class.java
                )
            } else {
                @Suppress("Deprecation") savedInstanceState.getParcelableArrayList<Note>(EXTRA_STATE)
            }

            if (list != null) {
                adapter.listNotes = list
            }
        }
    }

    // onSaveInstanceState dipanggil sistem SEBELUM Activity dihancurkan karena rotasi/perubahan konfigurasi.
    // Kita simpan list yang sedang tampil agar bisa dipulihkan di onCreate tanpa query ulang ke DB.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(EXTRA_STATE, adapter.listNotes)
    }

    private fun loadNotesAsync() {
        // lifecycleScope.launch memastikan coroutine ini otomatis dibatalkan jika Activity dihancurkan,
        // mencegah memory leak atau crash karena Activity sudah tidak ada saat data selesai dimuat.
        lifecycleScope.launch {
            binding.progressbar.visibility = View.VISIBLE
            val noteHelper = NoteHelper.getInstance(applicationContext)
            noteHelper.open()

            // async + Dispatchers.IO memindahkan operasi database ke background thread,
            // agar Main Thread (UI thread) tidak terblokir dan aplikasi tidak ANR (Application Not Responding).
            val deferredNotes = async(Dispatchers.IO) {
                val cursor = noteHelper.queryAll()
                MappingHelper.mapCursorToArrayList(cursor) // Konversi Cursor mentah menjadi List<Note>
            }
            val notes = deferredNotes.await()  // Tunggu hingga background thread selesai, baru lanjutkan di Main Thread
            binding.progressbar.visibility = View.INVISIBLE
            if (notes.size > 0) {
                adapter.listNotes = notes
            } else {
                adapter.listNotes = ArrayList()
                showSnackbarMessage("Tidak ada data saat ini")
            }
            noteHelper.close() // Tutup koneksi database setelah selesai dipakai
        }
    }


    private fun showSnackbarMessage(message: String) {
        // Snackbar ditampilkan di atas rvNotes (bukan root view) agar tidak tertutup FAB.
        Snackbar.make(binding.rvNotes, message, Snackbar.LENGTH_SHORT).show()
    }
}
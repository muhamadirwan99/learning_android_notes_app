package com.dicoding.mynotesapp

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dicoding.mynotesapp.databinding.ActivityNoteAddUpdateBinding
import com.dicoding.mynotesapp.db.DatabaseContract
import com.dicoding.mynotesapp.db.DatabaseContract.NoteColumns.Companion.DATE
import com.dicoding.mynotesapp.db.NoteHelper
import com.dicoding.mynotesapp.entity.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Activity ini menangani DUA mode sekaligus: tambah catatan baru dan edit catatan yang ada.
// Mode ditentukan berdasarkan apakah EXTRA_NOTE diterima dari Intent atau tidak.
class NoteAddUpdateActivity : AppCompatActivity(), View.OnClickListener {
    private var isEdit = false   // Flag untuk membedakan mode tambah vs edit, menghindari duplikasi logika
    private var note: Note? = null
    private var position: Int = 0  // Posisi item di RecyclerView — dikirim balik ke MainActivity agar update tepat sasaran
    private lateinit var noteHelper: NoteHelper

    private lateinit var binding: ActivityNoteAddUpdateBinding

    companion object {
        const val EXTRA_NOTE = "extra_note"         // Kunci untuk mengirim objek Note antar Activity
        const val EXTRA_POSITION = "extra_position" // Kunci untuk mengirim posisi item di RecyclerView
        const val RESULT_ADD = 101      // Kode unik agar MainActivity bisa membedakan hasil dari aksi tambah...
        const val RESULT_UPDATE = 201   // ...update...
        const val RESULT_DELETE = 301   // ...dan hapus, sehingga reaksi UI bisa berbeda tiap aksi.
        const val ALERT_DIALOG_CLOSE = 10   // Tipe dialog untuk konfirmasi batal edit
        const val ALERT_DIALOG_DELETE = 20  // Tipe dialog untuk konfirmasi hapus catatan
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNoteAddUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        noteHelper = NoteHelper.getInstance(applicationContext)
        noteHelper.open()

        // Ambil objek Note dari Intent — jika null, berarti Activity dibuka untuk mode tambah baru.
        // Pengecekan versi SDK diperlukan karena API lama sudah deprecated sejak Android 13 (TIRAMISU).
        note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_NOTE, Note::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_NOTE)
        }

        if (note != null) {
            position = intent.getIntExtra(EXTRA_POSITION, 0)
            isEdit = true
        } else {
            note = Note() // Buat objek Note kosong sebagai wadah data yang akan diisi user
        }

        val actionBarTitle: String
        val btnTitle: String

        if (isEdit) {
            actionBarTitle = "Ubah"
            btnTitle = "Update"

            // Pre-fill form dengan data catatan yang sudah ada agar user bisa langsung mengedit,
            // bukan mengetik ulang dari awal.
            note?.let {
                binding.edtTitle.setText(it.title)
                binding.edtDescription.setText(it.description)
            }
        } else {
            actionBarTitle = "Tambah"
            btnTitle = "Simpan"
        }

        supportActionBar?.title = actionBarTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Tampilkan tombol back di ActionBar

        binding.btnSubmit.text = btnTitle

        binding.btnSubmit.setOnClickListener(this)

        // OnBackPressedCallback menggantikan onBackPressed() yang deprecated.
        // Kita intercept aksi back agar user dikonfirmasi sebelum meninggalkan form yang sudah diisi.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showAlertDialog(ALERT_DIALOG_CLOSE)
            }
        })
    }

    override fun onClick(view: View) {
        if (view.id == R.id.btn_submit) {
            val title = binding.edtTitle.text.toString().trim()
            val description = binding.edtDescription.text.toString().trim()

            // Validasi dilakukan di sisi client sebelum menyentuh database
            // untuk memberikan feedback instan kepada user tanpa delay.
            if (title.isEmpty()) {
                binding.edtTitle.error = "Field can not be blank"
                return
            }

            note?.title = title
            note?.description = description

            // Intent dipersiapkan lebih awal agar bisa digunakan baik di blok insert maupun update.
            val intent = Intent()
            intent.putExtra(EXTRA_NOTE, note)
            intent.putExtra(EXTRA_POSITION, position)

            // ContentValues adalah format yang dibutuhkan SQLite — pasangan key-value
            // yang merepresentasikan satu baris data yang akan disimpan/diperbarui.
            val values = ContentValues()
            values.put(DatabaseContract.NoteColumns.TITLE, title)
            values.put(DatabaseContract.NoteColumns.DESCRIPTION, description)

            if (isEdit) {
                // update() memakai ID note sebagai acuan baris mana di SQLite yang harus diubah.
                val result = noteHelper.update(note?.id.toString(), values).toLong()
                if (result > 0) {
                    setResult(RESULT_UPDATE, intent) // Kirim kode RESULT_UPDATE agar MainActivity tahu ini adalah operasi edit
                    finish()
                } else {
                    Toast.makeText(this@NoteAddUpdateActivity, "Gagal mengupdate data", Toast.LENGTH_SHORT).show()
                }
            } else {
                note?.date = getCurrentDate() // Tanggal diisi di sini (sisi aplikasi), bukan oleh user
                values.put(DATE, getCurrentDate())
                val result = noteHelper.insert(values)

                if (result > 0) {
                    note?.id = result.toInt() // Perbarui ID di objek Note agar sinkron dengan ID yang dibuat SQLite
                    setResult(RESULT_ADD, intent) // Kirim kode RESULT_ADD agar MainActivity tahu ini adalah catatan baru
                    finish()
                } else {
                    Toast.makeText(this@NoteAddUpdateActivity, "Gagal menambah data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCurrentDate(): String {
        // Locale.getDefault() memastikan format tanggal sesuai dengan pengaturan regional perangkat pengguna.
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val date = Date()

        return dateFormat.format(date)
    }

    // Menu hapus hanya ditampilkan saat mode edit — tidak relevan saat mode tambah karena catatan belum ada.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (isEdit) {
            menuInflater.inflate(R.menu.menu_form, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> showAlertDialog(ALERT_DIALOG_DELETE)
            android.R.id.home -> showAlertDialog(ALERT_DIALOG_CLOSE) // Tombol back di ActionBar diperlakukan sama seperti tombol back fisik
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAlertDialog(type: Int) {
        // Satu fungsi untuk dua jenis dialog — dibedakan lewat parameter type
        // agar tidak ada duplikasi kode pembuatan AlertDialog.
        val isDialogClose = type == ALERT_DIALOG_CLOSE
        val dialogTitle: String
        val dialogMessage: String

        if (isDialogClose) {
            dialogTitle = "Batal"
            dialogMessage = "Apakah anda ingin membatalkan perubahan pada form?"
        } else {
            dialogTitle = "Hapus Note"
            dialogMessage = "Apakah anda yakin ingin menghapus item ini?"
        }

        val alertDialogBuilder = AlertDialog.Builder(this)

        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false) // Mencegah dialog ditutup dengan mengetuk di luar area dialog — user dipaksa memilih Ya atau Tidak
            .setPositiveButton("Ya") { _, _ ->
                if (isDialogClose) {
                    finish() // Tutup Activity dan buang semua perubahan yang belum disimpan
                } else {
                    // Hapus dari database terlebih dahulu, BARU kirim sinyal ke MainActivity
                    // untuk menghapus item dari RecyclerView juga.
                    val result = noteHelper.deleteById(note?.id.toString()).toLong()
                    if (result > 0) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_POSITION, position)
                        setResult(RESULT_DELETE, intent)
                        finish()
                    } else {
                        Toast.makeText(this@NoteAddUpdateActivity, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() } // Tutup dialog tanpa melakukan apa-apa

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}
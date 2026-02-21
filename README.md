# MyNotesApp 📝

Aplikasi catatan sederhana berbasis Android yang dibangun menggunakan **Kotlin** dan **SQLite**. Proyek ini dirancang sebagai latihan memahami konsep dasar pengembangan Android native: arsitektur Activity, CRUD database lokal, RecyclerView, dan komunikasi antar-Activity.

---

## 📱 Fitur

| Fitur | Keterangan |
|---|---|
| ➕ Tambah catatan | Buat catatan baru dengan judul dan deskripsi |
| ✏️ Edit catatan | Ubah isi catatan yang sudah ada |
| 🗑️ Hapus catatan | Hapus catatan dengan konfirmasi dialog |
| 💾 Persistensi data | Data tersimpan permanen di SQLite (tidak hilang saat app ditutup) |
| 🔄 State management | List tetap ada saat layar dirotasi (tidak query ulang ke DB) |

---

## 🏗️ Arsitektur & Struktur Proyek

```
app/src/main/java/com/dicoding/mynotesapp/
│
├── MainActivity.kt              # Entry point — menampilkan list semua catatan
├── NoteAddUpdateActivity.kt     # Form tambah & edit catatan (dual-mode)
│
├── adapter/
│   └── NoteAdapter.kt           # Jembatan antara List<Note> dan RecyclerView
│
├── db/
│   ├── DatabaseContract.kt      # Kontrak: nama tabel & kolom sebagai konstanta
│   ├── DatabaseHelper.kt        # Mengelola pembuatan & upgrade skema database
│   └── NoteHelper.kt            # DAO: semua operasi CRUD ke SQLite
│
├── entity/
│   └── Note.kt                  # Model data catatan (Parcelable)
│
└── helper/
    └── MappingHelper.kt         # Konversi Cursor SQLite → ArrayList<Note>
```

---

## 🔄 Alur Data (Data Flow)

```
User Action
    │
    ▼
Activity (UI Layer)
    │  Intent + Parcelable
    ▼
NoteAddUpdateActivity
    │  ContentValues
    ▼
NoteHelper (DAO Layer)
    │  SQL Query
    ▼
DatabaseHelper → SQLite Database
    │  Cursor
    ▼
MappingHelper
    │  ArrayList<Note>
    ▼
NoteAdapter → RecyclerView (UI)
```

---

## 🧩 Penjelasan Komponen Utama

### `Note.kt` — Model Data
Objek data yang merepresentasikan satu catatan. Menggunakan `@Parcelize` agar bisa dikirim antar-Activity melalui `Intent` secara efisien (tanpa refleksi seperti `Serializable`).

### `DatabaseContract.kt` — Kontrak Database
Menyimpan semua nama tabel dan kolom sebagai konstanta. Tujuannya agar tidak ada *typo* saat nama kolom dipakai di banyak tempat berbeda.

### `DatabaseHelper.kt` — Manajemen Skema
Mewarisi `SQLiteOpenHelper`:
- **`onCreate()`** → Dijalankan sekali saat database pertama kali dibuat di perangkat.
- **`onUpgrade()`** → Dijalankan saat `DATABASE_VERSION` dinaikkan (untuk migrasi skema).

### `NoteHelper.kt` — Data Access Object (DAO)
Lapisan yang memisahkan logika database dari UI. Menggunakan pola **Singleton** (`getInstance()`) agar hanya ada satu koneksi database aktif di seluruh aplikasi. Koneksi dibuka/ditutup secara eksplisit dengan `open()` dan `close()`.

### `MappingHelper.kt` — Data Mapper
Mengubah `Cursor` (format raw SQLite) menjadi `ArrayList<Note>` yang mudah dipakai di UI. Memisahkan tanggung jawab konversi data dari Activity.

### `NoteAdapter.kt` — RecyclerView Adapter
Mengelola tampilan list catatan. Menggunakan:
- **`ViewHolder` pattern** → Mencegah `findViewById()` dipanggil berulang saat scroll.
- **`notifyItemInserted/Changed/Removed`** → Update UI secara granular, lebih efisien dari `notifyDataSetChanged()`.
- **`OnItemClickCallback` interface** → Loose coupling antara Adapter dan Activity.

### `MainActivity.kt` — Layar Utama
Menampilkan semua catatan dalam `RecyclerView`. Poin penting:
- **`registerForActivityResult`** → Pengganti `startActivityForResult` yang deprecated, untuk menerima hasil dari `NoteAddUpdateActivity`.
- **`onSaveInstanceState`** → Menyimpan list saat rotasi layar agar tidak perlu query ulang ke database.
- **`lifecycleScope + Dispatchers.IO`** → Operasi database dijalankan di background thread untuk mencegah ANR.

### `NoteAddUpdateActivity.kt` — Form Tambah/Edit
Satu Activity untuk dua mode (tambah & edit), dibedakan oleh flag `isEdit`. Mode ditentukan saat Activity dibuka: jika `EXTRA_NOTE` ada di Intent maka mode edit, jika tidak maka mode tambah.

---

## 🛠️ Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Database | SQLite (via `SQLiteOpenHelper`) |
| UI List | RecyclerView + CardView |
| View Binding | View Binding |
| Async | Kotlin Coroutines (`lifecycleScope`, `Dispatchers.IO`) |
| State | `onSaveInstanceState` + `Parcelable` |
| Navigation | Activity + `registerForActivityResult` |

---

## 💡 Konsep Android yang Dipraktikkan

- **Activity Lifecycle** — `onCreate`, `onSaveInstanceState`
- **Intent & Parcelable** — Komunikasi antar-Activity dengan membawa objek data
- **RecyclerView & Adapter Pattern** — Menampilkan list data secara efisien
- **SQLite CRUD** — Create, Read, Update, Delete data lokal
- **Singleton Pattern** — Satu instance `NoteHelper` untuk seluruh aplikasi
- **Coroutines** — Operasi I/O di background thread tanpa memblokir UI
- **View Binding** — Akses view yang type-safe, pengganti `findViewById`
- **Edge-to-Edge UI** — Konten meluas ke area status bar dan navigation bar


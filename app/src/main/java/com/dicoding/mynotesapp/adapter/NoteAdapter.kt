package com.dicoding.mynotesapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.mynotesapp.R
import com.dicoding.mynotesapp.databinding.ItemNoteBinding
import com.dicoding.mynotesapp.entity.Note

// NoteAdapter menjembatani antara data (List<Note>) dan tampilan (RecyclerView).
// OnItemClickCallback diterima lewat constructor agar Activity yang menentukan aksi klik,
// bukan Adapter — ini menjaga agar Adapter tidak terikat ke Activity tertentu (loose coupling).
class NoteAdapter(private val onItemClickCallback: OnItemClickCallback) :
    RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    // Custom setter dipasang agar setiap kali listNotes diubah dari luar,
    // RecyclerView langsung diperbarui secara otomatis tanpa perlu memanggil notifyDataSetChanged() manual.
    var listNotes = ArrayList<Note>()
        set(listNotes) {
            if (listNotes.size > 0) {
                this.listNotes.clear() // Bersihkan dulu data lama agar tidak terjadi duplikasi
            }
            this.listNotes.addAll(listNotes)
            notifyDataSetChanged()    // Beritahu RecyclerView untuk me-render ulang semua item
        }

    // addItem() menggunakan notifyItemInserted agar animasi insert muncul di posisi yang tepat,
    // lebih efisien dari notifyDataSetChanged() yang me-render ulang seluruh list.
    fun addItem(note: Note) {
        this.listNotes.add(note)
        notifyItemInserted(this.listNotes.size - 1)
    }

    // updateItem() hanya me-render ulang satu item di posisi tertentu, bukan seluruh list.
    // Ini penting untuk performa, terutama jika list panjang.
    fun updateItem(position: Int, note: Note) {
        this.listNotes[position] = note
        notifyItemChanged(position, note)
    }

    // Setelah remove, notifyItemRangeChanged dipanggil agar posisi item di bawahnya
    // diperbarui — mencegah IndexOutOfBoundsException saat item diklik setelah penghapusan.
    fun removeItem(position: Int) {
        this.listNotes.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, this.listNotes.size)
    }

    // onCreateViewHolder dipanggil saat RecyclerView butuh ViewHolder baru (view belum ada di pool).
    // LayoutInflater mengubah file XML layout menjadi objek View yang bisa ditampilkan.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    // onBindViewHolder dipanggil setiap kali item akan ditampilkan atau di-scroll ke layar.
    // RecyclerView mendaur ulang ViewHolder yang sudah keluar layar — inilah kenapa dinamakan "Recycler".
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(listNotes[position])
    }

    override fun getItemCount(): Int = this.listNotes.size

    // inner class agar NoteViewHolder bisa mengakses properti Adapter (misal: listNotes).
    // ViewHolder adalah pola optimasi — menyimpan referensi View agar tidak perlu
    // dipanggil findViewById() berulang kali setiap item di-scroll.
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemNoteBinding.bind(itemView) // View Binding: akses view lebih aman dari findViewById
        fun bind(note: Note) {
            binding.tvItemTitle.text = note.title
            binding.tvItemDate.text = note.date
            binding.tvItemDescription.text = note.description

            // Klik listener dipasang di dalam bind() agar selalu menggunakan data Note terbaru.
            // bindingAdapterPosition dipakai (bukan adapterPosition) karena lebih akurat
            // saat ada animasi atau perubahan data yang belum selesai dirender.
            binding.cvItemNote.setOnClickListener {
                val position = bindingAdapterPosition

                // Pengecekan NO_POSITION mencegah crash jika item diklik saat sedang
                // dalam proses animasi penghapusan/penambahan (posisi belum valid).
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickCallback.onItemClicked(note, position)
                }
            }
        }

    }

    // Interface ini adalah kontrak — siapapun yang memakai NoteAdapter WAJIB mengimplementasikan
    // onItemClicked. Ini memungkinkan Adapter memberitahu Activity tanpa harus tahu Activity mana.
    interface OnItemClickCallback {
        fun onItemClicked(selectedNote: Note?, position: Int?)
    }
}
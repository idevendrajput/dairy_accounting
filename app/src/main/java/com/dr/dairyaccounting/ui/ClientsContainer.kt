package com.dr.dairyaccounting.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.databinding.FragmentClientsContainerBinding
import com.dr.dairyaccounting.databinding.RowItemClientsBinding
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.COLLECTION_CLIENTS
import com.dr.dairyaccounting.utils.AppConstants.ORDER_TIMESTAMP
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap

class ClientsContainer : Fragment() {

    private lateinit var mContext: Context
    private lateinit var binding: FragmentClientsContainerBinding
    private lateinit var room: MyDatabase
    private var records = ArrayList<ClientsEntity>()
    private var selectedTab = 1
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClientsContainerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        room = MyDatabase.getDatabase(mContext)

        toolBar()
        loadData()
        tabs()
        actions()
    }

    private fun actions() {

        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack(R.id.clientsContainer, true)
        }

    }

    private fun toolBar() {

        val menu = binding.tb.menu
        val searchView = menu[0].actionView as SearchView
        val addNewClient = menu[1]

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                search(query ?: "")
                 return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                search(newText ?: "")
                return false
            }

        })
        addNewClient.setOnMenuItemClickListener {
            findNavController().navigate(R.id.addClient)
            false
        }

    }

    private fun search(s : String) {

        if (s.trim().isEmpty()) {
            loadData()
            return
        }

        binding.rv.removeAllViewsInLayout()
        records.clear()

       records = room.clientDao().getAllClients() as ArrayList<ClientsEntity>

        val result = ArrayList<ClientsEntity>()

        for (i in records) {
            if (i.clientName.lowercase().contains(s.lowercase()) || i.phone.contains(s.lowercase())) {
                result.add(i)
            }
        }

        binding.rv.adapter = AdapterClients(result)

    }

    private fun tabs() {

        for (i in 0..2) {
            binding.tabs.getChildAt(i).setOnClickListener {
                selectedTab = i
                refreshSelection()
            }
        }

    }

    private fun refreshSelection() {
        for (i in 0..2) {
            if (i == selectedTab) {
                (binding.tabs.getChildAt(i) as TextView).background = ContextCompat.getDrawable(mContext,
                    R.drawable.bg_tabs
                )
                (binding.tabs.getChildAt(i) as TextView).setTextColor(Color.WHITE)
            } else {
                (binding.tabs.getChildAt(i) as TextView).background = null
                (binding.tabs.getChildAt(i) as TextView).setTextColor(Color.BLACK)
            }
        }
        loadData()
    }

    private fun loadData() {

        binding.rv.removeAllViewsInLayout()
        records.clear()

        records = when(selectedTab) {
            0-> room.clientDao().getAllPurchaseClients() as ArrayList<ClientsEntity>
            1-> room.clientDao().getAllClients() as ArrayList<ClientsEntity>
            2-> room.clientDao().getAllSalesClients() as ArrayList<ClientsEntity>
            else-> room.clientDao().getAllClients() as ArrayList<ClientsEntity>
        }


        binding.noResult.visibility = View.GONE
        if (records.isEmpty()) {
            binding.noResult.visibility = View.VISIBLE
        }

        val adapter = AdapterClients(records)

        val callback: ItemTouchHelper.Callback = ItemMoveCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.rv)

        binding.rv.adapter = adapter

    }

    inner class AdapterClients(val list : ArrayList<ClientsEntity>) : RecyclerView.Adapter<AdapterClients.ClientHolder>(), ItemTouchHelperContract {

        inner class ClientHolder(itemView: View, val dBinding : RowItemClientsBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ClientHolder {
            val binding = RowItemClientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ClientHolder(binding.root, binding)
        }

        override fun onBindViewHolder(holder: ClientHolder, position: Int) {

            val srNo = (position + 1).toString().plus(".       ")
            holder.dBinding.clientName.text =  srNo.plus(list[position].clientName)

            holder.dBinding.clientName.setTextColor(if (list[position].isActive) Color.BLACK else Color.RED)

            holder.itemView.setOnClickListener {

                val options = arrayOf("Edit", "View", "Delete")

                AlertDialog.Builder(mContext)
                    .setTitle("Options")
                    .setItems(options){_,i->
                        when(i) {
                            in 0..1-> {
                                val args = Bundle()
                                args.putString(CLIENT_ID, list[position].id)
                                findNavController().navigate(R.id.addClient, args)
                            }
                            2-> {
                                AlertDialog.Builder(mContext)
                                    .setTitle("Delete")
                                    .setMessage("Are you sure you want to delete this client permanently?")
                                    .setPositiveButton("Yes"){_,_->
                                        db.collection(COLLECTION_CLIENTS)
                                            .document(list[position].id).delete()
                                        room.clientDao().deleteById(list[position].id)
                                        list.removeAt(position)
                                        notifyItemRemoved(position)
                                    }
                                    .setNegativeButton("No", null).create().show()
                            }
                        }
                    }.create().show()
            }

        }

        private fun reOrderItems() {

            for (i in 0 until list.lastIndex) {

                list[i].orderTimeStamp = i.toLong()

                val map = HashMap<String, Any>()
                map[ORDER_TIMESTAMP] = i.toLong()
                db.collection(COLLECTION_CLIENTS).document(list[i].id)[map] = SetOptions.merge()

                CoroutineScope(IO)
                    .launch {
                        try {
                            room.clientDao().updateClient(list[i])
                        } catch (e: Exception) {}
                    }
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onRowMoved(fromPosition: Int, toPosition: Int) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(list, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(list, i, i - 1)
                }
            }
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onRowSelected(myViewHolder: ClientHolder?) {
            myViewHolder?.itemView?.setBackgroundColor(Color.GRAY);
        }

        override fun onRowClear(myViewHolder: ClientHolder?) {
            myViewHolder?.itemView?.setBackgroundColor(Color.WHITE)
            notifyDataSetChanged()
            if (list.size > 1) {
                reOrderItems()
            }
        }

    }

    inner class ItemMoveCallback(private val mAdapter: ItemTouchHelperContract) : ItemTouchHelper.Callback() {

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {}

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            return makeMovementFlags(dragFlags, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            mAdapter.onRowMoved(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSelectedChanged(
            viewHolder: RecyclerView.ViewHolder?,
            actionState: Int
        ) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                if (viewHolder is AdapterClients.ClientHolder) {
                    val myViewHolder: AdapterClients.ClientHolder = viewHolder
                    mAdapter.onRowSelected(myViewHolder)
                }
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) {
            super.clearView(recyclerView, viewHolder)
            if (viewHolder is AdapterClients.ClientHolder) {
                val myViewHolder: AdapterClients.ClientHolder = viewHolder
                mAdapter.onRowClear(myViewHolder)
            }
        }
    }

    interface ItemTouchHelperContract {
        fun onRowMoved(fromPosition: Int, toPosition: Int)
        fun onRowSelected(myViewHolder: AdapterClients.ClientHolder?)
        fun onRowClear(myViewHolder: AdapterClients.ClientHolder?)
    }

}
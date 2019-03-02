package app.opass.ccip.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.opass.ccip.R
import app.opass.ccip.adapter.EventAdapter
import app.opass.ccip.model.Event
import app.opass.ccip.network.PortalClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EventActivity : AppCompatActivity() {

    private lateinit var mActivity: Activity
    private lateinit var noNetworkView: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        mActivity = this
        setSupportActionBar(findViewById(R.id.toolbar))
        setTitle(R.string.select_event)

        getEvents()
    }

    internal fun getEvents() {
        swipeRefreshLayout = findViewById(R.id.swipeContainer)
        recyclerView = findViewById(R.id.events)
        noNetworkView = findViewById(R.id.no_network)
        viewManager = LinearLayoutManager(mActivity)
        swipeRefreshLayout.isRefreshing = true

        val events = PortalClient.get().getEvents()
        events.enqueue(object : Callback<List<Event>> {
            override fun onResponse(call: Call<List<Event>>, response: Response<List<Event>>) {
                when {
                    response.isSuccessful -> {
                        swipeRefreshLayout.isRefreshing = false
                        swipeRefreshLayout.isEnabled = false
                        noNetworkView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE

                        viewAdapter = EventAdapter(mActivity, response.body())

                        recyclerView.apply {
                            setHasFixedSize(true)
                            layoutManager = viewManager
                            adapter = viewAdapter
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<Event>>, t: Throwable) {
                swipeRefreshLayout.isRefreshing = false
                recyclerView.visibility = GONE
                noNetworkView.visibility = VISIBLE
                noNetworkView.setOnClickListener {
                    swipeRefreshLayout.isRefreshing = true
                    noNetworkView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    getEvents()
                }
            }
        })
    }
}
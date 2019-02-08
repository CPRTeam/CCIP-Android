package app.opass.ccip.fragment

import android.app.Activity
import android.graphics.PorterDuff.Mode
import android.os.Build.VERSION
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import app.opass.ccip.R
import app.opass.ccip.adapter.ScheduleTabAdapter
import app.opass.ccip.model.Submission
import app.opass.ccip.network.ConfClient
import app.opass.ccip.util.PreferenceUtil
import com.google.android.material.tabs.TabLayout
import com.google.gson.internal.bind.util.ISO8601Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

class ScheduleTabFragment : Fragment() {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var menuItemStar: MenuItem
    private lateinit var mActivity: Activity
    private var starFilter = false
    private var mSubmissions: List<Submission>? = null
    private var scheduleTabAdapter: ScheduleTabAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_schedule_tab, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipeContainer)
        tabLayout = view.findViewById(R.id.tabs)
        viewPager = view.findViewById(R.id.pager)

        mActivity = requireActivity()

        if (VERSION.SDK_INT >= 21) {
            mActivity.findViewById<View>(R.id.appbar).elevation = 0f
        }

        setHasOptionsMenu(true)

        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }

        val submissionCall = ConfClient.get().submission()
        submissionCall.enqueue(object : Callback<List<Submission>> {
            override fun onResponse(call: Call<List<Submission>>, response: Response<List<Submission>>) {
                if (response.isSuccessful) {
                    swipeRefreshLayout.isRefreshing = false

                    mSubmissions = response.body()
                    PreferenceUtil.savePrograms(mActivity, mSubmissions!!)
                } else {
                    loadOfflineSchedule()
                }
                setupViewPager()
            }

            override fun onFailure(call: Call<List<Submission>>, t: Throwable) {
                loadOfflineSchedule()
                setupViewPager()
            }
        })

        return view
    }

    override fun onDestroy() {
        tabLayout.setupWithViewPager(null)
        super.onDestroy()
    }

    private fun setupViewPager() {
        if (isAdded) {
            scheduleTabAdapter = ScheduleTabAdapter(childFragmentManager)
            addSubmissionFragments(mSubmissions!!)
            viewPager.adapter = scheduleTabAdapter
            tabLayout.setupWithViewPager(viewPager)
            menuItemStar.isVisible = true
        }
    }

    private fun addSubmissionFragments(submissions: List<Submission>) {
        val map = HashMap<String, MutableList<Submission>>()
        for (submission in submissions) {
            try {
                val dateKey = SDF_DATE.format(ISO8601Utils.parse(submission.start, ParsePosition(0)))
                if (map.containsKey(dateKey)) {
                    val tmp = map[dateKey]
                    tmp!!.add(submission)
                    map[dateKey] = tmp
                } else {
                    val arrayList = ArrayList<Submission>()
                    arrayList.add(submission)
                    map[dateKey] = arrayList
                }
            } catch (e: ParseException) {
                e.printStackTrace()
            }

        }
        val keys = TreeSet(map.keys)
        for (key in keys) {
            val value = map[key]
            scheduleTabAdapter!!.addFragment(ScheduleFragment.newInstance(key, value!!), key)
        }
        scheduleTabAdapter!!.notifyDataSetChanged()

        if (map.size == 1) {
            tabLayout.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu!!.add("star")
            .setIcon(R.drawable.ic_bookmark_border_black_24dp)
            .setOnMenuItemClickListener { item ->
                starFilter = !starFilter
                if (starFilter) {
                    item.setIcon(R.drawable.ic_bookmark_black_24dp)
                } else {
                    item.setIcon(R.drawable.ic_bookmark_border_black_24dp)
                }
                item.icon.setColorFilter(
                    resources.getColor(R.color.colorWhite),
                    Mode.SRC_ATOP
                )
                scheduleTabAdapter!!.toggleStarFilter(starFilter)
                false
            }
            .setVisible(false)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menuItemStar = menu.getItem(0)
        menuItemStar.icon.setColorFilter(
            resources.getColor(R.color.colorWhite),
            Mode.SRC_ATOP
        )
    }

    fun loadOfflineSchedule() {
        swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = false }
        Toast.makeText(mActivity, R.string.offline, Toast.LENGTH_LONG).show()
        val submissions = PreferenceUtil.loadPrograms(mActivity)
        if (submissions != null) {
            mSubmissions = submissions
        }
    }

    companion object {

        private val SDF_DATE = SimpleDateFormat("MM/dd")
    }
}
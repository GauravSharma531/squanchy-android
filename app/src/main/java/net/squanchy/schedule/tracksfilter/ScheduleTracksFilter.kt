package net.squanchy.schedule.tracksfilter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import androidx.view.postOnAnimationDelayed
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxItemDecoration
import com.google.android.flexbox.FlexboxLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_track_filters.*
import net.squanchy.R
import net.squanchy.schedule.domain.view.Track
import net.squanchy.service.repository.TracksRepository
import net.squanchy.support.view.setAdapterIfNone
import kotlin.math.hypot

class ScheduleTracksFilterActivity : AppCompatActivity() {

    private lateinit var tracksRepository: TracksRepository
    private lateinit var tracksFilter: TracksFilter
    private lateinit var trackAdapter: TracksFilterAdapter

    private lateinit var appearInterpolator: Interpolator

    private var subscription: Disposable? = null
    private var checkableTracks: List<CheckableTrack> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_track_filters)

        backgroundDim.setOnClickListener { finish() }
        closeButton.setOnClickListener { finish() }

        val component = tracksFilterComponent(this)
        tracksRepository = component.tracksRepository()
        tracksFilter = component.tracksFilter()

        trackAdapter = TracksFilterAdapter(this) { track, selected ->
            val selectedTracks = checkableTracks.allSelected()
            val newSelectedTracks = selectedTracks.addOrRemove(track, selected)
            tracksFilter.updateSelectedTracks(newSelectedTracks)
        }

        trackFiltersList.layoutManager = FlexboxLayoutManager(this, FlexDirection.ROW)
        trackFiltersList.addItemDecoration(FlexboxItemDecoration(this).apply {
            setDrawable(resources.getDrawable(R.drawable.filters_separator, theme))
            setOrientation(FlexboxItemDecoration.BOTH)
        })
        trackFiltersList.itemAnimator = null
    }

    private fun Set<Track>.addOrRemove(track: Track, selected: Boolean): Set<Track> =
        if (selected) this + track else this - track

    override fun onStart() {
        super.onStart()

        subscription = Observable.combineLatest(tracksRepository.tracks(), tracksFilter.selectedTracks, combineIntoCheckableTracks())
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { checkableTracks ->
                trackFiltersList.setAdapterIfNone(trackAdapter)
                this.checkableTracks = checkableTracks
                trackAdapter.submitList(checkableTracks)
            }

        prepareAppearanceAnimation()
        filtersRoot.postOnAnimation { animateAppearing() }
    }

    private fun prepareAppearanceAnimation() {
        appearInterpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in)

        val titleDeltaY = resources.getDimension(R.dimen.track_filters_title_appear_delta_y)
        dialogTitle.apply {
            translationY = -titleDeltaY
            alpha = 0F
        }

        val subtitleDeltaY = resources.getDimension(R.dimen.track_filters_subtitle_appear_delta_y)
        dialogSubtitle.apply {
            translationY = -subtitleDeltaY
            alpha = 0F
        }

        val tracksDeltaY = resources.getDimension(R.dimen.track_filters_tracks_appear_delta_y)
        trackFiltersList.apply {
            translationY = -tracksDeltaY
            alpha = 0F
        }
    }

    @Suppress("MagicNumber") // Just animation code… needs a few magic numbers
    private fun animateAppearing() {
        val centerX = closeButton.x + closeButton.width / 2
        val centerY = closeButton.y + closeButton.height / 2

        ViewAnimationUtils.createCircularReveal(
            filtersRoot,
            centerX.toInt(),
            centerY.toInt(),
            0F,
            hypot(filtersRoot.width.toDouble(), filtersRoot.height.toDouble()).toFloat()
        ).apply {
            val totalDuration = resources.getInteger(R.integer.track_filters_appear_duration).toLong()
            val delay = resources.getInteger(R.integer.track_filters_appear_delay).toLong()

            duration = totalDuration

            filtersRoot.postOnAnimationDelayed(totalDuration / 2) {
                dialogTitle.slideDownAndFadeIn(duration = totalDuration - delay, delay = 0)
                dialogSubtitle.slideDownAndFadeIn(duration = totalDuration - 2 * delay, delay = delay)
                trackFiltersList.slideDownAndFadeIn(duration = totalDuration - 3 * delay, delay = 2 * delay)
            }

            start()
        }
    }

    private fun View.slideDownAndFadeIn(duration: Long, delay: Long = 0) = this.animate()
        .translationY(0F)
        .alpha(1F)
        .setInterpolator(appearInterpolator)
        .setDuration(duration)
        .setStartDelay(delay)
        .start()

    private fun combineIntoCheckableTracks(): BiFunction<List<Track>, Set<Track>, List<CheckableTrack>> {
        return BiFunction { tracks, selectedTracks ->
            tracks.map { track -> CheckableTrack(track, selectedTracks.contains(track)) }
        }
    }

    override fun onStop() {
        super.onStop()
        subscription?.dispose()
    }
}

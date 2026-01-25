package cn.sffzh.tempus.ui.activity.controller

import android.os.Handler
import android.os.Looper
import android.view.View
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ActivityMainBinding
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.fragment.PlayerBottomSheetFragment
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import kotlin.math.max
import kotlin.math.min

class BottomSheetController(
    private val activity: MainActivity,
    private val binding: ActivityMainBinding,
    private val mainViewModel: MainViewModel
) {

    val behavior: BottomSheetBehavior<View> =
        BottomSheetBehavior.from(binding.playerBottomSheet)

    init {
        binding.root.post {
            initBottomSheet()
        }
    }

    private fun initBottomSheet() {
        behavior.addBottomSheetCallback(bottomSheetCallback)
        activity.supportFragmentManager.beginTransaction()
            .replace(
                R.id.player_bottom_sheet,
                PlayerBottomSheetFragment(),
                "PlayerBottomSheet"
            )
            .commit()

        checkBottomSheetAfterStateChanged()
    }

    fun setInPeek(isVisible: Boolean) {
        behavior.state =
            if (isVisible) BottomSheetBehavior.STATE_COLLAPSED
            else BottomSheetBehavior.STATE_HIDDEN
    }

    fun setVisibility(visible: Boolean) {
        binding.playerBottomSheet.visibility =
            if (visible) View.VISIBLE else View.GONE
    }

    fun expand() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun collapseDelayed(delayMs: Long = 100L) {
        Handler(Looper.getMainLooper()).postDelayed(
            { behavior.state = BottomSheetBehavior.STATE_COLLAPSED },
            delayMs
        )
    }

    fun setDraggable(isDraggable: Boolean) {
        behavior.isDraggable = isDraggable
    }

    private fun checkBottomSheetAfterStateChanged() {
        Handler(Looper.getMainLooper()).postDelayed(
            { setInPeek(mainViewModel.isQueueLoaded()) },
            100L
        )
    }

    private val bottomSheetCallback = object : BottomSheetCallback() {
        private var navigationHeight: Int = 0

        override fun onStateChanged(view: View, newState: Int) {
            val fragment =
                activity.supportFragmentManager.findFragmentByTag("PlayerBottomSheet")
                        as? PlayerBottomSheetFragment

            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> activity.resetMusicSession()
                BottomSheetBehavior.STATE_COLLAPSED -> fragment?.goBackToFirstPage()
                BottomSheetBehavior.STATE_SETTLING,
                BottomSheetBehavior.STATE_EXPANDED,
                BottomSheetBehavior.STATE_DRAGGING,
                BottomSheetBehavior.STATE_HALF_EXPANDED -> Unit
            }
        }

        override fun onSlide(view: View, slideOffset: Float) {
            animateBottomSheet(slideOffset)
            animateBottomNavigation(slideOffset, navigationHeight)
        }
    }


    private fun animateBottomSheet(slideOffset: Float) {
        val fragment =
            activity.supportFragmentManager.findFragmentByTag("PlayerBottomSheet")
                    as? PlayerBottomSheetFragment ?: return

        val condensed = max(0f, min(0.2f, slideOffset - 0.2f)) / 0.2f
        fragment.playerHeader.alpha = 1 - condensed
        fragment.playerHeader.visibility =
            if (condensed > 0.99f) View.GONE else View.VISIBLE
    }

    private fun animateBottomNavigation(slideOffset: Float, navigationHeight: Int) {
        if (slideOffset < 0) return

        var height = navigationHeight
        if (height == 0) {
            height = binding.bottomNavigation.height
        }

        val slideY = height - height * (1 - slideOffset)
        binding.bottomNavigation.translationY = slideY
    }
}

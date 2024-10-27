package com.drdisagree.iconify.ui.fragments.xposed

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.drdisagree.iconify.Iconify.Companion.appContext
import com.drdisagree.iconify.Iconify.Companion.appContextLocale
import com.drdisagree.iconify.R
import com.drdisagree.iconify.common.Const.AI_PLUGIN_PACKAGE
import com.drdisagree.iconify.common.Const.AI_PLUGIN_URL
import com.drdisagree.iconify.common.Preferences.DEPTH_WALLPAPER_AI_MODE
import com.drdisagree.iconify.common.Preferences.DEPTH_WALLPAPER_AI_STATUS
import com.drdisagree.iconify.common.Preferences.DEPTH_WALLPAPER_CHANGED
import com.drdisagree.iconify.common.Preferences.DEPTH_WALLPAPER_SWITCH
import com.drdisagree.iconify.common.Resources.DEPTH_WALL_BG_DIR
import com.drdisagree.iconify.common.Resources.DEPTH_WALL_FG_DIR
import com.drdisagree.iconify.config.RPrefs
import com.drdisagree.iconify.config.RPrefs.putBoolean
import com.drdisagree.iconify.ui.activities.MainActivity
import com.drdisagree.iconify.ui.base.ControlledPreferenceFragmentCompat
import com.drdisagree.iconify.ui.preferences.FilePickerPreference
import com.drdisagree.iconify.ui.preferences.PreferenceMenu
import com.drdisagree.iconify.ui.preferences.SwitchPreference
import com.drdisagree.iconify.utils.AppUtils
import com.drdisagree.iconify.utils.FileUtils.getRealPath
import com.drdisagree.iconify.utils.FileUtils.launchFilePicker
import com.drdisagree.iconify.utils.FileUtils.moveToIconifyHiddenDir
import com.drdisagree.iconify.xposed.modules.utils.BitmapSubjectSegmenter
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse

class DepthWallpaper : ControlledPreferenceFragmentCompat() {

    override val title: String
        get() = getString(R.string.activity_title_depth_wallpaper)

    override val backButtonEnabled: Boolean
        get() = true

    override val layoutResource: Int
        get() = R.xml.xposed_depth_wallpaper

    override val hasMenu: Boolean
        get() = true

    private var startActivityIntentForBackgroundImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val path = getRealPath(data)

            if (path != null && moveToIconifyHiddenDir(path, DEPTH_WALL_BG_DIR)) {
                putBoolean(DEPTH_WALLPAPER_CHANGED, false)
                putBoolean(DEPTH_WALLPAPER_CHANGED, true)

                Toast.makeText(
                    appContext,
                    appContextLocale.resources.getString(R.string.toast_applied),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    appContext,
                    appContextLocale.resources.getString(R.string.toast_rename_file),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private var startActivityIntentForForegroundImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val path = getRealPath(data)

            if (path != null && moveToIconifyHiddenDir(path, DEPTH_WALL_FG_DIR)) {
                putBoolean(DEPTH_WALLPAPER_CHANGED, false)
                putBoolean(DEPTH_WALLPAPER_CHANGED, true)

                Toast.makeText(
                    appContext,
                    appContextLocale.resources.getString(R.string.toast_applied),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    appContext,
                    appContextLocale.resources.getString(R.string.toast_rename_file),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun updateScreen(key: String?) {
        super.updateScreen(key)

        when (key) {
            DEPTH_WALLPAPER_SWITCH -> {
                MainActivity.showOrHidePendingActionButton(
                    activityBinding = (requireActivity() as MainActivity).binding,
                    requiresSystemUiRestart = true
                )
            }
            DEPTH_WALLPAPER_AI_MODE -> {
                checkAiStatus()
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        findPreference<SwitchPreference>(DEPTH_WALLPAPER_SWITCH)?.apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
                setSummary(
                    getString(
                        R.string.enable_depth_wallpaper_desc,
                        getString(R.string.use_custom_lockscreen_clock)
                    )
                )
            } else {
                setSummary(
                    getString(
                        R.string.enable_depth_wallpaper_desc,
                        ""
                    ).replace("\n", "") // hide args
                )
            }
        }

        checkAiStatus()

        findPreference<FilePickerPreference>("xposed_depthwallpaperbgimagepicker")?.apply {
            setOnButtonClick {
                launchFilePicker(context, "image", startActivityIntentForBackgroundImage)
            }
        }

        findPreference<FilePickerPreference>("xposed_depthwallpaperfgimagepicker")?.apply {
            setOnButtonClick {
                launchFilePicker(context, "image", startActivityIntentForForegroundImage)
            }
        }
    }

    private fun checkAiStatus() {
        findPreference<PreferenceMenu>(DEPTH_WALLPAPER_AI_STATUS)?.apply {
            if (RPrefs.getString(DEPTH_WALLPAPER_AI_MODE, "0") == "0") {
                BitmapSubjectSegmenter(requireContext())
                    .checkModelAvailability { moduleAvailabilityResponse: ModuleAvailabilityResponse? ->
                        setSummary(
                            getString(
                                if (moduleAvailabilityResponse?.areModulesAvailable() == true) {
                                    R.string.depth_wallpaper_model_ready
                                } else {
                                    R.string.depth_wallpaper_model_not_available
                                }
                            )
                        )
                    }
            } else {
                setShowArrow(true)
                if (AppUtils.isAppInstalled(AI_PLUGIN_PACKAGE)) {
                    setSummary(getString(R.string.depth_wallpaper_ai_status_plugin_installed))
                    setOnPreferenceClickListener {
                        val intent = requireContext().packageManager.getLaunchIntentForPackage(AI_PLUGIN_PACKAGE)
                        startActivity(intent!!)
                        true
                    }
                } else {
                    setSummary(getString(R.string.depth_wallpaper_ai_status_plugin_not_installed))
                    setOnPreferenceClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AI_PLUGIN_URL))
                        startActivity(intent)
                        true
                    }
                }
            }
        }
    }

}

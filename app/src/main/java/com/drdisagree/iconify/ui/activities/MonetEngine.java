package com.drdisagree.iconify.ui.activities;

import static com.drdisagree.iconify.common.Preferences.COLOR_ACCENT_PRIMARY;
import static com.drdisagree.iconify.common.Preferences.COLOR_ACCENT_PRIMARY_LIGHT;
import static com.drdisagree.iconify.common.Preferences.COLOR_ACCENT_SECONDARY;
import static com.drdisagree.iconify.common.Preferences.COLOR_ACCENT_SECONDARY_LIGHT;
import static com.drdisagree.iconify.common.Preferences.CUSTOM_ACCENT;
import static com.drdisagree.iconify.common.Preferences.CUSTOM_PRIMARY_COLOR_SWITCH;
import static com.drdisagree.iconify.common.Preferences.CUSTOM_SECONDARY_COLOR_SWITCH;
import static com.drdisagree.iconify.common.Preferences.MONET_ACCENT_SATURATION;
import static com.drdisagree.iconify.common.Preferences.MONET_ACCURATE_SHADES;
import static com.drdisagree.iconify.common.Preferences.MONET_BACKGROUND_LIGHTNESS;
import static com.drdisagree.iconify.common.Preferences.MONET_BACKGROUND_SATURATION;
import static com.drdisagree.iconify.common.Preferences.MONET_COLOR_PALETTE;
import static com.drdisagree.iconify.common.Preferences.MONET_ENGINE_SWITCH;
import static com.drdisagree.iconify.common.Preferences.MONET_STYLE;
import static com.drdisagree.iconify.common.Preferences.STR_NULL;
import static com.drdisagree.iconify.utils.ColorSchemeUtil.generateColorPalette;

import android.annotation.SuppressLint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.drdisagree.iconify.R;
import com.drdisagree.iconify.config.Prefs;
import com.drdisagree.iconify.databinding.ActivityMonetEngineBinding;
import com.drdisagree.iconify.overlaymanager.MonetEngineManager;
import com.drdisagree.iconify.ui.utils.ViewBindingHelpers;
import com.drdisagree.iconify.utils.ColorUtil;
import com.drdisagree.iconify.utils.FabricatedUtil;
import com.drdisagree.iconify.utils.OverlayUtil;
import com.drdisagree.iconify.utils.SystemUtil;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MonetEngine extends BaseActivity implements ColorPickerDialogListener {

    private static String accentPrimary, accentSecondary, selectedStyle;
    private static boolean isSelectedPrimary = false, isSelectedSecondary = false, accurateShades = Prefs.getBoolean(MONET_ACCURATE_SHADES, true);
    private final List<List<List<Object>>> finalPalette = new ArrayList<>();
    private final int[] selectedChild = new int[2];
    int[] monetAccentSaturation = new int[]{Prefs.getInt(MONET_ACCENT_SATURATION, 100)};
    int[] monetBackgroundSaturation = new int[]{Prefs.getInt(MONET_BACKGROUND_SATURATION, 100)};
    int[] monetBackgroundLightness = new int[]{Prefs.getInt(MONET_BACKGROUND_LIGHTNESS, 100)};
    private ActivityMonetEngineBinding binding;
    private LinearLayout[] colorTableRows;
    private int[][] systemColors;
    private ColorPickerDialog.Builder colorPickerDialogPrimary, colorPickerDialogSecondary, colorPickerDialogCustom;
    private boolean isDarkMode = SystemUtil.isDarkMode();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMonetEngineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Header
        ViewBindingHelpers.setHeader(this, binding.header.collapsingToolbar, binding.header.toolbar, R.string.activity_title_monet_engine);

        colorTableRows = new LinearLayout[]{
                binding.monetEngine.systemAccent1,
                binding.monetEngine.systemAccent2,
                binding.monetEngine.systemAccent3,
                binding.monetEngine.systemNeutral1,
                binding.monetEngine.systemNeutral2
        };
        systemColors = ColorUtil.getSystemColors(MonetEngine.this);

        List<List<Object>> temp = new ArrayList<>();
        for (int[] row : systemColors) {
            List<Object> temp2 = new ArrayList<>();
            for (int col : row) {
                temp2.add(col);
            }
            temp.add(temp2);
        }
        finalPalette.clear();
        finalPalette.add(temp);
        finalPalette.add(temp);

        isDarkMode = SystemUtil.isDarkMode();
        selectedStyle = Prefs.getString(MONET_STYLE, getResources().getString(R.string.monet_neutral));

        if (Objects.equals(selectedStyle, getResources().getString(R.string.monet_neutral)))
            binding.neutralStyle.setChecked(true);
        else if (Objects.equals(selectedStyle, getResources().getString(R.string.monet_monochrome)))
            binding.monochromeStyle.setChecked(true);
        else if (Objects.equals(selectedStyle, getResources().getString(R.string.monet_tonalspot)))
            binding.tonalspotStyle.setChecked(true);
        else if (Objects.equals(selectedStyle, getResources().getString(R.string.monet_vibrant)))
            binding.vibrantStyle.setChecked(true);
        else if (Objects.equals(selectedStyle, getResources().getString(R.string.monet_expressive)))
            binding.expressiveStyle.setChecked(true);
        else if (Objects.equals(selectedStyle, getResources().getString(R.string.monet_fidelity)))
            binding.fidelityStyle.setChecked(true);
        else if (Objects.equals(selectedStyle, getResources().getString(R.string.monet_content)))
            binding.contentStyle.setChecked(true);
        else {
            Prefs.putBoolean(MONET_ENGINE_SWITCH, false);
            binding.monetStyles1.clearCheck();
            binding.monetStyles2.clearCheck();
        }

        accentPrimary = String.valueOf(getResources().getColor(isDarkMode ? android.R.color.system_accent1_300 : android.R.color.system_accent1_600, getTheme()));
        accentSecondary = String.valueOf(getResources().getColor(isDarkMode ? android.R.color.system_accent3_300 : android.R.color.system_accent3_600, getTheme()));

        updatePrimaryColor();
        updateSecondaryColor();

        binding.monetStyles1.setOnCheckedChangeListener(listener1);
        binding.monetStyles2.setOnCheckedChangeListener(listener2);

        assignStockColorToPalette();

        colorPickerDialogPrimary = ColorPickerDialog.newBuilder();
        colorPickerDialogSecondary = ColorPickerDialog.newBuilder();
        colorPickerDialogCustom = ColorPickerDialog.newBuilder();

        colorPickerDialogPrimary.setDialogStyle(R.style.ColorPicker).setColor(Integer.parseInt(accentPrimary)).setDialogType(ColorPickerDialog.TYPE_CUSTOM).setAllowCustom(false).setAllowPresets(true).setDialogId(1).setShowAlphaSlider(false).setShowColorShades(true);
        colorPickerDialogSecondary.setDialogStyle(R.style.ColorPicker).setColor(Integer.parseInt(accentSecondary)).setDialogType(ColorPickerDialog.TYPE_CUSTOM).setAllowCustom(false).setAllowPresets(true).setDialogId(2).setShowAlphaSlider(false).setShowColorShades(true);

        binding.previewColoraccentprimary.setOnClickListener(v -> colorPickerDialogPrimary.show(MonetEngine.this));

        binding.previewColoraccentsecondary.setOnClickListener(v -> colorPickerDialogSecondary.show(MonetEngine.this));

        // Monet Accurate Shades
        binding.monetAccurateShades.setChecked(Prefs.getBoolean(MONET_ACCURATE_SHADES, true));
        binding.monetAccurateShades.setOnCheckedChangeListener((buttonView, isChecked) -> {
            accurateShades = isChecked;
            assignCustomColorToPalette();
            binding.enableCustomMonet.setVisibility(View.VISIBLE);
        });

        // Monet accent saturation
        binding.monetAccentSaturationOutput.setText(getResources().getString(R.string.opt_selected) + ' ' + (Prefs.getInt(MONET_ACCENT_SATURATION, 100) - 100) + "%");
        binding.monetAccentSaturationSeekbar.setProgress(Prefs.getInt(MONET_ACCENT_SATURATION, 100));

        // Long Click Reset
        binding.resetAccentSaturation.setVisibility(Prefs.getInt(MONET_ACCENT_SATURATION, 100) == 100 ? View.INVISIBLE : View.VISIBLE);

        binding.resetAccentSaturation.setOnLongClickListener(v -> {
            monetAccentSaturation[0] = 100;
            binding.monetAccentSaturationSeekbar.setProgress(100);
            assignCustomColorToPalette();
            binding.resetAccentSaturation.setVisibility(View.INVISIBLE);
            binding.enableCustomMonet.setVisibility(View.VISIBLE);
            return true;
        });

        binding.monetAccentSaturationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                monetAccentSaturation[0] = progress;
                if (progress == 100) binding.resetAccentSaturation.setVisibility(View.INVISIBLE);
                binding.monetAccentSaturationOutput.setText(getResources().getString(R.string.opt_selected) + ' ' + (progress - 100) + "%");
                assignCustomColorToPalette();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
                binding.resetAccentSaturation.setVisibility(monetAccentSaturation[0] == 100 ? View.INVISIBLE : View.VISIBLE);
            }
        });

        // Monet background saturation
        binding.monetBackgroundSaturationOutput.setText(getResources().getString(R.string.opt_selected) + ' ' + (Prefs.getInt(MONET_BACKGROUND_SATURATION, 100) - 100) + "%");
        binding.monetBackgroundSaturationSeekbar.setProgress(Prefs.getInt(MONET_BACKGROUND_SATURATION, 100));

        // Reset button
        binding.resetBackgroundSaturation.setVisibility(Prefs.getInt(MONET_BACKGROUND_SATURATION, 100) == 100 ? View.INVISIBLE : View.VISIBLE);

        binding.resetBackgroundSaturation.setOnLongClickListener(v -> {
            monetBackgroundSaturation[0] = 100;
            binding.monetBackgroundSaturationSeekbar.setProgress(100);
            assignCustomColorToPalette();
            binding.resetBackgroundSaturation.setVisibility(View.INVISIBLE);
            binding.enableCustomMonet.setVisibility(View.VISIBLE);
            return true;
        });

        binding.monetBackgroundSaturationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                monetBackgroundSaturation[0] = progress;
                if (progress == 100)
                    binding.resetBackgroundSaturation.setVisibility(View.INVISIBLE);
                binding.monetBackgroundSaturationOutput.setText(getResources().getString(R.string.opt_selected) + ' ' + (progress - 100) + "%");
                assignCustomColorToPalette();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
                binding.resetBackgroundSaturation.setVisibility(monetBackgroundSaturation[0] == 100 ? View.INVISIBLE : View.VISIBLE);
            }
        });

        // Monet background lightness
        binding.monetBackgroundLightnessOutput.setText(getResources().getString(R.string.opt_selected) + ' ' + (Prefs.getInt(MONET_BACKGROUND_LIGHTNESS, 100) - 100) + "%");
        binding.monetBackgroundLightnessSeekbar.setProgress(Prefs.getInt(MONET_BACKGROUND_LIGHTNESS, 100));

        // Long Click Reset
        binding.resetBackgroundLightness.setVisibility(Prefs.getInt(MONET_BACKGROUND_LIGHTNESS, 100) == 100 ? View.INVISIBLE : View.VISIBLE);

        binding.resetBackgroundLightness.setOnLongClickListener(v -> {
            monetBackgroundLightness[0] = 100;
            binding.monetBackgroundLightnessSeekbar.setProgress(100);
            assignCustomColorToPalette();
            binding.resetBackgroundLightness.setVisibility(View.INVISIBLE);
            binding.enableCustomMonet.setVisibility(View.VISIBLE);
            return true;
        });

        binding.monetBackgroundLightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                monetBackgroundLightness[0] = progress;
                if (progress == 100) binding.resetBackgroundLightness.setVisibility(View.INVISIBLE);
                binding.monetBackgroundLightnessOutput.setText(getResources().getString(R.string.opt_selected) + ' ' + (progress - 100) + "%");
                assignCustomColorToPalette();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
                binding.resetBackgroundLightness.setVisibility(monetBackgroundLightness[0] == 100 ? View.INVISIBLE : View.VISIBLE);
            }
        });

        // Enable custom colors button
        binding.enableCustomMonet.setVisibility(View.GONE);
        binding.enableCustomMonet.setOnClickListener(v -> {
            if (!Environment.isExternalStorageManager()) {
                SystemUtil.getStoragePermission(this);
            } else if (Objects.equals(selectedStyle, STR_NULL)) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_select_style), Toast.LENGTH_SHORT).show();
            } else {
                Prefs.putBoolean(MONET_ACCURATE_SHADES, accurateShades);
                Prefs.putInt(MONET_ACCENT_SATURATION, monetAccentSaturation[0]);
                Prefs.putInt(MONET_BACKGROUND_SATURATION, monetBackgroundSaturation[0]);
                Prefs.putInt(MONET_BACKGROUND_LIGHTNESS, monetBackgroundLightness[0]);

                if (isSelectedPrimary) Prefs.putString(COLOR_ACCENT_PRIMARY, accentPrimary);
                if (isSelectedSecondary) Prefs.putString(COLOR_ACCENT_SECONDARY, accentSecondary);

                disableBasicColors();

                AtomicBoolean hasErroredOut = new AtomicBoolean(false);

                Runnable runnable1 = () -> {
                    try {
                        if (MonetEngineManager.enableOverlay(finalPalette, true)) {
                            Prefs.clearPref(MONET_COLOR_PALETTE);
                            hasErroredOut.set(true);
                        } else {
                            Prefs.putString(MONET_STYLE, selectedStyle);
                        }
                    } catch (Exception e) {
                        hasErroredOut.set(true);
                        Log.e("MonetEngine", e.toString());
                    }

                    runOnUiThread(() -> {
                        if (!hasErroredOut.get()) {
                            Prefs.putBoolean(MONET_ENGINE_SWITCH, true);
                            if (Prefs.getBoolean("IconifyComponentQSPBD.overlay")) {
                                OverlayUtil.changeOverlayState("IconifyComponentQSPBD.overlay", false, "IconifyComponentQSPBD.overlay", true);
                            } else if (Prefs.getBoolean("IconifyComponentQSPBA.overlay")) {
                                OverlayUtil.changeOverlayState("IconifyComponentQSPBA.overlay", false, "IconifyComponentQSPBA.overlay", true);
                            }
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!hasErroredOut.get()) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_applied), Toast.LENGTH_SHORT).show();
                                binding.enableCustomMonet.setVisibility(View.GONE);
                                binding.disableCustomMonet.setVisibility(View.VISIBLE);
                            } else
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_error), Toast.LENGTH_SHORT).show();
                        }, 20);
                    });
                };
                Thread thread1 = new Thread(runnable1);
                thread1.start();
            }
        });

        // Disable custom colors button
        binding.disableCustomMonet.setVisibility(Prefs.getBoolean(MONET_ENGINE_SWITCH) ? View.VISIBLE : View.GONE);
        binding.disableCustomMonet.setOnClickListener(v -> {
            Runnable runnable2 = () -> {
                Prefs.putBoolean(MONET_ENGINE_SWITCH, false);
                Prefs.putString(COLOR_ACCENT_PRIMARY, STR_NULL);
                Prefs.putString(COLOR_ACCENT_SECONDARY, STR_NULL);
                OverlayUtil.disableOverlays("IconifyComponentDM.overlay", "IconifyComponentME.overlay");

                runOnUiThread(() -> {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_disabled), Toast.LENGTH_SHORT).show();
                        binding.disableCustomMonet.setVisibility(View.GONE);
                        isSelectedPrimary = false;
                        isSelectedSecondary = false;
                    }, 2000);
                });
            };
            Thread thread2 = new Thread(runnable2);
            thread2.start();
        });

        for (int i = 0; i < colorTableRows.length; i++) {
            for (int j = 0; j < colorTableRows[i].getChildCount(); j++) {
                View child = colorTableRows[i].getChildAt(j);
                int finalI = i;
                int finalJ = j;
                child.setOnClickListener(view -> {
                    selectedChild[0] = finalI;
                    selectedChild[1] = finalJ;
                    int[] color = ((GradientDrawable) child.getBackground()).getColors();
                    colorPickerDialogCustom.setDialogStyle(R.style.ColorPicker).setColor(color[0]).setDialogType(ColorPickerDialog.TYPE_CUSTOM).setAllowCustom(false).setAllowPresets(true).setDialogId(3).setShowAlphaSlider(false).setShowColorShades(true).show(MonetEngine.this);
                });
            }
        }
    }

    private void updatePrimaryColor() {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Integer.parseInt(accentPrimary), Integer.parseInt(accentPrimary)});
        gd.setCornerRadius(getResources().getDimension(R.dimen.preview_color_picker_radius) * getResources().getDisplayMetrics().density);
        binding.previewColorPickerPrimary.setBackground(gd);
    }

    private void updateSecondaryColor() {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Integer.parseInt(accentSecondary), Integer.parseInt(accentSecondary)});
        gd.setCornerRadius(getResources().getDimension(R.dimen.preview_color_picker_radius) * getResources().getDisplayMetrics().density);
        binding.previewColorPickerSecondary.setBackground(gd);
    }

    private void assignStockColorToPalette() {
        for (int i = 0; i < colorTableRows.length; i++) {
            for (int j = 0; j < colorTableRows[i].getChildCount(); j++) {
                GradientDrawable colorbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{systemColors[i][j], systemColors[i][j]});
                colorbg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
                colorTableRows[i].getChildAt(j).setBackground(colorbg);
            }
        }
    }

    private void assignCustomColorToPalette() {
        List<List<Object>> palette = generateColorPalette(MonetEngine.this, selectedStyle, Integer.parseInt(accentPrimary));
        List<List<Object>> palette_night = cloneList(palette);

        // Set accent saturation
        if (!Objects.equals(selectedStyle, getResources().getString(R.string.monet_monochrome))) {
            for (int i = 0; i < palette.size() - 2; i++) {
                for (int j = palette.get(i).size() - 2; j >= 1; j--) {
                    int color;

                    if (j == 1)
                        color = ColorUtil.setSaturation(Integer.parseInt(String.valueOf((int) palette.get(i).get(j + 1))), -0.1F);
                    else
                        color = ColorUtil.setSaturation(Integer.parseInt(String.valueOf((int) palette.get(i).get(j))), ((float) (monetAccentSaturation[0] - 100) / 1000.0F) * (Math.min((3.0F - j / 5F), 3.0F)));

                    palette.get(i).set(j, color);
                    palette_night.get(i).set(j, color);

                    if (!accurateShades) {
                        if (i == 0 && j == 8)
                            palette.get(i).set(j, Integer.parseInt(accentPrimary));

                        if (i == 0 && j == 5)
                            palette_night.get(i).set(j, Integer.parseInt(accentPrimary));
                    }
                }
            }
        }

        // Set background saturation
        if (!Objects.equals(selectedStyle, getResources().getString(R.string.monet_monochrome))) {
            for (int i = 3; i < palette.size(); i++) {
                for (int j = palette.get(i).size() - 2; j >= 1; j--) {
                    int color;
                    if (j == 1)
                        color = ColorUtil.setSaturation(Integer.parseInt(String.valueOf((int) palette.get(i).get(j + 1))), -0.1F);
                    else
                        color = ColorUtil.setSaturation(Integer.parseInt(String.valueOf((int) palette.get(i).get(j))), ((float) (monetBackgroundSaturation[0] - 100) / 1000.0F) * (Math.min((3.0F - j / 5F), 3.0F)));

                    palette.get(i).set(j, color);
                    palette_night.get(i).set(j, color);
                }
            }
        }

        // Set background lightness
        for (int i = Objects.equals(selectedStyle, getResources().getString(R.string.monet_monochrome)) ? 0 : 3; i < palette.size(); i++) {
            for (int j = 1; j < palette.get(i).size() - 1; j++) {
                int color = ColorUtil.setLightness(Integer.parseInt(String.valueOf((int) palette.get(i).get(j))), (float) (monetBackgroundLightness[0] - 100) / 1000.0F);

                palette.get(i).set(j, color);
                palette_night.get(i).set(j, color);
            }
        }

        for (int i = 0; i < colorTableRows.length; i++) {
            if (i == 2 && (Prefs.getBoolean(CUSTOM_SECONDARY_COLOR_SWITCH) || isSelectedSecondary) && !Objects.equals(selectedStyle, getResources().getString(R.string.monet_monochrome))) {
                Prefs.putBoolean(CUSTOM_SECONDARY_COLOR_SWITCH, true);
                List<List<Object>> secondaryPalette = generateColorPalette(MonetEngine.this, selectedStyle, Integer.parseInt(accentSecondary));

                for (int j = colorTableRows[i].getChildCount() - 1; j >= 0; j--) {
                    int color;

                    if (j == 0 || j == colorTableRows[i].getChildCount() - 1)
                        color = (int) secondaryPalette.get(0).get(j);
                    else if (j == 1)
                        color = ColorUtil.setSaturation(Integer.parseInt(String.valueOf((int) palette.get(i).get(j + 1))), -0.1F);
                    else
                        color = ColorUtil.setSaturation(Integer.parseInt(String.valueOf((int) secondaryPalette.get(0).get(j))), ((float) (monetAccentSaturation[0] - 100) / 1000.0F) * (Math.min((3.0F - j / 5F), 3.0F)));

                    palette.get(i).set(j, color);
                    palette_night.get(i).set(j, color);

                    if (!accurateShades) {
                        if (j == 8) {
                            palette.get(i).set(j, Integer.parseInt(accentSecondary));
                        }
                        if (j == 5) {
                            palette_night.get(i).set(j, Integer.parseInt(accentSecondary));
                        }
                    }

                    GradientDrawable colorbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{!isDarkMode ? (int) palette.get(i).get(j) : (int) palette_night.get(i).get(j), !isDarkMode ? (int) palette.get(i).get(j) : (int) palette_night.get(i).get(j)});
                    colorbg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
                    colorTableRows[i].getChildAt(j).setBackground(colorbg);
                }
            } else {
                for (int j = 0; j < colorTableRows[i].getChildCount(); j++) {
                    try {
                        GradientDrawable colorbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{!isDarkMode ? (int) palette.get(i).get(j) : (int) palette_night.get(i).get(j), !isDarkMode ? (int) palette.get(i).get(j) : (int) palette_night.get(i).get(j)});
                        colorbg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
                        colorTableRows[i].getChildAt(j).setBackground(colorbg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        finalPalette.clear();
        finalPalette.add(palette);
        finalPalette.add(palette_night);
    }

    private void disableBasicColors() {
        Prefs.clearPrefs(CUSTOM_ACCENT, CUSTOM_PRIMARY_COLOR_SWITCH, CUSTOM_SECONDARY_COLOR_SWITCH);

        FabricatedUtil.disableOverlays(COLOR_ACCENT_PRIMARY, COLOR_ACCENT_PRIMARY_LIGHT, COLOR_ACCENT_SECONDARY, COLOR_ACCENT_SECONDARY_LIGHT);
    }

    private List<List<Object>> cloneList(final List<List<Object>> src) {
        List<List<Object>> cloned = new ArrayList<>();
        for (List<Object> sublist : src) {
            cloned.add(new ArrayList<>(sublist));
        }
        return cloned;
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        switch (dialogId) {
            case 1:
                isSelectedPrimary = true;
                accentPrimary = String.valueOf(color);
                updatePrimaryColor();
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
                assignCustomColorToPalette();
                colorPickerDialogPrimary.setDialogStyle(R.style.ColorPicker).setColor(Integer.parseInt(accentPrimary)).setDialogType(ColorPickerDialog.TYPE_CUSTOM).setAllowCustom(false).setAllowPresets(true).setDialogId(1).setShowAlphaSlider(false).setShowColorShades(true);
                break;
            case 2:
                isSelectedSecondary = true;
                accentSecondary = String.valueOf(color);
                updateSecondaryColor();
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
                assignCustomColorToPalette();
                colorPickerDialogSecondary.setDialogStyle(R.style.ColorPicker).setColor(Integer.parseInt(accentSecondary)).setDialogType(ColorPickerDialog.TYPE_CUSTOM).setAllowCustom(false).setAllowPresets(true).setDialogId(2).setShowAlphaSlider(false).setShowColorShades(true);
                break;
            case 3:
                GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{color, color});
                gd.setCornerRadius(8 * getResources().getDisplayMetrics().density);
                colorTableRows[selectedChild[0]].getChildAt(selectedChild[1]).setBackground(gd);
                finalPalette.get(0).get(selectedChild[0]).set(selectedChild[1], color);
                finalPalette.get(1).get(selectedChild[0]).set(selectedChild[1], color);
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
                break;
        }
    }

    private final RadioGroup.OnCheckedChangeListener listener1 = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId != -1) {
                selectedStyle = ((RadioButton) findViewById(checkedId)).getText().toString();
                binding.monetStyles2.setOnCheckedChangeListener(null);
                binding.monetStyles2.clearCheck();
                binding.monetStyles2.setOnCheckedChangeListener(listener2);
                assignCustomColorToPalette();
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public void onDialogDismissed(int dialogId) {
    }

    private final RadioGroup.OnCheckedChangeListener listener2 = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId != -1) {
                selectedStyle = ((RadioButton) findViewById(checkedId)).getText().toString();
                binding.monetStyles1.setOnCheckedChangeListener(null);
                binding.monetStyles1.clearCheck();
                binding.monetStyles1.setOnCheckedChangeListener(listener1);
                assignCustomColorToPalette();
                binding.enableCustomMonet.setVisibility(View.VISIBLE);
            }
        }
    };


}
package arun.com.chromer;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.List;

import arun.com.chromer.chrometabutilites.MyCustomActivityHelper;
import arun.com.chromer.chrometabutilites.MyCustomTabHelper;
import arun.com.chromer.extra.Licenses;
import arun.com.chromer.fragments.PreferenceFragment;
import arun.com.chromer.intro.AppIntroMy;
import arun.com.chromer.util.ChangelogUtil;
import arun.com.chromer.util.StringConstants;
import arun.com.chromer.util.Util;
import de.psdev.licensesdialog.LicensesDialog;

public class MainActivity extends AppCompatActivity implements ColorChooserDialog.ColorCallback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String GOOGLE_URL = "http://www.google.com/";
    private static final String CUSTOM_TAB_URL = "https://developer.chrome.com/multidevice/android/customtabs#whentouse";
    private static final String CHROME_PACKAGE = "com.android.chrome";

    private MyCustomActivityHelper mCustomTabActivityHelper;

    private SharedPreferences mPreferences;

    private View mColorView;

    @Override
    protected void onStart() {
        super.onStart();
        mCustomTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCustomTabActivityHelper.unbindCustomTabsService(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        if (isFirstRun()) {
            startActivity(new Intent(this, AppIntroMy.class));
        }

        if (ChangelogUtil.shouldShowChangelog(this)) {
            ChangelogUtil.showChangelogDialog(this);
        }

        setupDrawer(toolbar);

        setupFAB();

        setupCustomTab();

        setupColorPicker();

        findViewById(R.id.set_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDefaultBehaviour();
            }
        });

        setupDefaultProvider();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preference_fragment, new PreferenceFragment())
                .commit();

        checkAndEducateUser();
    }

    private boolean isFirstRun() {
        if (mPreferences.getBoolean("firstrun", true)) {
            mPreferences.edit().putBoolean("firstrun", false).apply();
            return true;
        }
        return false;
    }

    private void setupDefaultProvider() {
        findViewById(R.id.default_provider).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] packagesArray = new String[0];
                final List<String> suppPackages = MyCustomTabHelper.
                        getCustomTabSupportingPackages(getApplicationContext());
                if (suppPackages != null) {
                    packagesArray = Util.getAppNameFromPackages(getApplicationContext(), suppPackages);
                }
                int choice = -1;
                String pack = mPreferences.getString("preferred_package", null);
                if (suppPackages != null && Util.isPackageInstalled(getApplicationContext(),
                        pack)) {
                    choice = suppPackages.indexOf(pack);
                }
                new MaterialDialog.Builder(MainActivity.this)
                        .title(getString(R.string.choose_default_provider))
                        .items(packagesArray)
                        .itemsCallbackSingleChoice(choice,
                                new MaterialDialog.ListCallbackSingleChoice() {
                                    @Override
                                    public boolean onSelection(MaterialDialog dialog, View itemView,
                                                               int which, CharSequence text) {
                                        if (suppPackages != null) {
                                            mPreferences.edit().putString("preferred_package",
                                                    suppPackages.get(which)).apply();
                                        }
                                        return true;
                                    }
                                })
                        .show();
            }
        });
    }

    private void setupFAB() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCustomTab(GOOGLE_URL);
            }
        });
    }

    private void setupColorPicker() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int choosenColor = sharedPreferences.getInt("toolbar_color",
                ContextCompat.getColor(this, R.color.primary));
        findViewById(R.id.color_picker_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ColorChooserDialog.Builder(MainActivity.this, R.string.md_choose_label)
                        .titleSub(R.string.md_presets_label)
                        .doneButton(R.string.md_done_label)  // changes label of the done button
                        .cancelButton(R.string.md_cancel_label)  // changes label of the cancel button
                        .backButton(R.string.md_back_label)  // changes label of the back button
                        .allowUserColorInputAlpha(false)
                        .preselect(choosenColor)
                        .dynamicButtonColor(false)
                        .show();
            }
        });
        mColorView = findViewById(R.id.color_preview);
        mColorView.setBackgroundColor(choosenColor);
    }

    @SuppressWarnings("SameParameterValue")
    private void handleDefaultBehaviour() {
        Uri googleURI = Uri.parse(GOOGLE_URL);
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, googleURI);
        if (!isDefaultSet(activityIntent)) {
            if (activityIntent.resolveActivity(getPackageManager()) != null) {
                PackageManager p = getPackageManager();
                ComponentName cN = new ComponentName(this, DummyActivity.class);
                p.setComponentEnabledSetting(cN,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);

                startActivity(activityIntent);

                p.setComponentEnabledSetting(cN,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        } else {
            // TODO replace view something more reliable
            Snackbar.make(mColorView, "Already set!", Snackbar.LENGTH_SHORT).show();
        }
    }

    private boolean isDefaultSet(Intent web) {
        ResolveInfo defaultViewHandlerInfo = getPackageManager().resolveActivity(web, 0);
        String defaultViewHandlerPackageName = null;
        if (defaultViewHandlerInfo != null) {
            defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName;
        }
        if (defaultViewHandlerPackageName != null) {
            if (defaultViewHandlerPackageName.trim().equalsIgnoreCase(getPackageName())) {
                Log.d(TAG, "Chromer defaulted");
                return true;
            }
        }
        return false;
    }

    private void setupDrawer(Toolbar toolbar) {
        Drawer drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(new AccountHeaderBuilder()
                        .withActivity(this)
                        .withHeaderBackground(R.drawable.chromer)
                        .withHeaderBackgroundScaleType(ImageView.ScaleType.CENTER_CROP)
                        .withDividerBelowHeader(true)
                        .build())
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(getString(R.string.intro)).withIdentifier(4)
                                .withIcon(GoogleMaterial.Icon.gmd_assignment)
                                .withSelectable(false),
                        new PrimaryDrawerItem().withName(getString(R.string.feedback)).withIdentifier(2)
                                .withIcon(GoogleMaterial.Icon.gmd_feedback)
                                .withSelectable(false),
                        new PrimaryDrawerItem().withName(getString(R.string.rate_play_store)).withIdentifier(3)
                                .withIcon(GoogleMaterial.Icon.gmd_rate_review)
                                .withSelectable(false),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(getString(R.string.more_custom_tbs))
                                .withIcon(GoogleMaterial.Icon.gmd_open_in_new)
                                .withIdentifier(5)
                                .withSelectable(false),
                        new SecondaryDrawerItem().withName(getString(R.string.share))
                                .withIcon(GoogleMaterial.Icon.gmd_share)
                                .withDescription(getString(R.string.help_chromer_grow))
                                .withIdentifier(7)
                                .withSelectable(false),
                        new SecondaryDrawerItem().withName(getString(R.string.licenses))
                                .withIcon(GoogleMaterial.Icon.gmd_card_membership)
                                .withIdentifier(6)
                                .withSelectable(false),
                        new SecondaryDrawerItem().withName(getString(R.string.about))
                                .withIcon(GoogleMaterial.Icon.gmd_info_outline)
                                .withIdentifier(8)
                                .withSelectable(false)
                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem == null)
                            return false;
                        int i = drawerItem.getIdentifier();
                        switch (i) {
                            case 2:
                                Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                                        Uri.fromParts("mailto", StringConstants.MAILID, null));
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                                startActivity(Intent.createChooser(emailIntent,
                                        getString(R.string.send_email)));
                                break;
                            case 3:
                                Util.openPlayStore(MainActivity.this, getPackageName());
                                break;
                            case 4:
                                startActivity(new Intent(MainActivity.this, AppIntroMy.class));
                                break;
                            case 5:
                                launchCustomTab(CUSTOM_TAB_URL);
                                break;
                            case 6:
                                new LicensesDialog.Builder(MainActivity.this)
                                        .setNotices(Licenses.getNotices())
                                        .setTitle(R.string.licenses)
                                        .build()
                                        .showAppCompat();
                                break;
                            case 7:
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                                shareIntent.setType("text/plain");
                                startActivity(Intent.createChooser(shareIntent,
                                        getString(R.string.share_via)));
                                break;
                            case 8:
                                Intent aboutActivityIntent = new Intent(MainActivity.this,
                                        AboutAppActivity.class);
                                startActivity(aboutActivityIntent,
                                        ActivityOptions.makeCustomAnimation(MainActivity.this,
                                                R.anim.slide_in_right,
                                                R.anim.slide_out_left).toBundle()
                                );
                                break;
                        }
                        return false;
                    }
                })
                .build();
        drawer.setSelection(-1);
    }

    private void launchCustomTab(String url) {
        CustomTabsIntent mCustomTabsIntent = Util.getCustomizedTabIntent(getApplicationContext(), url);
        MyCustomActivityHelper.openCustomTab(this, mCustomTabsIntent, Uri.parse(url),
                TabActivity.mCustomTabsFallback);
    }

    private void setupCustomTab() {
        mCustomTabActivityHelper = new MyCustomActivityHelper();
        mCustomTabActivityHelper.setConnectionCallback(
                new MyCustomActivityHelper.ConnectionCallback() {
                    @Override
                    public void onCustomTabsConnected() {
                        Log.d(TAG, "Connect to custom tab");
                        try {
                            Log.d(TAG, "Gave may launch command");
                            mCustomTabActivityHelper.mayLaunchUrl(
                                    Uri.parse(GOOGLE_URL)
                                    , null, null);
                        } catch (Exception e) {
                            // Don't care. Yes.. You heard me.
                        }
                    }

                    @Override
                    public void onCustomTabsDisconnected() {
                        Log.d(TAG, "Disconnect to custom tab");
                    }
                });
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mColorView.setBackgroundColor(selectedColor);
        sharedPreferences.edit().putInt("toolbar_color", selectedColor).apply();
    }

    private void checkAndEducateUser() {
        // TODO add automatic detection
        List packages = MyCustomTabHelper.getCustomTabSupportingPackages(this);
        if (packages.size() == 0) {
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.custom_tab_provider_not_found))
                    .content(getString(R.string.custom_tab_provider_not_found_expln))
                    .positiveText(getString(R.string.install))
                    .negativeText(getString(android.R.string.no))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Util.openPlayStore(MainActivity.this, CHROME_PACKAGE);
                        }
                    }).show();
        }
    }

}

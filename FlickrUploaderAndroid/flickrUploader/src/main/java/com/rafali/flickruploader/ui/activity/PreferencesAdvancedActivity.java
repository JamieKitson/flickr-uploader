package com.rafali.flickruploader.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.view.MenuItem;
import android.widget.EditText;

import com.rafali.flickruploader.FlickrUploader;
import com.rafali.flickruploader.tool.Utils;
import com.rafali.flickruploader2.R;

import org.androidannotations.api.BackgroundExecutor;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public class PreferencesAdvancedActivity extends AbstractPreferenceActivity implements OnSharedPreferenceChangeListener {

    static final org.slf4j.Logger LOG = LoggerFactory.getLogger(PreferencesAdvancedActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        addPreferencesFromResource(R.xml.preferences_advanced);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        findPreference("upload_description").setOnPreferenceClickListener(new PremiumOnclick("upload_description"));
        findPreference("custom_tags").setOnPreferenceClickListener(new PremiumOnclick("custom_tags"));
        render();
    }

    class PremiumOnclick implements OnPreferenceClickListener {

        private String prefKey;

        public PremiumOnclick(String prefKey) {
            this.prefKey = prefKey;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder alert = new AlertDialog.Builder(PreferencesAdvancedActivity.this);
            alert.setTitle(findPreference(prefKey).getTitle());
            // Set an EditText view to get user input
            final EditText input = new EditText(PreferencesAdvancedActivity.this);
            if (prefKey.equals("upload_description")) {
                input.setText(Utils.getUploadDescription());
            } else {
                input.setText(Utils.getStringProperty(prefKey));
            }
            alert.setView(input);

            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    LOG.debug("value : " + value);
                    Utils.setStringProperty(prefKey, value);
                    render();
                }
            });

            alert.setNegativeButton("Cancel", null);

            alert.show();
            return false;
        }
    }

    private void render() {
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final long size = FlickrUploader.getLogSize();
                final List<String> autoupload_delay_values = Arrays.asList(getResources().getStringArray(R.array.autoupload_delay_values));
                final String[] autoupload_delay_entries = getResources().getStringArray(R.array.autoupload_delay_entries);
                final String autoupload_delay_value = Utils.getStringProperty("autoupload_delay", autoupload_delay_values.get(0));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String autoupload_delay = autoupload_delay_entries[autoupload_delay_values.indexOf(autoupload_delay_value)];
                        if (autoupload_delay.equalsIgnoreCase("custom")) {
                            findPreference("autoupload_delay").setSummary(autoupload_delay);
                        } else {
                            findPreference("autoupload_delay").setSummary(autoupload_delay);
                        }
                        findPreference("upload_description").setSummary(Html.fromHtml(Utils.getUploadDescription()));
                        findPreference("custom_tags").setSummary(Utils.getStringProperty("custom_tags"));
                    }
                });
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if ("autoupload_delay".equals(key) && sp.getString(key, "").equalsIgnoreCase("custom")) {
            LOG.debug("custom");
        }
        render();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }
}

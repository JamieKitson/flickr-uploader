package com.rafali.flickruploader.ui.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.rafali.common.STR;
import com.rafali.common.ToolString;
import com.rafali.flickruploader.api.FlickrApi;
import com.rafali.flickruploader.enums.VIEW_SIZE;
import com.rafali.flickruploader.model.FlickrSet;
import com.rafali.flickruploader.model.Folder;
import com.rafali.flickruploader.model.Media;
import com.rafali.flickruploader.tool.Utils;
import com.rafali.flickruploader2.R;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import uk.co.senab.bitmapcache.CacheableImageView;

@EActivity(R.layout.auto_upload_folders_activity)
public class AutoUploadFoldersActivity extends AppCompatActivity implements OnItemClickListener {

    private static final String DEFAULT_SET = "Default set (" + STR.instantUpload + ")";
    private static final String PREVIOUS_SET = "Existing set: ";
    private static final String EXISTING_SET = "Existing set…";
    private static final String NEW_SET = "New set…";
    private static final String DISABLE_AUTO_UPLOAD = "Disable auto-upload";
    private static final Logger LOG = LoggerFactory.getLogger(AutoUploadFoldersActivity.class);

    AutoUploadFoldersActivity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        load();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @ViewById(R.id.list_view)
    ListView listView;

    @ViewById(R.id.loading)
    TextView loading;

    private Map<String, FlickrSet> cachedPhotoSets;

    @Background
    void load() {
        folders = new ArrayList<Folder>(Utils.getFolders(true).values());
        Collections.sort(folders, new Comparator<Folder>() {
            @Override
            public int compare(Folder lhs, Folder rhs) {
                return rhs.getSize() - lhs.getSize();
            }
        });
        render();
        cachedPhotoSets = FlickrApi.getPhotoSets(true);
        refresh();
    }

    @UiThread
    void refresh() {
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            renderImageView(view);
        }
    }

    @UiThread
    void render() {
        if (loading != null) {
            ((ViewGroup) loading.getParent()).removeView(loading);
            loading = null;
        }
        if (folders != null && listView != null) {
            listView.setAdapter(new PhotoAdapter());
            listView.setOnItemClickListener(this);
        }
    }

    List<Folder> folders;

    class PhotoAdapter extends BaseAdapter {

        public PhotoAdapter() {
        }

        @Override
        public int getCount() {
            return folders.size();
        }

        @Override
        public Object getItem(int position) {
            return folders.get(position);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = getItem(position);
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.folder_list_thumb, parent, false);
                convertView.setTag(R.id.image_view, convertView.findViewById(R.id.image_view));
                convertView.setTag(R.id.title, convertView.findViewById(R.id.title));
                convertView.setTag(R.id.size, convertView.findViewById(R.id.size));
                convertView.setTag(R.id.sub_title, convertView.findViewById(R.id.sub_title));
            }
            if (convertView.getTag() != item) {
                convertView.setTag(item);
                renderImageView(convertView);
            }
            return convertView;
        }
    }

    String previousSet;

    @Override
    public void onItemClick(final AdapterView<?> arg0, final View convertView, final int arg2, final long arg3) {
        if (convertView.getTag() instanceof Folder) {
            final Folder folder = (Folder) convertView.getTag();
            String title = "Auto-upload " + folder.getName() + " to";
            ArrayList<String> itemsList = Lists.newArrayList(DEFAULT_SET, NEW_SET, EXISTING_SET);
            if (previousSet != null) {
                itemsList.add(PREVIOUS_SET + previousSet);
            }
            if (folder.isAutoUploaded()) {
                itemsList.add(DISABLE_AUTO_UPLOAD);
            }
            final String[] items = itemsList.toArray(new String[0]);
            new AlertDialog.Builder(this).setTitle(title).setPositiveButton(null, null).setNegativeButton(null, null).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String item = items[which];
                    LOG.debug(item);
                    if (which <= 2) {
                        if (item.equals(DEFAULT_SET)) {
                            setAutoUploaded(folder, STR.instantUpload);
                        } else if (item.equals(NEW_SET)) {
                            FlickrUploaderActivity.showNewSetDialog(activity, folder.getName(), new Utils.Callback<String>() {
                                @Override
                                public void onResult(String result) {
                                    LOG.debug(result);
                                    if (ToolString.isNotBlank(result)) {
                                        setAutoUploaded(folder, result);
                                    }
                                }
                            });
                        } else if (item.equals(EXISTING_SET)) {
                            Utils.showExistingSetDialog(activity, new Utils.Callback<String>() {
                                @Override
                                public void onResult(String result) {
                                    previousSet = result;
                                    setAutoUploaded(folder, result);
                                }
                            }, cachedPhotoSets);
                        } else {
                            LOG.error("unknown item:" + item);
                        }
                    } else {
                        if (item.equals(DISABLE_AUTO_UPLOAD)) {
                            setAutoUploaded(folder, null);
                        } else if (previousSet != null && item.contains(PREVIOUS_SET) && item.contains(previousSet)) {
                            setAutoUploaded(folder, previousSet);
                        } else {
                            LOG.error("unknown item:" + item);
                        }
                    }
                }

            }).show();
        }
    }

    @Background
    void setAutoUploaded(Folder folder, String setTitle) {
        folder.setFlickrSetTitle(setTitle);
        folder.save();
        refresh();
    }

    ProgressDialog progressDialog;

    @UiThread
    void showLoading(String title, String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    @UiThread
    void hideLoading() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    private void renderImageView(final View convertView) {
        if (convertView.getTag() instanceof Folder) {
            final Folder folder = (Folder) convertView.getTag();
            final CacheableImageView imageView = (CacheableImageView) convertView.getTag(R.id.image_view);
            ((TextView) convertView.getTag(R.id.title)).setText(folder.getName());
            ((TextView) convertView.getTag(R.id.size)).setText("(" + folder.getSize() + ")");
            String summary;
            TextView subTitle = (TextView) convertView.getTag(R.id.sub_title);
            String photoSetTitle = folder.getFlickrSetTitle();
            if (ToolString.isBlank(photoSetTitle)) {
                summary = "not monitored";
                subTitle.setTextColor(getResources().getColor(R.color.midgray));
            } else {
                summary = "⇒ Flickr (" + photoSetTitle + ")";
                subTitle.setTextColor(Color.WHITE);
            }
            subTitle.setText(summary);

            final Media media = folder.getMedia();
            imageView.setTag(media);
            if (media == null) {
                imageView.setImageDrawable(null);
            } else {
                final CacheableBitmapDrawable wrapper = Utils.getCache().getFromMemoryCache(media.getPath() + "_" + VIEW_SIZE.small);
                if (wrapper != null && !wrapper.getBitmap().isRecycled()) {
                    // The cache has it, so just display it
                    imageView.setImageDrawable(wrapper);
                } else {
                    imageView.setImageDrawable(null);
                }

                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (imageView.getTag() == media) {
                            final CacheableBitmapDrawable bitmapDrawable;
                            if (wrapper != null && !wrapper.getBitmap().isRecycled()) {
                                bitmapDrawable = wrapper;
                            } else {
                                Bitmap bitmap = Utils.getBitmap(media, VIEW_SIZE.small);
                                if (bitmap != null) {
                                    bitmapDrawable = Utils.getCache().put(media.getPath() + "_" + VIEW_SIZE.small, bitmap);
                                } else {
                                    bitmapDrawable = null;
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (imageView.getTag() == media) {
                                        if (wrapper != bitmapDrawable) {
                                            imageView.setImageDrawable(bitmapDrawable);
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }
    }
}

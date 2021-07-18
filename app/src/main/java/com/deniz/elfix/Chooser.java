package com.deniz.elfix;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Chooser extends ListActivity {
    AppAdapter adapter = null;

    private ArrayList<ResolveInfo> selected;
    private boolean multiple;
    private Button btn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chooser_layout);

        multiple = getIntent().getBooleanExtra("multiple", false);

        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);

        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchables = pm.queryIntentActivities(main, 0);
        Collections.sort(launchables,
                new ResolveInfo.DisplayNameComparator(pm));

        adapter = new AppAdapter(pm, launchables);
        setListAdapter(adapter);
        final ListView listView = getListView();
        btn = findViewById(R.id.submit);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        if (!multiple) {
            btn.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "Select more than 1 app", Toast.LENGTH_SHORT).show();
        }

        selected = new ArrayList<>();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
                if (checkedItems != null) {
                    for (int i = 0; i < checkedItems.size(); i++) {
                        if (checkedItems.valueAt(i)) {
                            selected.add((ResolveInfo) listView.getAdapter().getItem(
                                    checkedItems.keyAt(i)));
                        }
                    }
                    AppLock.setAvailablePackageNames(selected);
                    setResult(1, null);
                    finish();
                }
            }
        });
    }

    private String extractPackName(ResolveInfo ri) {
        ActivityInfo activity = ri.activityInfo;
        ComponentName name = new ComponentName(activity.applicationInfo.packageName,
                activity.name);

        return name.getPackageName();
    }

    @Override
    protected void onListItemClick(ListView l, View v,
                                   int position, long id) {
        ResolveInfo launchable = adapter.getItem(position);
        if (!multiple) {
            Intent intentMessage = new Intent();
            if (launchable != null) {
                intentMessage.putExtra("package_name", extractPackName(launchable));
            }
            setResult(1, intentMessage);
            finish();
        } else {
            if (selected.contains(launchable)) {
                selected.remove(launchable);
                v.setBackgroundColor(Color.WHITE);
            } else {
                selected.add(launchable);
                v.setBackgroundColor(Color.CYAN);
            }
        }
        btn.setEnabled(selected.size() > 1);
    }

    class AppAdapter extends ArrayAdapter<ResolveInfo> {
        private PackageManager pm;

        AppAdapter(PackageManager pm, List<ResolveInfo> apps) {
            super(Chooser.this, R.layout.row, apps);
            this.pm = pm;
        }

        @Override
        public View getView(int position, View convertView,
                            ViewGroup parent) {
            if (convertView == null) {
                convertView = newView(parent);
            }

            bindView(position, convertView);

            return (convertView);
        }

        private View newView(ViewGroup parent) {
            return (getLayoutInflater().inflate(R.layout.row, parent, false));
        }

        private void bindView(int position, View row) {
            TextView label = row.findViewById(R.id.label);
            label.setText(getItem(position).loadLabel(pm));
            ImageView icon = row.findViewById(R.id.icon);
            icon.setImageDrawable(getItem(position).loadIcon(pm));

            if (selected.contains(getItem(position))) {
                row.setBackgroundColor(Color.CYAN);
            } else {
                row.setBackgroundColor(Color.WHITE);
            }
        }
    }
}
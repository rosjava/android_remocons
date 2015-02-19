/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * Copyright (c) 2013, Yujin Robot.
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.rosjava.android_remocons.rocon_remocon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.master.MasterId;
import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;
import com.github.rosjava.android_remocons.common_tools.nfc.NfcReaderActivity;
import com.github.rosjava.android_remocons.common_tools.zeroconf.MasterSearcher;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.DiscoveredService;
import com.google.zxing.IntentIntegrator;
import com.google.zxing.IntentResult;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A rewrite of ye olde RobotMasterChooser to work with rocon masters (i.e.
 * those that have rocon master info and an interactions manager present).
 */
public class MasterChooser extends Activity {

	private static final int ADD_URI_DIALOG_ID = 0;
	private static final int ADD_DELETION_DIALOG_ID = 1;
	private static final int ADD_SEARCH_CONCERT_DIALOG_ID = 2;

    private static final int QR_CODE_SCAN_REQUEST_CODE = 101;
    private static final int NFC_TAG_SCAN_REQUEST_CODE = 102;

	private List<RoconDescription> masters;
	private boolean[] selections;
	private MasterSearcher masterSearcher;
	private ListView listView;
    private Yaml yaml = new Yaml();

    public MasterChooser() {
		masters = new ArrayList<RoconDescription>();
	}

	private void readMasterList() {
		String str = null;
		Cursor c = getContentResolver().query(
				Database.CONTENT_URI, null, null, null, null);
		if (c == null) {
			masters = new ArrayList<RoconDescription>();
			Log.e("Remocon", "master chooser provider failed!!!");
			return;
		}
		if (c.getCount() > 0) {
			c.moveToFirst();
			str = c.getString(c.getColumnIndex(Database.TABLE_COLUMN));
			Log.i("Remocon", "master chooser found a rocon master: " + str);
		}
		if (str != null) {
			masters = (List<RoconDescription>) yaml.load(str);
		} else {
			masters = new ArrayList<RoconDescription>();
		}
	}

	public void writeMasterList() {
		Log.i("Remocon", "master chooser saving rocon master details...");
		String str = null;
		final List<RoconDescription> tmp = masters; // Avoid race conditions
        if (tmp != null) {
            str = yaml.dump(tmp);
		}
		ContentValues cv = new ContentValues();
		cv.put(Database.TABLE_COLUMN, str);
		Uri newEmp = getContentResolver().insert(Database.CONTENT_URI, cv);
		if (newEmp != Database.CONTENT_URI) {
			Log.e("Remocon", "master chooser could not save concert, non-equal URI's");
		}
	}

	private void refresh() {
		readMasterList();
		updateListView();
	}

	private void updateListView() {
		setContentView(R.layout.master_chooser);
		ListView listview = (ListView) findViewById(R.id.master_list);
		listview.setAdapter(new MasterAdapter(this, masters));
		registerForContextMenu(listview);

		listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				choose(position);
			}
		});
	}

    /**
     * Called when the user clicks on one of the listed masters in master chooser
     * view. Should probably check the connection status before
     * proceeding here, but perhaps we can just rely on the user clicking
     * refresh so this process stays without any lag delay.
     *
     * @param position
     */
	private void choose(int position) {
		RoconDescription concert = masters.get(position);
		if (concert == null || concert.getConnectionStatus() == null
				|| concert.getConnectionStatus().equals(RoconDescription.ERROR)) {
			AlertDialog d = new AlertDialog.Builder(MasterChooser.this)
					.setTitle("Error!")
					.setCancelable(false)
					.setMessage("Failed: Cannot contact concert")
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).create();
			d.show();
        } else if ( concert.getConnectionStatus().equals(RoconDescription.UNAVAILABLE) ) {
            AlertDialog d = new AlertDialog.Builder(MasterChooser.this)
                    .setTitle("Master Unavailable!")
                    .setCancelable(false)
                    .setMessage("Currently busy serving another.")
                    .setNeutralButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                }
                            }).create();
            d.show();
        } else {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RoconDescription.UNIQUE_KEY, concert);
            setResult(RESULT_OK, resultIntent);
            finish();
		}
	}

	private void addMaster(MasterId masterId) {
		addMaster(masterId, false);
	}

	private void addMaster(MasterId masterId, boolean connectToDuplicates) {
		Log.i("MasterChooserActivity", "adding master to the concert master chooser [" + masterId.toString() + "]");
		if (masterId == null || masterId.getMasterUri() == null) {
		} else {
			for (int i = 0; i < masters.toArray().length; i++) {
				RoconDescription concert = masters.get(i);
				if (concert.getMasterId().equals(masterId)) {
					if (connectToDuplicates) {
						choose(i);
						return;
					} else {
						Toast.makeText(this, "That concert is already listed.",
								Toast.LENGTH_SHORT).show();
						return;
					}
				}
			}
			Log.i("MasterChooserActivity", "creating concert description: "
					+ masterId.toString());
			masters.add(RoconDescription.createUnknown(masterId));
			Log.i("MasterChooserActivity", "description created");
			onMastersChanged();
		}
	}

	private void onMastersChanged() {
		writeMasterList();
		updateListView();
	}

	private void deleteAllMasters() {
		masters.clear();
		onMastersChanged();
	}

	private void deleteSelectedMasters(boolean[] array) {
		int j = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i]) {
				masters.remove(j);
			} else {
				j++;
			}
		}
		onMastersChanged();
	}

	private void deleteUnresponsiveMasters() {
		Iterator<RoconDescription> iter = masters.iterator();
		while (iter.hasNext()) {
			RoconDescription concert = iter.next();
			if (concert == null || concert.getConnectionStatus() == null
					|| concert.getConnectionStatus().equals(RoconDescription.ERROR)) {
				Log.i("Remocon", "concert master chooser removing concert with connection status '"
						+ concert.getConnectionStatus() + "'");
				iter.remove();
			}
		}
		onMastersChanged();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readMasterList();
		updateListView();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Sub-activity to gather concert connection data completed: can be QR code or NFC tag scan
        // TODO: cannot unify both calls?

        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = null;
        if (requestCode == QR_CODE_SCAN_REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(
                    requestCode, resultCode, intent);
            if (scanResult != null && scanResult.getContents() != null) {
                Yaml yaml = new Yaml();
                String scanned_data = scanResult.getContents().toString();
                data = (Map<String, Object>) yaml.load(scanned_data);
            }
        }
        else if (requestCode == NFC_TAG_SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            if (intent.hasExtra("tag_data")) {
                data = (Map<String, Object>) intent.getExtras().getSerializable("tag_data");
            }
        }
        else {
            Log.w("Remocon", "Unknown activity request code: " + requestCode);
            return;
        }

        if (data == null) {
            Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                Log.d("Remocon", "master chooser OBJECT: " + data.toString());
                addMaster(new MasterId(data), false);
            } catch (Exception e) {
                Toast.makeText(this, "invalid rocon master description: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		readMasterList();
		final Dialog dialog;
		Button button;
		AlertDialog.Builder builder;
		switch (id) {
		case ADD_URI_DIALOG_ID:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.add_uri_dialog);
			dialog.setTitle("Add a Master");
			dialog.setOnKeyListener(new DialogKeyListener());
			EditText uriField = (EditText) dialog.findViewById(R.id.uri_editor);
			uriField.setText("http://localhost:11311/",
					TextView.BufferType.EDITABLE);
			button = (Button) dialog.findViewById(R.id.enter_button);
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					enterMasterInfo(dialog);
					removeDialog(ADD_URI_DIALOG_ID);
				}
			});
            button = (Button) dialog.findViewById(R.id.qr_code_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanQRCodeClicked(v);
                }
            });
            button = (Button) dialog.findViewById(R.id.nfc_tag_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanNFCTagClicked(v);
                }
            });
			button = (Button) dialog.findViewById(R.id.search_master_button);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					searchMasterClicked(v);
				}
			});

			button = (Button) dialog.findViewById(R.id.cancel_button);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					removeDialog(ADD_URI_DIALOG_ID);
				}
			});
			break;
		case ADD_DELETION_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			String newline = System.getProperty("line.separator");
			if (masters.size() > 0) {
				selections = new boolean[masters.size()];
				Spannable[] concert_names = new Spannable[masters.size()];
				Spannable name;
				for (int i = 0; i < masters.size(); i++) {
					name = Factory.getInstance().newSpannable(
							masters.get(i).getMasterName() + newline + masters.get(i).getMasterId());
					name.setSpan(new ForegroundColorSpan(0xff888888),
                            masters.get(i).getMasterName().length(), name.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					name.setSpan(new RelativeSizeSpan(0.8f),
                            masters.get(i).getMasterName().length(), name.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					concert_names[i] = name;
				}
				builder.setTitle("Delete a concert");
				builder.setMultiChoiceItems(concert_names, selections,
						new DialogSelectionClickHandler());
				builder.setPositiveButton("Delete Selections",
						new DeletionDialogButtonClickHandler());
				builder.setNegativeButton("Cancel",
						new DeletionDialogButtonClickHandler());
				dialog = builder.create();
			} else {
				builder.setTitle("No masters to delete.");
				dialog = builder.create();
				final Timer t = new Timer();
				t.schedule(new TimerTask() {
					public void run() {
						removeDialog(ADD_DELETION_DIALOG_ID);
					}
				}, 2 * 1000);
			}
			break;
		case ADD_SEARCH_CONCERT_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setTitle("Scanning on the local network...");
			LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			listView = (ListView) layoutInflater.inflate(R.layout.zeroconf_master_list, null);
			masterSearcher = new MasterSearcher(this, listView, "concert-master", R.drawable.conductor, R.drawable.turtle);
			builder.setView(listView);
			builder.setPositiveButton("Select", new SearchMasterDialogButtonClickHandler());
			builder.setNegativeButton("Cancel", new SearchMasterDialogButtonClickHandler());
			dialog = builder.create();
			dialog.setOnKeyListener(new DialogKeyListener());
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	public class DialogSelectionClickHandler implements
			DialogInterface.OnMultiChoiceClickListener {
		public void onClick(DialogInterface dialog, int clicked,
				boolean selected) {
			return;
		}
	}

	public class DeletionDialogButtonClickHandler implements
			DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int clicked) {
			switch (clicked) {
			case DialogInterface.BUTTON_POSITIVE:
				deleteSelectedMasters(selections);
				removeDialog(ADD_DELETION_DIALOG_ID);
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				removeDialog(ADD_DELETION_DIALOG_ID);
				break;
			}
		}
	}

	public class SearchMasterDialogButtonClickHandler implements
			DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int clicked) {
			switch (clicked) {
			case DialogInterface.BUTTON_POSITIVE:
				SparseBooleanArray positions = listView
						.getCheckedItemPositions();

				for (int i = 0; i < positions.size(); i++) {
					if (positions.valueAt(i)) {
						enterMasterInfo((DiscoveredService) listView.getAdapter()
                                .getItem(positions.keyAt(i)));
					}
				}
				removeDialog(ADD_DELETION_DIALOG_ID);
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				removeDialog(ADD_DELETION_DIALOG_ID);
				break;
			}
		}
	}

	public void enterMasterInfo(DiscoveredService discovered_service) {
        /*
          This could be better - it should actually contact and check off each
          resolvable zeroconf address looking for the master. Instead, we just grab
          the first ipv4 address and totally ignore the possibility of an ipv6 master.
         */
        String newMasterUri = null;
        if ( discovered_service.ipv4_addresses.size() != 0 ) {
            newMasterUri = "http://" + discovered_service.ipv4_addresses.get(0) + ":"
                    + discovered_service.port + "/";
        }
        if (newMasterUri != null && newMasterUri.length() > 0) {
            android.util.Log.i("Remocon", newMasterUri);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("URL", newMasterUri);
            try {
                addMaster(new MasterId(data));
            } catch (Exception e) {
                Toast.makeText(MasterChooser.this, "Invalid Parameters.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MasterChooser.this, "No valid resolvable master URI.",
                    Toast.LENGTH_SHORT).show();
        }
	}

	public void enterMasterInfo(Dialog dialog) {
		EditText uriField = (EditText) dialog.findViewById(R.id.uri_editor);
		String newMasterUri = uriField.getText().toString();
		EditText wifiNameField = (EditText) dialog
				.findViewById(R.id.wifi_name_editor);
		String newWifiName = wifiNameField.getText().toString();
		EditText wifiPasswordField = (EditText) dialog
				.findViewById(R.id.wifi_password_editor);
		String newWifiPassword = wifiPasswordField.getText().toString();
		if (newMasterUri != null && newMasterUri.length() > 0) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("URL", newMasterUri);
			if (newWifiName != null && newWifiName.length() > 0) {
				data.put("WIFI", newWifiName);
			}
			if (newWifiPassword != null && newWifiPassword.length() > 0) {
				data.put("WIFIPW", newWifiPassword);
			}
			try {
				addMaster(new MasterId(data));
			} catch (Exception e) {
				Toast.makeText(MasterChooser.this, "Invalid Parameters.",
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(MasterChooser.this, "Must specify Master URI.",
					Toast.LENGTH_SHORT).show();
		}
	}

	public class DialogKeyListener implements DialogInterface.OnKeyListener {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN
					&& keyCode == KeyEvent.KEYCODE_ENTER) {
				Dialog dlg = (Dialog) dialog;
				enterMasterInfo(dlg);
				removeDialog(ADD_URI_DIALOG_ID);
				return true;
			}
			return false;
		}
	}

	public void addMasterClicked(View view) {
		showDialog(ADD_URI_DIALOG_ID);
	}

	public void refreshClicked(View view) {
		refresh();
	}

    public void scanQRCodeClicked(View view) {
        dismissDialog(ADD_URI_DIALOG_ID);
        IntentIntegrator.initiateScan(this, IntentIntegrator.DEFAULT_TITLE,
                IntentIntegrator.DEFAULT_MESSAGE, IntentIntegrator.DEFAULT_YES,
                IntentIntegrator.DEFAULT_NO, IntentIntegrator.QR_CODE_TYPES);
    }

    public void scanNFCTagClicked(View view) {
        dismissDialog(ADD_URI_DIALOG_ID);
        Intent i = new Intent(this, NfcReaderActivity.class);
        // Set the request code so we can identify the callback via this code
        startActivityForResult(i, NFC_TAG_SCAN_REQUEST_CODE);
    }

	public void searchMasterClicked(View view) {
		removeDialog(ADD_URI_DIALOG_ID);
		showDialog(ADD_SEARCH_CONCERT_DIALOG_ID);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.master_chooser_option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.add_concert) {
			showDialog(ADD_URI_DIALOG_ID);
			return true;
		} else if (id == R.id.delete_selected) {
			showDialog(ADD_DELETION_DIALOG_ID);
			return true;
		} else if (id == R.id.delete_unresponsive) {
			deleteUnresponsiveMasters();
			return true;
		} else if (id == R.id.delete_all) {
			deleteAllMasters();
			return true;
		} else if (id == R.id.kill) {
			Intent intent = new Intent();
			setResult(RESULT_CANCELED, intent);
			finish();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}

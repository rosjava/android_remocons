/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * Copyright (c) 2013, Yujin Concert.
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

package com.github.rosjava.android_remocons.concert_remocon;

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

import com.github.rosjava.android_apps.application_management.RobotDescription;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertsContentProvider;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertDescription;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertId;
import com.github.rosjava.android_remocons.concert_remocon.zeroconf.MasterSearcher;
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
 * @author hersh@willowgarage.com
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class ConcertChooser extends Activity {

	private static final int ADD_URI_DIALOG_ID = 0;
	private static final int ADD_DELETION_DIALOG_ID = 1;
	private static final int ADD_SEARCH_CONCERT_DIALOG_ID = 2;

    private static final int QR_CODE_SCAN_REQUEST_CODE = 101;
    private static final int NFC_TAG_SCAN_REQUEST_CODE = 102;

	private List<ConcertDescription> concerts;
	private boolean[] selections;
	private MasterSearcher masterSearcher;
	private ListView listView;

	public ConcertChooser() {
		concerts = new ArrayList<ConcertDescription>();
	}

	private void readConcertList() {
		String str = null;
		Cursor c = getContentResolver().query(
				ConcertsContentProvider.CONTENT_URI, null, null, null, null);
		if (c == null) {
			concerts = new ArrayList<ConcertDescription>();
			Log.e("ConcertRemocon", "concert master chooser provider failed!!!");
			return;
		}
		if (c.getCount() > 0) {
			c.moveToFirst();
			str = c.getString(c
					.getColumnIndex(ConcertsContentProvider.TABLE_COLUMN));
			Log.i("ConcertRemocon", "concert master chooser found a concert: " + str);
		}
		if (str != null) {
			Yaml yaml = new Yaml();
			concerts = (List<ConcertDescription>) yaml.load(str);
		} else {
			concerts = new ArrayList<ConcertDescription>();
		}
	}

	public void writeConcertList() {
		Log.i("ConcertRemocon", "concert master chooser saving concert...");
		Yaml yaml = new Yaml();
		String txt = null;
		final List<ConcertDescription> concert = concerts; // Avoid race conditions
		if (concert != null) {
			txt = yaml.dump(concert);
		}
		ContentValues cv = new ContentValues();
		cv.put(ConcertsContentProvider.TABLE_COLUMN, txt);
		Uri newEmp = getContentResolver().insert(
				ConcertsContentProvider.CONTENT_URI, cv);
		if (newEmp != ConcertsContentProvider.CONTENT_URI) {
			Log.e("ConcertRemocon", "concert master chooser could not save concert, non-equal URI's");
		}
	}

	private void refresh() {
		readConcertList();
		updateListView();
	}

	private void updateListView() {
		setContentView(R.layout.concert_master_chooser);
		ListView listview = (ListView) findViewById(R.id.master_list);
		listview.setAdapter(new MasterAdapter(this, concerts));
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
     * Called when the user clicks on one of the concerts in master chooser
     * view. Should probably check the connection status before
     * proceeding here, but perhaps we can just rely on the user clicking
     * refresh so this process stays without any lag delay.
     *
     * @param position
     */
	private void choose(int position) {
		ConcertDescription concert = concerts.get(position);
		if (concert == null || concert.getConnectionStatus() == null
				|| concert.getConnectionStatus().equals(concert.ERROR)) {
			AlertDialog d = new AlertDialog.Builder(ConcertChooser.this)
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
        } else if ( concert.getConnectionStatus().equals(concert.UNAVAILABLE) ) {
            AlertDialog d = new AlertDialog.Builder(ConcertChooser.this)
                    .setTitle("Concert Unavailable!")
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
            concertChosen(concert);
		}
	}

    private void concertChosen(final ConcertDescription concert) {
        Log.i("ConcertRemocon", "Concert chosen; show choose user role dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose your role");

        builder.setSingleChoiceItems(concert.getUserRoles(), 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selectedRole) {
                String role = concert.getUserRoles()[selectedRole];
                Toast.makeText(ConcertChooser.this, role + " selected", Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent
                        .putExtra(ConcertDescription.UNIQUE_KEY, concert)
                        .putExtra("UserRole", role);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

	private void addMaster(ConcertId concertId) {
		addMaster(concertId, false);
	}

	private void addMaster(ConcertId concertId, boolean connectToDuplicates) {
		Log.i("MasterChooserActivity", "adding master to the concert master chooser [" + concertId.toString() + "]");
		if (concertId == null || concertId.getMasterUri() == null) {
		} else {
			for (int i = 0; i < concerts.toArray().length; i++) {
				ConcertDescription concert = concerts.get(i);
				if (concert.getConcertId().equals(concertId)) {
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
					+ concertId.toString());
			concerts.add(ConcertDescription.createUnknown(concertId));
			Log.i("MasterChooserActivity", "description created");
			onConcertsChanged();
		}
	}

	private void onConcertsChanged() {
		writeConcertList();
		updateListView();
	}

	private void deleteAllConcerts() {
		concerts.clear();
		onConcertsChanged();
	}

	private void deleteSelectedConcerts(boolean[] array) {
		int j = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i]) {
				concerts.remove(j);
			} else {
				j++;
			}
		}
		onConcertsChanged();
	}

	private void deleteUnresponsiveConcerts() {
		Iterator<ConcertDescription> iter = concerts.iterator();
		while (iter.hasNext()) {
			ConcertDescription concert = iter.next();
			if (concert == null || concert.getConnectionStatus() == null
					|| concert.getConnectionStatus().equals(concert.ERROR)) {
				Log.i("ConcertRemocon", "concert master chooser removing concert with connection status '"
						+ concert.getConnectionStatus() + "'");
				iter.remove();
			}
		}
		onConcertsChanged();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readConcertList();
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

        String scanned_data = null;

        if (requestCode == QR_CODE_SCAN_REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(
                    requestCode, resultCode, intent);
            if (scanResult != null && scanResult.getContents() != null) {
                scanned_data = scanResult.getContents().toString();
            }
        }
        else if (requestCode == NFC_TAG_SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            if (intent.hasExtra("tag_data")) {
                scanned_data = intent.getExtras().getString("tag_data");
            }
        }
        else {
            Log.w("ConcertRemocon", "Unknown activity request code: " + requestCode);
            return;
        }

        if (scanned_data == null) {
            Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                Yaml yaml = new Yaml();
                Map<String, Object> data = (Map<String, Object>) yaml.load(scanned_data);
                Log.d("ConcertRemocon", "ConcertChooser OBJECT: " + data.toString());
                addMaster(new ConcertId(data), false);
            } catch (Exception e) {
                Toast.makeText(this,
                        "Invalid concert description: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		readConcertList();
		final Dialog dialog;
		Button button;
		AlertDialog.Builder builder;
		switch (id) {
		case ADD_URI_DIALOG_ID:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.add_uri_dialog);
			dialog.setTitle("Add a concert");
			dialog.setOnKeyListener(new DialogKeyListener());
			EditText uriField = (EditText) dialog.findViewById(R.id.uri_editor);
			EditText controlUriField = (EditText) dialog
					.findViewById(R.id.control_uri_editor);
			uriField.setText("http://localhost:11311/",
					TextView.BufferType.EDITABLE);
			// controlUriField.setText("http://prX1.willowgarage.com/cgi-bin/control.py",TextView.BufferType.EDITABLE
			// );
			button = (Button) dialog.findViewById(R.id.enter_button);
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					enterConcertInfo(dialog);
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
					searchConcertClicked(v);
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
			if (concerts.size() > 0) {
				selections = new boolean[concerts.size()];
				Spannable[] concert_names = new Spannable[concerts.size()];
				Spannable name;
				for (int i = 0; i < concerts.size(); i++) {
					name = Factory.getInstance().newSpannable(
							concerts.get(i).getConcertName() + newline
									+ concerts.get(i).getConcertId());
					name.setSpan(new ForegroundColorSpan(0xff888888), concerts
							.get(i).getConcertName().length(), name.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					name.setSpan(new RelativeSizeSpan(0.8f), concerts.get(i)
							.getConcertName().length(), name.length(),
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
				builder.setTitle("No concerts to delete.");
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
			listView = (ListView) layoutInflater.inflate(
					R.layout.zeroconf_master_list, null);
			masterSearcher = new MasterSearcher(this, listView);
			builder.setView(listView);
			builder.setPositiveButton("Select",
					new SearchConcertDialogButtonClickHandler());
			builder.setNegativeButton("Cancel",
					new SearchConcertDialogButtonClickHandler());
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
				deleteSelectedConcerts(selections);
				removeDialog(ADD_DELETION_DIALOG_ID);
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				removeDialog(ADD_DELETION_DIALOG_ID);
				break;
			}
		}
	}

	public class SearchConcertDialogButtonClickHandler implements
			DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int clicked) {
			switch (clicked) {
			case DialogInterface.BUTTON_POSITIVE:
				SparseBooleanArray positions = listView
						.getCheckedItemPositions();

				for (int i = 0; i < positions.size(); i++) {
					if (positions.valueAt(i)) {
						enterConcertInfo((DiscoveredService) listView.getAdapter()
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

	public void enterConcertInfo(DiscoveredService discovered_service) {
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
            android.util.Log.i("ConcertRemocon", newMasterUri);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("URL", newMasterUri);
            try {
                addMaster(new ConcertId(data));
            } catch (Exception e) {
                Toast.makeText(ConcertChooser.this, "Invalid Parameters.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(ConcertChooser.this, "No valid resolvable master URI.",
                    Toast.LENGTH_SHORT).show();
        }
	}

	public void enterConcertInfo(Dialog dialog) {
		EditText uriField = (EditText) dialog.findViewById(R.id.uri_editor);
		String newMasterUri = uriField.getText().toString();
		EditText controlUriField = (EditText) dialog
				.findViewById(R.id.control_uri_editor);
		String newControlUri = controlUriField.getText().toString();
		EditText wifiNameField = (EditText) dialog
				.findViewById(R.id.wifi_name_editor);
		String newWifiName = wifiNameField.getText().toString();
		EditText wifiPasswordField = (EditText) dialog
				.findViewById(R.id.wifi_password_editor);
		String newWifiPassword = wifiPasswordField.getText().toString();
		if (newMasterUri != null && newMasterUri.length() > 0) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("URL", newMasterUri);
			if (newControlUri != null && newControlUri.length() > 0) {
				data.put("CURL", newControlUri);
			}
			if (newWifiName != null && newWifiName.length() > 0) {
				data.put("WIFI", newWifiName);
			}
			if (newWifiPassword != null && newWifiPassword.length() > 0) {
				data.put("WIFIPW", newWifiPassword);
			}
			try {
				addMaster(new ConcertId(data));
			} catch (Exception e) {
				Toast.makeText(ConcertChooser.this, "Invalid Parameters.",
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(ConcertChooser.this, "Must specify Master URI.",
					Toast.LENGTH_SHORT).show();
		}
	}

	public class DialogKeyListener implements DialogInterface.OnKeyListener {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN
					&& keyCode == KeyEvent.KEYCODE_ENTER) {
				Dialog dlg = (Dialog) dialog;
				enterConcertInfo(dlg);
				removeDialog(ADD_URI_DIALOG_ID);
				return true;
			}
			return false;
		}
	}

	public void addConcertClicked(View view) {
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
        Intent i = new Intent(this,
                com.github.rosjava.android_remocons.concert_remocon.nfc.ForegroundDispatch.class);
        // Set the request code so we can identify the callback via this code
        startActivityForResult(i, NFC_TAG_SCAN_REQUEST_CODE);
    }

	public void searchConcertClicked(View view) {
		removeDialog(ADD_URI_DIALOG_ID);
		showDialog(ADD_SEARCH_CONCERT_DIALOG_ID);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.concert_master_chooser_option_menu, menu);
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
			deleteUnresponsiveConcerts();
			return true;
		} else if (id == R.id.delete_all) {
			deleteAllConcerts();
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

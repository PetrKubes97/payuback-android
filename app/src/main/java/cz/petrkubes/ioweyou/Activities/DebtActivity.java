package cz.petrkubes.ioweyou.Activities;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;

import org.parceler.Parcels;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.petrkubes.ioweyou.Adapters.FriendsSuggestionAdapter;
import cz.petrkubes.ioweyou.Database.DatabaseHandler;
import cz.petrkubes.ioweyou.Fragments.DebtsFragment;
import cz.petrkubes.ioweyou.Fragments.FriendsFragment;
import cz.petrkubes.ioweyou.Pojos.Currency;
import cz.petrkubes.ioweyou.Pojos.Debt;
import cz.petrkubes.ioweyou.Pojos.Friend;
import cz.petrkubes.ioweyou.Pojos.User;
import cz.petrkubes.ioweyou.R;
import cz.petrkubes.ioweyou.Tools.Const;
import cz.petrkubes.ioweyou.Tools.Tools;

/**
 * Activity for adding or editing a new debt
 * This activity is launched after clicking the floating plus button
 *
 * @author Petr Kubes
 */
public class DebtActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_created_at";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_created_at";
    private static final String defaultCurrencyPos = "DEFAULT_CURRENCY_POS";

    // Widgets
    private AutoCompleteTextView txtName;
    private TextInputEditText txtWhat;
    private Spinner spnCurrency;
    private RadioButton rdioMyDebt;
    private RadioButton rdioTheirDebt;
    private RadioButton rdioThing;
    private RadioButton rdioMoney;
    private EditText txtNote;
    private FloatingActionButton btnAddDebt;
    private DatabaseHandler db;
    private TextInputLayout txtILWhat;
    private TextInputLayout txtILWho;
    private Button btnCreatedAt;
    private Button btnDelete;
    private CheckBox chckLocked;
    private Integer tempFacebookFriendId = null;
    private ArrayList<Currency> currencies = null;
    private ArrayList<Friend> friends = null;
    private ArrayAdapter<Friend> friendsSuggestionAdapter = null;
    private SharedPreferences sharedPreferences;
    private User user;
    private Date createdAt;
    private Debt debtToEdit;
    private Calendar createdAtCal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt);

        // Setup actionbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.debt_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Setup all widgets
        txtName = (AutoCompleteTextView) findViewById(R.id.txt_who);
        txtWhat = (TextInputEditText) findViewById(R.id.txt_what);
        spnCurrency = (Spinner) findViewById(R.id.spn_currency);
        rdioMyDebt = (RadioButton) findViewById(R.id.rdio_my_debt);
        rdioTheirDebt = (RadioButton) findViewById(R.id.rdio_their_debt);
        rdioThing = (RadioButton) findViewById(R.id.rdio_thing);
        rdioMoney = (RadioButton) findViewById(R.id.rdio_money);
        txtNote = (EditText) findViewById(R.id.txt_note);
        btnAddDebt = (FloatingActionButton) findViewById(R.id.btn_add_debt);
        txtILWhat = (TextInputLayout) findViewById(R.id.txtIL_what);
        txtILWho = (TextInputLayout) findViewById(R.id.txtIL_who);
        btnCreatedAt = (Button) findViewById(R.id.btn_created_at);
        btnDelete = (Button) findViewById(R.id.btn_delete);
        chckLocked = (CheckBox) findViewById(R.id.chck_locked);

        // Set the hint after the animation completes, workaround for Android bug
        txtNote.setHint(getResources().getString(R.string.note));

        db = new DatabaseHandler(getApplicationContext());

        // Get current user
        user = db.getUser();

        // Array of all friends who will be suggested
        // 1) facebook friends
        friends = db.getFriends();
        // 2) contacts
        for (String contact : getContacts()) {
            friends.add(new Friend(null, contact, ""));
        }

        // Create the adapter to convert the array to views
        friendsSuggestionAdapter = new FriendsSuggestionAdapter(this, friends);

        // Setup autocomplete
        txtName.setAdapter(friendsSuggestionAdapter);
        txtName.setThreshold(1);
        // Save the information, that selected user is facebook friend (his id) or a contact
        txtName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Friend selectedFriend = (Friend) adapterView.getItemAtPosition(i);
                tempFacebookFriendId = selectedFriend.id;

                // Set blue text, so that user knows that facebook friend was selected
                if (selectedFriend.id != null) {
                    txtName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.facebook_lighter));
                } else {
                    txtName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }
            }
        });

        // Set up currencies spinner
        currencies = db.getCurrencies();
        ArrayAdapter<Currency> currenciesAdapter = new ArrayAdapter<Currency>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, currencies);
        spnCurrency.setPrompt("Currency:");
        spnCurrency.setAdapter(currenciesAdapter);

        // Set default currency
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        spnCurrency.setSelection(sharedPreferences.getInt(defaultCurrencyPos, 0));


        // Delete facebook friend info, when user types anything into the box
        // Also hide error messages
        txtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                tempFacebookFriendId = null;
                txtName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                txtILWho.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Remove error messages
        txtWhat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                txtILWhat.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Change input fields if user chooses a thing or money
        rdioThing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                txtWhat.setText("");
                // if button is checked
                if (b) {
                    spnCurrency.setVisibility(View.GONE);
                    txtWhat.setInputType(InputType.TYPE_CLASS_TEXT);
                    txtILWhat.setHint(getResources().getString(R.string.thing));
                } else {
                    spnCurrency.setVisibility(View.VISIBLE);
                    txtWhat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    txtILWhat.setHint(getResources().getString(R.string.amount));
                }
            }
        });

        // Created at button
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        btnCreatedAt.setText(df.format(new Date()));
        btnCreatedAt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });
        createdAtCal = Calendar.getInstance();

        // Init delete button
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(DebtActivity.this);
                builder.setMessage(R.string.do_you_want_to_delete_this_debt)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                debtToEdit.deletedAt = new Date();
                                addDebt();
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                builder.create().show();
            }
        });


        // --------------------- Pre-filling widgets when editing debt ----------------------------
        // Temporary disable animation to avoid textInputLayout bug
        txtILWhat.setHintAnimationEnabled(false);
        txtILWho.setHintAnimationEnabled(false);

        // Check my or their debt
        if (getIntent().getBooleanExtra(MainActivity.MY_DEBT, true)) {
            rdioMyDebt.setChecked(true);
        } else {
            rdioTheirDebt.setChecked(true);
        }

        // Add a new debt to a friend
        Friend friend = Parcels.unwrap(getIntent().getParcelableExtra(FriendsFragment.ADD_TO_FRIEND));
        if (friend != null) {
            txtName.setText(friend.name);
            tempFacebookFriendId = friend.id;
            // color text
            txtName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.facebook_lighter));
        }

        // Edit debt
        debtToEdit = Parcels.unwrap(getIntent().getParcelableExtra(DebtsFragment.DEBT_TO_EDIT));
        if (debtToEdit != null) {

            // Change activity title
            setTitle(getString(R.string.edit));

            // disable fields, which cannot be edited
            txtName.setFocusable(false);
            txtName.setEnabled(false);
            txtName.setCursorVisible(false);
            txtName.setKeyListener(null);
            txtName.setBackgroundColor(Color.TRANSPARENT);

            chckLocked.setVisibility(View.GONE);

            txtName.setText(debtToEdit.who);
            txtNote.setText(debtToEdit.note);
            btnCreatedAt.setText(debtToEdit.createdAtString());

            // Mark facebook friend
            if ((debtToEdit.creditorId != null && debtToEdit.debtorId != null)) {
                txtName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.facebook_lighter));

                if (debtToEdit.creditorId.equals(user.id)) {
                    tempFacebookFriendId = debtToEdit.debtorId;
                } else {
                    tempFacebookFriendId = debtToEdit.creditorId;
                }
            } else {
                tempFacebookFriendId = null;
                txtName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            }

            // Check radio buttons
            if (debtToEdit.thingName != null && !debtToEdit.thingName.isEmpty()) {
                rdioThing.setChecked(true);
                txtWhat.setText(debtToEdit.what);
            } else {
                rdioMoney.setChecked(true);
                txtWhat.setText(String.valueOf(debtToEdit.amount));

                // For some reason I can't use functions like indexOf or getPosition ¯\_(ツ)_/¯
                for (Currency currency : currencies) {
                    if (db.getCurrency(debtToEdit.currencyId).id == (currency.id)) {
                        spnCurrency.setSelection(currenciesAdapter.getPosition(currency));
                    }
                }
            }

            if (debtToEdit.creditorId != null && debtToEdit.creditorId.equals(user.id)) {
                rdioTheirDebt.setChecked(true);
            } else {
                rdioMyDebt.setChecked(true);
            }

            txtWhat.requestFocus();
            showKeyboard(250);

        } else {
            // hide buttons which have no use for a new debt
            btnDelete.setVisibility(View.GONE);

            // Set the right focus
            if (txtName.getText().length() > 0) {
                txtWhat.requestFocus();
            } else {
                txtName.requestFocus();
            }

            showKeyboard(400);
        }

        // Re-enable animation of textInputLayouts
        txtILWhat.setHintAnimationEnabled(true);
        txtILWho.setHintAnimationEnabled(true);


        // Add debt button
        btnAddDebt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addDebt();
            }
        });
    }

    /**
     * Returns a list of users's contacts
     *
     * @return List<String> contacts
     */
    private List<String> getContacts() {

        List<String> contacts = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {

            // Permission is granted
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, new String[]{ContactsContract.Data.DISPLAY_NAME_PRIMARY}, null, null, null);

            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                if (name != null && !name.contains("@")) {
                    contacts.add(name);
                }
            }

            cursor.close();

        }
        // empty list is returned, when permission isn't granted
        return contacts;
    }

    /**
     * Handle user's answer to 'request permission' dialog
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted

                // Reset friends array and create it again
                friends.clear();
                // 1) facebook friends
                friends.addAll(db.getFriends());
                // 2) contacts
                for (String contact : getContacts()) {
                    friends.add(new Friend(null, contact, ""));
                }

                friendsSuggestionAdapter.notifyDataSetChanged();

            } else {
                Toast.makeText(this, "We won't be able to suggest you people from your contacts. :(", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Adds a new debt to the local database
     * Finishes activity and returns to main activity
     */
    private void addDebt() {

        Integer id = null;
        Integer creditorId = null;
        Integer debtorId = null;
        String customFriendName = null;
        String thingName = null;
        Double amount = null;
        Integer currencyId = null;
        Date paidAt = null;
        Date deletedAt = null;
        Integer managerId = null;
        Long version = System.currentTimeMillis();


        // set 'invisible' variables if editing a debt
        if (debtToEdit != null) {
            id = debtToEdit.id;
            deletedAt = debtToEdit.deletedAt;
            managerId = debtToEdit.managerId;
        }

        // Set user
        if (rdioMyDebt.isChecked()) {
            debtorId = user.id;
        } else {
            creditorId = user.id;
        }

        // Lock debt
        if (chckLocked.isChecked()) {
            managerId = user.id;
        }

        // Set the other person taking part in this debt
        if (tempFacebookFriendId != null) {
            if (rdioMyDebt.isChecked()) {
                creditorId = tempFacebookFriendId;
            } else {
                debtorId = tempFacebookFriendId;
            }
        } else {
            customFriendName = txtName.getText().toString();
        }

        // Check if both sides of debt are set
        if ((creditorId == null || debtorId == null) && (customFriendName == null || customFriendName.isEmpty())) {
            txtILWho.setError(getString(R.string.this_field_cant_be_empty));
            return;
        }

        // Check if the owed thing is valid
        if (rdioThing.isChecked()) {
            thingName = txtWhat.getText().toString();

            if (thingName.isEmpty()) {
                txtILWhat.setError(getString(R.string.this_field_cant_be_empty));
                return;
            }

        } else {

            try {
                amount = Double.parseDouble(txtWhat.getText().toString());
            } catch (NumberFormatException e) {
                txtILWhat.setError(getString(R.string.this_field_has_to_be_a_number));
                return;
            }

            // Check if amount is not too big
            if (amount > 999999999) {
                txtILWhat.setError(getString(R.string.this_number_is_too_big));
                return;
            }

            currencyId = currencies.get(spnCurrency.getSelectedItemPosition()).id;
            // Add currency to shared pref, so that this currency is used by default
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(defaultCurrencyPos, spnCurrency.getSelectedItemPosition());
            editor.apply();

            if (amount < 1) {
                txtILWhat.setError(getString(R.string.you_cant_owe_zero));
                return;
            }
        }

        // Set createdAt
        if (createdAt == null) {
            createdAt = new Date();
        }

        Log.d(Const.TAG, "Adding debt to database: " + String.valueOf(tempFacebookFriendId));

        Debt debt = new Debt(
                id,
                creditorId,
                debtorId,
                customFriendName,
                amount,
                currencyId,
                thingName,
                txtNote.getText().toString(),
                paidAt,
                deletedAt,
                new Date(),
                createdAt,
                managerId,
                version
        );

        // Add debt into the local database
        db.addOrUpdateDebt(debt);

        btnAddDebt.setBackgroundColor(getResources().getColor(R.color.bpBlue));

        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        setResult(RESULT_OK);
        finish();
    }

    /**
     * Shows dialog for picking a date of creation
     */
    private void showDatePickerDialog() {
        createdAtCal.clear();

        CalendarDatePickerDialogFragment cdp = new CalendarDatePickerDialogFragment()
                .setOnDateSetListener(DebtActivity.this);
        cdp.show(getSupportFragmentManager(), FRAG_TAG_DATE_PICKER);
    }

    /**
     * Shows dialog for picking a time of creation
     */
    private void showTimePickerDialog() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(DebtActivity.this);
        rtpd.show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);
    }


    /**
     * Handles date picking dialog
     * Shows time picking dialog
     */
    @Override
    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
        createdAtCal.set(year, monthOfYear, dayOfMonth);
        showTimePickerDialog();
    }

    /**
     * Handles time picking dialog
     */
    @Override
    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
        createdAtCal.set(Calendar.HOUR, hourOfDay);
        createdAtCal.set(Calendar.MINUTE, minute);
        createdAt = createdAtCal.getTime();
        btnCreatedAt.setText(Tools.formatDate(createdAt));
    }

    private void showKeyboard(long delay) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
            }
        }, delay);
    }
}

package cz.petrkubes.ioweyou.Fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder;
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment;

import org.parceler.Parcels;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;

import cz.petrkubes.ioweyou.Activities.DebtActivity;
import cz.petrkubes.ioweyou.Activities.MainActivity;
import cz.petrkubes.ioweyou.Adapters.DebtsAdapter;
import cz.petrkubes.ioweyou.Database.DatabaseHandler;
import cz.petrkubes.ioweyou.Pojos.Debt;
import cz.petrkubes.ioweyou.Pojos.User;
import cz.petrkubes.ioweyou.R;
import cz.petrkubes.ioweyou.Tools.Const;

import static android.view.View.GONE;


/**
 * Fragment representing the MyDebts and TheirDebts tabs
 *
 * @author Petr Kubes
 */
public class DebtsFragment extends Fragment implements UpdateableFragment {

    public static final String ARG_MY = "argMy";
    public static final String DEBT_TO_EDIT = "debtToEdit";

    private DatabaseHandler db;
    private ListView lstDebts;
    private ProgressBar prgDebts;
    private User user;
    private DebtsAdapter adapter;
    private ArrayList<Debt> debts;
    private boolean myDebts = true;
    private View rootView;

    // Dialog elements
    private Button btnDialogPay;
    private ImageButton btnDialogEdit;
    private Button btnDialogCancel;
    private TextView txtDialogWho;
    private TextView txtDialogWhat;
    private TextView txtDialogNote;
    private TextView txtDialogDate;
    private Switch swtchPayment;
    private TextView txtNote;

    // Dialog variables
    private Double amount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create new database instance
        db = new DatabaseHandler(getContext());
        // Get user
        user = db.getUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_list, container, false);

        prgDebts = (ProgressBar) rootView.findViewById(R.id.prg_tab);
        lstDebts = (ListView) rootView.findViewById(R.id.lst_tab);
        txtNote = (TextView) rootView.findViewById(R.id.txt_tab_note);


        Bundle args = getArguments();

        // Get the correct list of debts - my debts or their debts
        debts = new ArrayList<Debt>();
        myDebts = args.getBoolean(ARG_MY);

        // set note on an empty screen
        if (myDebts) {
            txtNote.setText(getString(R.string.you_do_not_owe_anyone));
        } else {
            txtNote.setText(getString(R.string.no_one_owes_you));
        }

        toggleNote();

        // Populate the list view
        adapter = new DebtsAdapter(getContext(), debts, myDebts);
        lstDebts.setAdapter(adapter);

        // Set up a click listener for actions with debts
        lstDebts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Debt debt = (Debt) adapterView.getItemAtPosition(i);
                showDialog(debt);
            }
        });

        this.update();

        return rootView;
    }

    @Override
    public void update() {
        new Task().execute();
        Log.d(Const.TAG, "updated debts fragment");
    }

    /**
     * Toggles the note that is supposed to show only when the list is empty
     */
    public void toggleNote() {
        // Show note
        if (debts.size() == 0) {
            txtNote.setVisibility(View.VISIBLE);
            if (myDebts) {
                txtNote.setText(getString(R.string.you_do_not_owe_anyone));
            } else {
                txtNote.setText(getString(R.string.no_one_owes_you));
            }
        } else {
            txtNote.setVisibility(GONE);
        }
    }

    /**
     * Creates and show the dialog with debts actions
     *
     * @param debt Selected debt
     */
    private void showDialog(final Debt debt) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate layout
        View debtDialogView = inflater.inflate(R.layout.dialog_debt, null);
        builder.setView(debtDialogView);
        final AlertDialog dialog = builder.create();

        // Set up buttons, text views and switch
        btnDialogPay = (Button) debtDialogView.findViewById(R.id.btn_pay);
        btnDialogCancel = (Button) debtDialogView.findViewById(R.id.btn_cancel);
        btnDialogEdit = (ImageButton) debtDialogView.findViewById(R.id.btn_edit);
        swtchPayment = (Switch) debtDialogView.findViewById(R.id.swtch_payment);

        txtDialogWhat = (TextView) debtDialogView.findViewById(R.id.txt_what);
        txtDialogWho = (TextView) debtDialogView.findViewById(R.id.txt_who);
        txtDialogNote = (TextView) debtDialogView.findViewById(R.id.txt_note);
        txtDialogDate = (TextView) debtDialogView.findViewById(R.id.txt_date);

        txtDialogWhat.setText(debt.what);
        txtDialogWho.setText(debt.who);
        txtDialogNote.setText(debt.note);
        txtDialogDate.setText(debt.createdAtString());

        if (debt.note == null) {
            txtDialogNote.setVisibility(View.GONE);
        }

        if (debt.amount == null) {
            swtchPayment.setVisibility(View.GONE);
        }

        amount = debt.amount;

        // Disable buttons for locked debts
        if (debt.managerId != null && debt.managerId != user.id) {
            btnDialogPay.setVisibility(GONE);
            btnDialogEdit.setVisibility(GONE);
            swtchPayment.setVisibility(GONE);
        } else {
            // Pay button
            btnDialogPay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!swtchPayment.isChecked()) { // a) Pay back the whole debt
                        debt.paidAt = new Date();
                        updateDebt(debt);
                        dialog.cancel();
                    } else {
                        // Show payment dialog
                        NumberPickerBuilder npb = new NumberPickerBuilder()
                                .setFragmentManager(getFragmentManager())
                                .setStyleResId(R.style.BetterPickersDialogFragment)
                                .setMaxNumber(BigDecimal.valueOf(debt.amount))
                                .setPlusMinusVisibility(View.GONE)
                                .setMinNumber(BigDecimal.ONE)
                                .setDecimalVisibility(View.GONE)
                                .addNumberPickerDialogHandler(new NumberPickerDialogFragment.NumberPickerDialogHandlerV2() {
                                    @Override
                                    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber) {
                                        Integer payment = Integer.parseInt(number.toString());

                                        if (debt.amount - payment <= 0) {
                                            debt.paidAt = new Date();
                                        } else {
                                            debt.amount -= payment;
                                        }

                                        updateDebt(debt);
                                        dialog.cancel();
                                    }
                                });
                        npb.show();
                    }
                }
            });

            // Edit button
            btnDialogEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getContext(), DebtActivity.class);
                    intent.putExtra(DEBT_TO_EDIT, Parcels.wrap(debt));
                    getActivity().startActivityForResult(intent, MainActivity.ADD_DEBT_REQUEST);
                    dialog.cancel();
                }
            });
        }

        // Cancel button
        btnDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });


        // display dialog
        dialog.show();
    }

    private void updateDebt(Debt debt) {
        debt.version = System.currentTimeMillis();
        db.addOrUpdateDebt(debt);
        ((MainActivity) getActivity()).updateDebtsAndActions();
    }

    /**
     * Task for asynchronous populating of the list
     */
    private class Task extends AsyncTask<Void, Void, ArrayList<Debt>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            prgDebts.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<Debt> doInBackground(Void[] params) {
            if (user != null) {
                debts = db.getExtendedDebts(myDebts, user.id);
            }
            return debts;
        }

        @Override
        protected void onPostExecute(ArrayList<Debt> debts) {
            toggleNote();
            prgDebts.setVisibility(GONE);
            adapter.clear();
            adapter.addAll(debts);
            adapter.notifyDataSetChanged();
        }
    }
}
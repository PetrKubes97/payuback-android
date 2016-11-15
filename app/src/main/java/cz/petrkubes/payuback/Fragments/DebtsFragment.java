package cz.petrkubes.payuback.Fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cz.petrkubes.payuback.Activities.MainActivity;
import cz.petrkubes.payuback.Adapters.DebtsAdapter;
import cz.petrkubes.payuback.Const;
import cz.petrkubes.payuback.Database.DatabaseHandler;
import cz.petrkubes.payuback.R;
import cz.petrkubes.payuback.Structs.Debt;
import cz.petrkubes.payuback.Structs.User;


public class DebtsFragment extends Fragment implements UpdateableFragment {

    public static final String ARG_MY = "argMy";
    private DatabaseHandler db;
    private ListView lstDebts;
    private User user;
    private DebtsAdapter adapter;
    private ArrayList<Debt> debts;
    private boolean myDebts = true;
    private View rootView;

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

        rootView = inflater.inflate(R.layout.fragment_debts, container, false);
        lstDebts = (ListView) rootView.findViewById(R.id.lst_debts);

        Bundle args = getArguments();

        // Get the correct list of debts - my debts or their debts
        debts = new ArrayList<>();
        myDebts = args.getBoolean(ARG_MY);

        if (user != null) {
             debts = db.getExtendedDebts(myDebts, user.id);
        }

        // Populate the listview
        adapter = new DebtsAdapter(getContext(), debts);
        lstDebts.setAdapter(adapter);

        // Set up a click listener for actions with debts
        lstDebts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Debt debt = (Debt) adapterView.getItemAtPosition(i);
                showDialog(debt);

            }
        });

        return rootView;
    }

    @Override
    public void update() {
        Log.d(Const.TAG, "UPDATING FRAgMENT");

        if (user != null) {
            debts = db.getExtendedDebts(myDebts, user.id);
            Log.d(Const.TAG, debts.toString());
        }
        adapter.clear();
        adapter.addAll(debts);
        adapter.notifyDataSetChanged();
    }

    private void showDialog(Debt debt) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View debtDialogView = inflater.inflate(R.layout.dialog_debt, null);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(debtDialogView);

        AlertDialog dialog = builder.create();
        // display dialog
        dialog.show();
    }
}
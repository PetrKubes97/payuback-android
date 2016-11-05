package cz.petrkubes.payuback.Fragments;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cz.petrkubes.payuback.Adapters.DebtsAdapter;
import cz.petrkubes.payuback.Database.DatabaseHandler;
import cz.petrkubes.payuback.R;
import cz.petrkubes.payuback.Structs.Debt;


public class DebtsFragment extends Fragment {

    public static final String ARG_MY = "argMy";
    private DatabaseHandler db;
    private ListView lstDebts;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create new database instance
        db = new DatabaseHandler(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_debts, container, false);
        lstDebts = (ListView) rootView.findViewById(R.id.lst_debts);

        Bundle args = getArguments();

        // Get the correct list of debts - my debts or their debts
        ArrayList<Debt> debts = db.getExtendedDebts(args.getBoolean(ARG_MY), db.getUser().id);
        // Populate the listview
        DebtsAdapter adapter = new DebtsAdapter(getContext(), debts);
        lstDebts.setAdapter(adapter);

        return rootView;
    }




}
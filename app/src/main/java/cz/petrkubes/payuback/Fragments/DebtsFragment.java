package cz.petrkubes.payuback.Fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cz.petrkubes.payuback.R;

/**
 * Created by petr on 22.10.16.
 */

// Instances of this class are fragments representing a single
// object in our collection.
public class DebtsFragment extends Fragment {
    public static final String ARG_OBJECT = "object";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_debts, container, false);

        Bundle args = getArguments();

        return rootView;
    }
}
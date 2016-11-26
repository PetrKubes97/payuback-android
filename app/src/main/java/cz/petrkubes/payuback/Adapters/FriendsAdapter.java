package cz.petrkubes.payuback.Adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.petrkubes.payuback.Const;
import cz.petrkubes.payuback.R;
import cz.petrkubes.payuback.Pojos.Friend;

/**
 * Created by petr on 21.11.16.
 */

public class  FriendsAdapter extends ArrayAdapter<Friend> {

    ArrayList<Friend> friends;

    public FriendsAdapter(Context context, List<Friend> friends) {
        super(context, R.layout.item_debt, friends);
        this.friends = (ArrayList<Friend>) friends;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Friend friend = getItem(position);

        FriendsAdapter.ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new FriendsAdapter.ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());

            convertView = inflater.inflate(R.layout.item_friend, parent, false);

            viewHolder.txtName = (TextView) convertView.findViewById(R.id.txt_name);
            viewHolder.txtStuff = (TextView) convertView.findViewById(R.id.txt_stuff);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FriendsAdapter.ViewHolder) convertView.getTag();
        }

        // Populate the data from the data object via the viewHolder object
        // into the template view.
        if (friend != null) {
            viewHolder.txtName.setText(friend.name);
            viewHolder.txtStuff.setText(fromHtml(friend.debtsString));
            Log.d(Const.TAG, friend.debtsString);
        }

        // Return the completed view to render on screen
        return convertView;
    }

    @Nullable
    @Override
    public Friend getItem(int position) {
        return friends.get(position);
    }

    @Override
    public int getCount() {
        return friends.size();
    }

    // View lookup cache
    private static class ViewHolder {
        TextView txtName;
        TextView txtStuff;
    }

    // I don't even have a comment for this...
    @SuppressWarnings("deprecation")
    private Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }
}
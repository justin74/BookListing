package com.example.apple.booklist;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by apple on 2017/7/13.
 */

public class BookAdapter extends ArrayAdapter<Book> {

    public BookAdapter(Activity context, ArrayList<Book> books) {
        super(context, 0, books);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        ViewHolder holder = new ViewHolder();
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.book_list_item, parent, false);
            holder.bookNameTextView = (TextView) listItemView.findViewById(R.id.book_name_text_view);
            holder.authorTextView = (TextView) listItemView.findViewById(R.id.book_author_text_view);
            listItemView.setTag(holder);
        } else {
            holder = (ViewHolder) listItemView.getTag();
        }

        Book currentBook = getItem(position);

        holder.bookNameTextView.setText(currentBook.getBookName());
        holder.authorTextView.setText(currentBook.getBookAuthor());

        return listItemView;
    }

    class ViewHolder{
        TextView bookNameTextView;
        TextView authorTextView;
    }
}

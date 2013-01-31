package sk.uniba.fmph.noandroid.multichat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

public class MessageAdapter extends ArrayAdapter<MessageEntry> {

	private ArrayList<MessageEntry> messages;
	private MainActivity context;
    private Filter filter;
    private ArrayList<MessageEntry> filteredMessages;

	public MessageAdapter(Context context, int textViewResourceId, ArrayList<MessageEntry> messages) {
		super(context, textViewResourceId, messages);
		this.messages = messages;
		this.context = (MainActivity) context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {		
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.message_row, null);
		}
		MessageEntry message = messages.get(position);
		if (message != null) {
			TextView timestamp = (TextView) v.findViewById(R.id.message_timestamp);
			TextView name = (TextView) v.findViewById(R.id.message_name);
			TextView text = (TextView) v.findViewById(R.id.message_text);
			ResizableImageView avatar = (ResizableImageView) v.findViewById(R.id.message_avatar);

			User u = context.getUser(message.getUserID());
			
			DateFormat df = SimpleDateFormat.getDateTimeInstance();
			timestamp.setText(df.format(message.getTimestamp()));
			name.setText(u.getName());
			text.setText(message.getMessage());
			if(u.getAvatar() != null) {
				avatar.setImageBitmap(u.getAvatar());	
			}
			else {
				avatar.setImageDrawable(getContext().getResources().getDrawable(R.drawable.com_facebook_profile_default_icon));
			}
		}
		return v;
	}
	
	@Override
    public Filter getFilter() {
        if(filter == null) {
            filter = new UserFilter();
        }
        
        return filter;
    }

    private class UserFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            // NOTE: this function is *always* called from a background thread, and
            // not the UI thread.
            FilterResults result = new FilterResults();
            if(constraint != null && constraint.length() > 0) {
                ArrayList<MessageEntry> filt = new ArrayList<MessageEntry>();
                ArrayList<MessageEntry> lItems = new ArrayList<MessageEntry>();
                synchronized (this) {
                    lItems.addAll(messages);
                }
                for(int i = 0, l = lItems.size(); i < l; i++) {
                    MessageEntry m = lItems.get(i);
                    if(m.getUserID().equals(constraint)) {
                        filt.add(m);
                    }
                }
                result.count = filt.size();
                result.values = filt;
            }
            else {
                synchronized(this) {
                    result.values = messages;
                    result.count = messages.size();
                }
            }
            
            return result;
        }

        @SuppressWarnings("unchecked")
		@Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // NOTE: this function is *always* called from the UI thread.
            filteredMessages = (ArrayList<MessageEntry>) results.values;
            notifyDataSetChanged();
            clear();
            for(int i = 0, l = filteredMessages.size(); i < l; i++) {
                add(filteredMessages.get(i));
            }
            
            notifyDataSetInvalidated();
        }
    }
}

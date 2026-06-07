package com.example.geomemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {

    public interface OnMemoClickListener {
        void onMemoClick(Memo memo);
        void onViewClick(Memo memo);
        void onEditClick(Memo memo);
        void onDeleteClick(Memo memo);
    }

    private final List<Memo> memos;
    private final OnMemoClickListener listener;

    public MemoAdapter(List<Memo> memos, OnMemoClickListener listener) {
        this.memos = memos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memo, parent, false);
        return new MemoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
        Memo memo = memos.get(position);

        // Set custom pointer icon drawable
        int resId = holder.itemView.getContext().getResources().getIdentifier(
                memo.getPointerIcon(), "drawable", holder.itemView.getContext().getPackageName()
        );
        if (resId != 0) {
            holder.imgIcon.setImageResource(resId);
        } else {
            holder.imgIcon.setImageResource(R.drawable.pointer_1);
        }

        holder.textTitle.setText(memo.getTitle());

        String text = memo.getText();
        if (text == null || text.trim().isEmpty()) {
            holder.textText.setText("No description");
        } else {
            holder.textText.setText(text);
        }

        holder.textDate.setText(memo.getCreatedAt());

        // Set card click listener for full details popup
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemoClick(memo);
            }
        });

        // Set options menu click listener (View, Edit, Delete)
        holder.btnOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            
            // Force icons to show on PopupMenu using reflection
            try {
                java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
                field.setAccessible(true);
                Object menuPopupHelper = field.get(popup);
                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                java.lang.reflect.Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                setForceShowIcon.invoke(menuPopupHelper, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            popup.getMenu().add(0, 1, 0, "View").setIcon(android.R.drawable.ic_menu_view);
            popup.getMenu().add(0, 2, 1, "Edit").setIcon(android.R.drawable.ic_menu_edit);
            popup.getMenu().add(0, 3, 2, "Delete").setIcon(android.R.drawable.ic_menu_delete);
            
            popup.setOnMenuItemClickListener(item -> {
                if (listener != null) {
                    switch (item.getItemId()) {
                        case 1:
                            listener.onViewClick(memo);
                            return true;
                        case 2:
                            listener.onEditClick(memo);
                            return true;
                        case 3:
                            listener.onDeleteClick(memo);
                            return true;
                    }
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return memos == null ? 0 : memos.size();
    }

    public void updateMemos(List<Memo> newMemos) {
        this.memos.clear();
        this.memos.addAll(newMemos);
        notifyDataSetChanged();
    }

    static class MemoViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textText;
        TextView textDate;
        ImageButton btnOptions;
        ImageView imgIcon;

        MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_memo_title);
            textText = itemView.findViewById(R.id.text_memo_text);
            textDate = itemView.findViewById(R.id.text_memo_date);
            btnOptions = itemView.findViewById(R.id.btn_memo_options);
            imgIcon = itemView.findViewById(R.id.img_memo_icon);
        }
    }
}

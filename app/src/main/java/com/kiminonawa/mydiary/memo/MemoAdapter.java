package com.kiminonawa.mydiary.memo;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kiminonawa.mydiary.R;
import com.kiminonawa.mydiary.db.DBManager;
import com.kiminonawa.mydiary.shared.EditMode;
import com.kiminonawa.mydiary.shared.ScreenHelper;
import com.kiminonawa.mydiary.shared.ThemeManager;
import com.marshalchen.ultimaterecyclerview.dragsortadapter.DragSortAdapter;

import java.util.List;


/**
 * Created by daxia on 2016/10/17.
 */

public class MemoAdapter extends DragSortAdapter<DragSortAdapter.ViewHolder> implements EditMode {


    //Data
    private List<MemoEntity> memoList;

    private FragmentActivity mActivity;
    private long topicId;
    private DBManager dbManager;
    private boolean isEditMode = false;
    private EditMemoDialogFragment.MemoCallback callback;
    private RecyclerView recyclerView;


    public MemoAdapter(FragmentActivity activity, long topicId, List<MemoEntity> memoList, DBManager dbManager, EditMemoDialogFragment.MemoCallback callback, RecyclerView recyclerView) {
        super(recyclerView);
        this.mActivity = activity;
        this.topicId = topicId;
        this.memoList = memoList;
        this.dbManager = dbManager;
        this.callback = callback;
        this.recyclerView = recyclerView;
    }


    @Override
    public DragSortAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_memo_item, parent, false);
        return new MemoViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return memoList.size();
    }


    @Override
    public long getItemId(int position) {
        return memoList.get(position).getMemoId();
    }

    @Override
    public void onBindViewHolder(final DragSortAdapter.ViewHolder holder, final int position) {
        if (holder instanceof MemoViewHolder) {
            ((MemoViewHolder) holder).setItemPosition(position);
            setMemoContent(((MemoViewHolder) holder), position);
        }
    }

    private void setMemoContent(MemoViewHolder holder, final int position) {
        if (memoList.get(position).isChecked()) {
            SpannableString spannableContent = new SpannableString(memoList.get(position).getContent());
            spannableContent.setSpan(new StrikethroughSpan(), 0, spannableContent.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.getTVContent().setText(spannableContent);
            holder.getTVContent().setAlpha(0.4F);
        } else {
            holder.getTVContent().setText(memoList.get(position).getContent());
            holder.getTVContent().setAlpha(1F);
        }
    }


    @Override
    public boolean isEditMode() {
        return isEditMode;
    }

    @Override
    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
    }

    @Override
    public int getPositionForId(long id) {
        for (MemoEntity memoEntity : memoList) {
            if (memoEntity.getMemoId() == id) {
                return memoList.indexOf(memoEntity);
            }
        }
        return -1;
    }

    @Override
    public boolean move(int fromPosition, int toPosition) {
        memoList.add(toPosition, memoList.remove(fromPosition));
        return true;
    }

    @Override
    public void onDrop() {
        super.onDrop();
        recyclerView.findViewHolderForItemId(getDraggingId()).itemView.setBackgroundColor(Color.WHITE);
        int order = memoList.size();
        dbManager.opeDB();
        for (MemoEntity memoEntity : memoList) {
            dbManager.updateMemoOrder(memoEntity.getMemoId(), order--);
        }
        dbManager.closeDB();
    }

    protected class MemoViewHolder extends DragSortAdapter.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private View rootView;
        private ImageView IV_memo_item_dot;
        private TextView TV_memo_item_content;
        private ImageView IV_memo_item_delete;
        private RelativeLayout RL_memo_item_root_view;
        private int itemPosition;


        protected MemoViewHolder(View view) {
            super(MemoAdapter.this, view);
            this.rootView = view;
            RL_memo_item_root_view = (RelativeLayout) rootView.findViewById(R.id.RL_memo_item_root_view);
            IV_memo_item_dot = (ImageView) rootView.findViewById(R.id.IV_memo_item_dot);
            TV_memo_item_content = (TextView) rootView.findViewById(R.id.TV_memo_item_content);
            IV_memo_item_delete = (ImageView) rootView.findViewById(R.id.IV_memo_item_delete);
            TV_memo_item_content.setTextColor(ThemeManager.getInstance().getThemeDarkColor(mActivity));
        }

        public TextView getTVContent() {
            return TV_memo_item_content;
        }


        public void setItemPosition(int itemPosition) {
            if (isEditMode) {
                IV_memo_item_dot.setImageResource(R.drawable.ic_memo_swap_vert_black_24dp);
                ViewGroup.LayoutParams layoutParams = IV_memo_item_dot.getLayoutParams();
                layoutParams.width = layoutParams.height = ScreenHelper.dpToPixel(mActivity.getResources(), 24);
                IV_memo_item_delete.setVisibility(View.VISIBLE);
                IV_memo_item_delete.setOnClickListener(this);
                RL_memo_item_root_view.setOnClickListener(this);
                RL_memo_item_root_view.setOnLongClickListener(this);
            } else {
                IV_memo_item_dot.setImageResource(R.drawable.ic_memo_dot_24dp);
                ViewGroup.LayoutParams layoutParams = IV_memo_item_dot.getLayoutParams();
                layoutParams.width = layoutParams.height = ScreenHelper.dpToPixel(mActivity.getResources(), 10);
                IV_memo_item_delete.setVisibility(View.GONE);
                IV_memo_item_delete.setOnClickListener(null);
                RL_memo_item_root_view.setOnClickListener(this);
                RL_memo_item_root_view.setOnLongClickListener(null);
            }
            this.itemPosition = itemPosition;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.IV_memo_item_delete:
                    dbManager.opeDB();
                    dbManager.delMemo(memoList.get(itemPosition).getMemoId());
                    dbManager.closeDB();
                    memoList.remove(itemPosition);
                    notifyDataSetChanged();
                    break;
                case R.id.RL_memo_item_root_view:
                    if (isEditMode) {
                        EditMemoDialogFragment editMemoDialogFragment = EditMemoDialogFragment.newInstance(
                                topicId, memoList.get(itemPosition).getMemoId(), false, memoList.get(itemPosition).getContent());
                        editMemoDialogFragment.show(mActivity.getSupportFragmentManager(), "editMemoDialogFragment");
                    } else {
                        memoList.get(itemPosition).toggleChecked();
                        dbManager.opeDB();
                        dbManager.updateMemoChecked(memoList.get(itemPosition).getMemoId(), memoList.get(itemPosition).isChecked());
                        dbManager.closeDB();
                        setMemoContent(this, itemPosition);
                    }
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            v.setBackgroundColor(ThemeManager.getInstance().getThemeMainColor(mActivity));
            startDrag();
            return false;
        }
    }
}

package com.hpplay.sdk.source.test.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hpplay.sdk.source.browse.api.LelinkServiceInfo;
import com.hpplay.sdk.source.test.OnItemClickListener;
import com.hpplay.sdk.source.test.utils.AssetsUtil;
import com.mxiaotu.flutter_plugin_newhpplay.R;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Zippo on 2018/6/8.
 * Date: 2018/6/8
 * Time: 17:00:39
 */
public class BrowseAdapter extends RecyclerView.Adapter<BrowseAdapter.RecyclerHolder> {

    private static final String TAG = "BrowseAdapter";
    private Context mContext;
    private List<LelinkServiceInfo> mDatas;
    private LayoutInflater mInflater;
    private OnItemClickListener mItemClickListener;
    private LelinkServiceInfo mSelectInfo;

    public BrowseAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mDatas = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.mItemClickListener = l;
    }

    @Override
    public RecyclerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.item_browse, parent, false);
        return new RecyclerHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerHolder holder, int position) {
        LelinkServiceInfo info = mDatas.get(position);
        if (null == info) {
            return;
        }
        String item = info.getName() + " uid:" + info.getUid() + " types:" + info.getTypes();
        holder.textView.setText(item);
        if (AssetsUtil.isContains(mSelectInfo, info)) {
            // 选中了，则重置底色
            holder.textView.setBackgroundColor(Color.GRAY);
        } else {
            holder.textView.setBackgroundColor(Color.YELLOW);
        }
//        holder.textView.setTag(R.id.id_position, position);
//        holder.textView.setTag(R.id.id_info, info);
        holder.textView.setOnClickListener(mOnItemClickListener);
    }

    @Override
    public int getItemCount() {
        return null == mDatas ? 0 : mDatas.size();
    }

    public void updateDatas(List<LelinkServiceInfo> infos) {
        if (null != infos) {
            mDatas.clear();
            mDatas.addAll(infos);
            notifyDataSetChanged();
        }
    }

    public LelinkServiceInfo getSelectInfo() {
        return mSelectInfo;
    }

    public void setSelectInfo(LelinkServiceInfo selectInfo) {
        mSelectInfo = selectInfo;
    }

    class RecyclerHolder extends RecyclerView.ViewHolder {

        TextView textView;

        private RecyclerHolder(android.view.View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.textview);
        }

    }

    private View.OnClickListener mOnItemClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
//            int position = (int) v.getTag(R.id.id_position);
//            LelinkServiceInfo info = (LelinkServiceInfo) v.getTag(R.id.id_info);
//            if (null != mItemClickListener) {
//                mItemClickListener.onClick(position, info);
//            }
        }

    };
}

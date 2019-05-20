package com.zyyoona7.appsupport.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.zyyoona7.appsupport.R;

import java.util.ArrayList;
import java.util.List;

public class VerticalAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public static VerticalAdapter newInstance(int itemCount) {
        List<String> list = new ArrayList<>(1);
        for (int i = 0; i < itemCount; i++) {
            list.add("哈哈哈 我是 item " + i);
        }
        return new VerticalAdapter(list);
    }

    public VerticalAdapter() {
        this(null);
    }

    public VerticalAdapter(List<String> list) {
        super(R.layout.item_main, list);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tv_text, item);
    }
}

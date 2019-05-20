package com.zyyoona7.appsupport.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.zyyoona7.appsupport.R;

import java.util.ArrayList;
import java.util.List;

public class HorizontalAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public static HorizontalAdapter newInstance(int itemCount) {
        List<String> list = new ArrayList<>(1);
        for (int i = 0; i < itemCount; i++) {
            list.add("item " + i);
        }
        return new HorizontalAdapter(list);
    }

    public HorizontalAdapter() {
        this(null);
    }

    public HorizontalAdapter(List<String> list) {
        super(R.layout.item_main2, list);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tv_text, item);
    }
}

/*
 * Copyright (C) 2016 Jared Rummler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package lib.demo.spinner;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 自定义 PopupWindow实现样式
 * 布局中直接以用，样式已经设置完成，自己需要自定义具体位置在res/drawable/ms_selector_style.xml
 */
public class MaterialSpinner extends android.support.v7.widget.AppCompatTextView {

    private OnNothingSelectedListener onNothingSelectedListener;
    private OnItemSelectedListener onItemSelectedListener;
    private MaterialSpinnerBaseAdapter adapter; //listView的适配
    private PopupWindow popupWindow; //使用popupWindow控件 样式
    private ListView listView; //布局
    private Drawable arrowDrawable; //箭头布局 视图
    private boolean hideArrow;//xml中arrow是否隐藏，默认不隐藏false
    private boolean nothingSelected;
    private int popupWindowMaxHeight;//spinner下拉框整体高度 最大高度
    private int popupWindowHeight;//spinner下拉框整体高度
    private int selectedIndex;
    private int backgroundColor;
    private int backgroundSelector;
    private int arrowColor;
    private int arrowColorDisabled;
    private int textColor; //字体颜色
    private Context context;

    public MaterialSpinner(Context context) {
        super(context);
        this.context = context;
        init(context, null);
    }

    public MaterialSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context, attrs);
    }

    public MaterialSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context, attrs);
    }

    /**
     * 构建
     *
     * @param context
     * @param attrs
     */
    private void init(Context context, AttributeSet attrs) {
        //获取attrs.xml中设置的参数
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaterialSpinner);

        //默认色设置
        int defaultColor = getTextColors().getDefaultColor();

        boolean rtl = Utils.isRtl(context);

        try {
            //背景色 默认白色
            backgroundColor = typedArray.getColor(R.styleable.MaterialSpinner_ms_background_color, Color.WHITE);
            //背景 选中色
            backgroundSelector = typedArray.getResourceId(R.styleable.MaterialSpinner_ms_background_selector, 0);
            textColor = typedArray.getColor(R.styleable.MaterialSpinner_ms_text_color, defaultColor);
            arrowColor = typedArray.getColor(R.styleable.MaterialSpinner_ms_arrow_tint, textColor);
            hideArrow = typedArray.getBoolean(R.styleable.MaterialSpinner_ms_hide_arrow, false);
            popupWindowMaxHeight = typedArray.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_popupwindow_maxheight, 0);
            popupWindowHeight = typedArray.getLayoutDimension(R.styleable.MaterialSpinner_ms_popupwindow_height, WindowManager.LayoutParams.WRAP_CONTENT);
            arrowColorDisabled = Utils.lighter(arrowColor, 0.8f);
        } finally {
            typedArray.recycle();
        }

        //设置字体显示 居中
        Resources resources = getResources();
        int left = resources.getDimensionPixelSize(R.dimen.ms_padding_left);
        int right = resources.getDimensionPixelSize(R.dimen.ms_padding_right);
        if (rtl) {
            right = resources.getDimensionPixelSize(R.dimen.ms_padding_left);
        } else {
            left = resources.getDimensionPixelSize(R.dimen.ms_padding_right);
        }

        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setClickable(true);
        setPadding(left, 0, right, 0);

        //设置 在界面中的显示样式，以及点击样式
        setBackgroundResource(R.drawable.ms_selector_style);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && rtl) {
            setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            setTextDirection(View.TEXT_DIRECTION_RTL);
        }

        /**设置箭头布局
         *
         */
        if (!hideArrow) {
            arrowDrawable = ContextCompat.getDrawable(context, R.drawable.ms_arrow).mutate();
            arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_IN);
            if (rtl) {
                setCompoundDrawablesWithIntrinsicBounds(arrowDrawable, null, null, null);
            } else {
                setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null);
            }
        }

        //创建布局 使用listView控件展示items
        listView = new ListView(context);
        listView.setId(getId());
        listView.setDivider(null);
        listView.setItemsCanFocus(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= selectedIndex && position < adapter.getCount()) {
                    position++;
                }
                selectedIndex = position;
                nothingSelected = false;
                Object item = adapter.get(position);
                adapter.notifyItemSelected(position);
                setText(item.toString());
                collapse();
                if (onItemSelectedListener != null) {
                    //noinspection unchecked
                    onItemSelectedListener.onItemSelected(MaterialSpinner.this, position, id, item);
                }
            }
        });

        //使用PopupWindow控件承载数据
        popupWindow = new PopupWindow(context);
        popupWindow.setContentView(listView);//
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        //设置背景色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(16);//设置阴影
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ms_popwindow_bg));// R.drawable.ms__drawable
        } else {
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ms_popwindow_bg));
        }

        //设置背景
        if (backgroundColor != Color.WHITE) { // default color is white
            setBackgroundColor(backgroundColor);
        } else if (backgroundSelector != 0) {
            //改变最底层颜色
            setBackgroundResource(backgroundSelector);
        }
        //数据显示颜色
        if (textColor != defaultColor) {
            setTextColor(textColor);
        }

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                if (nothingSelected && onNothingSelectedListener != null) {
                    onNothingSelectedListener.onNothingSelected(MaterialSpinner.this);
                }
                if (!hideArrow) {
                    animateArrow(false);
                }
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        popupWindow.setWidth(MeasureSpec.getSize(widthMeasureSpec));
        popupWindow.setHeight(calculatePopupWindowHeight());

        if (adapter != null) {
            CharSequence currentText = getText();
            String longestItem = currentText.toString();
            for (int i = 0; i < adapter.getCount(); i++) {
                String itemText = adapter.getItemText(i);
                if (itemText.length() > longestItem.length()) {
                    longestItem = itemText;
                }
            }
            setText(longestItem);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setText(currentText);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * 处理分发，该处处理简单，只对ACTION_UP做简单处理
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isEnabled() && isClickable()) {
                if (!popupWindow.isShowing()) {
                    expand();
                } else {
                    collapse();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 重写
     *
     * @param color
     */
    @Override
    public void setBackgroundColor(int color) {
        backgroundColor = color;
        Drawable background = getBackground();
        if (background instanceof StateListDrawable) { // pre-L
            try {
                Method getStateDrawable = StateListDrawable.class.getDeclaredMethod("getStateDrawable", int.class);
                if (!getStateDrawable.isAccessible())
                    getStateDrawable.setAccessible(true);
                int[] colors = {Utils.darker(color, 0.85f), color};
                for (int i = 0; i < colors.length; i++) {
                    ColorDrawable drawable = (ColorDrawable) getStateDrawable.invoke(background, i);
                    drawable.setColor(colors[i]);
                }
            } catch (Exception e) {
                Log.e("MaterialSpinner", "Error setting background color", e);
            }
        } else if (background != null) { // 21+ (RippleDrawable)
            background.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        popupWindow.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    /**
     * 重写
     *
     * @param color
     */
    @Override
    public void setTextColor(int color) {
        textColor = color;
        super.setTextColor(color);
    }

    /**
     * 保存状态
     *
     * @return
     */
    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("state", super.onSaveInstanceState());
        bundle.putInt("selected_index", selectedIndex);
        if (popupWindow != null) {
            bundle.putBoolean("is_popup_showing", popupWindow.isShowing());
            collapse();
        } else {
            bundle.putBoolean("is_popup_showing", false);
        }
        return bundle;
    }

    /**
     * 获取状态
     *
     * @param savedState
     */
    @Override
    public void onRestoreInstanceState(Parcelable savedState) {
        if (savedState instanceof Bundle) {
            Bundle bundle = (Bundle) savedState;
            selectedIndex = bundle.getInt("selected_index");
            if (adapter != null) {
                setText(adapter.get(selectedIndex).toString());
                adapter.notifyItemSelected(selectedIndex);
            }
            if (bundle.getBoolean("is_popup_showing")) {
                if (popupWindow != null) {
                    // Post the show request into the looper to avoid bad token exception
                    post(new Runnable() {

                        @Override
                        public void run() {
                            expand();
                        }
                    });
                }
            }
            savedState = bundle.getParcelable("state");
        }
        super.onRestoreInstanceState(savedState);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (arrowDrawable != null) {
            arrowDrawable.setColorFilter(enabled ? arrowColor : arrowColorDisabled, PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * @return the selected item position
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Set the default spinner item using its index
     * 初始界面 常用
     *
     * @param position the item's position
     */
    public void setSelectedIndex(int position) {
        if (adapter != null) {
            if (position >= 0 && position <= adapter.getCount()) {
                adapter.notifyItemSelected(position);
                selectedIndex = position;
                setText(adapter.get(position).toString());
            } else {
                throw new IllegalArgumentException("Position must be lower than adapter count!");
            }
        }
    }


    /**
     * Set the dropdown items
     *
     * @param items A list of items
     * @param <T>   The item type
     */
    public <T> void setItems(@NonNull T... items) {
        setItems(Arrays.asList(items));
    }

    /**
     * Set the dropdown items
     *
     * @param items A list of items
     * @param <T>   The item type
     */
    public <T> void setItems(@NonNull List<T> items) {
        adapter = new MaterialSpinnerAdapter<>(getContext(), items).setBackgroundSelector(backgroundSelector).setTextColor(textColor);
        setAdapterInternal(adapter);
    }

    /**
     * Get the list of items in the adapter
     *
     * @param <T> The item type
     * @return A list of items or {@code null} if no items are set.
     */
    public <T> List<T> getItems() {
        if (adapter == null) {
            return null;
        }
        //noinspection unchecked
        return adapter.getItems();
    }

    /**
     * Set a custom adapter for the dropdown items
     *
     * @param adapter The list adapter
     */
    public void setAdapter(@NonNull ListAdapter adapter) {
        this.adapter = new MaterialSpinnerAdapterWrapper(getContext(), adapter).setBackgroundSelector(backgroundSelector)
                .setTextColor(textColor);
        setAdapterInternal(this.adapter);
    }

    /**
     * Set the custom adapter for the dropdown items
     *
     * @param adapter The adapter
     * @param <T>     The type
     */
    public <T> void setAdapter(MaterialSpinnerAdapter<T> adapter) {
        this.adapter = adapter;
        this.adapter.setTextColor(textColor);
        this.adapter.setBackgroundSelector(backgroundSelector);
        setAdapterInternal(adapter);
    }

    /**
     * 数据绑定+显示
     *
     * @param adapter
     */
    private void setAdapterInternal(@NonNull MaterialSpinnerBaseAdapter adapter) {
        listView.setAdapter(adapter);
        if (selectedIndex >= adapter.getCount()) {
            selectedIndex = 0;
        }
        if (adapter.getCount() > 0) {
            setText(adapter.get(selectedIndex).toString());
        } else {
            setText("");
        }
    }

    /**
     * 展开
     */
    public void expand() {
        if (!hideArrow) {
            animateArrow(true);
        }
        nothingSelected = true;
        popupWindow.showAsDropDown(this);
    }

    /**
     * 收起
     */
    public void collapse() {
        if (!hideArrow) {
            animateArrow(false);
        }
        popupWindow.dismiss();
    }

    /**
     * Set the tint color for the dropdown arrow
     *
     * @param color the color value
     */
    public void setArrowColor(@ColorInt int color) {
        arrowColor = color;
        arrowColorDisabled = Utils.lighter(arrowColor, 0.8f);
        if (arrowDrawable != null) {
            arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void animateArrow(boolean shouldRotateUp) {
        int start = shouldRotateUp ? 0 : 10000;
        int end = shouldRotateUp ? 10000 : 0;
        ObjectAnimator animator = ObjectAnimator.ofInt(arrowDrawable, "level", start, end);
        animator.start();
    }

    /**
     * 计算popupWindow控件 弹窗的高度
     *
     * @return
     */
    private int calculatePopupWindowHeight() {
        if (adapter == null) {
            return WindowManager.LayoutParams.WRAP_CONTENT;
        }
        //计算出listView的总高度
        float listViewHeight = adapter.getCount() * getResources().getDimension(R.dimen.ms_item_height);

        //如果xml布局中设置了最高高度，且listViewHeight高度满足，优先使用最高高度
        if (popupWindowMaxHeight > 0 && listViewHeight > popupWindowMaxHeight) {

            return popupWindowMaxHeight;

        } else if (popupWindowHeight != WindowManager.LayoutParams.MATCH_PARENT
                && popupWindowHeight != WindowManager.LayoutParams.WRAP_CONTENT
                && popupWindowHeight <= listViewHeight) {
            return popupWindowHeight;
        }
        return WindowManager.LayoutParams.WRAP_CONTENT;
    }

    /**
     * Get the {@link PopupWindow}.
     *
     * @return The {@link PopupWindow} that is displayed when the view has been clicked.
     */
    public PopupWindow getPopupWindow() {
        return popupWindow;
    }

    /**
     * Get the {@link ListView} that is used in the dropdown menu
     *
     * @return the ListView shown in the PopupWindow.
     */
    public ListView getListView() {
        return listView;
    }


    /**
     * Interface definition for a callback to be invoked when an item in this view has been selected.
     *
     * @param <T> Adapter item type
     */
    public interface OnItemSelectedListener<T> {

        /**
         * <p>Callback method to be invoked when an item in this view has been selected. This callback is invoked only when
         * the newly selected position is different from the previously selected position or if there was no selected
         * item.</p>
         *
         * @param view     The {@link MaterialSpinner} view
         * @param position The position of the view in the adapter
         * @param id       The row id of the item that is selected
         * @param item     The selected item
         */
        void onItemSelected(MaterialSpinner view, int position, long id, T item);
    }

    /**
     * Register a callback to be invoked when an item in the dropdown is selected.
     *
     * @param onItemSelectedListener The callback that will run
     */
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    /**
     * Interface definition for a callback to be invoked when the dropdown is dismissed and no item was selected.
     */
    public interface OnNothingSelectedListener {

        /**
         * Callback method to be invoked when the {@link PopupWindow} is dismissed and no item was selected.
         *
         * @param spinner the {@link MaterialSpinner}
         */
        void onNothingSelected(MaterialSpinner spinner);
    }

    /**
     * Register a callback to be invoked when the {@link PopupWindow} is shown but the user didn't select an item.
     *
     * @param onNothingSelectedListener the callback that will run
     */
    public void setOnNothingSelectedListener(@Nullable OnNothingSelectedListener onNothingSelectedListener) {
        this.onNothingSelectedListener = onNothingSelectedListener;
    }
}

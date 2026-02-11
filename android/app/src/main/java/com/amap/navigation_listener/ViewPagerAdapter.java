package com.amap.navigation_listener;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new NavigationFragment();
            case 1:
                return new BluetoothFragment();
            case 2:
                return new DataConverterFragment();
            default:
                return new NavigationFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // 三个页面
    }
}
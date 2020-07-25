package com.uni.julio.supertvplus.adapter;



import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.uni.julio.supertvplus.view.FreeFragment;

public class MypagerAdpater extends FragmentStatePagerAdapter {


    public MypagerAdpater(@NonNull FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return FreeFragment.newInstance(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 1: return "Premium";
            case 2: return "Premium Plus";
            default: return "Free";
        }
    }

    @Override
    public int getCount() {
        return 3;
    }


}
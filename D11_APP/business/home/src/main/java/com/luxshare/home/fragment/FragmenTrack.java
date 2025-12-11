package com.luxshare.home.fragment;

import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Set;

public class FragmenTrack {
    private Set fragments = new HashSet<Fragment>();

    private FragmenTrack() {
    }

    private static FragmenTrack fragmenTrack = new FragmenTrack();

    public static FragmenTrack getInstance() {
        return fragmenTrack;
    }

    public void put(Fragment fragment) {
        fragments.add(fragment);
    }

    public boolean take(Fragment fragment) {
        return fragments.remove(fragment);
    }

    public void clear() {
        fragments.clear();
    }

    public int size() {
        return fragments.size();
    }
}

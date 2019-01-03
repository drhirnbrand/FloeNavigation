package de.awi.floenavigation.grid;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.awi.floenavigation.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class GridContainerFragment extends Fragment {


    public GridContainerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_grid_container, container, false);
    }

}

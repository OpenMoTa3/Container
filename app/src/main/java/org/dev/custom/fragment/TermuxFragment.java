package org.dev.custom.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.dev.custom.databinding.FragmentTermuxBinding;
import org.dev.custom.viewmodel.TermuxViewModel;
public class TermuxFragment extends Fragment {

    private FragmentTermuxBinding ftb;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TermuxViewModel tvm =
                new ViewModelProvider(this).get(TermuxViewModel.class);

        ftb = FragmentTermuxBinding.inflate(inflater, container, false);
        View root = ftb.getRoot();

        final TextView textView = ftb.textDashboard;
        tvm.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ftb = null;
    }
}
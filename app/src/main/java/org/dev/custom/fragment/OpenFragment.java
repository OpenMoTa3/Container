package org.dev.custom.fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class OpenFragment extends Fragment {

    private FragmentOpenBinding fob;

    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fob = FragmentFileBinding.inflate(inflater, container, false);
        return fob.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fob = null;
    }
}

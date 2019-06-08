package com.unix4all.rypi.distort;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class TimedRemoveFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_POSITION = "position";

    private String mTitleString;
    private String mDescriptionString;
    private Integer mPosition;
    private TimedEnableTask mTimedEnableTask;

    private Activity mActivity;

    private TextView mTitle;
    private TextView mDescription;
    private Button mRemoveButton;

    private OnFragmentFinishedListener mListener;

    public TimedRemoveFragment() {
        // Required empty public constructor
    }

    public static TimedRemoveFragment newInstance(Activity activity, String title, String description, Integer position) {
        TimedRemoveFragment fragment = new TimedRemoveFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_POSITION, position);
        fragment.setActivity(activity);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timed_remove, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if (getArguments() != null) {
            mTitleString = getArguments().getString(ARG_TITLE);
            mDescriptionString = getArguments().getString(ARG_DESCRIPTION);
            mPosition = getArguments().getInt(ARG_POSITION);
        }

        mListener = (OnFragmentFinishedListener) getActivity();

        mTitle = view.findViewById(R.id.timedRemoveTitle);
        mTitle.setText(mTitleString);

        mDescription = view.findViewById(R.id.timedRemoveDescription);
        mDescription.setText(mDescriptionString);

        mRemoveButton = view.findViewById(R.id.timedRemoveAcceptButton);
        mRemoveButton.setEnabled(false);
        ((GradientDrawable)mRemoveButton.getBackground()).setColor(getResources().getColor(R.color.removeButtonDisabled));
        mTimedEnableTask = new TimedEnableTask(5000L);
        mTimedEnableTask.execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    public interface OnFragmentFinishedListener {
        void onTimedRemoveFinished(Boolean removeChoice, @Nullable Integer selectedIndex);
    }

    public class TimedEnableTask extends AsyncTask<Void, Void, Boolean> {

        Long mMillisecondDelay;

        TimedEnableTask(Long millisecondDelay) {
            mMillisecondDelay = millisecondDelay;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Attempt authentication against a network service.
            try {
                Thread.sleep(mMillisecondDelay);
                if(mRemoveButton != null && getActivity() != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Enable remove button
                            mRemoveButton.setEnabled(true);
                            ((GradientDrawable)mRemoveButton.getBackground()).setColor(getResources().getColor(R.color.removeButton));

                            mRemoveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mListener.onTimedRemoveFinished(true, mPosition);
                                    dismiss();
                                }
                            });
                        }
                    });
                } else {
                    throw new Exception("Timed Remove-Button is not set");
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mTimedEnableTask = null;
        }
    }
}

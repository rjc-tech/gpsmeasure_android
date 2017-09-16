package jp.co.rjc.gpsmeasure.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;

import jp.co.rjc.gpsmeasure.R;
import jp.co.rjc.gpsmeasure.util.MyLocationUtil;

public class MainActivity extends AppCompatActivity {

    private static final String ID_TARGET_PLACE_1 = "target_place_1";
    private static final String ID_TARGET_PLACE_2 = "target_place_2";

    protected TextView mLatitude1;
    protected TextView mLongitude1;
    protected Button mButton1;

    protected TextView mLatitude2;
    protected TextView mLongitude2;
    protected Button mButton2;

    protected TextView mDistance;
    protected Button mClearButton;

    private MyLocationUtil mLocationUtil;
    private MyLocationUtil.OnProcessCallbackListener mOnProcessCallbackListener;

    private String mChangeTargetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLatitude1 = (TextView) findViewById(R.id.latitude_1);
        mLongitude1 = (TextView) findViewById(R.id.longitude_1);
        mButton1 = (Button) findViewById(R.id.button_place_1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeButtonAvairable(false);
                mChangeTargetId = ID_TARGET_PLACE_1;
                mLocationUtil.startLocationService();
            }
        });
        mButton1.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams params = mButton1.getLayoutParams();
                params.width = mButton1.getHeight();
                mButton1.setLayoutParams(params);
            }
        });

        mLatitude2 = (TextView) findViewById(R.id.latitude_2);
        mLongitude2 = (TextView) findViewById(R.id.longitude_2);
        mButton2 = (Button) findViewById(R.id.button_place_2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeButtonAvairable(false);
                mChangeTargetId = ID_TARGET_PLACE_2;
                mLocationUtil.startLocationService();
            }
        });
        mButton2.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams params = mButton2.getLayoutParams();
                params.width = mButton2.getHeight();
                mButton2.setLayoutParams(params);
            }
        });

        mDistance = (TextView) findViewById(R.id.distance);
        mClearButton = (Button) findViewById(R.id.clear);
        mClearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mLatitude1.setText("");
                mLongitude1.setText("");
                mLatitude2.setText("");
                mLongitude2.setText("");
                mDistance.setText("");
            }
        });

        mLocationUtil = MyLocationUtil.getsInstance(this);
        mOnProcessCallbackListener = new MyLocationUtil.OnProcessCallbackListener() {

            @Override
            public void onSuccessLocation(double latitude, double longitude) {
                switch (mChangeTargetId) {
                    case ID_TARGET_PLACE_1:
                        mLatitude1.setText(String.valueOf(latitude));
                        mLongitude1.setText(String.valueOf(longitude));
                        calcDistance();
                        break;
                    case ID_TARGET_PLACE_2:
                        mLatitude2.setText(String.valueOf(latitude));
                        mLongitude2.setText(String.valueOf(longitude));
                        calcDistance();
                        break;
                    default:
                        changeButtonAvairable(true);
                        break;
                }
                mChangeTargetId = null;
            }

            @Override
            public void onFailedLodation() {
                changeButtonAvairable(true);
            }
        };
        mLocationUtil.setOnProcessCallbackListener(mOnProcessCallbackListener);
    }

    private void calcDistance() {
        try {
            if (!TextUtils.isEmpty(mLatitude1.getText())
                    && !TextUtils.isEmpty(mLongitude1.getText())
                    && !TextUtils.isEmpty(mLatitude2.getText())
                    && !TextUtils.isEmpty(mLongitude2.getText())) {

                final float result = mLocationUtil.distFrom(
                        Float.valueOf((String) mLatitude1.getText()),
                        Float.valueOf((String) mLongitude1.getText()),
                        Float.valueOf((String) mLatitude2.getText()),
                        Float.valueOf((String) mLongitude2.getText())
                );
                BigDecimal decimal = new BigDecimal(String.valueOf(result));
                mDistance.setText(decimal.setScale(3, BigDecimal.ROUND_DOWN).floatValue() + getResources().getString(R.string.meter));
            }
        } catch (Exception e) {
            // ignore
        }
        changeButtonAvairable(true);
    }

    private void changeButtonAvairable(final boolean avairable) {
        mButton1.setClickable(avairable);
        mButton1.setEnabled(avairable);
        mButton2.setClickable(avairable);
        mButton2.setEnabled(avairable);
        mClearButton.setClickable(avairable);
        mClearButton.setEnabled(avairable);
    }
}

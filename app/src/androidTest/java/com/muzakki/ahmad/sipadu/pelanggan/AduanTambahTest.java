package com.muzakki.ahmad.sipadu.pelanggan;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.view.ContextThemeWrapper;

import com.muzakki.ahmad.sipadupelanggan.R;
import com.muzakki.ahmad.sipadupelanggan.main.AduanTambah;

import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class AduanTambahTest extends ActivityUnitTestCase<AduanTambah> {
    AduanTambah at = null;
    final CountDownLatch signal = new CountDownLatch(1);

    public AduanTambahTest() {
        super(AduanTambah.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ContextThemeWrapper context = new ContextThemeWrapper(getInstrumentation().getTargetContext(), R.style.AppTheme);
        setActivityContext(context);
        Intent i = new Intent(getInstrumentation().getTargetContext(),AduanTambah.class);
        startActivity(i, null, null);

        at = getActivity();
    }


}
package com.baker.abaker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.vending.expansion.downloader.impl.DownloaderService;

public class ABakerDownloaderService extends DownloaderService {

    // You must use the public key belonging to your publisher account
    public static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi5cG3UzG2JnXjpjjL+GPJ2YgntA7dW4P0ozwSX3ufWCOajvo/K+Iffv7HURigHBJ6Uwbqy0ZbKI02fPeKRJfFoHuKcNqgVMyCu73hOVfGEzM9Rh1Egi8+OuN+B4iEuqVC7Zfra+jTxfN6/whoPNEtXzIrl2b77Hcvbp8qmN2+f+9A4n5TG6GOsIKJJP0LrEHgFHR4bxX+1foU3ZyUJW0Pgn3tty1iW+HnnNICF32is4P4c93uJok2QVgmqMT/Gn3NAbzCh1zpxE7b5m+0ySdqMBVkBqzJPM6otY8VC353A6M2pZQPmR+ikXPwprCZNNBjwgCD7EpUMJ58UZ2MORJQQIDAQAB";
    // You should also modify this salt
    public static final byte[] SALT = new byte[] { 3, 42, -12, -1, 54, 98,
            -82, -12, 73, 2, -8, -4, 90, 5, -106, -107, -33, 78, -1, 32
    };

    @Override
    public String getPublicKey() {
        return BASE64_PUBLIC_KEY;
    }

    @Override
    public byte[] getSALT() {
        return SALT;
    }

    @Override
    public String getAlarmReceiverClassName() {
        return ABakerAlarmReceiver.class.getName();
    }
}

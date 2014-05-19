package com.baker.abaker.test;

import android.content.Intent;
import android.test.mock.MockContext;

import java.util.ArrayList;
import java.util.List;

public class TestContext extends MockContext
{
    private List<Intent> receivedIntents = new ArrayList<Intent>();

    @Override
    public String getPackageName()
    {
        return "com.baker.abaker";
    }

    @Override
    public void startActivity(Intent intent)
    {
        receivedIntents.add(intent);
    }

    public List<Intent> getReceivedIntents()
    {
        return receivedIntents;
    }
}
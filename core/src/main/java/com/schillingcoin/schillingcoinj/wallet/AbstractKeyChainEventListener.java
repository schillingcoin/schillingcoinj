package com.schillingcoin.schillingcoinj.wallet;

import com.schillingcoin.schillingcoinj.core.ECKey;

import java.util.List;

public class AbstractKeyChainEventListener implements KeyChainEventListener {
    @Override
    public void onKeysAdded(List<ECKey> keys) {
    }
}


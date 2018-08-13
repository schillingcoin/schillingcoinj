/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.schillingcoin.schillingcoinj.params;

import com.schillingcoin.schillingcoinj.core.NetworkParameters;
import com.schillingcoin.schillingcoinj.core.Sha256Hash;
import com.schillingcoin.schillingcoinj.core.Utils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends NetworkParameters {
    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1d00ffffL);
        dumpedPrivateKeyHeader = 183;
        addressHeader = 63; // addresses begin with 'S'
        p2shHeader = 125; // addresses begin with 's'
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 9976;
        packetMagic = 0xe8f8a422L;
        genesisBlock.setDifficultyTarget(0x1d00ffffL);
        genesisBlock.setTime(1528727751L);
        genesisBlock.setNonce(907246021L);
        id = ID_MAINNET;
        spendableCoinbaseDepth = 500;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("00000000b45c8fb2ef515c4cf22f29bbe47a7acca246fb25b0ea241577d46e2f"), genesisHash);

        checkpoints.put(0, new Sha256Hash(genesisHash));

        dnsSeeds = new String[] {
                "seed1.schillingcoin.com",
                "seed2.schillingcoin.com",
                "explorer.schillingcoin.com",
                "pool.schillingcoin.com",
                "dev.miu.at"
        };
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }

    @Override
    public String toString() {
        return "Schillingcoin";
    }
}

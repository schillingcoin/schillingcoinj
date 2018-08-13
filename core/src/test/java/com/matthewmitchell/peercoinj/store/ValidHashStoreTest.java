package com.matthewmitchell.peercoinj.store;

import com.matthewmitchell.peercoinj.core.Sha256Hash;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ValidHashStoreTest {
    @Test
    public void basics() throws Exception {
        File directory = new File(".");
        String filePrefix = "fetchblock";
        File validHashFile = new File(directory, filePrefix + ".hashes");
        ValidHashStore validHashStore = new ValidHashStore(validHashFile);

        boolean res = validHashStore.isValidHash(new Sha256Hash("00000000004a4b5825c3941f5b2a9f18a2e8e33901c30c42943ab21e40e0f25a"), null, false);

        Assert.assertTrue(res);
    }
}

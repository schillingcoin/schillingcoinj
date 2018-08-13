/*
 * Copyright 2012, 2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.schillingcoin.schillingcoinj.uri;

import com.schillingcoin.schillingcoinj.core.Address;
import com.schillingcoin.schillingcoinj.params.MainNetParams;
import com.schillingcoin.schillingcoinj.uri.SchillingcoinURI;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.schillingcoin.schillingcoinj.core.Coin.*;
import static org.junit.Assert.*;

public class SchillingcoinURITest {
    private SchillingcoinURI testObject = null;

    private static final String MAINNET_GOOD_ADDRESS = "ShidnFvgMXU36DtCSArMWu25YT8eFoY3oM";
    private static final String MAINNET_BAD_ADDRESS = "mranY19RYUjgJjXY4BJNYp88WXXAg7Pr9T";

    @Test
    public void testConvertToSchillingcoinURI() throws Exception {
        Address goodAddress = new Address(MainNetParams.get(), MAINNET_GOOD_ADDRESS);

        // simple example
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello&message=AMessage", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("12.34"), "Hello", "AMessage"));

        // example with spaces, ampersand and plus
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello%20World&message=Mess%20%26%20age%20%2B%20hope", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("12.34"), "Hello World", "Mess & age + hope"));

        // no amount, label present, message present
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?label=Hello&message=glory", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, null, "Hello", "glory"));

        // amount present, no label, message present
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("0.1"), null, "glory"));
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("0.1"), "", "glory"));

        // amount present, label present, no message
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("12.34"), "Hello", null));
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("12.34"), "Hello", ""));

        // amount present, no label, no message
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=1000", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("1000"), null, null));
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?amount=1000", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, parseCoin("1000"), "", ""));

        // no amount, label present, no message
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?label=Hello", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, null, "Hello", null));

        // no amount, no label, message present
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?message=Agatha", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, null, null, "Agatha"));
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS + "?message=Agatha", SchillingcoinURI.convertToSchillingcoinURI(goodAddress, null, "", "Agatha"));

        // no amount, no label, no message
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS, SchillingcoinURI.convertToSchillingcoinURI(goodAddress, null, null, null));
        assertEquals("schillingcoin:" + MAINNET_GOOD_ADDRESS, SchillingcoinURI.convertToSchillingcoinURI(goodAddress, null, "", ""));
    }

    @Test
    public void testGood_Simple() throws SchillingcoinURIParseException {

        for (String scheme : SchillingcoinURI.PEERCOIN_SCHEMES) {
            testObject = new SchillingcoinURI(
                    MainNetParams.get(), scheme + ":" + MAINNET_GOOD_ADDRESS
            );
            assertNotNull(testObject);
            assertNull("Unexpected amount", testObject.getAmount());
            assertNull("Unexpected label", testObject.getLabel());
            assertEquals(
                    "Unexpected label", 20, testObject.getAddress().getHash160().length
            );
        }

    }

    /**
     * Test a broken URI (bad scheme)
     */
    @Test
    public void testBad_Scheme() {
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), "blimpcoin:" + MAINNET_GOOD_ADDRESS);
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad syntax)
     */
    @Test
    public void testBad_BadSyntax() {
        // Various illegal characters
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + "|" + MAINNET_GOOD_ADDRESS);
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "\\");
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        // Separator without field
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":");
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }
    }

    /**
     * Test a broken URI (missing address)
     */
    @Test
    public void testBad_Address() {
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME);
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad address type)
     */
    @Test
    public void testBad_IncorrectAddressType() {
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_BAD_ADDRESS);
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad address"));
        }
    }

    /**
     * Handles a simple amount
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Amount() throws SchillingcoinURIParseException {
        // Test the decimal parsing
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210.123456");
        assertEquals("6543210123456", testObject.getAmount().toString());

        // Test the decimal parsing
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=.123456");
        assertEquals("123456", testObject.getAmount().toString());

        // Test the integer parsing
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210");
        assertEquals("6543210000000", testObject.getAmount().toString());
    }

    /**
     * Handles a simple label
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Label() throws SchillingcoinURIParseException {
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?label=Hello%20World");
        assertEquals("Hello World", testObject.getLabel());
    }

    /**
     * Handles a simple label with an embedded ampersand and plus
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testGood_LabelWithAmpersandAndPlus() throws Exception {
        String testString = "Hello Earth & Mars + Venus";
        String encodedLabel = SchillingcoinURI.encodeURLString(testString);
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(testString, testObject.getLabel());
    }

    /**
     * Handles a Russian label (Unicode test)
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testGood_LabelWithRussian() throws Exception {
        // Moscow in Russian in Cyrillic
        String moscowString = "\u041c\u043e\u0441\u043a\u0432\u0430";
        String encodedLabel = SchillingcoinURI.encodeURLString(moscowString);
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(moscowString, testObject.getLabel());
    }

    /**
     * Handles a simple message
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Message() throws SchillingcoinURIParseException {
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?message=Hello%20World");
        assertEquals("Hello World", testObject.getMessage());
    }

    /**
     * Handles various well-formed combinations
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Combinations() throws SchillingcoinURIParseException {
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210&label=Hello%20World&message=Be%20well");
        assertEquals(
                "SchillingcoinURI['amount'='6543210000000','label'='Hello World','message'='Be well','address'='ShidnFvgMXU36DtCSArMWu25YT8eFoY3oM']",
                testObject.toString());
    }

    /**
     * Handles a badly formatted amount field
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Amount() throws SchillingcoinURIParseException {
        // Missing
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=");
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }

        // Non-decimal (BIP 21)
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=12X4");
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }
    }

    @Test
    public void testEmpty_Label() throws SchillingcoinURIParseException {
        assertNull(new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?label=").getLabel());
    }

    @Test
    public void testEmpty_Message() throws SchillingcoinURIParseException {
        assertNull(new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?message=").getMessage());
    }

    /**
     * Handles duplicated fields (sneaky address overwrite attack)
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Duplicated() throws SchillingcoinURIParseException {
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?address=aardvark");
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("address"));
        }
    }

    @Test
    public void testGood_ManyEquals() throws SchillingcoinURIParseException {
        assertEquals("aardvark=zebra", new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":"
                + MAINNET_GOOD_ADDRESS + "?label=aardvark=zebra").getLabel());
    }

    /**
     * Handles unknown fields (required and not required)
     *
     * @throws SchillingcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testUnknown() throws SchillingcoinURIParseException {
        // Unknown not required field
        testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?aardvark=true");
        assertEquals("SchillingcoinURI['aardvark'='true','address'='ShidnFvgMXU36DtCSArMWu25YT8eFoY3oM']", testObject.toString());

        assertEquals("true", (String) testObject.getParameterByName("aardvark"));

        // Unknown not required field (isolated)
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?aardvark");
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("no separator"));
        }

        // Unknown and required field
        try {
            testObject = new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?req-aardvark=true");
            fail("Expecting SchillingcoinURIParseException");
        } catch (SchillingcoinURIParseException e) {
            assertTrue(e.getMessage().contains("req-aardvark"));
        }
    }

    @Test
    public void brokenURIs() throws SchillingcoinURIParseException {
        // Check we can parse the incorrectly formatted URIs produced by blockchain.info and its iPhone app.
        String str = "schillingcoin://ShidnFvgMXU36DtCSArMWu25YT8eFoY3oM?amount=0.01000000";
        SchillingcoinURI uri = new SchillingcoinURI(str);
        assertEquals("ShidnFvgMXU36DtCSArMWu25YT8eFoY3oM", uri.getAddress().toString());
        assertEquals(CENT, uri.getAmount());
    }

    @Test(expected = SchillingcoinURIParseException.class)
    public void testBad_AmountTooPrecise() throws SchillingcoinURIParseException {
        new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=0.123456789");
    }

    @Test(expected = SchillingcoinURIParseException.class)
    public void testBad_NegativeAmount() throws SchillingcoinURIParseException {
        new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=-1");
    }

    @Test(expected = SchillingcoinURIParseException.class)
    public void testBad_TooLargeAmount() throws SchillingcoinURIParseException {
        new SchillingcoinURI(MainNetParams.get(), SchillingcoinURI.PREFERED_PEERCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=2000000001");
    }

    @Test
    public void testPaymentProtocolReq() throws Exception {
        // Non-backwards compatible form ...
        SchillingcoinURI uri = new SchillingcoinURI(MainNetParams.get(), "schillingcoin:?r=https%3A%2F%2Fpeercoincore.org%2F%7Egavin%2Ff.php%3Fh%3Db0f02e7cea67f168e25ec9b9f9d584f9");
        assertEquals("https://peercoincore.org/~gavin/f.php?h=b0f02e7cea67f168e25ec9b9f9d584f9", uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of("https://peercoincore.org/~gavin/f.php?h=b0f02e7cea67f168e25ec9b9f9d584f9"),
                uri.getPaymentRequestUrls());
        assertNull(uri.getAddress());
    }

    @Test
    public void testMultiplePaymentProtocolReq() throws Exception {
        SchillingcoinURI uri = new SchillingcoinURI(MainNetParams.get(),
                "schillingcoin:?r=https%3A%2F%2Fpeercoincore.org%2F%7Egavin&r1=bt:112233445566");
        assertEquals(ImmutableList.of("bt:112233445566", "https://peercoincore.org/~gavin"), uri.getPaymentRequestUrls());
        assertEquals("https://peercoincore.org/~gavin", uri.getPaymentRequestUrl());
    }

    @Test
    public void testNoPaymentProtocolReq() throws Exception {
        SchillingcoinURI uri = new SchillingcoinURI(MainNetParams.get(), "schillingcoin:" + MAINNET_GOOD_ADDRESS);
        assertNull(uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of(), uri.getPaymentRequestUrls());
        assertNotNull(uri.getAddress());
    }

    @Test
    public void testUnescapedPaymentProtocolReq() throws Exception {
        SchillingcoinURI uri = new SchillingcoinURI(MainNetParams.get(),
                "schillingcoin:?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe");
        assertEquals("https://merchant.com/pay.php?h=2a8628fc2fbe", uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of("https://merchant.com/pay.php?h=2a8628fc2fbe"), uri.getPaymentRequestUrls());
        assertNull(uri.getAddress());
    }
}


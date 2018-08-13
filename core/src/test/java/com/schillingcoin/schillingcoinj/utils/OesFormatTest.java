/*
 * Copyright 2014 Adam Mackler
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

package com.schillingcoin.schillingcoinj.utils;

import com.schillingcoin.schillingcoinj.core.Coin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigDecimal;
import java.text.*;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.schillingcoin.schillingcoinj.core.Coin.*;
import static com.schillingcoin.schillingcoinj.core.NetworkParameters.MAX_MONEY;
import static com.schillingcoin.schillingcoinj.utils.OesAutoFormat.Style.CODE;
import static com.schillingcoin.schillingcoinj.utils.OesAutoFormat.Style.SYMBOL;
import static com.schillingcoin.schillingcoinj.utils.OesFixedFormat.REPEATING_DOUBLETS;
import static com.schillingcoin.schillingcoinj.utils.OesFixedFormat.REPEATING_TRIPLETS;
import static java.text.NumberFormat.Field.DECIMAL_SEPARATOR;
import static java.util.Locale.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class OesFormatTest {

    @Parameters
    public static Set<Locale[]> data() {
        Set<Locale[]> localeSet = new HashSet<Locale[]>();
        for (Locale locale : Locale.getAvailableLocales()) {
            localeSet.add(new Locale[]{locale});
        }
        return localeSet;
    }

    public OesFormatTest(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);
    }
 
    @Test
    public void prefixTest() { // prefix b/c symbol is prefixed
        OesFormat usFormat = OesFormat.getSymbolInstance(Locale.US);
        assertEquals("S1.00", usFormat.format(COIN));
        assertEquals("S1.01", usFormat.format(1010000));
        assertEquals("₥S0.01", usFormat.format(10));
        assertEquals("₥S1,011.00", usFormat.format(1011000));
        assertEquals("₥S1,000.01", usFormat.format(1000010));
        assertEquals("µS1,000,001", usFormat.format(1000001));
        assertEquals("µS1", usFormat.format(1));
    }

    @Test
    public void suffixTest() {
        OesFormat deFormat = OesFormat.getSymbolInstance(Locale.GERMANY);
        // int
        assertEquals("1,00 S", deFormat.format(1000000));
        assertEquals("1,01 S", deFormat.format(1010000));
        assertEquals("1.011,00 ₥S", deFormat.format(1011000));
        assertEquals("1.000,01 ₥S", deFormat.format(1000010));
        assertEquals("1.000.001 µS", deFormat.format(1000001));
    }

    @Test
    public void defaultLocaleTest() {
        assertEquals(
             "Default Locale is " + Locale.getDefault().toString(),
             OesFormat.getInstance().pattern(), OesFormat.getInstance(Locale.getDefault()).pattern()
        );
        assertEquals(
            "Default Locale is " + Locale.getDefault().toString(),
            OesFormat.getCodeInstance().pattern(),
            OesFormat.getCodeInstance(Locale.getDefault()).pattern()
       );
    }

    @Test
    public void symbolCollisionTest() {
        Locale[] locales = OesFormat.getAvailableLocales();
        for (int i = 0; i < locales.length; ++i) {
            String cs = ((DecimalFormat)NumberFormat.getCurrencyInstance(locales[i])).
                        getDecimalFormatSymbols().getCurrencySymbol();
            if (cs.contains("S")) {
                OesFormat bf = OesFormat.getSymbolInstance(locales[i]);
                String coin = bf.format(COIN);
                assertTrue(coin.contains("S"));
                String milli = bf.format(valueOf(1000));
                assertTrue(milli.contains("₥S"));
                String micro = bf.format(valueOf(1));
                assertTrue(micro.contains("µS"));
                OesFormat ff = OesFormat.builder().scale(0).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("S", ((OesFixedFormat)ff).symbol());
                assertEquals("S", ff.coinSymbol());
                coin = ff.format(COIN);
                assertTrue(coin.contains("S"));
                OesFormat mlff = OesFormat.builder().scale(3).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("₥S", ((OesFixedFormat)mlff).symbol());
                assertEquals("S", mlff.coinSymbol());
                milli = mlff.format(valueOf(1000));
                assertTrue(milli.contains("₥S"));
                OesFormat mcff = OesFormat.builder().scale(6).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("µS", ((OesFixedFormat)mcff).symbol());
                assertEquals("S", mcff.coinSymbol());
                micro = mcff.format(valueOf(1));
                assertTrue(micro.contains("µS"));
            }
            if (cs.contains("S")) {  // NB: We don't know of any such existing locale, but check anyway.
                OesFormat bf = OesFormat.getInstance(locales[i]);
                String coin = bf.format(COIN);
                assertTrue(coin.contains("OES"));
                String milli = bf.format(valueOf(1000));
                assertTrue(milli.contains("mOES"));
                String micro = bf.format(valueOf(1));
                assertTrue(micro.contains("µOES"));
            }
        }
    }

    @Test
    public void argumentTypeTest() {
        OesFormat usFormat = OesFormat.getSymbolInstance(Locale.US);
        // longs are tested above
        // Coin
        assertEquals("µS1,000,001", usFormat.format(COIN.add(valueOf(1))));
        // Integer
        assertEquals("µS2,147,483,647" ,usFormat.format(Integer.MAX_VALUE));
        assertEquals("(µS2,147,483,648)" ,usFormat.format(Integer.MIN_VALUE));
        // Long
        assertEquals("µS9,223,372,036,854,775,807" ,usFormat.format(Long.MAX_VALUE));
        assertEquals("(µS9,223,372,036,854,775,808)" ,usFormat.format(Long.MIN_VALUE));
        // BigInteger
        assertEquals("₥S0.01" ,usFormat.format(java.math.BigInteger.TEN));
        assertEquals("S0.00" ,usFormat.format(java.math.BigInteger.ZERO));
        // BigDecimal
        assertEquals("S1.00" ,usFormat.format(java.math.BigDecimal.ONE));
        assertEquals("S0.00" ,usFormat.format(java.math.BigDecimal.ZERO));
        // Bad type
        try {
            usFormat.format("1");
            fail("should not have tried to format a String");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void columnAlignmentTest() {
        OesFormat germany = OesFormat.getCoinInstance(2,OesFixedFormat.REPEATING_PLACES);
        char separator = germany.symbols().getDecimalSeparator();
        Coin[] rows = {MAX_MONEY, MAX_MONEY.subtract(SATOSHI), Coin.parseCoin("1234"),
                       COIN, COIN.add(SATOSHI), COIN.subtract(SATOSHI),
                        COIN.divide(1000).add(SATOSHI), COIN.divide(1000), COIN.divide(1000).subtract(SATOSHI),
                       valueOf(100), valueOf(1000), valueOf(10000),
                       SATOSHI};
        FieldPosition fp = new FieldPosition(DECIMAL_SEPARATOR);
        String[] output = new String[rows.length];
        int[] indexes = new int[rows.length];
        int maxIndex = 0;
        for (int i = 0; i < rows.length; i++) {
            output[i] = germany.format(rows[i], new StringBuffer(), fp).toString();
            indexes[i] = fp.getBeginIndex();
            if (indexes[i] > maxIndex) maxIndex = indexes[i];
        }
        for (int i = 0; i < output.length; i++) {
            // uncomment to watch printout
            // System.out.println(repeat(" ", (maxIndex - indexes[i])) + output[i]);
            assertEquals(output[i].indexOf(separator), indexes[i]);
        }
    }

    @Test
    public void repeatingPlaceTest() {
        OesFormat mega = OesFormat.getInstance(-6, US);
        Coin value = MAX_MONEY.subtract(SATOSHI);
        assertEquals("1,999.999999999999", mega.format(value, 0, OesFixedFormat.REPEATING_PLACES));
        assertEquals("1,999.999999999999", mega.format(value, 0, OesFixedFormat.REPEATING_PLACES));
        assertEquals("1,999.999999999999", mega.format(value, 1, OesFixedFormat.REPEATING_PLACES));
        assertEquals("1,999.999999999999", mega.format(value, 2, OesFixedFormat.REPEATING_PLACES));
        assertEquals("1,999.999999999999", mega.format(value, 3, OesFixedFormat.REPEATING_PLACES));
        assertEquals("1,999.999999999999", mega.format(value, 0, OesFixedFormat.REPEATING_DOUBLETS));
        assertEquals("1,999.999999999999", mega.format(value, 1, OesFixedFormat.REPEATING_DOUBLETS));
        assertEquals("1,999.999999999999", mega.format(value, 2, OesFixedFormat.REPEATING_DOUBLETS));
        assertEquals("1,999.999999999999", mega.format(value, 3, OesFixedFormat.REPEATING_DOUBLETS));
        assertEquals("1,999.999999999999", mega.format(value, 0, OesFixedFormat.REPEATING_TRIPLETS));
        assertEquals("1,999.999999999999", mega.format(value, 1, OesFixedFormat.REPEATING_TRIPLETS));
        assertEquals("1,999.999999999999", mega.format(value, 2, OesFixedFormat.REPEATING_TRIPLETS));
        assertEquals("1,999.999999999999", mega.format(value, 3, OesFixedFormat.REPEATING_TRIPLETS));
        assertEquals("1.000005", OesFormat.getCoinInstance(US).
                                   format(COIN.add(Coin.valueOf(5)), 0, OesFixedFormat.REPEATING_PLACES));
    }

    @Test
    public void characterIteratorTest() {
        OesFormat usFormat = OesFormat.getInstance(Locale.US);
        AttributedCharacterIterator i = usFormat.formatToCharacterIterator(parseCoin("1234.5"));
        java.util.Set<Attribute> a = i.getAllAttributeKeys();
        assertTrue("Missing currency attribute", a.contains(NumberFormat.Field.CURRENCY));
        assertTrue("Missing integer attribute", a.contains(NumberFormat.Field.INTEGER));
        assertTrue("Missing fraction attribute", a.contains(NumberFormat.Field.FRACTION));
        assertTrue("Missing decimal separator attribute", a.contains(NumberFormat.Field.DECIMAL_SEPARATOR));
        assertTrue("Missing grouping separator attribute", a.contains(NumberFormat.Field.GROUPING_SEPARATOR));
        assertTrue("Missing currency attribute", a.contains(NumberFormat.Field.CURRENCY));

        char c;
        i = OesFormat.getCodeInstance(Locale.US).formatToCharacterIterator(new BigDecimal("0.19246362747414458"));
        // formatted as "µOES 192,464"
        assertEquals(0, i.getBeginIndex());
        assertEquals(12, i.getEndIndex());
        int n = 0;
        for(c = i.first(); i.getAttribute(NumberFormat.Field.CURRENCY) != null; c = i.next()) {
            n++;
        }
        assertEquals(4, n);
        n = 0;
        for(i.next(); i.getAttribute(NumberFormat.Field.INTEGER) != null && i.getAttribute(NumberFormat.Field.GROUPING_SEPARATOR) != NumberFormat.Field.GROUPING_SEPARATOR; c = i.next()) {
            n++;
        }
        assertEquals(3, n);
        assertEquals(NumberFormat.Field.INTEGER, i.getAttribute(NumberFormat.Field.INTEGER));
        n = 0;
        for(c = i.next(); i.getAttribute(NumberFormat.Field.INTEGER) != null; c = i.next()) {
            n++;
        }
        assertEquals(3, n);

        // immutability check
        OesFormat fa = OesFormat.getSymbolInstance(US);
        OesFormat fb = OesFormat.getSymbolInstance(US);
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
        fa.formatToCharacterIterator(COIN.multiply(1000000));
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
        fb.formatToCharacterIterator(COIN.divide(1000000));
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
    }

    @Test
    public void parseTest() throws java.text.ParseException {
        OesFormat us = OesFormat.getSymbolInstance(Locale.US);
        OesFormat usCoded = OesFormat.getCodeInstance(Locale.US);
        // Coins
        assertEquals(valueOf(2000000), us.parseObject("OES2"));
        assertEquals(valueOf(2000000), us.parseObject("OES2"));
        assertEquals(valueOf(2000000), us.parseObject("S2"));
        assertEquals(valueOf(2000000), us.parseObject("S2"));
        assertEquals(valueOf(2000000), us.parseObject("2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2000000), us.parseObject("S2.0"));
        assertEquals(valueOf(2000000), us.parseObject("S2.0"));
        assertEquals(valueOf(2000000), us.parseObject("2.0"));
        assertEquals(valueOf(2000000), us.parseObject("OES2.0"));
        assertEquals(valueOf(2000000), us.parseObject("OES2.0"));
        assertEquals(valueOf(2000000), usCoded.parseObject("S 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("S 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject(" 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2022224200000L), us.parseObject("2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("S2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("S2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("OES2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("OES2,022,224.20"));
        assertEquals(valueOf(2202000000L), us.parseObject("2,202.0"));
        assertEquals(valueOf(21000000000000L), us.parseObject("21000000.000000"));
        // MilliCoins
        assertEquals(valueOf(2000), usCoded.parseObject("mOES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("mOES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("mS 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("mS 2"));
        assertEquals(valueOf(2000), us.parseObject("mOES2"));
        assertEquals(valueOf(2000), us.parseObject("mOES2"));
        assertEquals(valueOf(2000), us.parseObject("₥S2"));
        assertEquals(valueOf(2000), us.parseObject("₥S2"));
        assertEquals(valueOf(2000), us.parseObject("₥2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2.00"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2.00"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥S 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥S 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥ 2"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥S2,022,224"));
        assertEquals(valueOf(2022224200L), us.parseObject("₥S2,022,224.20"));
        assertEquals(valueOf(2022224000L), us.parseObject("mS2,022,224"));
        assertEquals(valueOf(2022224200L), us.parseObject("mS2,022,224.20"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥OES2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥OES2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("mOES2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("mOES2,022,224"));
        assertEquals(valueOf(2022224200L), us.parseObject("₥2,022,224.20"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥S 2,022,224"));
        assertEquals(valueOf(2022224200L), usCoded.parseObject("₥S 2,022,224.20"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("mS 2,022,224"));
        assertEquals(valueOf(2022224200L), usCoded.parseObject("mS 2,022,224.20"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥OES 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥OES 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("mOES 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("mOES 2,022,224"));
        assertEquals(valueOf(2022224200L), usCoded.parseObject("₥ 2,022,224.20"));
        // Microcoins
        assertEquals(valueOf(4), us.parseObject("µS4"));
        assertEquals(valueOf(4), us.parseObject("uS4"));
        assertEquals(valueOf(4), us.parseObject("uS4"));
        assertEquals(valueOf(4), us.parseObject("µS4"));
        assertEquals(valueOf(4), us.parseObject("uOES4"));
        assertEquals(valueOf(4), us.parseObject("uOES4"));
        assertEquals(valueOf(4), us.parseObject("µOES4"));
        assertEquals(valueOf(4), us.parseObject("µOES4"));
        assertEquals(valueOf(4), usCoded.parseObject("uOES 4"));
        assertEquals(valueOf(4), usCoded.parseObject("uOES 4"));
        assertEquals(valueOf(4), usCoded.parseObject("µOES 4"));
        assertEquals(valueOf(4), usCoded.parseObject("µOES 4"));
        // fractional satoshi; round up
        assertEquals(valueOf(5), us.parseObject("uOES4.5"));
        assertEquals(valueOf(5), us.parseObject("uOES4.5"));
        // negative with mu symbol
        assertEquals(valueOf(-1), usCoded.parseObject("(µS 1)"));
        assertEquals(valueOf(-10), us.parseObject("(µOES10)"));
        assertEquals(valueOf(-10), us.parseObject("(µOES10)"));

        // Same thing with addition of custom code, symbol
        us = OesFormat.builder().locale(US).style(SYMBOL).symbol("£").code("XYZ").build();
        usCoded = OesFormat.builder().locale(US).scale(0).symbol("£").code("XYZ").
                            pattern("¤ #,##0.00").build();
        // Coins
        assertEquals(valueOf(2000000), us.parseObject("XYZ2"));
        assertEquals(valueOf(2000000), us.parseObject("OES2"));
        assertEquals(valueOf(2000000), us.parseObject("OES2"));
        assertEquals(valueOf(2000000), us.parseObject("£2"));
        assertEquals(valueOf(2000000), us.parseObject("S2"));
        assertEquals(valueOf(2000000), us.parseObject("S2"));
        assertEquals(valueOf(2000000), us.parseObject("2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("XYZ 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2000000), us.parseObject("£2.0"));
        assertEquals(valueOf(2000000), us.parseObject("S2.0"));
        assertEquals(valueOf(2000000), us.parseObject("S2.0"));
        assertEquals(valueOf(2000000), us.parseObject("2.0"));
        assertEquals(valueOf(2000000), us.parseObject("XYZ2.0"));
        assertEquals(valueOf(2000000), us.parseObject("OES2.0"));
        assertEquals(valueOf(2000000), us.parseObject("OES2.0"));
        assertEquals(valueOf(2000000), usCoded.parseObject("£ 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("S 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("S 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject(" 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("XYZ 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2000000), usCoded.parseObject("OES 2"));
        assertEquals(valueOf(2022224200000L), us.parseObject("2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("£2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("S2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("S2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("XYZ2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("OES2,022,224.20"));
        assertEquals(valueOf(2022224200000L), us.parseObject("OES2,022,224.20"));
        assertEquals(valueOf(2202000000L), us.parseObject("2,202.0"));
        assertEquals(valueOf(21000000000000L), us.parseObject("21000000.00000000"));
        // MilliCoins
        assertEquals(valueOf(2000), usCoded.parseObject("mXYZ 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("mOES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("mOES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("m£ 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("mS 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("mS 2"));
        assertEquals(valueOf(2000), us.parseObject("mXYZ2"));
        assertEquals(valueOf(2000), us.parseObject("mOES2"));
        assertEquals(valueOf(2000), us.parseObject("mOES2"));
        assertEquals(valueOf(2000), us.parseObject("₥£2"));
        assertEquals(valueOf(2000), us.parseObject("₥S2"));
        assertEquals(valueOf(2000), us.parseObject("₥S2"));
        assertEquals(valueOf(2000), us.parseObject("₥2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥XYZ 2.00"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2.00"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2.00"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥XYZ 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥OES 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥£ 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥S 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥S 2"));
        assertEquals(valueOf(2000), usCoded.parseObject("₥ 2"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥£2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥S2,022,224"));
        assertEquals(valueOf(2022224200L), us.parseObject("₥S2,022,224.20"));
        assertEquals(valueOf(2022224000L), us.parseObject("m£2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("mS2,022,224"));
        assertEquals(valueOf(2022224200L), us.parseObject("mS2,022,224.20"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥XYZ2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥OES2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("₥OES2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("mXYZ2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("mOES2,022,224"));
        assertEquals(valueOf(2022224000L), us.parseObject("mOES2,022,224"));
        assertEquals(valueOf(2022224200L), us.parseObject("₥2,022,224.20"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥£ 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥S 2,022,224"));
        assertEquals(valueOf(2022224200L), usCoded.parseObject("₥S 2,022,224.20"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("m£ 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("mS 2,022,224"));
        assertEquals(valueOf(2022224200L), usCoded.parseObject("mS 2,022,224.20"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥XYZ 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥OES 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("₥OES 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("mXYZ 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("mOES 2,022,224"));
        assertEquals(valueOf(2022224000L), usCoded.parseObject("mOES 2,022,224"));
        assertEquals(valueOf(2022224200L), usCoded.parseObject("₥ 2,022,224.20"));
        // Microcoins
        assertEquals(valueOf(4), us.parseObject("µ£4"));
        assertEquals(valueOf(4), us.parseObject("µS4"));
        assertEquals(valueOf(4), us.parseObject("uS4"));
        assertEquals(valueOf(4), us.parseObject("u£4"));
        assertEquals(valueOf(4), us.parseObject("uS4"));
        assertEquals(valueOf(4), us.parseObject("µS4"));
        assertEquals(valueOf(4), us.parseObject("uXYZ4"));
        assertEquals(valueOf(4), us.parseObject("uOES4"));
        assertEquals(valueOf(4), us.parseObject("uOES4"));
        assertEquals(valueOf(4), us.parseObject("µXYZ4"));
        assertEquals(valueOf(4), us.parseObject("µOES4"));
        assertEquals(valueOf(4), us.parseObject("µOES4"));
        assertEquals(valueOf(4), usCoded.parseObject("uXYZ 4"));
        assertEquals(valueOf(4), usCoded.parseObject("uOES 4"));
        assertEquals(valueOf(4), usCoded.parseObject("uOES 4"));
        assertEquals(valueOf(4), usCoded.parseObject("µXYZ 4"));
        assertEquals(valueOf(4), usCoded.parseObject("µOES 4"));
        assertEquals(valueOf(4), usCoded.parseObject("µOES 4"));
        // fractional satoshi; round up
        assertEquals(valueOf(5), us.parseObject("uXYZ4.5"));
        assertEquals(valueOf(5), us.parseObject("uOES4.5"));
        assertEquals(valueOf(5), us.parseObject("uOES4.5"));
        // negative with mu symbol
        assertEquals(valueOf(-1), usCoded.parseObject("µ£ -1"));
        assertEquals(valueOf(-1), usCoded.parseObject("µS -1"));
        assertEquals(valueOf(-10), us.parseObject("(µXYZ10)"));
        assertEquals(valueOf(-10), us.parseObject("(µOES10)"));
        assertEquals(valueOf(-10), us.parseObject("(µOES10)"));

        // parse() method as opposed to parseObject
        try {
            OesFormat.getInstance().parse("abc");
            fail("bad parse must raise exception");
        } catch (ParseException e) {}
    }

    @Test
    public void parseMetricTest() throws ParseException {
        OesFormat cp = OesFormat.getCodeInstance(Locale.US);
        OesFormat sp = OesFormat.getSymbolInstance(Locale.US);
        // coin
        assertEquals(parseCoin("1"), cp.parseObject("OES 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("OES1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("S 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("S1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("S 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("S1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("S 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("S1.00"));
        // milli
        assertEquals(parseCoin("0.001"), cp.parseObject("mOES 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mOES1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("mS 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mS1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("mS 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mS1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("mS 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mS1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥OES 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥OES1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥S 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥S1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥S 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥S1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥S 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥S1.00"));
        // micro
        assertEquals(parseCoin("0.000001"), cp.parseObject("uOES 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uOES1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uS 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uS1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uS 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uS1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uS 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uS1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µOES 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µOES1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µS 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µS1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µS 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µS1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µS 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µS1.00"));
        // satoshi
        assertEquals(parseCoin("0.000001"), cp.parseObject("uOES 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uOES1"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uS 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uS1"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uS 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uS1"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uS 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uS1"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µOES 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µOES1"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µS 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µS1"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µS 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µS1"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µS 1"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µS1"));
        // cents
        assertEquals(parseCoin("0.012345"), cp.parseObject("cOES 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("cOES1.2345"));
        assertEquals(parseCoin("0.012345"), cp.parseObject("cS 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("cS1.2345"));
        assertEquals(parseCoin("0.012345"), cp.parseObject("cS 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("cS1.2345"));
        assertEquals(parseCoin("0.012345"), cp.parseObject("cS 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("cS1.2345"));
        assertEquals(parseCoin("0.012345"), cp.parseObject("¢OES 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("¢OES1.2345"));
        assertEquals(parseCoin("0.012345"), cp.parseObject("¢S 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("¢S1.2345"));
        assertEquals(parseCoin("0.012345"), cp.parseObject("¢S 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("¢S1.2345"));
        assertEquals(parseCoin("0.012345"), cp.parseObject("¢S 1.2345"));
        assertEquals(parseCoin("0.012345"), sp.parseObject("¢S1.2345"));
        // dekacoins
        assertEquals(parseCoin("12.34567"), cp.parseObject("daOES 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daOES1.234567"));
        assertEquals(parseCoin("12.34567"), cp.parseObject("daS 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daS1.234567"));
        assertEquals(parseCoin("12.34567"), cp.parseObject("daS 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daS1.234567"));
        assertEquals(parseCoin("12.34567"), cp.parseObject("daS 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daS1.234567"));
        // hectocoins
        assertEquals(parseCoin("123.4567"), cp.parseObject("hOES 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hOES1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("hS 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hS1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("hS 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hS1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("hS 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hS1.234567"));
        // kilocoins
        assertEquals(parseCoin("1234.567"), cp.parseObject("kOES 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kOES1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("kS 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kS1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("kS 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kS1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("kS 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kS1.234567"));
        // megacoins
        assertEquals(parseCoin("1234567"), cp.parseObject("MOES 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MOES1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("MS 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MS1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("MS 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MS1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("MS 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MS1.234567"));
    }

    @Test
    public void parsePositionTest() {
        OesFormat usCoded = OesFormat.getCodeInstance(Locale.US);
        // Test the field constants
        FieldPosition intField = new FieldPosition(NumberFormat.Field.INTEGER);
        assertEquals(
          "987",
          usCoded.format(valueOf(987650000L), new StringBuffer(), intField).
          substring(intField.getBeginIndex(), intField.getEndIndex())
        );
        FieldPosition fracField = new FieldPosition(NumberFormat.Field.FRACTION);
        assertEquals(
          "65",
          usCoded.format(valueOf(987650000L), new StringBuffer(), fracField).
          substring(fracField.getBeginIndex(), fracField.getEndIndex())
        );

        // for currency we use a locale that puts the units at the end
        OesFormat de = OesFormat.getSymbolInstance(Locale.GERMANY);
        OesFormat deCoded = OesFormat.getCodeInstance(Locale.GERMANY);
        FieldPosition currField = new FieldPosition(NumberFormat.Field.CURRENCY);
        assertEquals(
          "µS",
          de.format(valueOf(987654321L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "µOES",
          deCoded.format(valueOf(987654321L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "₥S",
          de.format(valueOf(987654000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "mOES",
          deCoded.format(valueOf(987654000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "S",
          de.format(valueOf(987000000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "OES",
          deCoded.format(valueOf(987000000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
    }

    @Test
    public void currencyCodeTest() {
        /* Insert needed space AFTER currency-code */
        OesFormat usCoded = OesFormat.getCodeInstance(Locale.US);
        assertEquals("µOES 1", usCoded.format(1));
        assertEquals("OES 1.00", usCoded.format(COIN));

        /* Do not insert unneeded space BEFORE currency-code */
        OesFormat frCoded = OesFormat.getCodeInstance(Locale.FRANCE);
        assertEquals("1 µOES", frCoded.format(1));
        assertEquals("1,00 OES", frCoded.format(COIN));

        /* Insert needed space BEFORE currency-code: no known currency pattern does this? */

        /* Do not insert unneeded space AFTER currency-code */
        OesFormat deCoded = OesFormat.getCodeInstance(Locale.ITALY);
        assertEquals("µOES 1", deCoded.format(1));
        assertEquals("OES 1,00", deCoded.format(COIN));
    }

    @Test
    public void coinScaleTest() throws Exception {
        OesFormat coinFormat = OesFormat.getCoinInstance(Locale.US);
        assertEquals("1.00", coinFormat.format(Coin.COIN));
        assertEquals("-1.00", coinFormat.format(Coin.COIN.negate()));
        assertEquals(Coin.parseCoin("1"), coinFormat.parseObject("1.00"));
        assertEquals(valueOf(10000), coinFormat.parseObject("0.01"));
        assertEquals(Coin.parseCoin("1000"), coinFormat.parseObject("1,000.00"));
        assertEquals(Coin.parseCoin("1000"), coinFormat.parseObject("1000"));
    }

    @Test
    public void millicoinScaleTest() throws Exception {
        OesFormat coinFormat = OesFormat.getMilliInstance(Locale.US);
        assertEquals("1,000.00", coinFormat.format(Coin.COIN));
        assertEquals("-1,000.00", coinFormat.format(Coin.COIN.negate()));
        assertEquals(Coin.parseCoin("0.001"), coinFormat.parseObject("1.00"));
        assertEquals(valueOf(10), coinFormat.parseObject("0.01"));
        assertEquals(Coin.parseCoin("1"), coinFormat.parseObject("1,000.00"));
        assertEquals(Coin.parseCoin("1"), coinFormat.parseObject("1000"));
    }

    @Test
    public void microcoinScaleTest() throws Exception {
        OesFormat coinFormat = OesFormat.getMicroInstance(Locale.US);
        assertEquals("1,000,000", coinFormat.format(Coin.COIN));
        assertEquals("-1,000,000", coinFormat.format(Coin.COIN.negate()));
        assertEquals("1,000,010", coinFormat.format(Coin.COIN.add(valueOf(10))));
        assertEquals(Coin.parseCoin("0.000001"), coinFormat.parseObject("1.00"));
        assertEquals(valueOf(1), coinFormat.parseObject("1"));
        assertEquals(Coin.parseCoin("0.001"), coinFormat.parseObject("1,000"));
        assertEquals(Coin.parseCoin("0.001"), coinFormat.parseObject("1000"));
    }

    @Test
    public void testGrouping() throws Exception {
        OesFormat usCoin = OesFormat.getInstance(0, Locale.US, 1, 2, 3);
        assertEquals("0.1", usCoin.format(Coin.parseCoin("0.1")));
        assertEquals("0.010", usCoin.format(Coin.parseCoin("0.01")));
        assertEquals("0.001", usCoin.format(Coin.parseCoin("0.001")));
        assertEquals("0.000100", usCoin.format(Coin.parseCoin("0.0001")));
        assertEquals("0.000010", usCoin.format(Coin.parseCoin("0.00001")));
        assertEquals("0.000001", usCoin.format(Coin.parseCoin("0.000001")));

        // no more than two fractional decimal places for the default coin-denomination
        assertEquals("0.01", OesFormat.getCoinInstance(Locale.US).format(Coin.parseCoin("0.005")));

        OesFormat usMilli = OesFormat.getInstance(3, Locale.US, 1, 2, 3);
        assertEquals("0.1", usMilli.format(Coin.parseCoin("0.0001")));
        assertEquals("0.010", usMilli.format(Coin.parseCoin("0.00001")));
        assertEquals("0.001", usMilli.format(Coin.parseCoin("0.000001")));
        // even though last group is 3, that would result in fractional satoshis, which we don't do
        assertEquals("0.010", usMilli.format(Coin.valueOf(10)));
        assertEquals("0.001", usMilli.format(Coin.valueOf(1)));

        OesFormat usMicro = OesFormat.getInstance(6, Locale.US, 1, 2, 3);
        assertEquals("10", usMicro.format(Coin.valueOf(10)));
        // even though second group is 2, that would result in fractional satoshis, which we don't do
        assertEquals("1", usMicro.format(Coin.valueOf(1)));
    }


    /* These just make sure factory methods don't raise exceptions.
     * Other tests inspect their return values. */
    @Test
    public void factoryTest() {
        OesFormat coded = OesFormat.getInstance(0, 1, 2, 3);
        OesFormat.getInstance(OesAutoFormat.Style.CODE);
        OesAutoFormat symbolic = (OesAutoFormat)OesFormat.getInstance(OesAutoFormat.Style.SYMBOL);
        assertEquals(2, symbolic.fractionPlaces());
        OesFormat.getInstance(OesAutoFormat.Style.CODE, 3);
        assertEquals(3, ((OesAutoFormat)OesFormat.getInstance(OesAutoFormat.Style.SYMBOL, 3)).fractionPlaces());
        OesFormat.getInstance(OesAutoFormat.Style.SYMBOL, Locale.US, 3);
        OesFormat.getInstance(OesAutoFormat.Style.CODE, Locale.US);
        OesFormat.getInstance(OesAutoFormat.Style.SYMBOL, Locale.US);
        OesFormat.getCoinInstance(2, OesFixedFormat.REPEATING_PLACES);
        OesFormat.getMilliInstance(1, 2, 3);
        OesFormat.getInstance(2);
        OesFormat.getInstance(2, Locale.US);
        OesFormat.getCodeInstance(3);
        OesFormat.getSymbolInstance(3);
        OesFormat.getCodeInstance(Locale.US, 3);
        OesFormat.getSymbolInstance(Locale.US, 3);
        try {
            OesFormat.getInstance(SMALLEST_UNIT_EXPONENT + 1);
            fail("should not have constructed an instance with denomination less than satoshi");
        } catch (IllegalArgumentException e) {}
    }
    @Test
    public void factoryArgumentsTest() {
        Locale locale;
        if (Locale.getDefault().equals(GERMANY)) locale = FRANCE;
        else locale = GERMANY;
        assertEquals(OesFormat.getInstance(), OesFormat.getCodeInstance());
        assertEquals(OesFormat.getInstance(locale), OesFormat.getCodeInstance(locale));
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.CODE), OesFormat.getCodeInstance());
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.SYMBOL), OesFormat.getSymbolInstance());
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.CODE,3), OesFormat.getCodeInstance(3));
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.SYMBOL,3), OesFormat.getSymbolInstance(3));
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.CODE,locale), OesFormat.getCodeInstance(locale));
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.SYMBOL,locale), OesFormat.getSymbolInstance(locale));
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.CODE,locale,3), OesFormat.getCodeInstance(locale,3));
        assertEquals(OesFormat.getInstance(OesAutoFormat.Style.SYMBOL,locale,3), OesFormat.getSymbolInstance(locale,3));
        assertEquals(OesFormat.getCoinInstance(), OesFormat.getInstance(0));
        assertEquals(OesFormat.getMilliInstance(), OesFormat.getInstance(3));
        assertEquals(OesFormat.getMicroInstance(), OesFormat.getInstance(6));
        assertEquals(OesFormat.getCoinInstance(3), OesFormat.getInstance(0,3));
        assertEquals(OesFormat.getMilliInstance(3), OesFormat.getInstance(3,3));
        assertEquals(OesFormat.getMicroInstance(3), OesFormat.getInstance(6,3));
        assertEquals(OesFormat.getCoinInstance(3,4,5), OesFormat.getInstance(0,3,4,5));
        assertEquals(OesFormat.getMilliInstance(3,4,5), OesFormat.getInstance(3,3,4,5));
        assertEquals(OesFormat.getMicroInstance(3,4,5), OesFormat.getInstance(6,3,4,5));
        assertEquals(OesFormat.getCoinInstance(locale), OesFormat.getInstance(0,locale));
        assertEquals(OesFormat.getMilliInstance(locale), OesFormat.getInstance(3,locale));
        assertEquals(OesFormat.getMicroInstance(locale), OesFormat.getInstance(6,locale));
        assertEquals(OesFormat.getCoinInstance(locale,4,5), OesFormat.getInstance(0,locale,4,5));
        assertEquals(OesFormat.getMilliInstance(locale,4,5), OesFormat.getInstance(3,locale,4,5));
        assertEquals(OesFormat.getMicroInstance(locale,4,5), OesFormat.getInstance(6,locale,4,5));
    }

    @Test
    public void autoDecimalTest() {
        OesFormat codedZero = OesFormat.getCodeInstance(Locale.US, 0);
        OesFormat symbolZero = OesFormat.getSymbolInstance(Locale.US, 0);
        assertEquals("S1", symbolZero.format(COIN));
        assertEquals("OES 1", codedZero.format(COIN));
        assertEquals("µS999,999", symbolZero.format(COIN.subtract(SATOSHI)));
        assertEquals("µOES 999,999", codedZero.format(COIN.subtract(SATOSHI)));
        assertEquals("µS999,950", symbolZero.format(COIN.subtract(Coin.valueOf(50))));
        assertEquals("µOES 999,950", codedZero.format(COIN.subtract(Coin.valueOf(50))));
        assertEquals("µS999,949", symbolZero.format(COIN.subtract(Coin.valueOf(51))));
        assertEquals("µOES 999,949", codedZero.format(COIN.subtract(Coin.valueOf(51))));
        assertEquals("S1,000", symbolZero.format(COIN.multiply(1000)));
        assertEquals("OES 1,000", codedZero.format(COIN.multiply(1000)));
        assertEquals("µS100", symbolZero.format(Coin.valueOf(100)));
        assertEquals("µOES 100", codedZero.format(Coin.valueOf(100)));
        assertEquals("µS50", symbolZero.format(Coin.valueOf(50)));
        assertEquals("µOES 50", codedZero.format(Coin.valueOf(50)));
        assertEquals("µS49", symbolZero.format(Coin.valueOf(49)));
        assertEquals("µOES 49", codedZero.format(Coin.valueOf(49)));
        assertEquals("µS1", symbolZero.format(Coin.valueOf(1)));
        assertEquals("µOES 1", codedZero.format(Coin.valueOf(1)));
        assertEquals("µS49,999,999", symbolZero.format(Coin.valueOf(49999999)));
        assertEquals("µOES 49,999,999", codedZero.format(Coin.valueOf(49999999)));

        assertEquals("µS499,500", symbolZero.format(Coin.valueOf(499500)));
        assertEquals("µOES 499,500", codedZero.format(Coin.valueOf(499500)));
        assertEquals("µS499,499", symbolZero.format(Coin.valueOf(499499)));
        assertEquals("µOES 499,499", codedZero.format(Coin.valueOf(499499)));
        assertEquals("µS500,490", symbolZero.format(Coin.valueOf(500490)));
        assertEquals("µOES 500,490", codedZero.format(Coin.valueOf(500490)));

        OesFormat codedTwo = OesFormat.getCodeInstance(Locale.US, 2);
        OesFormat symbolTwo = OesFormat.getSymbolInstance(Locale.US, 2);
        assertEquals("S1.00", symbolTwo.format(COIN));
        assertEquals("OES 1.00", codedTwo.format(COIN));
        assertEquals("µS999,999", symbolTwo.format(COIN.subtract(SATOSHI)));
        assertEquals("µOES 999,999", codedTwo.format(COIN.subtract(SATOSHI)));
        assertEquals("S1,000.00", symbolTwo.format(COIN.multiply(1000)));
        assertEquals("OES 1,000.00", codedTwo.format(COIN.multiply(1000)));
        assertEquals("₥S0.10", symbolTwo.format(Coin.valueOf(100)));
        assertEquals("mOES 0.10", codedTwo.format(Coin.valueOf(100)));
        assertEquals("₥S0.05", symbolTwo.format(Coin.valueOf(50)));
        assertEquals("mOES 0.05", codedTwo.format(Coin.valueOf(50)));
        assertEquals("µS49", symbolTwo.format(Coin.valueOf(49)));
        assertEquals("µOES 49", codedTwo.format(Coin.valueOf(49)));
        assertEquals("µS1", symbolTwo.format(Coin.valueOf(1)));
        assertEquals("µOES 1", codedTwo.format(Coin.valueOf(1)));

        OesFormat codedThree = OesFormat.getCodeInstance(Locale.US, 3);
        OesFormat symbolThree = OesFormat.getSymbolInstance(Locale.US, 3);
        assertEquals("S1.000", symbolThree.format(COIN));
        assertEquals("OES 1.000", codedThree.format(COIN));
        assertEquals("₥S999.999", symbolThree.format(COIN.subtract(SATOSHI)));
        assertEquals("mOES 999.999", codedThree.format(COIN.subtract(SATOSHI)));
        assertEquals("S1,000.000", symbolThree.format(COIN.multiply(1000)));
        assertEquals("OES 1,000.000", codedThree.format(COIN.multiply(1000)));
        assertEquals("₥S0.100", symbolThree.format(Coin.valueOf(100)));
        assertEquals("mOES 0.100", codedThree.format(Coin.valueOf(100)));
        assertEquals("₥S0.050", symbolThree.format(Coin.valueOf(50)));
        assertEquals("mOES 0.050", codedThree.format(Coin.valueOf(50)));
        assertEquals("₥S0.049", symbolThree.format(Coin.valueOf(49)));
        assertEquals("mOES 0.049", codedThree.format(Coin.valueOf(49)));
        assertEquals("₥S0.001", symbolThree.format(Coin.valueOf(1)));
        assertEquals("mOES 0.001", codedThree.format(Coin.valueOf(1)));
    }


    @Test
    public void symbolsCodesTest() {
        OesFixedFormat coin = (OesFixedFormat)OesFormat.getCoinInstance(US);
        assertEquals("OES", coin.code());
        assertEquals("S", coin.symbol());
        OesFixedFormat cent = (OesFixedFormat)OesFormat.getInstance(2, US);
        assertEquals("cOES", cent.code());
        assertEquals("¢S", cent.symbol());
        OesFixedFormat milli = (OesFixedFormat)OesFormat.getInstance(3, US);
        assertEquals("mOES", milli.code());
        assertEquals("₥S", milli.symbol());
        OesFixedFormat micro = (OesFixedFormat)OesFormat.getInstance(6, US);
        assertEquals("µOES", micro.code());
        assertEquals("µS", micro.symbol());
        OesFixedFormat deka = (OesFixedFormat)OesFormat.getInstance(-1, US);
        assertEquals("daOES", deka.code());
        assertEquals("daS", deka.symbol());
        OesFixedFormat hecto = (OesFixedFormat)OesFormat.getInstance(-2, US);
        assertEquals("hOES", hecto.code());
        assertEquals("hS", hecto.symbol());
        OesFixedFormat kilo = (OesFixedFormat)OesFormat.getInstance(-3, US);
        assertEquals("kOES", kilo.code());
        assertEquals("kS", kilo.symbol());
        OesFixedFormat mega = (OesFixedFormat)OesFormat.getInstance(-6, US);
        assertEquals("MOES", mega.code());
        assertEquals("MS", mega.symbol());
        OesFixedFormat noSymbol = (OesFixedFormat)OesFormat.getInstance(4, US);
        try {
            noSymbol.symbol();
            fail("non-standard denomination has no symbol()");
        } catch (IllegalStateException e) {}
        try {
            noSymbol.code();
            fail("non-standard denomination has no code()");
        } catch (IllegalStateException e) {}

        OesFixedFormat symbolCoin = (OesFixedFormat)OesFormat.builder().locale(US).scale(0).
                                                              symbol("\u0053").build();
        assertEquals("OES", symbolCoin.code());
        assertEquals("S", symbolCoin.symbol());
        OesFixedFormat symbolCent = (OesFixedFormat)OesFormat.builder().locale(US).scale(2).
                                                              symbol("\u0053").build();
        assertEquals("cOES", symbolCent.code());
        assertEquals("¢S", symbolCent.symbol());
        OesFixedFormat symbolMilli = (OesFixedFormat)OesFormat.builder().locale(US).scale(3).
                                                               symbol("\u0053").build();
        assertEquals("mOES", symbolMilli.code());
        assertEquals("₥S", symbolMilli.symbol());
        OesFixedFormat symbolMicro = (OesFixedFormat)OesFormat.builder().locale(US).scale(6).
                                                               symbol("\u0053").build();
        assertEquals("µOES", symbolMicro.code());
        assertEquals("µS", symbolMicro.symbol());
        OesFixedFormat symbolDeka = (OesFixedFormat)OesFormat.builder().locale(US).scale(-1).
                                                              symbol("\u0053").build();
        assertEquals("daOES", symbolDeka.code());
        assertEquals("daS", symbolDeka.symbol());
        OesFixedFormat symbolHecto = (OesFixedFormat)OesFormat.builder().locale(US).scale(-2).
                                                               symbol("\u0053").build();
        assertEquals("hOES", symbolHecto.code());
        assertEquals("hS", symbolHecto.symbol());
        OesFixedFormat symbolKilo = (OesFixedFormat)OesFormat.builder().locale(US).scale(-3).
                                                              symbol("\u0053").build();
        assertEquals("kOES", symbolKilo.code());
        assertEquals("kS", symbolKilo.symbol());
        OesFixedFormat symbolMega = (OesFixedFormat)OesFormat.builder().locale(US).scale(-6).
                                                              symbol("\u0053").build();
        assertEquals("MOES", symbolMega.code());
        assertEquals("MS", symbolMega.symbol());

        OesFixedFormat codeCoin = (OesFixedFormat)OesFormat.builder().locale(US).scale(0).
                                                            code("OES").build();
        assertEquals("OES", codeCoin.code());
        assertEquals("S", codeCoin.symbol());
        OesFixedFormat codeCent = (OesFixedFormat)OesFormat.builder().locale(US).scale(2).
                                                            code("OES").build();
        assertEquals("cOES", codeCent.code());
        assertEquals("¢S", codeCent.symbol());
        OesFixedFormat codeMilli = (OesFixedFormat)OesFormat.builder().locale(US).scale(3).
                                                             code("OES").build();
        assertEquals("mOES", codeMilli.code());
        assertEquals("₥S", codeMilli.symbol());
        OesFixedFormat codeMicro = (OesFixedFormat)OesFormat.builder().locale(US).scale(6).
                                                             code("OES").build();
        assertEquals("µOES", codeMicro.code());
        assertEquals("µS", codeMicro.symbol());
        OesFixedFormat codeDeka = (OesFixedFormat)OesFormat.builder().locale(US).scale(-1).
                                                            code("OES").build();
        assertEquals("daOES", codeDeka.code());
        assertEquals("daS", codeDeka.symbol());
        OesFixedFormat codeHecto = (OesFixedFormat)OesFormat.builder().locale(US).scale(-2).
                                                             code("OES").build();
        assertEquals("hOES", codeHecto.code());
        assertEquals("hS", codeHecto.symbol());
        OesFixedFormat codeKilo = (OesFixedFormat)OesFormat.builder().locale(US).scale(-3).
                                                            code("OES").build();
        assertEquals("kOES", codeKilo.code());
        assertEquals("kS", codeKilo.symbol());
        OesFixedFormat codeMega = (OesFixedFormat)OesFormat.builder().locale(US).scale(-6).
                                                            code("OES").build();
        assertEquals("MOES", codeMega.code());
        assertEquals("MS", codeMega.symbol());

        OesFixedFormat symbolCodeCoin = (OesFixedFormat)OesFormat.builder().locale(US).scale(0).
                                                                  symbol("\u0053").code("OES").build();
        assertEquals("OES", symbolCodeCoin.code());
        assertEquals("S", symbolCodeCoin.symbol());
        OesFixedFormat symbolCodeCent = (OesFixedFormat)OesFormat.builder().locale(US).scale(2).
                                                                  symbol("\u0053").code("OES").build();
        assertEquals("cOES", symbolCodeCent.code());
        assertEquals("¢S", symbolCodeCent.symbol());
        OesFixedFormat symbolCodeMilli = (OesFixedFormat)OesFormat.builder().locale(US).scale(3).
                                                                   symbol("\u0053").code("OES").build();
        assertEquals("mOES", symbolCodeMilli.code());
        assertEquals("₥S", symbolCodeMilli.symbol());
        OesFixedFormat symbolCodeMicro = (OesFixedFormat)OesFormat.builder().locale(US).scale(6).
                                                                   symbol("\u0053").code("OES").build();
        assertEquals("µOES", symbolCodeMicro.code());
        assertEquals("µS", symbolCodeMicro.symbol());
        OesFixedFormat symbolCodeDeka = (OesFixedFormat)OesFormat.builder().locale(US).scale(-1).
                                                                  symbol("\u0053").code("OES").build();
        assertEquals("daOES", symbolCodeDeka.code());
        assertEquals("daS", symbolCodeDeka.symbol());
        OesFixedFormat symbolCodeHecto = (OesFixedFormat)OesFormat.builder().locale(US).scale(-2).
                                                                   symbol("\u0053").code("OES").build();
        assertEquals("hOES", symbolCodeHecto.code());
        assertEquals("hS", symbolCodeHecto.symbol());
        OesFixedFormat symbolCodeKilo = (OesFixedFormat)OesFormat.builder().locale(US).scale(-3).
                                                                  symbol("\u0053").code("OES").build();
        assertEquals("kOES", symbolCodeKilo.code());
        assertEquals("kS", symbolCodeKilo.symbol());
        OesFixedFormat symbolCodeMega = (OesFixedFormat)OesFormat.builder().locale(US).scale(-6).
                                                                  symbol("\u0053").code("OES").build();
        assertEquals("MOES", symbolCodeMega.code());
        assertEquals("MS", symbolCodeMega.symbol());
    }

    /* copied from CoinFormatTest.java and modified */
    @Test
    public void parse() throws Exception {
        OesFormat coin = OesFormat.getCoinInstance(Locale.US);
        assertEquals(Coin.COIN, coin.parseObject("1"));
        assertEquals(Coin.COIN, coin.parseObject("1."));
        assertEquals(Coin.COIN, coin.parseObject("1.0"));
        assertEquals(Coin.COIN, OesFormat.getCoinInstance(Locale.GERMANY).parseObject("1,0"));
        assertEquals(Coin.COIN, coin.parseObject("01.0000000000"));
        // TODO work with express positive sign
        // assertEquals(Coin.COIN, coin.parseObject("+1.0"));
        assertEquals(Coin.COIN.negate(), coin.parseObject("-1"));
        assertEquals(Coin.COIN.negate(), coin.parseObject("-1.0"));

        assertEquals(Coin.CENT, coin.parseObject(".01"));

        OesFormat milli = OesFormat.getMilliInstance(Locale.US);
        assertEquals(Coin.MILLICOIN, milli.parseObject("1"));
        assertEquals(Coin.MILLICOIN, milli.parseObject("1.0"));
        assertEquals(Coin.MILLICOIN, milli.parseObject("01.0000000000"));
        // TODO work with express positive sign
        //assertEquals(Coin.MILLICOIN, milli.parseObject("+1.0"));
        assertEquals(Coin.MILLICOIN.negate(), milli.parseObject("-1"));
        assertEquals(Coin.MILLICOIN.negate(), milli.parseObject("-1.0"));

        OesFormat micro = OesFormat.getMicroInstance(Locale.US);
        assertEquals(Coin.MICROCOIN, micro.parseObject("1"));
        assertEquals(Coin.MICROCOIN, micro.parseObject("1.0"));
        assertEquals(Coin.MICROCOIN, micro.parseObject("01.0000000000"));
        // TODO work with express positive sign
        // assertEquals(Coin.MICROCOIN, micro.parseObject("+1.0"));
        assertEquals(Coin.MICROCOIN.negate(), micro.parseObject("-1"));
        assertEquals(Coin.MICROCOIN.negate(), micro.parseObject("-1.0"));
    }

    /* Copied (and modified) from CoinFormatTest.java */
    @Test
    public void oesRounding() throws Exception {
        OesFormat coinFormat = OesFormat.getCoinInstance(Locale.US);
        assertEquals("0", OesFormat.getCoinInstance(Locale.US, 0).format(ZERO));
        assertEquals("0", coinFormat.format(ZERO, 0));
        assertEquals("0.00", OesFormat.getCoinInstance(Locale.US, 2).format(ZERO));
        assertEquals("0.00", coinFormat.format(ZERO, 2));

        assertEquals("1", OesFormat.getCoinInstance(Locale.US, 0).format(COIN));
        assertEquals("1", coinFormat.format(COIN, 0));
        assertEquals("1.0", OesFormat.getCoinInstance(Locale.US, 1).format(COIN));
        assertEquals("1.0", coinFormat.format(COIN, 1));
        assertEquals("1.00", OesFormat.getCoinInstance(Locale.US, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2));
        assertEquals("1.00", OesFormat.getCoinInstance(Locale.US, 2, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2, 2));
        assertEquals("1.00", OesFormat.getCoinInstance(Locale.US, 2, 2, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2, 2, 2));
        assertEquals("1.000", OesFormat.getCoinInstance(Locale.US, 3).format(COIN));
        assertEquals("1.000", coinFormat.format(COIN, 3));
        assertEquals("1.0000", OesFormat.getCoinInstance(US, 4).format(COIN));
        assertEquals("1.0000", coinFormat.format(COIN, 4));

        final Coin justNot = COIN.subtract(SATOSHI);
        assertEquals("1", OesFormat.getCoinInstance(US, 0).format(justNot));
        assertEquals("1", coinFormat.format(justNot, 0));
        assertEquals("1.0", OesFormat.getCoinInstance(US, 1).format(justNot));
        assertEquals("1.0", coinFormat.format(justNot, 1));
        final Coin justNotUnder = Coin.valueOf(999950);
        assertEquals("1.00", OesFormat.getCoinInstance(US, 2, 2).format(justNot));
        assertEquals("1.00", coinFormat.format(justNot, 2, 2));
        assertEquals("1.00", OesFormat.getCoinInstance(US, 2, 2).format(justNotUnder));
        assertEquals("1.00", coinFormat.format(justNotUnder, 2, 2));
        assertEquals("1.00", OesFormat.getCoinInstance(US, 2, 2).format(justNot));
        assertEquals("1.00", coinFormat.format(justNot, 2, 2));
        assertEquals("0.999950", OesFormat.getCoinInstance(US, 2, 2, 2).format(justNotUnder));
        assertEquals("0.999950", coinFormat.format(justNotUnder, 2, 2, 2));
        assertEquals("0.999999", OesFormat.getCoinInstance(US, 2, 2, 2).format(justNot));
        assertEquals("0.999999", coinFormat.format(justNot, 2, 2, 2));
        assertEquals("0.999999", OesFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(justNot));
        assertEquals("0.999999", coinFormat.format(justNot, 2, REPEATING_DOUBLETS));
        assertEquals("0.999950", OesFormat.getCoinInstance(US, 2, 2, 2).format(justNotUnder));
        assertEquals("0.999950", coinFormat.format(justNotUnder, 2, 2, 2));
        assertEquals("0.999950", OesFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(justNotUnder));
        assertEquals("0.999950", coinFormat.format(justNotUnder, 2, REPEATING_DOUBLETS));
        assertEquals("1.000", OesFormat.getCoinInstance(US, 3).format(justNot));
        assertEquals("1.000", coinFormat.format(justNot, 3));
        assertEquals("1.0000", OesFormat.getCoinInstance(US, 4).format(justNot));
        assertEquals("1.0000", coinFormat.format(justNot, 4));

        final Coin slightlyMore = COIN.add(SATOSHI);
        assertEquals("1", OesFormat.getCoinInstance(US, 0).format(slightlyMore));
        assertEquals("1", coinFormat.format(slightlyMore, 0));
        assertEquals("1.0", OesFormat.getCoinInstance(US, 1).format(slightlyMore));
        assertEquals("1.0", coinFormat.format(slightlyMore, 1));
        assertEquals("1.00", OesFormat.getCoinInstance(US, 2, 2).format(slightlyMore));
        assertEquals("1.00", coinFormat.format(slightlyMore, 2, 2));
        assertEquals("1.000001", OesFormat.getCoinInstance(US, 2, 2, 2).format(slightlyMore));
        assertEquals("1.000001", coinFormat.format(slightlyMore, 2, 2, 2));
        assertEquals("1.000", OesFormat.getCoinInstance(US, 3).format(slightlyMore));
        assertEquals("1.000", coinFormat.format(slightlyMore, 3));
        assertEquals("1.0000", OesFormat.getCoinInstance(US, 4).format(slightlyMore));
        assertEquals("1.0000", coinFormat.format(slightlyMore, 4));

        final Coin pivot = COIN.add(SATOSHI.multiply(5));
        assertEquals("1.000005", OesFormat.getCoinInstance(US, 6).format(pivot));
        assertEquals("1.000005", coinFormat.format(pivot, 6));
        assertEquals("1.000005", OesFormat.getCoinInstance(US, 5, 1).format(pivot));
        assertEquals("1.000005", coinFormat.format(pivot, 5, 1));
        assertEquals("1.00001", OesFormat.getCoinInstance(US, 5).format(pivot));
        assertEquals("1.00001", coinFormat.format(pivot, 5));

        final Coin value = Coin.valueOf(11223344556677l);
        assertEquals("11,223,345", OesFormat.getCoinInstance(US, 0).format(value));
        assertEquals("11,223,345", coinFormat.format(value, 0));
        assertEquals("11,223,344.6", OesFormat.getCoinInstance(US, 1).format(value));
        assertEquals("11,223,344.6", coinFormat.format(value, 1));
        assertEquals("11,223,344.5567", OesFormat.getCoinInstance(US, 2, 2).format(value));
        assertEquals("11,223,344.5567", coinFormat.format(value, 2, 2));
        assertEquals("11,223,344.556677", OesFormat.getCoinInstance(US, 2, 2, 2).format(value));
        assertEquals("11,223,344.556677", coinFormat.format(value, 2, 2, 2));
        assertEquals("11,223,344.557", OesFormat.getCoinInstance(US, 3).format(value));
        assertEquals("11,223,344.557", coinFormat.format(value, 3));
        assertEquals("11,223,344.5567", OesFormat.getCoinInstance(US, 4).format(value));
        assertEquals("11,223,344.5567", coinFormat.format(value, 4));

        OesFormat megaFormat = OesFormat.getInstance(-6, US);
        assertEquals("2,000.00", megaFormat.format(MAX_MONEY));
        assertEquals("2,000", megaFormat.format(MAX_MONEY, 0));
        assertEquals("11.223344556677", megaFormat.format(value, 0, REPEATING_DOUBLETS));
    }

    @Test
    public void negativeTest() throws Exception {
        assertEquals("-1,00 OES", OesFormat.getInstance(FRANCE).format(COIN.multiply(-1)));
        assertEquals("OES -1,00", OesFormat.getInstance(ITALY).format(COIN.multiply(-1)));
        assertEquals("S -1,00", OesFormat.getSymbolInstance(ITALY).format(COIN.multiply(-1)));
        assertEquals("OES -1.00", OesFormat.getInstance(JAPAN).format(COIN.multiply(-1)));
        assertEquals("S-1.00", OesFormat.getSymbolInstance(JAPAN).format(COIN.multiply(-1)));
        assertEquals("(OES 1.00)", OesFormat.getInstance(US).format(COIN.multiply(-1)));
        assertEquals("(S1.00)", OesFormat.getSymbolInstance(US).format(COIN.multiply(-1)));
        // assertEquals("OES -१.००", OesFormat.getInstance(Locale.forLanguageTag("hi-IN")).format(COIN.multiply(-1)));
        assertEquals("OES -๑.๐๐", OesFormat.getInstance(new Locale("th","TH","TH")).format(COIN.multiply(-1)));
        assertEquals("S-๑.๐๐", OesFormat.getSymbolInstance(new Locale("th","TH","TH")).format(COIN.multiply(-1)));
    }

    /* Warning: these tests assume the state of Locale data extant on the platform on which
     * they were written: openjdk 7u21-2.3.9-5 */
    @Test
    public void equalityTest() throws Exception {
        // First, autodenominator
        assertEquals(OesFormat.getInstance(), OesFormat.getInstance());
        assertEquals(OesFormat.getInstance().hashCode(), OesFormat.getInstance().hashCode());

        assertNotEquals(OesFormat.getCodeInstance(), OesFormat.getSymbolInstance());
        assertNotEquals(OesFormat.getCodeInstance().hashCode(), OesFormat.getSymbolInstance().hashCode());

        assertEquals(OesFormat.getSymbolInstance(5), OesFormat.getSymbolInstance(5));
        assertEquals(OesFormat.getSymbolInstance(5).hashCode(), OesFormat.getSymbolInstance(5).hashCode());

        assertNotEquals(OesFormat.getSymbolInstance(5), OesFormat.getSymbolInstance(4));
        assertNotEquals(OesFormat.getSymbolInstance(5).hashCode(), OesFormat.getSymbolInstance(4).hashCode());

        /* The underlying formatter is mutable, and its currency code
         * and symbol may be reset each time a number is
         * formatted or parsed.  Here we check to make sure that state is
         * ignored when comparing for equality */
        // when formatting
        OesAutoFormat a = (OesAutoFormat)OesFormat.getSymbolInstance(US);
        OesAutoFormat b = (OesAutoFormat)OesFormat.getSymbolInstance(US);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        // when parsing
        a = (OesAutoFormat)OesFormat.getSymbolInstance(US);
        b = (OesAutoFormat)OesFormat.getSymbolInstance(US);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.parseObject("mOES2");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.parseObject("µS4.35");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // FRANCE and GERMANY have different pattterns
        assertNotEquals(OesFormat.getInstance(FRANCE).hashCode(), OesFormat.getInstance(GERMANY).hashCode());
        // TAIWAN and CHINA differ only in the Locale and Currency, i.e. the patterns and symbols are
        // all the same (after setting the currency symbols to peercoins)
        assertNotEquals(OesFormat.getInstance(TAIWAN), OesFormat.getInstance(CHINA));
        // but they hash the same because of the DecimalFormatSymbols.hashCode() implementation

        assertEquals(OesFormat.getSymbolInstance(4), OesFormat.getSymbolInstance(4));
        assertEquals(OesFormat.getSymbolInstance(4).hashCode(), OesFormat.getSymbolInstance(4).hashCode());

        assertNotEquals(OesFormat.getSymbolInstance(4), OesFormat.getSymbolInstance(5));
        assertNotEquals(OesFormat.getSymbolInstance(4).hashCode(), OesFormat.getSymbolInstance(5).hashCode());

        // Fixed-denomination
        assertEquals(OesFormat.getCoinInstance(), OesFormat.getCoinInstance());
        assertEquals(OesFormat.getCoinInstance().hashCode(), OesFormat.getCoinInstance().hashCode());

        assertEquals(OesFormat.getMilliInstance(), OesFormat.getMilliInstance());
        assertEquals(OesFormat.getMilliInstance().hashCode(), OesFormat.getMilliInstance().hashCode());

        assertEquals(OesFormat.getMicroInstance(), OesFormat.getMicroInstance());
        assertEquals(OesFormat.getMicroInstance().hashCode(), OesFormat.getMicroInstance().hashCode());

        assertEquals(OesFormat.getInstance(-6), OesFormat.getInstance(-6));
        assertEquals(OesFormat.getInstance(-6).hashCode(), OesFormat.getInstance(-6).hashCode());

        assertNotEquals(OesFormat.getCoinInstance(), OesFormat.getMilliInstance());
        assertNotEquals(OesFormat.getCoinInstance().hashCode(), OesFormat.getMilliInstance().hashCode());

        assertNotEquals(OesFormat.getCoinInstance(), OesFormat.getMicroInstance());
        assertNotEquals(OesFormat.getCoinInstance().hashCode(), OesFormat.getMicroInstance().hashCode());

        assertNotEquals(OesFormat.getMilliInstance(), OesFormat.getMicroInstance());
        assertNotEquals(OesFormat.getMilliInstance().hashCode(), OesFormat.getMicroInstance().hashCode());

        assertNotEquals(OesFormat.getInstance(SMALLEST_UNIT_EXPONENT),
                        OesFormat.getInstance(SMALLEST_UNIT_EXPONENT - 1));
        assertNotEquals(OesFormat.getInstance(SMALLEST_UNIT_EXPONENT).hashCode(),
                        OesFormat.getInstance(SMALLEST_UNIT_EXPONENT - 1).hashCode());

        assertNotEquals(OesFormat.getCoinInstance(TAIWAN), OesFormat.getCoinInstance(CHINA));

        assertNotEquals(OesFormat.getCoinInstance(2,3), OesFormat.getCoinInstance(2,4));
        assertNotEquals(OesFormat.getCoinInstance(2,3).hashCode(), OesFormat.getCoinInstance(2,4).hashCode());

        assertNotEquals(OesFormat.getCoinInstance(2,3), OesFormat.getCoinInstance(2,3,3));
        assertNotEquals(OesFormat.getCoinInstance(2,3).hashCode(), OesFormat.getCoinInstance(2,3,3).hashCode());


    }

    @Test
    public void attributeTest() throws Exception {
        String codePat = OesFormat.getCodeInstance(Locale.US).pattern();
        assertTrue(codePat.contains("OES") && ! codePat.contains("(^|[^S])S([^S]|$)") && ! codePat.contains("(^|[^¤])¤([^¤]|$)"));
        String symPat = OesFormat.getSymbolInstance(Locale.US).pattern();
        assertTrue(symPat.contains("S") && !symPat.contains("OES") && !symPat.contains("¤¤"));

        assertEquals("OES #,##0.00;(OES #,##0.00)", OesFormat.getCodeInstance(Locale.US).pattern());
        assertEquals("S#,##0.00;(S#,##0.00)", OesFormat.getSymbolInstance(Locale.US).pattern());
        assertEquals('0', OesFormat.getInstance(Locale.US).symbols().getZeroDigit());
        // assertEquals('०', OesFormat.getInstance(Locale.forLanguageTag("hi-IN")).symbols().getZeroDigit());
        // TODO will this next line work with other JREs?
        assertEquals('๐', OesFormat.getInstance(new Locale("th","TH","TH")).symbols().getZeroDigit());
    }

    @Test
    public void toStringTest() {
        assertEquals("Auto-format S#,##0.00;(S#,##0.00)", OesFormat.getSymbolInstance(Locale.US).toString());
        assertEquals("Auto-format S#,##0.0000;(S#,##0.0000)", OesFormat.getSymbolInstance(Locale.US, 4).toString());
        assertEquals("Auto-format OES #,##0.00;(OES #,##0.00)", OesFormat.getCodeInstance(Locale.US).toString());
        assertEquals("Auto-format OES #,##0.0000;(OES #,##0.0000)", OesFormat.getCodeInstance(Locale.US, 4).toString());
        assertEquals("Coin-format #,##0.00", OesFormat.getCoinInstance(Locale.US).toString());
        assertEquals("Millicoin-format #,##0.00", OesFormat.getMilliInstance(Locale.US).toString());
        assertEquals("Microcoin-format #,##0.00", OesFormat.getMicroInstance(Locale.US).toString());
        assertEquals("Coin-format #,##0.000", OesFormat.getCoinInstance(Locale.US,3).toString());
        assertEquals("Coin-format #,##0.000(####)(#######)", OesFormat.getCoinInstance(Locale.US,3,4,7).toString());
        assertEquals("Kilocoin-format #,##0.000", OesFormat.getInstance(-3,Locale.US,3).toString());
        assertEquals("Kilocoin-format #,##0.000(####)(#######)", OesFormat.getInstance(-3,Locale.US,3,4,7).toString());
        assertEquals("Decicoin-format #,##0.000", OesFormat.getInstance(1,Locale.US,3).toString());
        assertEquals("Decicoin-format #,##0.000(####)(#######)", OesFormat.getInstance(1,Locale.US,3,4,7).toString());
        assertEquals("Dekacoin-format #,##0.000", OesFormat.getInstance(-1,Locale.US,3).toString());
        assertEquals("Dekacoin-format #,##0.000(####)(#######)", OesFormat.getInstance(-1,Locale.US,3,4,7).toString());
        assertEquals("Hectocoin-format #,##0.000", OesFormat.getInstance(-2,Locale.US,3).toString());
        assertEquals("Hectocoin-format #,##0.000(####)(#######)", OesFormat.getInstance(-2,Locale.US,3,4,7).toString());
        assertEquals("Megacoin-format #,##0.000", OesFormat.getInstance(-6,Locale.US,3).toString());
        assertEquals("Megacoin-format #,##0.000(####)(#######)", OesFormat.getInstance(-6,Locale.US,3,4,7).toString());
        assertEquals("Fixed (-4) format #,##0.000", OesFormat.getInstance(-4,Locale.US,3).toString());
        assertEquals("Fixed (-4) format #,##0.000(####)", OesFormat.getInstance(-4,Locale.US,3,4).toString());
        assertEquals("Fixed (-4) format #,##0.000(####)(#######)",
                     OesFormat.getInstance(-4, Locale.US, 3, 4, 7).toString());

        assertEquals("Auto-format S#,##0.00;(S#,##0.00)",
                     OesFormat.builder().style(SYMBOL).code("USD").locale(US).build().toString());
        assertEquals("Auto-format #.##0,00 $",
                     OesFormat.builder().style(SYMBOL).symbol("$").locale(GERMANY).build().toString());
        assertEquals("Auto-format #.##0,0000 $",
                     OesFormat.builder().style(SYMBOL).symbol("$").fractionDigits(4).locale(GERMANY).build().toString());
        assertEquals("Auto-format OES#,00S;OES-#,00S",
                     OesFormat.builder().style(SYMBOL).locale(GERMANY).pattern("¤¤#¤").build().toString());
        assertEquals("Coin-format OES#,00S;OES-#,00S",
                     OesFormat.builder().scale(0).locale(GERMANY).pattern("¤¤#¤").build().toString());
        assertEquals("Millicoin-format OES#.00S;OES-#.00S",
                     OesFormat.builder().scale(3).locale(US).pattern("¤¤#¤").build().toString());
    }

    @Test
    public void patternDecimalPlaces() {
        /* The pattern format provided by DecimalFormat includes specification of fractional digits,
         * but we ignore that because we have alternative mechanism for specifying that.. */
        OesFormat f = OesFormat.builder().locale(US).scale(3).pattern("¤¤ #.0").fractionDigits(3).build();
        assertEquals("Millicoin-format OES #.000;OES -#.000", f.toString());
        assertEquals("mOES 1000.000", f.format(COIN));
    }

    @Test
    public void builderTest() {
        Locale locale;
        if (Locale.getDefault().equals(GERMANY)) locale = FRANCE;
        else locale = GERMANY;

        assertEquals(OesFormat.builder().build(), OesFormat.getCoinInstance());
        try {
            OesFormat.builder().scale(0).style(CODE);
            fail("Invoking both scale() and style() on a Builder should raise exception");
        } catch (IllegalStateException e) {}
        try {
            OesFormat.builder().style(CODE).scale(0);
            fail("Invoking both style() and scale() on a Builder should raise exception");
        } catch (IllegalStateException e) {}

        OesFormat built = OesFormat.builder().style(OesAutoFormat.Style.CODE).fractionDigits(4).build();
        assertEquals(built, OesFormat.getCodeInstance(4));
        built = OesFormat.builder().style(OesAutoFormat.Style.SYMBOL).fractionDigits(4).build();
        assertEquals(built, OesFormat.getSymbolInstance(4));

        built = OesFormat.builder().scale(0).build();
        assertEquals(built, OesFormat.getCoinInstance());
        built = OesFormat.builder().scale(3).build();
        assertEquals(built, OesFormat.getMilliInstance());
        built = OesFormat.builder().scale(6).build();
        assertEquals(built, OesFormat.getMicroInstance());

        built = OesFormat.builder().locale(locale).scale(0).build();
        assertEquals(built, OesFormat.getCoinInstance(locale));
        built = OesFormat.builder().locale(locale).scale(3).build();
        assertEquals(built, OesFormat.getMilliInstance(locale));
        built = OesFormat.builder().locale(locale).scale(6).build();
        assertEquals(built, OesFormat.getMicroInstance(locale));

        built = OesFormat.builder().minimumFractionDigits(3).scale(0).build();
        assertEquals(built, OesFormat.getCoinInstance(3));
        built = OesFormat.builder().minimumFractionDigits(3).scale(3).build();
        assertEquals(built, OesFormat.getMilliInstance(3));
        built = OesFormat.builder().minimumFractionDigits(3).scale(6).build();
        assertEquals(built, OesFormat.getMicroInstance(3));

        built = OesFormat.builder().fractionGroups(3,4).scale(0).build();
        assertEquals(built, OesFormat.getCoinInstance(2,3,4));
        built = OesFormat.builder().fractionGroups(3,4).scale(3).build();
        assertEquals(built, OesFormat.getMilliInstance(2,3,4));
        built = OesFormat.builder().fractionGroups(3,4).scale(6).build();
        assertEquals(built, OesFormat.getMicroInstance(2,3,4));

        built = OesFormat.builder().pattern("#,###.#").scale(3).locale(GERMANY).build();
        assertEquals("1.000,00", built.format(COIN));
        built = OesFormat.builder().pattern("#,###.#").scale(3).locale(GERMANY).build();
        assertEquals("-1.000,00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().localizedPattern("#.###,#").scale(3).locale(GERMANY).build();
        assertEquals("1.000,00", built.format(COIN));

        built = OesFormat.builder().pattern("¤#,###.#").style(CODE).locale(GERMANY).build();
        assertEquals("S-1,00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("¤¤ #,###.#").style(SYMBOL).locale(GERMANY).build();
        assertEquals("OES -1,00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("¤¤##,###.#").scale(3).locale(US).build();
        assertEquals("mOES1,000.00", built.format(COIN));
        built = OesFormat.builder().pattern("¤ ##,###.#").scale(3).locale(US).build();
        assertEquals("₥S 1,000.00", built.format(COIN));

        try {
            OesFormat.builder().pattern("¤¤##,###.#").scale(4).locale(US).build().format(COIN);
            fail("Pattern with currency sign and non-standard denomination should raise exception");
        } catch (IllegalStateException e) {}

        try {
            OesFormat.builder().localizedPattern("¤¤##,###.#").scale(4).locale(US).build().format(COIN);
            fail("Localized pattern with currency sign and non-standard denomination should raise exception");
        } catch (IllegalStateException e) {}

        built = OesFormat.builder().style(SYMBOL).symbol("\u0053").locale(US).build();
        assertEquals("S1.00", built.format(COIN));
        built = OesFormat.builder().style(CODE).code("OES").locale(US).build();
        assertEquals("OES 1.00", built.format(COIN));
        built = OesFormat.builder().style(SYMBOL).symbol("$").locale(GERMANY).build();
        assertEquals("1,00 $", built.format(COIN));
        // Setting the currency code on a DecimalFormatSymbols object can affect the currency symbol.
        built = OesFormat.builder().style(SYMBOL).code("USD").locale(US).build();
        assertEquals("S1.00", built.format(COIN));

        built = OesFormat.builder().style(SYMBOL).symbol("\u0053").locale(US).build();
        assertEquals("₥S1.00", built.format(COIN.divide(1000)));
        built = OesFormat.builder().style(CODE).code("OES").locale(US).build();
        assertEquals("mOES 1.00", built.format(COIN.divide(1000)));

        built = OesFormat.builder().style(SYMBOL).symbol("\u0053").locale(US).build();
        assertEquals("₥S0.10", built.format(valueOf(100)));
        built = OesFormat.builder().style(CODE).code("OES").locale(US).build();
        assertEquals("mOES 0.10", built.format(valueOf(100)));

        /* The prefix of a pattern can have number symbols in quotes.
         * Make sure our custom negative-subpattern creator handles this. */
        built = OesFormat.builder().pattern("'#'¤#0").scale(0).locale(US).build();
        assertEquals("#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("'#0'¤#0").scale(0).locale(US).build();
        assertEquals("#0S-1.00", built.format(COIN.multiply(-1)));
        // this is an escaped quote between two hash marks in one set of quotes, not
        // two adjacent quote-enclosed hash-marks:
        built = OesFormat.builder().pattern("'#''#'¤#0").scale(0).locale(US).build();
        assertEquals("#'#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("'#0''#'¤#0").scale(0).locale(US).build();
        assertEquals("#0'#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("'#0#'¤#0").scale(0).locale(US).build();
        assertEquals("#0#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("'#0'E'#'¤#0").scale(0).locale(US).build();
        assertEquals("#0E#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("E'#0''#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0'#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("E'#0#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("E'#0''''#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0''#S-1.00", built.format(COIN.multiply(-1)));
        built = OesFormat.builder().pattern("''#0").scale(0).locale(US).build();
        assertEquals("'-1.00", built.format(COIN.multiply(-1)));

        // immutability check for fixed-denomination formatters, w/ & w/o custom pattern
        OesFormat a = OesFormat.builder().scale(3).build();
        OesFormat b = OesFormat.builder().scale(3).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a = OesFormat.builder().scale(3).pattern("¤#.#").build();
        b = OesFormat.builder().scale(3).pattern("¤#.#").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

    }

}


/*
 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is
 * distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either
 * express or implied. See the License for the specific language
 * governing
 * permissions and limitations under the License.
 */
package com.amazonaws.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.internal.AWS4SignerUtils;

/**
 * Unit tests for the
 */
public class AWS4SignerTest {

    public static final String PRIVATE_KEY_STRING =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAILTGAZ/twm6FLNX/WDfMRtNGfiQ\n" +
            "3mUg4f8ACerU83pdJShkVxKSL1aKjAFZtagpOTbPbiHATyTAAUEjWhkIcLIP7+Xecx0zW3pu11yO\n" +
            "qjHU++oc8TL4+zTsPXrNU2+ohvw73g5aauVKgzW5qXO0r/wMsV/Y+T9BuBNvzcxt6xNNAgMBAAEC\n" +
            "gYACoxCj6OUenBKYvthLoJQT0c0JNppU/YGwqyaIBjzWJZR/KO6cYDf2DZre2BkSR6pwkWPN6Ftw\n" +
            "CTK9RmIliQcEeG6azpK0Rtv64ur5MIWRr1R+EPmDqVaIKm1m8C+tPbgMqZwYV0QPT4n2aD2Rgshd\n" +
            "MD3gw0MGvRDU46aHp2pHoQJBALs1BjkiNl8IA+aThZuDOqQvwAcePgu/IvQnv55js1Vv0h+HN/jI\n" +
            "nM8cLWl+RJzhOSmixTV6xuYPnzk35UUYCakCQQCy5gsFVoz+heX7mUgHWnlwU74NYOIoTc0uPJzW\n" +
            "cqgX7WzXZWGl9w1z/ENha8Pbj/pjVbmeTXDIQ+JjRTnrZ6sFAkEAq8mlk0ok0KG/Spg285TBfCbh\n" +
            "NOb4krjrr1bGTOc6in0ZOZtQrngRBTDbL0ISXk+CsAVQqJmo6TaOqufy+KttOQJASFlQS7BdBPIR\n" +
            "uFadEpw8UHkNe0ubKsGMIQ7SPsfPwx15zwJpIz1ua2GxvkxewQTRNisNDkAzREpv28W7TsB2IQJA\n" +
            "TJbwFpJd8KVG759unGT9kRFRfATQTwtbCM5Xi956Qiuknj0ErO2l/nOyTktdXZgp9KhWSAKKVUzp\n" +
            "7OSTTuwJ8A==\n" +
            "-----END PRIVATE KEY-----";

    public static final String CERT_STRING =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICeDCCAeGgAwIBAgIGANyqR9onMA0GCSqGSIb3DQEBBQUAMFMxCzAJBgNVBAYT\n" +
            "AlVTMRMwEQYDVQQKEwpBbWF6b24uY29tMQwwCgYDVQQLEwNBV1MxITAfBgNVBAMT\n" +
            "GEFXUyBMaW1pdGVkLUFzc3VyYW5jZSBDQTAeFw0wOTExMDMwMjQzMjlaFw0xMDEx\n" +
            "MDMwMjQzMjlaMFMxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpBbWF6b24uY29tMRcw\n" +
            "FQYDVQQLEw5BV1MtRGV2ZWxvcGVyczEWMBQGA1UEAxMNMWtwdXp2YzN0YXNmdDCB\n" +
            "nzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAgtMYBn+3CboUs1f9YN8xG00Z+JDe\n" +
            "ZSDh/wAJ6tTzel0lKGRXEpIvVoqMAVm1qCk5Ns9uIcBPJMABQSNaGQhwsg/v5d5z\n" +
            "HTNbem7XXI6qMdT76hzxMvj7NOw9es1Tb6iG/DveDlpq5UqDNbmpc7Sv/AyxX9j5\n" +
            "P0G4E2/NzG3rE00CAwEAAaNXMFUwDgYDVR0PAQH/BAQDAgWgMBYGA1UdJQEB/wQM\n" +
            "MAoGCCsGAQUFBwMCMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFLySbsuneGFATo5c\n" +
            "jKjkvEgs8cMOMA0GCSqGSIb3DQEBBQUAA4GBAHq2LLs3Av+HNjeD5Vd70vzMJGXx\n" +
            "tuWNvmrz98CznN+MvCB7meuygDSBuaVFA5Rkz3B5ucMsO2rT/ra5l5+XeVkdCyze\n" +
            "k+UrezIvY39xtrlrr5qxjuhXpgDmOPumdTb1vfGIb0mWTdnc86f92QFSmojgzY11\n" +
            "++q4zoqVhgiJOUh9\n" +
            "-----END CERTIFICATE-----";

    private AWS4Signer signer = new AWS4Signer();

    @Test
    public void testDoubleURLEncode() {
        // Sanity-check that doubleUrlEncode is true by default.
        Assert.assertTrue(signer.doubleUrlEncode);
    }

    @Test
    public void testSigning() throws Exception {

        final String EXPECTED_AUTHORIZATION_HEADER_WITHOUT_SHA256_HEADER =
            "AWS4-HMAC-SHA256 Credential=access/19810216/us-east-1/demo/aws4_request, SignedHeaders=host;x-amz-archive-description;x-amz-date, Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        final String EXPECTED_AUTHORIZATION_HEADER_WITH_SHA256_HEADER =
            "AWS4-HMAC-SHA256 Credential=access/19810216/us-east-1/demo/aws4_request, SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-sha256, Signature=e73e20539446307a5dc71252dbd5b97e861f1d1267456abda3ebd8d57e519951";


        AWSCredentials credentials = new BasicAWSCredentials("access", "secret");
        // Test request without 'x-amz-sha256' header
        Request<?> request = generateBasicRequest();

        Calendar c = new GregorianCalendar();
        c.set(1981, 1, 16, 6, 30, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        signer.setOverrideDate(c.getTime());

        signer.sign(request, credentials);
        assertEquals(EXPECTED_AUTHORIZATION_HEADER_WITHOUT_SHA256_HEADER,
                request.getHeaders().get("Authorization"));


        // Test request with 'x-amz-sha256' header
        request = generateBasicRequest();
        request.addHeader("x-amz-sha256", "required");

        signer.sign(request, credentials);
        assertEquals(EXPECTED_AUTHORIZATION_HEADER_WITH_SHA256_HEADER,
                request.getHeaders().get("Authorization"));
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature
     */
    @Test
    public void testAnonymous() throws Exception {
        AWSCredentials credentials = new AnonymousAWSCredentials();
        Request<?> request = generateBasicRequest();

        Calendar c = new GregorianCalendar();
        c.set(1981, 1, 16, 6, 30, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        signer.setOverrideDate(c.getTime());

        signer.sign(request, credentials);

        assertNull(request.getHeaders().get("Authorization"));
    }

    private Request<?> generateBasicRequest() {
        Request<?> request = new DefaultRequest<Void>("Foo");
        request.setContent(new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()));
        request.addHeader("Host", "demo.us-east-1.amazonaws.com");
        // HTTP header containing multiple spaces in a row.
        request.addHeader("x-amz-archive-description", "test  test");
        request.setResourcePath("/");
        request.setEndpoint(URI.create("http://demo.us-east-1.amazonaws.com"));
        return request;
    }

    private String getOldTimeStamp(Date date) {
        final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
                "yyyyMMdd'T'HHmmss'Z'");
        dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return dateTimeFormat.format(date);
    }

    @Test
    public void getTimeStamp() {
        Date now = new Date();
        String timeStamp = AWS4SignerUtils.formatTimestamp(now.getTime());
        String old = getOldTimeStamp(now);
        assertEquals(old, timeStamp);
    }

    private String getOldDateStamp(Date date) {
        final SimpleDateFormat dateStampFormat = new SimpleDateFormat("yyyyMMdd");
        dateStampFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return dateStampFormat.format(date);
    }

    @Test
    public void getDateStamp() {
        Date now = new Date();
        String dateStamp = AWS4SignerUtils.formatDateStamp(now.getTime());
        String old = getOldDateStamp(now);
        assertEquals(old, dateStamp);
    }
}
